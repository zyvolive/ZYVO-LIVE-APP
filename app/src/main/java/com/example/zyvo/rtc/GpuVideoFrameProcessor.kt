package com.example.zyvo.rtc

import android.graphics.Matrix
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.TextureBufferImpl
import org.webrtc.VideoFrame
import org.webrtc.YuvConverter

/**
 * GpuVideoFrameProcessor
 *
 * Dedicated GPU video processor operating strictly on a background GL thread with an
 * isolated EGLContext sharing the WebRTC root EGL context.
 *
 * Guarantees:
 * 1. Safe context ownership: all GLES20 operations occur with an active, current EGL context.
 * 2. Non-blocking & Thread-safe: frames are never processed on CameraX or arbitrary callback threads.
 * 3. Safe lazy initialization: resources allocate only when needed and on the GL thread.
 * 4. Texture pooling / Ping-pong buffering: prevents concurrent read/write data races with downstream WebRTC renderers.
 * 5. Safe fallback: if any failure occurs during initialization or rendering, falls back to raw frame.
 */
class GpuVideoFrameProcessor(
    private val sharedContext: EglBase.Context?
) {
    private val tag = "GpuVideoProcessor"

    private val glThread = HandlerThread("ZyvoGpuFilterThread").apply { start() }
    val glHandler = Handler(glThread.looper)

    private var eglBase: EglBase? = null
    private var beautyFilter: NinetiesVignetteBeautyFilter? = null
    private var yuvConverter: YuvConverter? = null

    @Volatile
    var isInitialized = false
        private set

    @Volatile
    var isFailed = false
        private set

    private class OutputTextureSlot(
        val textureId: Int,
        val fboId: Int,
        var width: Int = 0,
        var height: Int = 0,
        var inUse: Boolean = false
    )

    private val poolSize = 2
    private val slots = arrayOfNulls<OutputTextureSlot>(poolSize)

    /**
     * Initializes EGL context and GL resources lazily on the GL thread.
     */
    private fun ensureInitialized(): Boolean {
        if (isInitialized) return true
        if (isFailed) return false

        try {
            val rootEgl = PeerConnectionFactoryProvider.eglBase
            val contextToShare = sharedContext ?: rootEgl?.eglBaseContext
            if (contextToShare == null) {
                Log.w(tag, "No root EGLBaseContext available for GPU filter")
                isFailed = true
                return false
            }

            val egl = EglBase.create(contextToShare, EglBase.CONFIG_PIXEL_BUFFER)
            egl.createDummyPbufferSurface()
            egl.makeCurrent()
            this.eglBase = egl

            this.yuvConverter = YuvConverter()
            this.beautyFilter = NinetiesVignetteBeautyFilter()
            if (!this.beautyFilter!!.compile()) {
                Log.e(tag, "Failed to compile 90s beauty filter shaders")
                isFailed = true
                return false
            }

            // Allocate texture slots
            for (i in 0 until poolSize) {
                val textures = IntArray(1)
                GLES20.glGenTextures(1, textures, 0)
                val fbos = IntArray(1)
                GLES20.glGenFramebuffers(1, fbos, 0)
                if (textures[0] != 0 && fbos[0] != 0) {
                    slots[i] = OutputTextureSlot(textureId = textures[0], fboId = fbos[0])
                }
            }

            isInitialized = true
            Log.d(tag, "Dedicated GPU EGL processor initialized successfully on ZyvoGpuFilterThread")
            return true
        } catch (t: Throwable) {
            Log.e(tag, "Fatal error initializing GPU filter EGL context", t)
            isFailed = true
            return false
        }
    }

    /**
     * Acquires an available pooled texture slot, re-allocating if dimensions change.
     */
    private fun acquireSlot(width: Int, height: Int): OutputTextureSlot? {
        val available = slots.firstOrNull { it != null && !it.inUse } ?: return null

        if (available.width != width || available.height != height) {
            available.width = width
            available.height = height

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, available.textureId)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
            )
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, available.fboId)
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, available.textureId, 0
            )

            val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(tag, "Framebuffer incomplete for slot status=$status")
                return null
            }
        }

        return available
    }

    /**
     * Dispatches frame processing asynchronously to the dedicated GL thread.
     * Guaranteed to invoke [onComplete] with either the processed frame or the original raw frame.
     */
    fun processFrameAsync(
        frame: VideoFrame,
        onComplete: (VideoFrame) -> Unit
    ) {
        val textureBuffer = frame.buffer as? VideoFrame.TextureBuffer
        if (textureBuffer == null || isFailed) {
            onComplete(frame)
            return
        }

        glHandler.post {
            try {
                if (!ensureInitialized()) {
                    onComplete(frame)
                    return@post
                }

                eglBase?.makeCurrent()

                val filter = beautyFilter
                if (filter == null) {
                    onComplete(frame)
                    return@post
                }

                val slot = acquireSlot(textureBuffer.width, textureBuffer.height)
                if (slot == null) {
                    // All slots busy downstream; deliver raw frame smoothly
                    onComplete(frame)
                    return@post
                }

                val transformFloatArray = RendererCommon.convertMatrixFromAndroidGraphicsMatrix(textureBuffer.transformMatrix)

                val outTexId = filter.processToFbo(
                    inputTextureId = textureBuffer.textureId,
                    targetFboId = slot.fboId,
                    outputTextureId = slot.textureId,
                    width = textureBuffer.width,
                    height = textureBuffer.height,
                    transformMatrix = transformFloatArray
                )

                if (outTexId == 0) {
                    onComplete(frame)
                    return@post
                }

                slot.inUse = true
                val releaseCallback = Runnable {
                    glHandler.post {
                        slot.inUse = false
                    }
                }

                // Output buffer uses identity matrix since transform matrix was already baked by vertex shader
                val processedBuffer = TextureBufferImpl(
                    textureBuffer.width,
                    textureBuffer.height,
                    VideoFrame.TextureBuffer.Type.RGB,
                    outTexId,
                    Matrix(),
                    glHandler,
                    yuvConverter,
                    releaseCallback
                )

                val processedFrame = VideoFrame(processedBuffer, frame.rotation, frame.timestampNs)
                onComplete(processedFrame)
            } catch (t: Throwable) {
                Log.e(tag, "Error processing frame on GPU thread: ${t.message}", t)
                isFailed = true
                onComplete(frame)
            }
        }
    }

    /**
     * Releases all OpenGL and EGL resources cleanly on the GL thread.
     */
    fun release() {
        glHandler.post {
            try {
                eglBase?.makeCurrent()

                for (slot in slots) {
                    slot?.let {
                        if (it.fboId != 0) {
                            GLES20.glDeleteFramebuffers(1, intArrayOf(it.fboId), 0)
                        }
                        if (it.textureId != 0) {
                            GLES20.glDeleteTextures(1, intArrayOf(it.textureId), 0)
                        }
                    }
                }

                beautyFilter?.release()
                beautyFilter = null

                yuvConverter?.release()
                yuvConverter = null

                eglBase?.release()
                eglBase = null
                Log.d(tag, "GPU video frame processor cleanly released")
            } catch (t: Throwable) {
                Log.w(tag, "Error releasing GPU video frame processor", t)
            } finally {
                glThread.quitSafely()
            }
        }
    }
}

package com.example.zyvo.rtc

import android.content.Context
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraCapturer
 * 
 * Manages real Android Camera hardware using CameraX.
 * Handles front/back camera discovery, switching, starting, stopping,
 * surface provider binding, and frame capture pipeline ready for WebRTC VideoSource.
 */
class CameraCapturer(private val context: Context) {
    private val tag = "ZYVO_CameraCapturer"

    private var cameraProvider: ProcessCameraProvider? = null
    private var currentCamera: Camera? = null
    private var previewUseCase: Preview? = null
    private var imageAnalysisUseCase: ImageAnalysis? = null
    private var analysisExecutor: ExecutorService? = null

    private var isFrontFacing: Boolean = true
    private var isCameraStarted: Boolean = false
    private var isVideoEnabled: Boolean = true

    // Callbacks for media pipeline & error reporting
    var onFrameCaptured: ((ImageProxy) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    var hasFrontCamera: Boolean = false
        private set
    var hasBackCamera: Boolean = false
        private set

    /**
     * Discovers available camera hardware and prepares ProcessCameraProvider
     */
    suspend fun initialize(): Boolean {
        return try {
            val provider = ProcessCameraProvider.getInstance(context).await()
            cameraProvider = provider
            hasFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            hasBackCamera = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            
            // If front camera is not available, default to back camera
            if (!hasFrontCamera && hasBackCamera) {
                isFrontFacing = false
            }
            
            Log.d(tag, "Camera initialized: hasFront=$hasFrontCamera, hasBack=$hasBackCamera, defaultFront=$isFrontFacing")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize CameraX ProcessCameraProvider", e)
            onError?.invoke("Failed to initialize camera: ${e.localizedMessage ?: "Unknown error"}")
            false
        }
    }

    fun isFront(): Boolean = isFrontFacing
    fun isStarted(): Boolean = isCameraStarted
    fun isVideoActive(): Boolean = isVideoEnabled

    /**
     * Starts camera preview and analysis pipeline bound to the specified LifecycleOwner.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = null
    ): Boolean {
        val provider = cameraProvider
        if (provider == null) {
            Log.e(tag, "startCamera failed: ProcessCameraProvider is null")
            onError?.invoke("Camera hardware not ready")
            return false
        }

        try {
            provider.unbindAll()

            val selector = if (isFrontFacing) {
                if (hasFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                if (hasBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            }

            // Real Camera Preview use-case
            val preview = Preview.Builder().build().also {
                if (surfaceProvider != null) {
                    it.setSurfaceProvider(surfaceProvider)
                }
            }
            previewUseCase = preview

            // Image analysis use-case (WebRTC-ready frame tap)
            if (analysisExecutor == null || analysisExecutor?.isShutdown == true) {
                analysisExecutor = Executors.newSingleThreadExecutor()
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(analysisExecutor!!) { imageProxy ->
                        try {
                            if (isVideoEnabled) {
                                onFrameCaptured?.invoke(imageProxy)
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "Error analyzing camera frame", e)
                        } finally {
                            imageProxy.close()
                        }
                    }
                }
            imageAnalysisUseCase = analysis

            currentCamera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                analysis
            )

            isCameraStarted = true
            Log.d(tag, "Camera started successfully (isFront=$isFrontFacing)")
            return true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start camera", e)
            onError?.invoke("Could not start camera: ${e.localizedMessage ?: "Camera error"}")
            isCameraStarted = false
            return false
        }
    }

    /**
     * Updates preview surface provider without unbinding the whole camera
     */
    fun setSurfaceProvider(surfaceProvider: Preview.SurfaceProvider?) {
        previewUseCase?.setSurfaceProvider(surfaceProvider)
    }

    /**
     * Switches between front and back camera
     */
    fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = null
    ): Boolean {
        if (!hasFrontCamera && !hasBackCamera) {
            onError?.invoke("No cameras available on this device")
            return false
        }
        if (hasFrontCamera && !hasBackCamera) {
            onError?.invoke("Only front camera is available")
            return false
        }
        if (!hasFrontCamera && hasBackCamera) {
            onError?.invoke("Only back camera is available")
            return false
        }

        isFrontFacing = !isFrontFacing
        Log.d(tag, "Switching camera to: ${if (isFrontFacing) "FRONT" else "BACK"}")
        return startCamera(lifecycleOwner, surfaceProvider)
    }

    /**
     * Sets video enabled state. When disabled, stops preview/capturing.
     */
    fun setVideoEnabled(
        enabled: Boolean,
        lifecycleOwner: LifecycleOwner? = null,
        surfaceProvider: Preview.SurfaceProvider? = null
    ) {
        isVideoEnabled = enabled
        if (!enabled) {
            // Disable camera hardware capture cleanly
            stopCamera()
        } else {
            // Re-enable camera hardware capture
            if (lifecycleOwner != null) {
                startCamera(lifecycleOwner, surfaceProvider)
            }
        }
    }

    /**
     * Stops the camera and unbinds from lifecycle.
     */
    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w(tag, "Error unbinding camera use-cases", e)
        }
        currentCamera = null
        previewUseCase = null
        imageAnalysisUseCase = null
        isCameraStarted = false
        Log.d(tag, "Camera stopped")
    }

    /**
     * Completely releases all camera resources.
     */
    fun release() {
        stopCamera()
        try {
            analysisExecutor?.shutdown()
            analysisExecutor = null
        } catch (e: Exception) {
            Log.w(tag, "Error shutting down executor", e)
        }
        cameraProvider = null
        Log.d(tag, "Camera resources released")
    }
}

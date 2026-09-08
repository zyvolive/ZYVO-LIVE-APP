package com.example.zyvo.rtc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

enum class MediaPermissionStatus {
    NOT_DETERMINED,
    ALL_GRANTED,
    CAMERA_DENIED,
    MIC_DENIED,
    BOTH_DENIED
}

data class LocalMediaState(
    val isCameraEnabled: Boolean = true,
    val isMicMuted: Boolean = false,
    val isFrontCamera: Boolean = true,
    val hasFrontCamera: Boolean = true,
    val hasBackCamera: Boolean = true,
    val isMediaReady: Boolean = false,
    val isCameraActive: Boolean = false,
    val isMicActive: Boolean = false,
    val isSpeaking: Boolean = false,
    val audioLevel: Float = 0f,
    val permissionStatus: MediaPermissionStatus = MediaPermissionStatus.NOT_DETERMINED,
    val mediaError: String? = null
)

/**
 * LocalMediaManager
 *
 * Coordinates CameraCapturer and MicrophoneCapturer to provide a unified,
 * lifecycle-safe local media pipeline. Exposes real-time media states and
 * hooks ready for WebRTC VideoSource & AudioSource connection in Step 3.
 */
class LocalMediaManager(private val context: Context) {
    private val tag = "ZYVO_LocalMediaManager"

    private val cameraCapturer = CameraCapturer(context)
    private val microphoneCapturer = MicrophoneCapturer(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _mediaState = MutableStateFlow(LocalMediaState())
    val mediaState: StateFlow<LocalMediaState> = _mediaState.asStateFlow()

    private var activeLifecycleOwner: LifecycleOwner? = null
    private var activeSurfaceProvider: Preview.SurfaceProvider? = null

    // Hooks ready for WebRTC (Step 3)
    var onVideoFrameCaptured: ((ImageProxy) -> Unit)? = null
    var onAudioFrameCaptured: ((ShortArray, Int) -> Unit)? = null

    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build()
        FaceDetection.getClient(options)
    }

    private val isDetectingFace = java.util.concurrent.atomic.AtomicBoolean(false)

    init {
        // Wire up camera callbacks
        cameraCapturer.onFrameCaptured = { frame ->
            try {
                processFaceDetection(frame)
            } catch (t: Throwable) {
                Log.w(tag, "Face detection error", t)
            }
            onVideoFrameCaptured?.invoke(frame)
        }
        cameraCapturer.onError = { error ->
            Log.e(tag, "Camera error: $error")
            _mediaState.update { it.copy(mediaError = error, isCameraActive = false) }
        }

        // Wire up microphone callbacks
        microphoneCapturer.onAudioLevelChanged = { level, isSpeaking ->
            _mediaState.update { current ->
                if (current.isMicMuted) {
                    current.copy(audioLevel = 0f, isSpeaking = false)
                } else {
                    current.copy(audioLevel = level, isSpeaking = isSpeaking)
                }
            }
        }
        microphoneCapturer.onPcmAudioCaptured = { data, size ->
            onAudioFrameCaptured?.invoke(data, size)
        }
        microphoneCapturer.onError = { error ->
            Log.e(tag, "Microphone error: $error")
            _mediaState.update { it.copy(mediaError = error, isMicActive = false) }
        }
    }

    /**
     * Checks permissions and initializes underlying camera/mic hardware availability
     */
    fun initialize() {
        checkPermissions()
        scope.launch {
            val cameraInitialized = cameraCapturer.initialize()
            _mediaState.update { current ->
                current.copy(
                    hasFrontCamera = cameraCapturer.hasFrontCamera,
                    hasBackCamera = cameraCapturer.hasBackCamera,
                    isFrontCamera = cameraCapturer.isFront(),
                    isMediaReady = cameraInitialized
                )
            }
        }
    }

    /**
     * Checks and updates current permission status.
     */
    fun checkPermissions(): MediaPermissionStatus {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        val status = when {
            hasCamera && hasAudio -> MediaPermissionStatus.ALL_GRANTED
            !hasCamera && !hasAudio -> MediaPermissionStatus.BOTH_DENIED
            !hasCamera -> MediaPermissionStatus.CAMERA_DENIED
            else -> MediaPermissionStatus.MIC_DENIED
        }

        _mediaState.update { it.copy(permissionStatus = status) }
        return status
    }

    /**
     * Updates permission status externally when Compose activity result fires
     */
    fun updatePermissions(cameraGranted: Boolean, micGranted: Boolean) {
        val status = when {
            cameraGranted && micGranted -> MediaPermissionStatus.ALL_GRANTED
            !cameraGranted && !micGranted -> MediaPermissionStatus.BOTH_DENIED
            !cameraGranted -> MediaPermissionStatus.CAMERA_DENIED
            else -> MediaPermissionStatus.MIC_DENIED
        }
        _mediaState.update { it.copy(permissionStatus = status, mediaError = null) }
    }

    /**
     * Starts local camera and microphone capture bound to the given LifecycleOwner and preview surface.
     */
    fun startMedia(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = null
    ) {
        activeLifecycleOwner = lifecycleOwner
        if (surfaceProvider != null) {
            activeSurfaceProvider = surfaceProvider
        }

        val permStatus = checkPermissions()
        if (permStatus != MediaPermissionStatus.ALL_GRANTED) {
            Log.w(tag, "startMedia deferred: Permissions not all granted ($permStatus)")
            return
        }

        // 1. Start Camera if enabled
        if (_mediaState.value.isCameraEnabled) {
            val cameraStarted = cameraCapturer.startCamera(lifecycleOwner, activeSurfaceProvider)
            _mediaState.update { it.copy(isCameraActive = cameraStarted) }
        }

        // 2. Start Microphone
        val micStarted = microphoneCapturer.startCapture()
        microphoneCapturer.setMuted(_mediaState.value.isMicMuted)
        _mediaState.update { it.copy(isMicActive = micStarted) }

        Log.d(tag, "startMedia complete: cameraActive=${_mediaState.value.isCameraActive}, micActive=${_mediaState.value.isMicActive}")
    }

    /**
     * Binds preview surface provider when View/Composable is ready.
     */
    fun attachPreviewSurface(surfaceProvider: Preview.SurfaceProvider, lifecycleOwner: LifecycleOwner) {
        activeSurfaceProvider = surfaceProvider
        activeLifecycleOwner = lifecycleOwner
        cameraCapturer.setSurfaceProvider(surfaceProvider)
        if (_mediaState.value.isCameraEnabled && !cameraCapturer.isStarted()) {
            val started = cameraCapturer.startCamera(lifecycleOwner, surfaceProvider)
            _mediaState.update { it.copy(isCameraActive = started) }
        }
    }

    /**
     * Detaches preview surface provider
     */
    fun detachPreviewSurface() {
        activeSurfaceProvider = null
        cameraCapturer.setSurfaceProvider(null)
    }

    /**
     * Switches between front and back camera
     */
    fun switchCamera(): Boolean {
        val owner = activeLifecycleOwner ?: return false
        val success = cameraCapturer.switchCamera(owner, activeSurfaceProvider)
        if (success) {
            _mediaState.update { it.copy(isFrontCamera = cameraCapturer.isFront()) }
        }
        return success
    }

    /**
     * Toggles camera video on / off.
     * When off, real camera hardware unbinds/pauses. When on, resumes capture.
     */
    fun toggleCamera(explicitState: Boolean? = null) {
        val targetEnabled = explicitState ?: !_mediaState.value.isCameraEnabled
        _mediaState.update { it.copy(isCameraEnabled = targetEnabled, isCameraActive = targetEnabled) }
        cameraCapturer.setVideoEnabled(targetEnabled, activeLifecycleOwner, activeSurfaceProvider)
    }

    /**
     * Toggles microphone mute / unmute.
     * Real mute disables PCM audio capture pipeline.
     */
    fun toggleMic(explicitMuted: Boolean? = null) {
        val targetMuted = explicitMuted ?: !_mediaState.value.isMicMuted
        _mediaState.update { it.copy(isMicMuted = targetMuted) }
        microphoneCapturer.setMuted(targetMuted)
    }

    /**
     * Stops camera and microphone when screen leaves foreground or user stops live.
     */
    fun stopMedia() {
        cameraCapturer.stopCamera()
        microphoneCapturer.stopCapture()
        _mediaState.update {
            it.copy(
                isCameraActive = false,
                isMicActive = false,
                isSpeaking = false,
                audioLevel = 0f
            )
        }
        Log.d(tag, "Local media stopped")
    }

    /**
     * Clears error banner
     */
    fun clearError() {
        _mediaState.update { it.copy(mediaError = null) }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processFaceDetection(imageProxy: ImageProxy) {
        if (!isDetectingFace.compareAndSet(false, true)) {
            // Drop frame for ML analysis if previous inference is still processing
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromBitmap(bitmap, rotation)
            val isRotated = rotation == 90 || rotation == 270
            val imgWidth = if (isRotated) imageProxy.height.toFloat() else imageProxy.width.toFloat()
            val imgHeight = if (isRotated) imageProxy.width.toFloat() else imageProxy.height.toFloat()
            val width = imgWidth.coerceAtLeast(1f)
            val height = imgHeight.coerceAtLeast(1f)

            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    try {
                        if (faces.isNotEmpty()) {
                            val face = faces[0]
                            val boundingBox = face.boundingBox

                            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
                            val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
                            val leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK)
                            val rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK)
                            val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
                            val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)
                            val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)

                            val mouthX = if (mouthLeft != null && mouthRight != null) {
                                (mouthLeft.position.x + mouthRight.position.x) / 2f / width
                            } else if (mouthBottom != null) {
                                mouthBottom.position.x / width
                            } else {
                                boundingBox.centerX().toFloat() / width
                            }

                            val mouthY = if (mouthLeft != null && mouthRight != null) {
                                (mouthLeft.position.y + mouthRight.position.y) / 2f / height
                            } else if (mouthBottom != null) {
                                (mouthBottom.position.y - 10f) / height
                            } else {
                                (boundingBox.centerY().toFloat() + boundingBox.height() * 0.25f) / height
                            }

                            FaceTrackerState.update(
                                FaceData(
                                    hasFace = true,
                                    centerX = (boundingBox.centerX().toFloat() / width).coerceIn(0f, 1f),
                                    centerY = (boundingBox.centerY().toFloat() / height).coerceIn(0f, 1f),
                                    boundLeft = (boundingBox.left.toFloat() / width).coerceIn(0f, 1f),
                                    boundTop = (boundingBox.top.toFloat() / height).coerceIn(0f, 1f),
                                    boundRight = (boundingBox.right.toFloat() / width).coerceIn(0f, 1f),
                                    boundBottom = (boundingBox.bottom.toFloat() / height).coerceIn(0f, 1f),
                                    leftEyeX = (leftEye?.position?.x?.div(width) ?: 0.4f).coerceIn(0f, 1f),
                                    leftEyeY = (leftEye?.position?.y?.div(height) ?: 0.4f).coerceIn(0f, 1f),
                                    rightEyeX = (rightEye?.position?.x?.div(width) ?: 0.6f).coerceIn(0f, 1f),
                                    rightEyeY = (rightEye?.position?.y?.div(height) ?: 0.4f).coerceIn(0f, 1f),
                                    mouthCenterX = mouthX.coerceIn(0f, 1f),
                                    mouthCenterY = mouthY.coerceIn(0f, 1f),
                                    leftCheekX = (leftCheek?.position?.x?.div(width) ?: 0.35f).coerceIn(0f, 1f),
                                    leftCheekY = (leftCheek?.position?.y?.div(height) ?: 0.55f).coerceIn(0f, 1f),
                                    rightCheekX = (rightCheek?.position?.x?.div(width) ?: 0.65f).coerceIn(0f, 1f),
                                    rightCheekY = (rightCheek?.position?.y?.div(height) ?: 0.55f).coerceIn(0f, 1f)
                                )
                            )
                        } else {
                            FaceTrackerState.reset()
                        }
                    } catch (t: Throwable) {
                        Log.w(tag, "Error parsing face landmarks", t)
                        FaceTrackerState.reset()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Face detection model failure", e)
                    FaceTrackerState.reset()
                }
                .addOnCompleteListener {
                    isDetectingFace.set(false)
                }
        } catch (t: Throwable) {
            Log.w(tag, "Failed to initiate face detection", t)
            isDetectingFace.set(false)
            FaceTrackerState.reset()
        }
    }

    /**
     * Releases all hardware handles, threads, and scopes cleanly.
     */
    fun release() {
        stopMedia()
        cameraCapturer.release()
        microphoneCapturer.release()
        try {
            faceDetector.close()
        } catch (e: Exception) {
            Log.w(tag, "Failed to close face detector", e)
        }
        activeLifecycleOwner = null
        activeSurfaceProvider = null
        Log.d(tag, "LocalMediaManager completely released")
    }
}

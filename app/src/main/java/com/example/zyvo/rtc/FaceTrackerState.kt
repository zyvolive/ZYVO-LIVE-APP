package com.example.zyvo.rtc

import android.graphics.PointF
import android.graphics.RectF

/**
 * Immutable snapshot of face detection tracking data.
 * Guarantees race-free reading by OpenGL rendering thread.
 */
data class FaceData(
    val hasFace: Boolean = false,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val boundLeft: Float = 0.3f,
    val boundTop: Float = 0.3f,
    val boundRight: Float = 0.7f,
    val boundBottom: Float = 0.7f,
    val leftEyeX: Float = 0.4f,
    val leftEyeY: Float = 0.4f,
    val rightEyeX: Float = 0.6f,
    val rightEyeY: Float = 0.4f,
    val mouthCenterX: Float = 0.5f,
    val mouthCenterY: Float = 0.7f,
    val leftCheekX: Float = 0.35f,
    val leftCheekY: Float = 0.55f,
    val rightCheekX: Float = 0.65f,
    val rightCheekY: Float = 0.55f
)

/**
 * FaceTrackerState
 *
 * Thread-safe singleton containing real-time normalized face landmarks and bounds.
 * Written by the background CameraX ImageAnalysis thread and read by the OpenGL rendering thread.
 * Uses atomic reference swaps of immutable [FaceData] to prevent concurrent modification exceptions.
 */
object FaceTrackerState {
    @Volatile
    var currentFace: FaceData = FaceData()
        private set

    val hasFace: Boolean
        get() = currentFace.hasFace

    val faceCenter: PointF
        get() = PointF(currentFace.centerX, currentFace.centerY)

    val leftEye: PointF
        get() = PointF(currentFace.leftEyeX, currentFace.leftEyeY)

    val rightEye: PointF
        get() = PointF(currentFace.rightEyeX, currentFace.rightEyeY)

    val leftCheek: PointF
        get() = PointF(currentFace.leftCheekX, currentFace.leftCheekY)

    val rightCheek: PointF
        get() = PointF(currentFace.rightCheekX, currentFace.rightCheekY)

    val mouthCenter: PointF
        get() = PointF(currentFace.mouthCenterX, currentFace.mouthCenterY)

    val faceBoundingBox: RectF
        get() = RectF(
            currentFace.boundLeft,
            currentFace.boundTop,
            currentFace.boundRight,
            currentFace.boundBottom
        )

    /**
     * Atomically updates the face tracking state with safe normalized coordinates.
     */
    fun update(data: FaceData) {
        currentFace = data
    }

    /**
     * Resets the face tracking state back to default center values when no face is detected.
     */
    fun reset() {
        currentFace = FaceData(hasFace = false)
    }
}

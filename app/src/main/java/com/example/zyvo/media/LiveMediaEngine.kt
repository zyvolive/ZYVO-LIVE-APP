package com.example.zyvo.media

import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.StateFlow

/**
 * Connection states for live media transport.
 */
enum class MediaConnectionState {
    IDLE,
    INITIALIZING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED
}

/**
 * Remote stream published in the active room.
 */
data class RemoteStreamInfo(
    val streamId: String,
    val userId: String,
    val userName: String = ""
)

/**
 * Clean abstraction separating UI / Firebase business logic from live media transport engines.
 */
interface LiveMediaEngine {
    val connectionState: StateFlow<MediaConnectionState>
    val isPublishing: StateFlow<Boolean>
    val isPlaying: StateFlow<Boolean>
    val remoteStreams: StateFlow<List<RemoteStreamInfo>>
    val isCameraEnabled: StateFlow<Boolean>
    val isMicMuted: StateFlow<Boolean>
    val isFrontCamera: StateFlow<Boolean>

    fun initialize(context: Context, appId: Long, appSign: String): Boolean

    fun loginRoom(
        roomId: String,
        userId: String,
        userName: String,
        isHost: Boolean,
        onComplete: (success: Boolean, errorCode: Int, errorMessage: String?) -> Unit
    )

    fun startPreview(view: View)
    fun stopPreview()

    fun startPublishing(
        streamId: String,
        onComplete: ((success: Boolean, errorCode: Int) -> Unit)? = null
    )
    fun stopPublishing()

    fun startPlaying(
        streamId: String,
        view: View,
        onComplete: ((success: Boolean, errorCode: Int) -> Unit)? = null
    )
    fun stopPlaying(streamId: String)

    fun enableCamera(enable: Boolean)
    fun muteMicrophone(mute: Boolean)
    fun switchCamera()

    fun leaveRoom(roomId: String)
    fun destroy()
}

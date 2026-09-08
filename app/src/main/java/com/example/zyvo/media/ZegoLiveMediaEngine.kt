package com.example.zyvo.media

import android.app.Application
import android.content.Context
import android.util.Log
import android.view.View
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.callback.IZegoRoomLoginCallback
import im.zego.zegoexpress.constants.ZegoPlayerState
import im.zego.zegoexpress.constants.ZegoPublisherState
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoUpdateType
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoStream
import im.zego.zegoexpress.entity.ZegoUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.ArrayList

/**
 * ZegoLiveMediaEngine
 *
 * Implements LiveMediaEngine using the official ZEGOCLOUD Express Video SDK.
 * Handles:
 * - Engine initialization with AppID and AppSign
 * - Room login / logout
 * - Host camera/mic capture and stream publishing
 * - Viewer stream playback and multi-viewer live media distribution
 * - Device controls (camera toggle, mic mute, front/back camera switch)
 * - Required ZYVO_ZEGO diagnostic logging
 */
class ZegoLiveMediaEngine private constructor() : LiveMediaEngine {

    companion object {
        private const val TAG_INIT = "ZYVO_ZEGO_INIT"
        private const val TAG_LOGIN = "ZYVO_ZEGO_LOGIN"
        private const val TAG_ROOM_JOIN = "ZYVO_ZEGO_ROOM_JOIN"
        private const val TAG_ROOM_SUCCESS = "ZYVO_ZEGO_ROOM_JOIN_SUCCESS"
        private const val TAG_ROOM_FAILED = "ZYVO_ZEGO_ROOM_JOIN_FAILED"
        private const val TAG_PUBLISH_START = "ZYVO_ZEGO_PUBLISH_START"
        private const val TAG_PUBLISH_SUCCESS = "ZYVO_ZEGO_PUBLISH_SUCCESS"
        private const val TAG_PUBLISH_FAILED = "ZYVO_ZEGO_PUBLISH_FAILED"
        private const val TAG_REMOTE_USER = "ZYVO_ZEGO_REMOTE_USER"
        private const val TAG_REMOTE_VIDEO = "ZYVO_ZEGO_REMOTE_VIDEO"
        private const val TAG_REMOTE_AUDIO = "ZYVO_ZEGO_REMOTE_AUDIO"
        private const val TAG_LEAVE = "ZYVO_ZEGO_LEAVE"
        private const val TAG_ERROR = "ZYVO_ZEGO_ERROR"

        @Volatile
        private var instance: ZegoLiveMediaEngine? = null

        fun getInstance(): ZegoLiveMediaEngine {
            return instance ?: synchronized(this) {
                instance ?: ZegoLiveMediaEngine().also { instance = it }
            }
        }
    }

    private var engine: ZegoExpressEngine? = null
    private var isInitialized = false

    private var currentRoomId: String? = null
    private var currentUserId: String? = null
    private var currentUserName: String? = null
    private var currentPublishStreamId: String? = null
    private val activePlayingStreams = mutableSetOf<String>()

    private val _connectionState = MutableStateFlow(MediaConnectionState.IDLE)
    override val connectionState: StateFlow<MediaConnectionState> = _connectionState.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    override val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _remoteStreams = MutableStateFlow<List<RemoteStreamInfo>>(emptyList())
    override val remoteStreams: StateFlow<List<RemoteStreamInfo>> = _remoteStreams.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(true)
    override val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    override val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    override val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val eventHandler = object : IZegoEventHandler() {
        override fun onRoomStateChanged(
            roomID: String,
            reason: ZegoRoomStateChangedReason,
            errorCode: Int,
            extendedData: JSONObject?
        ) {
            Log.d(TAG_ROOM_JOIN, "onRoomStateChanged: roomID=$roomID, reason=$reason, errorCode=$errorCode")
            when (reason) {
                ZegoRoomStateChangedReason.LOGINED -> {
                    Log.i(TAG_ROOM_SUCCESS, "Room login confirmed: roomID=$roomID")
                    _connectionState.value = MediaConnectionState.CONNECTED
                }
                ZegoRoomStateChangedReason.LOGIN_FAILED -> {
                    Log.e(TAG_ROOM_FAILED, "Room login failed: roomID=$roomID, errorCode=$errorCode")
                    _connectionState.value = MediaConnectionState.FAILED
                }
                ZegoRoomStateChangedReason.RECONNECTING -> {
                    Log.w(TAG_ROOM_JOIN, "Reconnecting to roomID=$roomID")
                    _connectionState.value = MediaConnectionState.CONNECTING
                }
                ZegoRoomStateChangedReason.RECONNECTED -> {
                    Log.i(TAG_ROOM_SUCCESS, "Reconnected to roomID=$roomID")
                    _connectionState.value = MediaConnectionState.CONNECTED
                }
                ZegoRoomStateChangedReason.LOGOUT,
                ZegoRoomStateChangedReason.KICK_OUT -> {
                    Log.i(TAG_LEAVE, "Logged out / kicked out from roomID=$roomID, reason=$reason")
                    _connectionState.value = MediaConnectionState.DISCONNECTED
                    _isPublishing.value = false
                    _isPlaying.value = false
                    _remoteStreams.value = emptyList()
                }
                else -> {}
            }
        }

        override fun onPublisherStateUpdate(
            streamID: String,
            state: ZegoPublisherState,
            errorCode: Int,
            extendedData: JSONObject?
        ) {
            Log.d(TAG_PUBLISH_START, "onPublisherStateUpdate: streamID=$streamID, state=$state, errorCode=$errorCode")
            when (state) {
                ZegoPublisherState.PUBLISHING -> {
                    Log.i(TAG_PUBLISH_SUCCESS, "Live publishing verified: streamID=$streamID")
                    _isPublishing.value = true
                }
                ZegoPublisherState.PUBLISH_REQUESTING -> {
                    Log.d(TAG_PUBLISH_START, "Publish requesting: streamID=$streamID")
                }
                ZegoPublisherState.NO_PUBLISH -> {
                    _isPublishing.value = false
                    if (errorCode != 0) {
                        Log.e(TAG_PUBLISH_FAILED, "Publish failed for streamID=$streamID, errorCode=$errorCode")
                    } else {
                        Log.i(TAG_PUBLISH_START, "Publish stopped for streamID=$streamID")
                    }
                }
            }
        }

        override fun onPlayerStateUpdate(
            streamID: String,
            state: ZegoPlayerState,
            errorCode: Int,
            extendedData: JSONObject?
        ) {
            Log.d(TAG_REMOTE_VIDEO, "onPlayerStateUpdate: streamID=$streamID, state=$state, errorCode=$errorCode")
            when (state) {
                ZegoPlayerState.PLAYING -> {
                    Log.i(TAG_REMOTE_VIDEO, "Playback active: streamID=$streamID")
                    _isPlaying.value = true
                }
                ZegoPlayerState.PLAY_REQUESTING -> {
                    Log.d(TAG_REMOTE_VIDEO, "Playback requesting: streamID=$streamID")
                }
                ZegoPlayerState.NO_PLAY -> {
                    activePlayingStreams.remove(streamID)
                    _isPlaying.value = activePlayingStreams.isNotEmpty()
                    if (errorCode != 0) {
                        Log.e(TAG_ERROR, "Playback failed for streamID=$streamID, errorCode=$errorCode")
                    }
                }
            }
        }

        override fun onRoomStreamUpdate(
            roomID: String,
            updateType: ZegoUpdateType,
            streamList: ArrayList<ZegoStream>,
            extendedData: JSONObject?
        ) {
            Log.d(TAG_ROOM_JOIN, "onRoomStreamUpdate: roomID=$roomID, updateType=$updateType, count=${streamList.size}")
            val currentList = _remoteStreams.value.toMutableList()

            for (stream in streamList) {
                val sId = stream.streamID
                val uId = stream.user?.userID ?: ""
                val uName = stream.user?.userName ?: ""

                if (updateType == ZegoUpdateType.ADD) {
                    Log.i(TAG_REMOTE_USER, "Remote stream added: streamID=$sId, userID=$uId, userName=$uName")
                    Log.i(TAG_REMOTE_VIDEO, "Remote video available: streamID=$sId")
                    Log.i(TAG_REMOTE_AUDIO, "Remote audio available: streamID=$sId")

                    if (currentList.none { it.streamId == sId }) {
                        currentList.add(RemoteStreamInfo(streamId = sId, userId = uId, userName = uName))
                    }
                } else if (updateType == ZegoUpdateType.DELETE) {
                    Log.i(TAG_REMOTE_USER, "Remote stream removed: streamID=$sId, userID=$uId")
                    currentList.removeAll { it.streamId == sId }
                    stopPlaying(sId)
                }
            }
            _remoteStreams.value = currentList
        }

        override fun onDebugError(errorCode: Int, funcName: String, info: String) {
            Log.e(TAG_ERROR, "ZegoExpressEngine debug error: errorCode=$errorCode, funcName=$funcName, info=$info")
        }
    }

    override fun initialize(context: Context, appId: Long, appSign: String): Boolean {
        if (isInitialized && engine != null) {
            Log.d(TAG_INIT, "ZegoExpressEngine already initialized")
            return true
        }

        if (appId <= 0L || appSign.isBlank()) {
            Log.w(
                TAG_INIT,
                "ZEGOCLOUD AppID ($appId) or AppSign is missing. Live media streaming requires valid credentials in .env (ZEGO_APP_ID, ZEGO_APP_SIGN)."
            )
            // Still report as false so the caller knows real credentials are needed
            return false
        }

        return try {
            val app = context.applicationContext as? Application
                ?: throw IllegalStateException("Context must be an Application context")

            Log.i(TAG_INIT, "Initializing ZegoExpressEngine with AppID: $appId (AppSign present)")

            val profile = ZegoEngineProfile().apply {
                this.appID = appId
                this.appSign = appSign
                this.scenario = ZegoScenario.BROADCAST
                this.application = app
            }

            engine = ZegoExpressEngine.createEngine(profile, eventHandler)
            isInitialized = (engine != null)
            if (isInitialized) {
                Log.i(TAG_INIT, "ZegoExpressEngine initialized successfully")
                _connectionState.value = MediaConnectionState.IDLE
            } else {
                Log.e(TAG_ERROR, "ZegoExpressEngine.createEngine returned null")
            }
            isInitialized
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Failed to initialize ZegoExpressEngine: ${e.message}", e)
            isInitialized = false
            false
        }
    }

    override fun loginRoom(
        roomId: String,
        userId: String,
        userName: String,
        isHost: Boolean,
        onComplete: (success: Boolean, errorCode: Int, errorMessage: String?) -> Unit
    ) {
        val eng = engine ?: run {
            Log.e(TAG_ERROR, "Cannot loginRoom: engine is null (not initialized)")
            onComplete(false, -1, "ZEGOCLOUD engine not initialized")
            return
        }

        val safeUserId = userId.ifBlank { "guest_${System.currentTimeMillis() % 10000}" }
        val safeUserName = userName.ifBlank { "User_${safeUserId.takeLast(4)}" }

        currentRoomId = roomId
        currentUserId = safeUserId
        currentUserName = safeUserName

        Log.i(
            TAG_LOGIN,
            "Firebase UID: $userId, ZEGOCLOUD userId: $safeUserId, userName: $safeUserName, roomId: $roomId, isHost: $isHost"
        )
        Log.i(TAG_ROOM_JOIN, "Attempting to join ZEGOCLOUD room: roomId=$roomId, userId=$safeUserId")

        _connectionState.value = MediaConnectionState.CONNECTING

        val zegoUser = ZegoUser(safeUserId, safeUserName)
        val roomConfig = ZegoRoomConfig().apply {
            isUserStatusNotify = true
        }

        eng.loginRoom(roomId, zegoUser, roomConfig, object : IZegoRoomLoginCallback {
            override fun onRoomLoginResult(errorCode: Int, extendedData: JSONObject?) {
                if (errorCode == 0) {
                    Log.i(TAG_ROOM_SUCCESS, "Room login successful: roomId=$roomId, userId=$safeUserId")
                    _connectionState.value = MediaConnectionState.CONNECTED
                    onComplete(true, 0, null)
                } else {
                    Log.e(TAG_ROOM_FAILED, "Room login failed: roomId=$roomId, errorCode=$errorCode")
                    _connectionState.value = MediaConnectionState.FAILED
                    onComplete(false, errorCode, "Login failed with code $errorCode")
                }
            }
        })
    }

    override fun startPreview(view: View) {
        val eng = engine ?: run {
            Log.w(TAG_ERROR, "Cannot startPreview: engine is null")
            return
        }
        try {
            Log.d(TAG_PUBLISH_START, "Starting local camera preview")
            val canvas = ZegoCanvas(view)
            eng.startPreview(canvas)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "startPreview failed: ${e.message}", e)
        }
    }

    override fun stopPreview() {
        val eng = engine ?: return
        try {
            Log.d(TAG_PUBLISH_START, "Stopping local camera preview")
            eng.stopPreview()
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "stopPreview failed: ${e.message}", e)
        }
    }

    override fun startPublishing(
        streamId: String,
        onComplete: ((success: Boolean, errorCode: Int) -> Unit)?
    ) {
        val eng = engine ?: run {
            Log.e(TAG_ERROR, "Cannot startPublishing: engine is null")
            onComplete?.invoke(false, -1)
            return
        }

        currentPublishStreamId = streamId
        Log.i(TAG_PUBLISH_START, "Starting stream publishing: streamID=$streamId")

        try {
            // Ensure hardware/software audio & video capture devices are enabled
            eng.enableCamera(_isCameraEnabled.value)
            eng.muteMicrophone(_isMicMuted.value)
            eng.startPublishingStream(streamId)
            onComplete?.invoke(true, 0)
        } catch (e: Exception) {
            Log.e(TAG_PUBLISH_FAILED, "startPublishingStream failed: ${e.message}", e)
            onComplete?.invoke(false, -1)
        }
    }

    override fun stopPublishing() {
        val eng = engine ?: return
        currentPublishStreamId?.let { sId ->
            Log.i(TAG_LEAVE, "Stopping publishing stream: $sId")
            try {
                eng.stopPublishingStream()
            } catch (e: Exception) {
                Log.e(TAG_ERROR, "stopPublishingStream failed: ${e.message}", e)
            }
        }
        currentPublishStreamId = null
        _isPublishing.value = false
    }

    override fun startPlaying(
        streamId: String,
        view: View,
        onComplete: ((success: Boolean, errorCode: Int) -> Unit)?
    ) {
        val eng = engine ?: run {
            Log.e(TAG_ERROR, "Cannot startPlaying: engine is null")
            onComplete?.invoke(false, -1)
            return
        }

        if (activePlayingStreams.contains(streamId)) {
            return
        }

        Log.i(TAG_REMOTE_VIDEO, "Starting playback for streamID=$streamId")
        try {
            val canvas = ZegoCanvas(view)
            eng.startPlayingStream(streamId, canvas)
            activePlayingStreams.add(streamId)
            _isPlaying.value = true
            onComplete?.invoke(true, 0)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "startPlayingStream failed for streamID=$streamId: ${e.message}", e)
            onComplete?.invoke(false, -1)
        }
    }

    override fun stopPlaying(streamId: String) {
        val eng = engine ?: return
        Log.i(TAG_REMOTE_VIDEO, "Stopping playback for streamID=$streamId")
        try {
            eng.stopPlayingStream(streamId)
            activePlayingStreams.remove(streamId)
            _isPlaying.value = activePlayingStreams.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "stopPlayingStream failed: ${e.message}", e)
        }
    }

    override fun enableCamera(enable: Boolean) {
        _isCameraEnabled.value = enable
        engine?.enableCamera(enable)
        Log.d(TAG_PUBLISH_START, "Camera enabled: $enable")
    }

    override fun muteMicrophone(mute: Boolean) {
        _isMicMuted.value = mute
        engine?.muteMicrophone(mute)
        Log.d(TAG_PUBLISH_START, "Microphone muted: $mute")
    }

    override fun switchCamera() {
        val next = !_isFrontCamera.value
        _isFrontCamera.value = next
        engine?.useFrontCamera(next)
        Log.d(TAG_PUBLISH_START, "Switched camera, front: $next")
    }

    override fun leaveRoom(roomId: String) {
        Log.i(TAG_LEAVE, "Leaving room: roomId=$roomId, currentUserId=$currentUserId")
        val eng = engine

        stopPublishing()
        stopPreview()

        for (streamId in activePlayingStreams.toList()) {
            stopPlaying(streamId)
        }
        activePlayingStreams.clear()

        try {
            eng?.logoutRoom(roomId)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "logoutRoom failed: ${e.message}", e)
        }

        currentRoomId = null
        _connectionState.value = MediaConnectionState.DISCONNECTED
        _isPublishing.value = false
        _isPlaying.value = false
        _remoteStreams.value = emptyList()
    }

    override fun destroy() {
        Log.i(TAG_LEAVE, "Destroying ZegoLiveMediaEngine")
        currentRoomId?.let { leaveRoom(it) }
        try {
            ZegoExpressEngine.destroyEngine(null)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "destroyEngine failed: ${e.message}", e)
        }
        engine = null
        isInitialized = false
        _connectionState.value = MediaConnectionState.IDLE
    }
}

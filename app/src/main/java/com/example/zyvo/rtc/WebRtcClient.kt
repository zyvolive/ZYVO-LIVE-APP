package com.example.zyvo.rtc

import android.content.Context
import android.util.Log
import com.example.zyvo.model.BeautifyFilter
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.*
import kotlin.coroutines.resume

/**
 * WebRtcClient
 *
 * Native Android WebRTC Client managing:
 * - Real hardware CameraVideoCapturer -> VideoSource -> VideoTrack
 * - Real Android Microphone -> AudioSource -> AudioTrack
 * - PeerConnection lifecycle, SDP negotiation (Offer/Answer), and ICE candidate exchanges
 * - Remote incoming audio/video track routing
 * - Full lifecycle disposal preventing camera and memory leaks
 */
class WebRtcClient(
    private val context: Context,
    private val rtcConfig: RtcConfig = RtcConfig()
) {
    companion object {
        private const val TAG = "ZYVO_WebRtcClient"
        const val VIDEO_TRACK_ID = "ZYVO_VIDEO_TRACK"
        const val AUDIO_TRACK_ID = "ZYVO_AUDIO_TRACK"
        const val STREAM_ID = "ZYVO_STREAM"
    }

    private val factory: PeerConnectionFactory by lazy {
        PeerConnectionFactoryProvider.getFactory(context)
    }

    // PeerConnection instance
    private var peerConnection: PeerConnection? = null

    // Video capture & tracks
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var _localVideoTrack: VideoTrack? = null
    val localVideoTrack: VideoTrack?
        get() = _localVideoTrack

    @Volatile
    private var activeFilter: BeautifyFilter = BeautifyFilter.ORIGINAL

    private var gpuProcessor: GpuVideoFrameProcessor? = null
    private val isGpuProcessingBusy = java.util.concurrent.atomic.AtomicBoolean(false)
    @Volatile
    private var isGpuFilterDisabled = false

    fun setActiveFilter(filter: BeautifyFilter) {
        activeFilter = filter
        Log.d(TAG, "WebRtcClient active filter updated to: $filter")
    }

    // Audio capture & tracks
    private var audioSource: AudioSource? = null
    private var _localAudioTrack: AudioTrack? = null
    val localAudioTrack: AudioTrack?
        get() = _localAudioTrack

    // Remote tracks
    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _remoteAudioTrack = MutableStateFlow<AudioTrack?>(null)
    val remoteAudioTrack: StateFlow<AudioTrack?> = _remoteAudioTrack.asStateFlow()

    // State flows
    private val _connectionState = MutableStateFlow(WebRtcState.NEW)
    val connectionState: StateFlow<WebRtcState> = _connectionState.asStateFlow()

    private val _iceConnectionState = MutableStateFlow("NEW")
    val iceConnectionState: StateFlow<String> = _iceConnectionState.asStateFlow()

    private val _peerConnectionState = MutableStateFlow("NEW")
    val peerConnectionState: StateFlow<String> = _peerConnectionState.asStateFlow()

    private val _iceCandidatesSentCount = MutableStateFlow(0)
    val iceCandidatesSentCount: StateFlow<Int> = _iceCandidatesSentCount.asStateFlow()

    private val _iceCandidatesReceivedCount = MutableStateFlow(0)
    val iceCandidatesReceivedCount: StateFlow<Int> = _iceCandidatesReceivedCount.asStateFlow()

    private val _pendingCandidatesCount = MutableStateFlow(0)
    val pendingCandidatesCount: StateFlow<Int> = _pendingCandidatesCount.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(true)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isMicrophoneEnabled = MutableStateFlow(true)
    val isMicrophoneEnabled: StateFlow<Boolean> = _isMicrophoneEnabled.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ICE Candidate flows & callbacks
    private val _generatedIceCandidates = MutableSharedFlow<IceCandidate>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val generatedIceCandidates: SharedFlow<IceCandidate> = _generatedIceCandidates.asSharedFlow()

    var onIceCandidateGenerated: ((IceCandidate) -> Unit)? = null

    // ICE Candidate Race Condition Queue & Deduplication
    private var isRemoteDescriptionSet = false
    private val queuedRemoteCandidates = mutableListOf<IceCandidate>()
    private val addedCandidateSignatures = mutableSetOf<String>()

    init {
        PeerConnectionFactoryProvider.initialize(context)
    }

    /**
     * Initializes the local camera capturer and creates the WebRTC local VideoTrack.
     */
    fun startLocalVideo(preferFront: Boolean = true): VideoTrack? {
        if (_localVideoTrack != null) return _localVideoTrack

        val egl = PeerConnectionFactoryProvider.eglBase ?: run {
            Log.e(TAG, "Cannot start local video: EGL context is null")
            _error.value = "EGL hardware context unavailable"
            return null
        }

        try {
            val capturer = createVideoCapturer(context, preferFront)
            if (capturer == null) {
                Log.e(TAG, "Failed to create VideoCapturer for device")
                _error.value = "Camera capturer unavailable"
                return null
            }
            videoCapturer = capturer
            _isFrontCamera.value = preferFront

            val helper = SurfaceTextureHelper.create("WebRtcCaptureThread", egl.eglBaseContext)
            surfaceTextureHelper = helper

            gpuProcessor = GpuVideoFrameProcessor(egl.eglBaseContext)
            isGpuFilterDisabled = false

            val source = factory.createVideoSource(capturer.isScreencast)
            videoSource = source

            val proxyObserver = object : CapturerObserver {
                override fun onCapturerStarted(success: Boolean) {
                    source.capturerObserver.onCapturerStarted(success)
                }

                override fun onCapturerStopped() {
                    source.capturerObserver.onCapturerStopped()
                }

                override fun onFrameCaptured(frame: VideoFrame) {
                    if (activeFilter != BeautifyFilter.VIGNETTE_90S || isGpuFilterDisabled || frame.buffer !is VideoFrame.TextureBuffer) {
                        source.capturerObserver.onFrameCaptured(frame)
                        return
                    }

                    val processor = gpuProcessor
                    if (processor == null) {
                        source.capturerObserver.onFrameCaptured(frame)
                        return
                    }

                    if (!isGpuProcessingBusy.compareAndSet(false, true)) {
                        // Previous frame still running on GPU thread, deliver raw frame smoothly
                        source.capturerObserver.onFrameCaptured(frame)
                        return
                    }

                    frame.retain()
                    processor.processFrameAsync(frame) { outputFrame ->
                        try {
                            source.capturerObserver.onFrameCaptured(outputFrame)
                        } catch (t: Throwable) {
                            Log.e(TAG, "Error delivering filtered frame to WebRTC observer", t)
                            isGpuFilterDisabled = true
                            source.capturerObserver.onFrameCaptured(frame)
                        } finally {
                            frame.release()
                            isGpuProcessingBusy.set(false)
                        }
                    }
                }
            }

            capturer.initialize(helper, context.applicationContext, proxyObserver)
            try {
                capturer.startCapture(1280, 720, 30)
            } catch (e: Exception) {
                Log.w(TAG, "Failed 720p capture, attempting 640x480", e)
                try {
                    capturer.startCapture(640, 480, 30)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed 480p capture", e2)
                }
            }

            val track = factory.createVideoTrack(VIDEO_TRACK_ID, source)
            track.setEnabled(_isCameraEnabled.value)
            _localVideoTrack = track

            peerConnection?.let { pc ->
                try {
                    pc.addTrack(track, listOf(STREAM_ID))
                    Log.d(TAG, "Attached local VideoTrack to existing PeerConnection")
                } catch (e: Exception) {
                    Log.e(TAG, "Error attaching VideoTrack to PeerConnection: ${e.message}")
                }
            }

            Log.d(TAG, "Local VideoTrack created successfully")
            return track
        } catch (e: Exception) {
            Log.e(TAG, "Error starting local video capture", e)
            _error.value = "Failed to start camera: ${e.localizedMessage}"
            return null
        }
    }

    /**
     * Initializes the local microphone and creates the WebRTC local AudioTrack.
     */
    fun startLocalAudio(): AudioTrack? {
        if (_localAudioTrack != null) return _localAudioTrack

        try {
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            }

            val source = factory.createAudioSource(audioConstraints)
            audioSource = source
            Log.d("ZYVO_AUDIO", "AUDIO_SOURCE_CREATED: WebRTC AudioSource created")

            val track = factory.createAudioTrack(AUDIO_TRACK_ID, source)
            track.setEnabled(_isMicrophoneEnabled.value)
            _localAudioTrack = track
            Log.d("ZYVO_AUDIO", "AUDIO_TRACK_CREATED: WebRTC AudioTrack created (id=$AUDIO_TRACK_ID, enabled=${_isMicrophoneEnabled.value})")

            peerConnection?.let { pc ->
                try {
                    val alreadyAdded = pc.senders.any { it.track()?.id() == AUDIO_TRACK_ID }
                    if (!alreadyAdded) {
                        val sender = pc.addTrack(track, listOf(STREAM_ID))
                        Log.d("ZYVO_AUDIO", "AUDIO_TRACK_ATTACHED / AUDIO_SENDER_CREATED: Attached AudioTrack to PeerConnection (senderId=${sender?.id()})")
                    }
                } catch (e: Exception) {
                    Log.e("ZYVO_AUDIO", "AUDIO_ERROR: Error attaching AudioTrack to PeerConnection: ${e.message}")
                }
            }

            Log.d("ZYVO_AUDIO", "Local AudioTrack created successfully")
            return track
        } catch (e: Exception) {
            Log.e("ZYVO_AUDIO", "AUDIO_ERROR: Exception in startLocalAudio", e)
            _error.value = "Failed to start microphone: ${e.localizedMessage}"
            return null
        }
    }

    /**
     * Instantiates the PeerConnection with ICE servers and attaches local tracks.
     */
    fun createPeerConnection(): PeerConnection? {
        if (peerConnection != null) return peerConnection

        try {
            val config = PeerConnection.RTCConfiguration(rtcConfig.iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                keyType = PeerConnection.KeyType.ECDSA
            }

            val pc = factory.createPeerConnection(config, peerConnectionObserver)
            if (pc == null) {
                Log.e(TAG, "PeerConnection creation returned null")
                _connectionState.value = WebRtcState.FAILED
                return null
            }
            peerConnection = pc
            _connectionState.value = WebRtcState.NEW

            // Attach local audio track if ready
            _localAudioTrack?.let { audioTrack ->
                try {
                    val alreadyAdded = pc.senders.any { it.track()?.id() == AUDIO_TRACK_ID }
                    if (!alreadyAdded) {
                        val sender = pc.addTrack(audioTrack, listOf(STREAM_ID))
                        Log.d("ZYVO_AUDIO", "AUDIO_TRACK_ATTACHED / AUDIO_SENDER_CREATED: Attached local AudioTrack to PeerConnection (senderId=${sender?.id()})")
                    }
                } catch (e: Exception) {
                    Log.e("ZYVO_AUDIO", "AUDIO_ERROR: Error attaching AudioTrack on PC creation", e)
                }
            }

            // Attach local video track if ready
            _localVideoTrack?.let { videoTrack ->
                try {
                    val alreadyAdded = pc.senders.any { it.track()?.id() == VIDEO_TRACK_ID }
                    if (!alreadyAdded) {
                        val sender = pc.addTrack(videoTrack, listOf(STREAM_ID))
                        Log.d(TAG, "Attached local VideoTrack to PeerConnection (senderId=${sender?.id()})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error attaching VideoTrack on PC creation: ${e.message}")
                }
            }

            Log.d(TAG, "PeerConnection successfully created and configured")
            return pc
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating PeerConnection", e)
            _connectionState.value = WebRtcState.FAILED
            _error.value = e.localizedMessage
            return null
        }
    }

    /**
     * Generates a WebRTC SDP Offer, sets it as LocalDescription, and returns it.
     */
    suspend fun createOffer(mediaConstraints: MediaConstraints = defaultMediaConstraints()): SessionDescription? =
        suspendCancellableCoroutine { continuation ->
            val pc = peerConnection ?: createPeerConnection() ?: run {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            _connectionState.value = WebRtcState.CONNECTING

            pc.createOffer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    if (desc != null) {
                        Log.d("ZYVO_RTC", "offer created")
                        pc.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                Log.d("ZYVO_RTC", "Local SDP set successfully (offer)")
                                if (continuation.isActive) continuation.resume(desc)
                            }

                            override fun onSetFailure(err: String?) {
                                Log.e("ZYVO_RTC", "Failed to set local SDP (offer): $err")
                                _error.value = err
                                if (continuation.isActive) continuation.resume(null)
                            }

                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, desc)
                    } else {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }

                override fun onCreateFailure(err: String?) {
                    Log.e("ZYVO_RTC", "Failed to create offer: $err")
                    _error.value = err
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onSetSuccess() {}
                override fun onSetFailure(p0: String?) {}
            }, mediaConstraints)
        }

    /**
     * Accepts a remote offer, sets RemoteDescription, creates an SDP Answer,
     * sets it as LocalDescription, and returns it.
     */
    suspend fun createAnswer(mediaConstraints: MediaConstraints = defaultMediaConstraints()): SessionDescription? =
        suspendCancellableCoroutine { continuation ->
            val pc = peerConnection ?: run {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            pc.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    if (desc != null) {
                        Log.d("ZYVO_RTC", "answer created")
                        pc.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                Log.d("ZYVO_RTC", "Local SDP set successfully (answer)")
                                if (continuation.isActive) continuation.resume(desc)
                            }

                            override fun onSetFailure(err: String?) {
                                Log.e("ZYVO_RTC", "Failed to set local SDP (answer): $err")
                                _error.value = err
                                if (continuation.isActive) continuation.resume(null)
                            }

                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, desc)
                    } else {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }

                override fun onCreateFailure(err: String?) {
                    Log.e("ZYVO_RTC", "Failed to create answer: $err")
                    _error.value = err
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onSetSuccess() {}
                override fun onSetFailure(p0: String?) {}
            }, mediaConstraints)
        }

    /**
     * Sets the remote SessionDescription (offer or answer received from peer).
     */
    suspend fun setRemoteDescription(desc: SessionDescription): Boolean =
        suspendCancellableCoroutine { continuation ->
            val pc = peerConnection ?: createPeerConnection() ?: run {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            pc.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.d("ZYVO_RTC", "Remote description successfully set: ${desc.type}")
                    synchronized(queuedRemoteCandidates) {
                        isRemoteDescriptionSet = true
                        drainQueuedCandidates(pc)
                    }
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onSetFailure(err: String?) {
                    Log.e("ZYVO_RTC", "Failed to set remote description: $err")
                    _error.value = err
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
            }, desc)
        }

    private fun drainQueuedCandidates(pc: PeerConnection) {
        if (queuedRemoteCandidates.isNotEmpty()) {
            Log.d("ZYVO_RTC", "Draining ${queuedRemoteCandidates.size} queued ICE candidates")
            for (queued in queuedRemoteCandidates) {
                val sig = "${queued.sdpMid}_${queued.sdpMLineIndex}_${queued.sdp}"
                if (addedCandidateSignatures.add(sig)) {
                    val added = pc.addIceCandidate(queued)
                    Log.d("ZYVO_RTC", "ICE candidate added from queue: $added ($sig)")
                }
            }
            queuedRemoteCandidates.clear()
            _pendingCandidatesCount.value = 0
        }
    }

    /**
     * Sets local description explicitly if needed.
     */
    suspend fun setLocalDescription(desc: SessionDescription): Boolean =
        suspendCancellableCoroutine { continuation ->
            val pc = peerConnection ?: run {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            pc.setLocalDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.d("ZYVO_RTC", "Explicit local description set: ${desc.type}")
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onSetFailure(err: String?) {
                    Log.e("ZYVO_RTC", "Failed to set explicit local description: $err")
                    _error.value = err
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
            }, desc)
        }

    /**
     * Receives and adds a remote ICE candidate to the PeerConnection.
     * Queues the candidate if remote description has not been set yet to avoid race conditions.
     */
    fun addIceCandidate(candidate: IceCandidate): Boolean {
        val sig = "${candidate.sdpMid}_${candidate.sdpMLineIndex}_${candidate.sdp}"
        synchronized(queuedRemoteCandidates) {
            _iceCandidatesReceivedCount.value += 1
            if (addedCandidateSignatures.contains(sig)) {
                Log.d("ZYVO_RTC", "Duplicate ICE candidate ignored: $sig")
                return true
            }

            if (!isRemoteDescriptionSet) {
                Log.d("ZYVO_RTC", "Remote description not set yet, queuing ICE candidate: $sig")
                queuedRemoteCandidates.add(candidate)
                _pendingCandidatesCount.value = queuedRemoteCandidates.size
                return true
            }

            val pc = peerConnection ?: return false
            addedCandidateSignatures.add(sig)
            val added = pc.addIceCandidate(candidate)
            Log.d("ZYVO_RTC", "ICE candidate added: $added ($sig)")
            return added
        }
    }

    /**
     * Switches camera between front and back lens.
     */
    fun switchCamera(onComplete: ((isFront: Boolean) -> Unit)? = null) {
        val capturer = videoCapturer as? CameraVideoCapturer ?: run {
            Log.w(TAG, "Cannot switch camera: capturer is not CameraVideoCapturer")
            return
        }

        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                _isFrontCamera.value = isFront
                Log.d(TAG, "Camera switch done: isFront=$isFront")
                onComplete?.invoke(isFront)
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                Log.e(TAG, "Camera switch error: $errorDescription")
                _error.value = errorDescription
            }
        })
    }

    /**
     * Enables or disables local camera video capture.
     */
    fun setCameraEnabled(enabled: Boolean) {
        _isCameraEnabled.value = enabled
        _localVideoTrack?.setEnabled(enabled)

        try {
            if (enabled) {
                videoCapturer?.startCapture(1280, 720, 30)
            } else {
                videoCapturer?.stopCapture()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error changing camera capture state to $enabled", e)
        }
        Log.d(TAG, "Camera video enabled set to: $enabled")
    }

    /**
     * Enables or disables local microphone audio capture (Mute/Unmute).
     */
    fun setMicrophoneEnabled(enabled: Boolean) {
        _isMicrophoneEnabled.value = enabled
        _localAudioTrack?.setEnabled(enabled)
        if (enabled) {
            Log.d("ZYVO_AUDIO", "AUDIO_UNMUTED: Microphone track enabled")
        } else {
            Log.d("ZYVO_AUDIO", "AUDIO_MUTED: Microphone track disabled")
        }
    }

    val isAudioSenderPresent: Boolean
        get() = peerConnection?.senders?.any { it.track()?.kind() == "audio" || it.track()?.id() == AUDIO_TRACK_ID } == true

    val isAudioTransceiverPresent: Boolean
        get() = peerConnection?.transceivers?.any { it.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO } == true

    val isRemoteAudioReceiverPresent: Boolean
        get() = peerConnection?.receivers?.any { it.track()?.kind() == "audio" } == true

    /**
     * Closes the active PeerConnection session and resets remote tracks.
     */
    fun close() {
        try {
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing PeerConnection", e)
        }
        _remoteVideoTrack.value = null
        _remoteAudioTrack.value = null
        _connectionState.value = WebRtcState.CLOSED
        _iceConnectionState.value = "CLOSED"
        _peerConnectionState.value = "CLOSED"
        _iceCandidatesSentCount.value = 0
        _iceCandidatesReceivedCount.value = 0
        _pendingCandidatesCount.value = 0
        synchronized(queuedRemoteCandidates) {
            isRemoteDescriptionSet = false
            queuedRemoteCandidates.clear()
            addedCandidateSignatures.clear()
        }
        Log.d("ZYVO_RTC", "PeerConnection closed")
    }

    /**
     * Fully releases all hardware, video capture, audio capture, and tracks.
     */
    fun dispose() {
        close()

        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing videoCapturer", e)
        }

        try {
            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing surfaceTextureHelper", e)
        }

        try {
            _localVideoTrack?.dispose()
            _localVideoTrack = null
            videoSource?.dispose()
            videoSource = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing video track/source", e)
        }

        try {
            gpuProcessor?.release()
            gpuProcessor = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing gpuProcessor", e)
        }

        try {
            _localAudioTrack?.dispose()
            _localAudioTrack = null
            audioSource?.dispose()
            audioSource = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing audio track/source", e)
        }

        Log.d(TAG, "WebRtcClient resources fully disposed")
    }

    // Helper: Camera VideoCapturer instantiation
    private fun createVideoCapturer(context: Context, preferFront: Boolean): VideoCapturer? {
        val enumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }

        val deviceNames = enumerator.deviceNames

        // 1. Try matching preferred facing
        for (deviceName in deviceNames) {
            if (preferFront && enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, cameraEventsHandler)
                if (capturer != null) return capturer
            } else if (!preferFront && enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, cameraEventsHandler)
                if (capturer != null) return capturer
            }
        }

        // 2. Fallback to any camera
        for (deviceName in deviceNames) {
            val capturer = enumerator.createCapturer(deviceName, cameraEventsHandler)
            if (capturer != null) return capturer
        }

        return null
    }

    private val cameraEventsHandler = object : CameraVideoCapturer.CameraEventsHandler {
        override fun onCameraError(errorDescription: String?) {
            Log.e(TAG, "Camera hardware error: $errorDescription")
            _error.value = errorDescription
        }

        override fun onCameraDisconnected() {
            Log.w(TAG, "Camera disconnected")
        }

        override fun onCameraFreezed(errorDescription: String?) {
            Log.w(TAG, "Camera preview frozen: $errorDescription")
        }

        override fun onCameraOpening(cameraName: String?) {
            Log.d(TAG, "Camera opening: $cameraName")
        }

        override fun onFirstFrameAvailable() {
            Log.d(TAG, "First camera frame ready")
        }

        override fun onCameraClosed() {
            Log.d(TAG, "Camera closed")
        }
    }

    private fun defaultMediaConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }
    }

    private fun handleIncomingTrack(track: MediaStreamTrack?) {
        when (track) {
            is VideoTrack -> {
                Log.d("ZYVO_RTC", "remote video track received: ${track.id()}")
                _remoteVideoTrack.value = track
            }
            is AudioTrack -> {
                Log.d("ZYVO_AUDIO", "REMOTE_AUDIO_TRACK_RECEIVED: Received remote AudioTrack (id=${track.id()})")
                _remoteAudioTrack.value = track
                track.setEnabled(true)
            }
            null -> {}
        }
    }

    // PeerConnection.Observer implementation
    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let {
                Log.d("ZYVO_RTC", "ICE candidate generated: ${it.sdpMid} [${it.sdpMLineIndex}]")
                _iceCandidatesSentCount.value += 1
                _generatedIceCandidates.tryEmit(it)
                onIceCandidateGenerated?.invoke(it)
            }
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
            Log.d("ZYVO_RTC", "ICE candidates removed: ${candidates?.size}")
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState?) {
            Log.d("ZYVO_RTC", "SignalingState changed: $state")
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            Log.d("ZYVO_RTC", "ICE connection state: $state")
            _iceConnectionState.value = state?.name ?: "UNKNOWN"
            when (state) {
                PeerConnection.IceConnectionState.NEW -> _connectionState.value = WebRtcState.NEW
                PeerConnection.IceConnectionState.CHECKING -> _connectionState.value = WebRtcState.CONNECTING
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED -> _connectionState.value = WebRtcState.CONNECTED
                PeerConnection.IceConnectionState.DISCONNECTED -> _connectionState.value = WebRtcState.DISCONNECTED
                PeerConnection.IceConnectionState.FAILED -> _connectionState.value = WebRtcState.FAILED
                PeerConnection.IceConnectionState.CLOSED -> _connectionState.value = WebRtcState.CLOSED
                null -> {}
            }
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {}

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
            Log.d("ZYVO_RTC", "IceGatheringState: $state")
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            Log.d("ZYVO_RTC", "PeerConnection state: $newState")
            _peerConnectionState.value = newState?.name ?: "UNKNOWN"
            when (newState) {
                PeerConnection.PeerConnectionState.NEW -> _connectionState.value = WebRtcState.NEW
                PeerConnection.PeerConnectionState.CONNECTING -> _connectionState.value = WebRtcState.CONNECTING
                PeerConnection.PeerConnectionState.CONNECTED -> _connectionState.value = WebRtcState.CONNECTED
                PeerConnection.PeerConnectionState.DISCONNECTED -> _connectionState.value = WebRtcState.DISCONNECTED
                PeerConnection.PeerConnectionState.FAILED -> _connectionState.value = WebRtcState.FAILED
                PeerConnection.PeerConnectionState.CLOSED -> _connectionState.value = WebRtcState.CLOSED
                null -> {}
            }
        }

        override fun onTrack(transceiver: RtpTransceiver?) {
            val track = transceiver?.receiver?.track()
            handleIncomingTrack(track)
        }

        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            handleIncomingTrack(receiver?.track())
        }

        override fun onAddStream(stream: MediaStream?) {
            Log.d(TAG, "Remote MediaStream added: ${stream?.id}")
            stream?.videoTracks?.firstOrNull()?.let { track ->
                _remoteVideoTrack.value = track
            }
            stream?.audioTracks?.firstOrNull()?.let { track ->
                _remoteAudioTrack.value = track
                track.setEnabled(true)
            }
        }

        override fun onRemoveStream(stream: MediaStream?) {
            Log.d(TAG, "Remote MediaStream removed: ${stream?.id}")
            if (stream?.videoTracks?.contains(_remoteVideoTrack.value) == true) {
                _remoteVideoTrack.value = null
            }
            if (stream?.audioTracks?.contains(_remoteAudioTrack.value) == true) {
                _remoteAudioTrack.value = null
            }
        }

        override fun onDataChannel(dataChannel: DataChannel?) {}

        override fun onRenegotiationNeeded() {
            Log.d(TAG, "WebRTC Renegotiation needed")
        }
    }
}

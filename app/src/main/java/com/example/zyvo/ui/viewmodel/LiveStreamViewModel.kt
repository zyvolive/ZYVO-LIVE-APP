package com.example.zyvo.ui.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.zyvo.auth.AuthState
import com.example.zyvo.data.ZyvoRepository
import com.example.zyvo.data.firebase.FirestoreSignalingRepository
import com.example.zyvo.data.model.IceCandidateModel
import com.example.zyvo.data.model.SessionDescriptionModel
import com.example.zyvo.data.model.User
import com.example.zyvo.model.*
import com.example.zyvo.rtc.AudioRouteManager
import com.example.zyvo.rtc.LocalMediaManager
import com.example.zyvo.rtc.LocalMediaState
import com.example.zyvo.rtc.MediaPermissionStatus
import com.example.zyvo.rtc.WebRtcClient
import com.example.zyvo.rtc.WebRtcState
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.util.UUID
import com.example.zyvo.BuildConfig
import com.example.zyvo.media.LiveMediaEngine
import com.example.zyvo.media.MediaConnectionState
import com.example.zyvo.media.RemoteStreamInfo
import com.example.zyvo.media.ZegoLiveMediaEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LiveStreamViewModel(
    private val repository: ZyvoRepository = ZyvoRepository(),
    val signalingRepository: FirestoreSignalingRepository = FirestoreSignalingRepository()
) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.authRepository.authState
    val authCurrentUser: StateFlow<User?> = repository.authRepository.currentUser

    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    Log.d("ZYVO_DISCOVERY", "User authenticated: ${state.user.uid}. Starting/refreshing active rooms observation")
                    repository.startActiveRoomsObservation(signalingRepository)
                }
            }
        }
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
                if (auth.currentUser != null) {
                    Log.d("ZYVO_DISCOVERY", "FirebaseAuth listener: user logged in (${auth.currentUser?.uid}). Restarting active rooms observation.")
                    repository.startActiveRoomsObservation(signalingRepository)
                }
            }
        } catch (e: Exception) {
            Log.w("ZYVO_DISCOVERY", "Could not attach FirebaseAuth listener: ${e.message}")
        }
    }

    fun refreshRoomsObservation() {
        Log.d("ZYVO_DISCOVERY", "refreshRoomsObservation called: refreshing active rooms listener")
        repository.startActiveRoomsObservation(signalingRepository)
    }

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn

    val currentUserIdentity: String
        get() = repository.currentUserIdentity

    val currentUserName: String
        get() = repository.currentUserName

    val currentUserAvatar: String
        get() = repository.currentUserAvatar

    // ZEGOCLOUD Live Media Engine
    val zegoMediaEngine: LiveMediaEngine = ZegoLiveMediaEngine.getInstance()
    val mediaConnectionState: StateFlow<MediaConnectionState> = zegoMediaEngine.connectionState
    val isPublishing: StateFlow<Boolean> = zegoMediaEngine.isPublishing
    val isPlaying: StateFlow<Boolean> = zegoMediaEngine.isPlaying
    val remoteStreams: StateFlow<List<RemoteStreamInfo>> = zegoMediaEngine.remoteStreams

    // Local Media Manager (Real Camera + Real Microphone)
    private var _localMediaManager: LocalMediaManager? = null
    val localMediaManager: LocalMediaManager?
        get() = _localMediaManager

    private val _localMediaState = MutableStateFlow(LocalMediaState())
    val localMediaState: StateFlow<LocalMediaState> = _localMediaState.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(true)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isMicrophoneEnabled = MutableStateFlow(true)
    val isMicrophoneEnabled: StateFlow<Boolean> = _isMicrophoneEnabled.asStateFlow()

    private val _cameraFacing = MutableStateFlow(true) // true = front, false = back
    val cameraFacing: StateFlow<Boolean> = _cameraFacing.asStateFlow()

    val isMicMuted: StateFlow<Boolean> = _isMicrophoneEnabled.map { !it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isVideoMuted: StateFlow<Boolean> = _isCameraEnabled.map { !it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isFrontCamera: StateFlow<Boolean> = _cameraFacing
    val isSpeaking: StateFlow<Boolean> = _localMediaState.map { it.isSpeaking }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val mediaReady: StateFlow<Boolean> = _localMediaState.map { it.isMediaReady }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val permissionState: StateFlow<MediaPermissionStatus> = _localMediaState.map { it.permissionStatus }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MediaPermissionStatus.NOT_DETERMINED)
    val mediaError: StateFlow<String?> = _localMediaState.map { it.mediaError }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Native WebRTC Engine
    private var _webRtcClient: WebRtcClient? = null
    val webRtcClient: WebRtcClient?
        get() = _webRtcClient

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _remoteAudioTrack = MutableStateFlow<AudioTrack?>(null)
    val remoteAudioTrack: StateFlow<AudioTrack?> = _remoteAudioTrack.asStateFlow()

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

    private val _webRtcError = MutableStateFlow<String?>(null)
    val webRtcError: StateFlow<String?> = _webRtcError.asStateFlow()

    private var hostSignalingJob: Job? = null
    private var viewerSignalingJob: Job? = null
    private var heartbeatJob: Job? = null

    private val _signalingStatus = MutableStateFlow("IDLE")
    val signalingStatus: StateFlow<String> = _signalingStatus.asStateFlow()

    private var appContext: android.content.Context? = null
    private var audioRouteManager: AudioRouteManager? = null

    val localAudioTrack: AudioTrack?
        get() = _webRtcClient?.localAudioTrack

    val isAudioSenderPresent: Boolean
        get() = _webRtcClient?.isAudioSenderPresent ?: false

    val isAudioTransceiverPresent: Boolean
        get() = _webRtcClient?.isAudioTransceiverPresent ?: false

    val isRemoteAudioReceiverPresent: Boolean
        get() = _webRtcClient?.isRemoteAudioReceiverPresent ?: false

    fun initPersistence(context: android.content.Context) {
        appContext = context.applicationContext
        repository.initPersistence(context)
        repository.startActiveRoomsObservation(signalingRepository)
        zegoMediaEngine.initialize(context.applicationContext, BuildConfig.ZEGO_APP_ID, BuildConfig.ZEGO_APP_SIGN)
        initMedia(context)
        initWebRtc(context)
    }

    fun initMedia(context: android.content.Context) {
        if (_localMediaManager == null) {
            val manager = LocalMediaManager(context.applicationContext)
            _localMediaManager = manager
            manager.initialize()
            viewModelScope.launch {
                manager.mediaState.collect { state ->
                    _localMediaState.value = state
                }
            }
        }
    }

    fun initWebRtc(context: android.content.Context) {
        appContext = context.applicationContext
        if (audioRouteManager == null) {
            audioRouteManager = AudioRouteManager(context.applicationContext)
        }
        if (_webRtcClient == null) {
            val client = WebRtcClient(context.applicationContext)
            _webRtcClient = client

            viewModelScope.launch {
                client.remoteVideoTrack.collect { track ->
                    _remoteVideoTrack.value = track
                }
            }
            viewModelScope.launch {
                client.remoteAudioTrack.collect { track ->
                    _remoteAudioTrack.value = track
                }
            }
            viewModelScope.launch {
                client.connectionState.collect { state ->
                    _connectionState.value = state
                }
            }
            viewModelScope.launch {
                client.iceConnectionState.collect { state ->
                    _iceConnectionState.value = state
                }
            }
            viewModelScope.launch {
                client.peerConnectionState.collect { state ->
                    _peerConnectionState.value = state
                }
            }
            viewModelScope.launch {
                client.iceCandidatesSentCount.collect { count ->
                    _iceCandidatesSentCount.value = count
                }
            }
            viewModelScope.launch {
                client.iceCandidatesReceivedCount.collect { count ->
                    _iceCandidatesReceivedCount.value = count
                }
            }
            viewModelScope.launch {
                client.pendingCandidatesCount.collect { count ->
                    _pendingCandidatesCount.value = count
                }
            }
            viewModelScope.launch {
                client.isFrontCamera.collect { isFront ->
                    _cameraFacing.value = isFront
                }
            }
            viewModelScope.launch {
                client.isCameraEnabled.collect { enabled ->
                    _isCameraEnabled.value = enabled
                }
            }
            viewModelScope.launch {
                client.isMicrophoneEnabled.collect { enabled ->
                    _isMicrophoneEnabled.value = enabled
                }
            }
            viewModelScope.launch {
                client.error.collect { err ->
                    _webRtcError.value = err
                }
            }
            viewModelScope.launch {
                repository.currentFilter.collect { filter ->
                    client.setActiveFilter(filter)
                }
            }
        }
    }

    private fun getOrCreateWebRtcClient(): WebRtcClient? {
        if (_webRtcClient == null) {
            val ctx = appContext
            if (ctx != null) {
                initWebRtc(ctx)
            }
        }
        return _webRtcClient
    }

    fun startWebRtcMedia(preferFront: Boolean = true) {
        val client = _webRtcClient ?: return
        val vTrack = client.startLocalVideo(preferFront)
        client.startLocalAudio()
        _localVideoTrack.value = vTrack
        client.createPeerConnection()
    }

    suspend fun createOffer(): SessionDescription? {
        return _webRtcClient?.createOffer()
    }

    suspend fun createAnswer(): SessionDescription? {
        return _webRtcClient?.createAnswer()
    }

    suspend fun setRemoteDescription(desc: SessionDescription): Boolean {
        return _webRtcClient?.setRemoteDescription(desc) ?: false
    }

    fun addIceCandidate(candidate: IceCandidate): Boolean {
        return _webRtcClient?.addIceCandidate(candidate) ?: false
    }

    fun cleanupWebRtc() {
        audioRouteManager?.stopLiveAudioSession()
        _webRtcClient?.dispose()
        _webRtcClient = null
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        _remoteAudioTrack.value = null
        _connectionState.value = WebRtcState.CLOSED
        _iceConnectionState.value = "CLOSED"
        _peerConnectionState.value = "CLOSED"
        _iceCandidatesSentCount.value = 0
        _iceCandidatesReceivedCount.value = 0
        _pendingCandidatesCount.value = 0
    }

    fun startHostSession(roomId: String) {
        audioRouteManager?.startLiveAudioSession()
        hostSignalingJob?.cancel()
        viewerSignalingJob?.cancel()
        heartbeatJob?.cancel()
        _signalingStatus.value = "HOST_CONNECTING"

        val ctx = appContext
        if (ctx != null) {
            zegoMediaEngine.initialize(ctx, BuildConfig.ZEGO_APP_ID, BuildConfig.ZEGO_APP_SIGN)
        }

        val currentUser = authCurrentUser.value
        val hostUid = currentUser?.uid ?: currentUserIdentity
        val hostName = currentUser?.displayName ?: currentUserName
        val streamId = "${roomId}_stream"

        Log.d("ZYVO_ROOM", "Host starting live session: roomId=$roomId, hostUid=$hostUid, streamId=$streamId")

        // 0. Start host room heartbeat in Firestore
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                signalingRepository.sendRoomHeartbeat(roomId)
                delay(30_000L)
            }
        }

        // 1. ZEGOCLOUD: Login host to room and publish real live camera & microphone
        zegoMediaEngine.enableCamera(_isCameraEnabled.value)
        zegoMediaEngine.muteMicrophone(!_isMicrophoneEnabled.value)

        zegoMediaEngine.loginRoom(
            roomId = roomId,
            userId = hostUid,
            userName = hostName,
            isHost = true
        ) { loginSuccess, errorCode, errorMessage ->
            if (loginSuccess) {
                Log.i("ZYVO_ZEGO_ROOM_JOIN_SUCCESS", "Host joined Zego room: $roomId")
                zegoMediaEngine.startPublishing(streamId) { pubSuccess, pubCode ->
                    if (pubSuccess) {
                        Log.i("ZYVO_ZEGO_PUBLISH_SUCCESS", "Host publishing live media stream $streamId active")
                        _signalingStatus.value = "HOST_LIVE"
                        viewModelScope.launch {
                            signalingRepository.updateRoomStatus(roomId, FirestoreSignalingRepository.STATUS_LIVE)
                        }
                    } else {
                        Log.e("ZYVO_ZEGO_PUBLISH_FAILED", "Host failed to publish stream $streamId: code $pubCode")
                        _signalingStatus.value = "PUBLISH_FAILED"
                    }
                }
            } else {
                Log.e("ZYVO_ZEGO_ROOM_JOIN_FAILED", "Host room login failed: $errorMessage (code $errorCode)")
                _signalingStatus.value = "ROOM_JOIN_FAILED"
            }
        }

        // WebRTC fallback local video track for preview compatibility
        hostSignalingJob = viewModelScope.launch {
            val client = getOrCreateWebRtcClient() ?: return@launch
            val vTrack = client.startLocalVideo(preferFront = _cameraFacing.value)
            client.startLocalAudio()
            _localVideoTrack.value = vTrack
        }
    }

    fun startViewerSession(roomId: String) {
        audioRouteManager?.startLiveAudioSession()
        hostSignalingJob?.cancel()
        viewerSignalingJob?.cancel()
        heartbeatJob?.cancel()
        _signalingStatus.value = "VIEWER_CONNECTING"

        val ctx = appContext
        if (ctx != null) {
            zegoMediaEngine.initialize(ctx, BuildConfig.ZEGO_APP_ID, BuildConfig.ZEGO_APP_SIGN)
        }

        val currentUser = authCurrentUser.value
        val viewerUid = currentUser?.uid ?: currentUserIdentity
        val viewerName = currentUser?.displayName ?: currentUserName

        Log.d("ZYVO_ROOM", "Viewer joining live session: roomId=$roomId, viewerUid=$viewerUid")

        viewerSignalingJob = viewModelScope.launch {
            // Validate room is LIVE in Firestore
            val roomDoc = signalingRepository.getLiveRoom(roomId)
            if (roomDoc != null && (!roomDoc.isLive || roomDoc.status == FirestoreSignalingRepository.STATUS_ENDED)) {
                Log.w("ZYVO_ROOM", "Room $roomId is no longer LIVE. Aborting viewer session.")
                _signalingStatus.value = "ROOM_ENDED"
                return@launch
            }

            // Track viewer presence & count in Firestore
            val profile = currentUserProfile.value
            signalingRepository.addParticipant(roomId, viewerUid, profile.displayName, profile.avatarEmoji)
            signalingRepository.incrementViewerCount(roomId)

            // ZEGOCLOUD: Login viewer to room
            zegoMediaEngine.loginRoom(
                roomId = roomId,
                userId = viewerUid,
                userName = viewerName,
                isHost = false
            ) { loginSuccess, errorCode, errorMessage ->
                if (loginSuccess) {
                    Log.i("ZYVO_ZEGO_ROOM_JOIN_SUCCESS", "Viewer joined Zego room: $roomId")
                    _signalingStatus.value = "VIEWER_CONNECTED"
                } else {
                    Log.e("ZYVO_ZEGO_ROOM_JOIN_FAILED", "Viewer failed to join Zego room $roomId: $errorMessage (code $errorCode)")
                    _signalingStatus.value = "ROOM_JOIN_FAILED"
                }
            }

            // Observe room status for host ending live
            launch {
                signalingRepository.observeRoom(roomId).collect { updatedRoom ->
                    if (updatedRoom != null && (!updatedRoom.isLive || updatedRoom.status == FirestoreSignalingRepository.STATUS_ENDED)) {
                        Log.d("ZYVO_ROOM", "Room ended by host in Firestore: $roomId")
                        _signalingStatus.value = "ROOM_ENDED"
                    }
                }
            }
        }
    }

    fun leaveRoomSession(isHost: Boolean) {
        val room = currentRoom.value
        val roomId = room?.id
        val currentUid = authCurrentUser.value?.uid
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: currentUserIdentity

        Log.d(FirestoreSignalingRepository.TAG_ROOM_END, "leaveRoomSession: roomId=${roomId ?: "none"}, isHost=$isHost, uid=$currentUid")
        Log.d("ZYVO_SIGNALING", "signaling listeners removed")

        hostSignalingJob?.cancel()
        hostSignalingJob = null
        viewerSignalingJob?.cancel()
        viewerSignalingJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        _signalingStatus.value = "IDLE"

        if (roomId != null) {
            zegoMediaEngine.leaveRoom(roomId)
            viewModelScope.launch {
                if (isHost) {
                    signalingRepository.endLiveRoom(roomId)
                } else {
                    signalingRepository.removeParticipant(roomId, currentUid)
                    signalingRepository.decrementViewerCount(roomId)
                }
            }
        }

        stopLocalMedia()
        cleanupWebRtc()
        repository.leaveRoom()
    }

    fun startLocalMedia(lifecycleOwner: LifecycleOwner) {
        _localMediaManager?.startMedia(lifecycleOwner)
    }

    fun stopLocalMedia() {
        _localMediaManager?.stopMedia()
    }

    fun updatePermissions(cameraGranted: Boolean, micGranted: Boolean) {
        _localMediaManager?.updatePermissions(cameraGranted, micGranted)
    }

    fun clearMediaError() {
        _localMediaManager?.clearError()
    }

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authErrorMessage.value = "Please provide both email and password."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = repository.authRepository.signInWithEmail(email.trim(), password)
            _isAuthLoading.value = false
            result.onFailure {
                _authErrorMessage.value = it.message ?: "Sign in failed"
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        username: String,
        avatar: String = "👑",
        bio: String = "Official ZYVO Broadcaster & Creator 🎙️ Live on ZYVO!",
        gender: String = "Unspecified",
        location: String = "Global HQ 🌍"
    ) {
        if (email.isBlank() || password.isBlank()) {
            _authErrorMessage.value = "Email and password are required."
            return
        }
        if (password.length < 6) {
            _authErrorMessage.value = "Password must be at least 6 characters."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = repository.authRepository.signUpWithEmail(
                email = email.trim(),
                password = password,
                displayName = displayName.ifBlank { "ZYVO Broadcaster" },
                username = username.ifBlank { displayName.lowercase().replace(" ", "_") },
                avatar = avatar
            )
            _isAuthLoading.value = false
            result.onFailure {
                _authErrorMessage.value = it.message ?: "Sign up failed"
            }
        }
    }

    fun signInAnonymously(
        displayName: String = "ZYVO Broadcaster",
        username: String = "broadcaster",
        avatar: String = "🚀"
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = repository.authRepository.signInAnonymously(
                displayName = displayName,
                username = username,
                avatar = avatar
            )
            _isAuthLoading.value = false
            result.onFailure {
                _authErrorMessage.value = it.message ?: "Quick sign-in failed"
            }
        }
    }

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    fun loginWithGoogle(
        idToken: String? = null,
        displayName: String,
        email: String,
        avatarEmoji: String,
        avatarUrl: String? = null
    ) {
        repository.loginWithGoogle(idToken, displayName, email, avatarEmoji, avatarUrl)
    }

    fun createCustomProfileAndLogin(
        displayName: String,
        username: String,
        email: String,
        avatarEmoji: String,
        avatarUrl: String? = null,
        bio: String = "Official ZYVO Broadcaster & Creator 🎙️ Live on ZYVO!",
        gender: String = "Unspecified",
        location: String = "Global HQ 🌍",
        password: String? = null
    ) {
        repository.createCustomProfileAndLogin(displayName, username, email, avatarEmoji, avatarUrl, bio, gender, location, password)
    }

    fun logout() {
        repository.logout()
    }

    val currentUserProfile: StateFlow<UserProfile> = repository.currentUserProfile
    val userProfiles: StateFlow<Map<String, UserProfile>> = repository.userProfiles
    val userCoinBalance: StateFlow<Int> = repository.userCoinBalance
    val userBeansBalance: StateFlow<Int> = repository.userBeansBalance
    val walletTransactions: StateFlow<List<WalletTransaction>> = repository.walletTransactions

    val followingUserIds: StateFlow<Set<String>> = repository.followingUserIds
    val blockedUserIds: StateFlow<Set<String>> = repository.blockedUserIds
    val conversations: StateFlow<List<ConversationSummary>> = repository.conversations
    val directMessages: StateFlow<Map<String, List<DirectMessage>>> = repository.directMessages

    val rooms: StateFlow<List<LiveRoom>> = repository.rooms
    val currentRoom: StateFlow<LiveRoom?> = repository.currentRoom
    val activeFloatingGifts: StateFlow<List<ChatMessage>> = repository.activeFloatingGifts
    val currentFilter: StateFlow<BeautifyFilter> = repository.currentFilter
    val lastPlayedSfx: StateFlow<String?> = repository.lastPlayedSfx

    // Navigation Tab: 0=Explore, 1=Following, 2=Rankings, 3=Profile/Wallet, 4=Analytics
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow<RoomType?>(null) // null = ALL
    val selectedCategory: StateFlow<RoomType?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dialog & Sheet States
    private val _selectedUserProfile = MutableStateFlow<UserProfile?>(null)
    val selectedUserProfile: StateFlow<UserProfile?> = _selectedUserProfile.asStateFlow()

    private val _showVipStoreDialog = MutableStateFlow(false)
    val showVipStoreDialog: StateFlow<Boolean> = _showVipStoreDialog.asStateFlow()

    private val _showRechargeDialog = MutableStateFlow(false)
    val showRechargeDialog: StateFlow<Boolean> = _showRechargeDialog.asStateFlow()

    private val _showWithdrawalDialog = MutableStateFlow(false)
    val showWithdrawalDialog: StateFlow<Boolean> = _showWithdrawalDialog.asStateFlow()

    private val _showTransactionsDialog = MutableStateFlow(false)
    val showTransactionsDialog: StateFlow<Boolean> = _showTransactionsDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _activeDmPeerUserId = MutableStateFlow<String?>(null)
    val activeDmPeerUserId: StateFlow<String?> = _activeDmPeerUserId.asStateFlow()

    private val _showGiftDialog = MutableStateFlow(false)
    val showGiftDialog: StateFlow<Boolean> = _showGiftDialog.asStateFlow()

    private val _showParticipantsSheet = MutableStateFlow(false)
    val showParticipantsSheet: StateFlow<Boolean> = _showParticipantsSheet.asStateFlow()

    private val _showFilterSheet = MutableStateFlow(false)
    val showFilterSheet: StateFlow<Boolean> = _showFilterSheet.asStateFlow()

    private val _showSoundboard = MutableStateFlow(false)
    val showSoundboard: StateFlow<Boolean> = _showSoundboard.asStateFlow()

    private val _showStreamStats = MutableStateFlow(false)
    val showStreamStats: StateFlow<Boolean> = _showStreamStats.asStateFlow()

    private val _showCreateRoomSheet = MutableStateFlow(false)
    val showCreateRoomSheet: StateFlow<Boolean> = _showCreateRoomSheet.asStateFlow()

    private val _showRoomCoverSheet = MutableStateFlow(false)
    val showRoomCoverSheet: StateFlow<Boolean> = _showRoomCoverSheet.asStateFlow()

    // Filtered rooms
    val filteredRooms: StateFlow<List<LiveRoom>> = combine(
        rooms,
        _selectedCategory,
        _searchQuery
    ) { allRooms, category, query ->
        allRooms.filter { room ->
            val matchesCategory = category == null || room.roomType == category
            val matchesQuery = query.isBlank() ||
                    room.title.contains(query, ignoreCase = true) ||
                    room.hostName.contains(query, ignoreCase = true) ||
                    room.tags.any { it.contains(query, ignoreCase = true) }
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Followed Rooms
    val followedRooms: StateFlow<List<LiveRoom>> = combine(
        rooms,
        followingUserIds
    ) { allRooms, following ->
        allRooms.filter { following.contains(it.creatorIdentity) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat messages for current room
    val currentRoomChat: StateFlow<List<ChatMessage>> = combine(
        currentRoom,
        repository.chatMessages
    ) { room, chatMap ->
        if (room == null) emptyList()
        else chatMap[room.id] ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Participants for current room
    val currentRoomParticipants: StateFlow<List<Participant>> = combine(
        currentRoom,
        repository.participants
    ) { room, partMap ->
        if (room == null) emptyList()
        else partMap[room.id] ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSelectedCategory(category: RoomType?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // User Profile Actions
    fun openUserProfile(userId: String) {
        val profile = repository.getUserProfile(userId)
        _selectedUserProfile.value = profile
    }

    fun closeUserProfile() {
        _selectedUserProfile.value = null
    }

    fun followUser(userId: String) {
        repository.followUser(userId)
        if (_selectedUserProfile.value?.userId == userId) {
            _selectedUserProfile.value = repository.getUserProfile(userId)
        }
    }

    fun unfollowUser(userId: String) {
        repository.unfollowUser(userId)
        if (_selectedUserProfile.value?.userId == userId) {
            _selectedUserProfile.value = repository.getUserProfile(userId)
        }
    }

    fun blockUser(userId: String) {
        repository.blockUser(userId)
        closeUserProfile()
    }

    fun unblockUser(userId: String) {
        repository.unblockUser(userId)
    }

    fun reportUser(userId: String, reason: String) {
        repository.reportUser(userId, reason)
    }

    fun updateProfile(
        displayName: String,
        bio: String,
        gender: String,
        location: String,
        avatarEmoji: String,
        username: String? = null,
        avatarUrl: String? = null,
        coverGradientIndex: Int? = null
    ) {
        repository.updateProfile(displayName, bio, gender, location, avatarEmoji, username, avatarUrl, coverGradientIndex)
    }

    // Wallet & Membership
    fun rechargeCoins(packageTitle: String, coinAmount: Int, bonusCoins: Int, usdPrice: Double, paymentMethod: String) {
        repository.rechargeCoins(packageTitle, coinAmount, bonusCoins, usdPrice, paymentMethod)
        _showRechargeDialog.value = false
    }

    fun buyVipPackage(tier: VipTier, months: Int, coinCost: Int) {
        repository.buyVipPackage(tier, months, coinCost)
        _showVipStoreDialog.value = false
    }

    fun requestWithdrawal(beansAmount: Int, payoutMethod: String, accountDetail: String) {
        repository.requestWithdrawal(beansAmount, payoutMethod, accountDetail)
        _showWithdrawalDialog.value = false
    }

    // Direct Messaging
    fun openDmChat(peerUserId: String) {
        _activeDmPeerUserId.value = peerUserId
        closeUserProfile()
    }

    fun closeDmChat() {
        _activeDmPeerUserId.value = null
    }

    fun sendDirectMessage(peerUserId: String, text: String, gift: Gift? = null) {
        repository.sendDirectMessage(peerUserId, text, gift)
    }

    // Room Actions
    private var isCreatingRoom = false

    fun resetSignalingStatus() {
        _signalingStatus.value = "IDLE"
    }

    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            _signalingStatus.value = "VIEWER_CONNECTING"
            val currentUid: String = authCurrentUser.value?.uid
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: currentUserIdentity
            Log.d(FirestoreSignalingRepository.TAG_ROOM_JOIN, "Viewer $currentUid attempting to join roomId=$roomId")

            val roomObj = signalingRepository.getLiveRoom(roomId)
            if (roomObj == null) {
                Log.w(FirestoreSignalingRepository.TAG_ROOM_JOIN, "Join failed: room $roomId not found in Firestore")
                _signalingStatus.value = "ROOM_ENDED"
                return@launch
            }

            if (roomObj.id != roomId) {
                Log.w(FirestoreSignalingRepository.TAG_ROOM_JOIN, "Join failed: roomObj.id (${roomObj.id}) does not match requested roomId ($roomId)")
                _signalingStatus.value = "ROOM_ENDED"
                return@launch
            }

            val hostId = roomObj.hostId.takeIf { !it.isNullOrBlank() } ?: roomObj.creatorIdentity
            if (hostId.isNullOrBlank()) {
                Log.w(FirestoreSignalingRepository.TAG_ROOM_JOIN, "Join failed: room $roomId has no host identity")
                _signalingStatus.value = "ROOM_ENDED"
                return@launch
            }

            val isStatusLive = roomObj.status.equals(FirestoreSignalingRepository.STATUS_LIVE, ignoreCase = true)
            if (!isStatusLive || !roomObj.isLive) {
                Log.w(FirestoreSignalingRepository.TAG_ROOM_JOIN, "Join failed: room $roomId is not LIVE (status=${roomObj.status}, isLive=${roomObj.isLive})")
                _signalingStatus.value = "ROOM_ENDED"
                return@launch
            }

            val now = System.currentTimeMillis()
            val lastHeartbeat = roomObj.lastHeartbeatAt ?: roomObj.createdAt ?: now
            val isFreshlyCreated = (now - roomObj.createdAt) in -900_000L..900_000L
            val isStale = !isFreshlyCreated && ((now - lastHeartbeat) > 15 * 60 * 1000L)
            if (isStale) {
                Log.w(FirestoreSignalingRepository.TAG_ROOM_JOIN, "Join failed: room $roomId is stale (lastHeartbeat=${now - lastHeartbeat}ms ago)")
                _signalingStatus.value = "ROOM_ENDED"
                return@launch
            }

            Log.d(FirestoreSignalingRepository.TAG_ROOM_JOIN, "Join validation passed for room $roomId (host: $hostId, title: ${roomObj.title}). Opening live room.")
            repository.setCurrentRoom(roomObj)
            repository.joinRoom(roomId, fallbackRoom = roomObj)
        }
    }

    fun leaveRoom() {
        val room = currentRoom.value
        val currentUid = authCurrentUser.value?.uid
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: currentUserIdentity
        val isHost = room?.creatorIdentity == currentUid ||
                     room?.hostId == currentUid ||
                     room?.creatorIdentity == currentUserIdentity ||
                     room?.hostId == currentUserIdentity
        leaveRoomSession(isHost)
        _showGiftDialog.value = false
        _showParticipantsSheet.value = false
        _showFilterSheet.value = false
        _showSoundboard.value = false
        _showStreamStats.value = false
    }

    fun createRoom(
        title: String,
        roomType: RoomType,
        category: String,
        tags: List<String>,
        isPrivate: Boolean = false,
        password: String? = null
    ) {
        if (isCreatingRoom) {
            Log.w(FirestoreSignalingRepository.TAG_ROOM_CREATE, "Duplicate createRoom call ignored")
            return
        }
        isCreatingRoom = true

        val hostUid: String? = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: authCurrentUser.value?.uid
            ?: currentUserIdentity
        if (hostUid.isNullOrBlank()) {
            Log.e(FirestoreSignalingRepository.TAG_ROOM_CREATE, "Cannot create room: host identity is missing. User must be authenticated.")
            isCreatingRoom = false
            return
        }

        // Close any previous room for this host to avoid stale duplicate rooms
        val prevRoom = currentRoom.value
        if (prevRoom != null && (prevRoom.creatorIdentity == hostUid || prevRoom.hostId == hostUid)) {
            Log.d(FirestoreSignalingRepository.TAG_ROOM_CREATE, "Closing previous host room ${prevRoom.id} before creating new room")
            viewModelScope.launch {
                signalingRepository.endLiveRoom(prevRoom.id)
            }
        }

        val newRoomId = "room_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        Log.d(FirestoreSignalingRepository.TAG_ROOM_CREATE, "Creating live room: id=$newRoomId, hostUid=$hostUid, title=$title")

        val room = repository.createRoomWithId(
            roomId = newRoomId,
            title = title,
            roomType = roomType,
            category = category,
            tags = tags,
            hostUid = hostUid,
            isPrivate = isPrivate,
            password = password
        )

        viewModelScope.launch {
            try {
                val success = signalingRepository.createLiveRoom(room, hostUid)
                if (success) {
                    Log.d(FirestoreSignalingRepository.TAG_ROOM_CREATE, "Live room $newRoomId successfully created in Firestore for host $hostUid")
                } else {
                    Log.e(FirestoreSignalingRepository.TAG_ROOM_CREATE, "Failed to create live room $newRoomId in Firestore")
                }
            } catch (e: Exception) {
                Log.e(FirestoreSignalingRepository.TAG_ROOM_CREATE, "Exception creating live room in Firestore: ${e.message}", e)
            } finally {
                isCreatingRoom = false
            }
        }
        _showCreateRoomSheet.value = false
    }

    fun sendTextMessage(text: String, mention: String? = null) {
        val room = currentRoom.value ?: return
        if (text.isNotBlank()) {
            repository.sendTextMessage(room.id, text, mention)
        }
    }

    fun sendGift(gift: Gift, count: Int = 1) {
        val room = currentRoom.value ?: return
        repository.sendGift(room.id, gift, count)
    }

    fun sendLike() {
        val room = currentRoom.value ?: return
        repository.sendLike(room.id)
    }

    fun toggleChat(enabled: Boolean) {
        val room = currentRoom.value ?: return
        repository.toggleChatEnabled(room.id, enabled)
    }

    fun requestToPresent(seatId: Int = -1) {
        val room = currentRoom.value ?: return
        repository.requestToPresent(room.id, seatId)
    }

    fun cancelRequestToPresent() {
        val room = currentRoom.value ?: return
        repository.cancelRequestToPresent(room.id)
    }

    fun inviteParticipantToStage(participantIdentity: String, seatId: Int = -1) {
        val room = currentRoom.value ?: return
        repository.inviteParticipantToStage(room.id, participantIdentity, seatId)
    }

    fun removeParticipantFromStage(participantIdentity: String) {
        val room = currentRoom.value ?: return
        repository.removeParticipantFromStage(room.id, participantIdentity)
    }

    fun toggleSeatLock(seatId: Int) {
        val room = currentRoom.value ?: return
        repository.toggleSeatLock(room.id, seatId)
    }

    fun makeAdmin(participantIdentity: String) {
        val room = currentRoom.value ?: return
        repository.makeAdmin(room.id, participantIdentity)
    }

    fun removeAdmin(participantIdentity: String) {
        val room = currentRoom.value ?: return
        repository.removeAdmin(room.id, participantIdentity)
    }

    fun muteParticipantAudio(participantIdentity: String, muted: Boolean) {
        val room = currentRoom.value ?: return
        repository.muteParticipantAudio(room.id, participantIdentity, muted)
    }

    fun blockParticipant(participantIdentity: String) {
        val room = currentRoom.value ?: return
        repository.blockParticipant(room.id, participantIdentity)
    }

    fun setFilter(filter: BeautifyFilter) { repository.setFilter(filter) }
    fun toggleMic() {
        val newEnabled = !_isMicrophoneEnabled.value
        _isMicrophoneEnabled.value = newEnabled
        zegoMediaEngine.muteMicrophone(!newEnabled)
        _webRtcClient?.setMicrophoneEnabled(newEnabled)
        repository.toggleMic()
    }
    fun toggleVideo() {
        val newEnabled = !_isCameraEnabled.value
        _isCameraEnabled.value = newEnabled
        zegoMediaEngine.enableCamera(newEnabled)
        _webRtcClient?.setCameraEnabled(newEnabled)
        repository.toggleVideo()
    }
    fun flipCamera() {
        val nextFacing = !_cameraFacing.value
        _cameraFacing.value = nextFacing
        zegoMediaEngine.switchCamera()
        _webRtcClient?.switchCamera()
        repository.flipCamera()
    }
    fun playSfx(sfx: String) = repository.playSfx(sfx)

    override fun onCleared() {
        super.onCleared()
        zegoMediaEngine.destroy()
        cleanupWebRtc()
        _localMediaManager?.release()
    }

    fun updateRoomCover(roomId: String, coverUrl: String, coverStyle: String = "FULL_BACKDROP") {
        repository.updateRoomCover(roomId, coverUrl, coverStyle)
    }

    fun updateBroadcasterProfilePic(userId: String, newAvatarUrl: String) {
        repository.updateBroadcasterProfilePic(userId, newAvatarUrl)
    }

    // Visibility Setters
    fun setShowVipStoreDialog(show: Boolean) { _showVipStoreDialog.value = show }
    fun setShowRechargeDialog(show: Boolean) { _showRechargeDialog.value = show }
    fun setShowWithdrawalDialog(show: Boolean) { _showWithdrawalDialog.value = show }
    fun setShowTransactionsDialog(show: Boolean) { _showTransactionsDialog.value = show }
    fun setShowSettingsDialog(show: Boolean) { _showSettingsDialog.value = show }
    fun setShowGiftDialog(show: Boolean) { _showGiftDialog.value = show }
    fun setShowParticipantsSheet(show: Boolean) { _showParticipantsSheet.value = show }
    fun setShowFilterSheet(show: Boolean) { _showFilterSheet.value = show }
    fun setShowSoundboard(show: Boolean) { _showSoundboard.value = show }
    fun setShowStreamStats(show: Boolean) { _showStreamStats.value = show }
    fun setShowCreateRoomSheet(show: Boolean) { _showCreateRoomSheet.value = show }
    fun setShowRoomCoverSheet(show: Boolean) { _showRoomCoverSheet.value = show }
}

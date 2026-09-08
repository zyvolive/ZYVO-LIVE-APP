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
        hostSignalingJob = viewModelScope.launch {
            val client = getOrCreateWebRtcClient() ?: run {
                Log.e("ZYVO_RTC", "WebRtcClient unavailable for host")
                _signalingStatus.value = "FAILED"
                return@launch
            }
            val currentUser = authCurrentUser.value
            val hostUid = currentUser?.uid ?: currentUserIdentity

            Log.d("ZYVO_ROOM", "room created / host session starting: $roomId")

            // 0. Start host room heartbeat
            heartbeatJob = launch {
                while (isActive) {
                    signalingRepository.sendRoomHeartbeat(roomId)
                    delay(30_000L)
                }
            }

            // 1. Start local camera & audio tracks
            val vTrack = client.startLocalVideo(preferFront = _cameraFacing.value)
            client.startLocalAudio()
            _localVideoTrack.value = vTrack

            // 2. Create PeerConnection
            val pc = client.createPeerConnection()
            if (pc == null) {
                Log.e("ZYVO_RTC", "Failed to create PeerConnection for host")
                _signalingStatus.value = "FAILED"
                return@launch
            }

            // 3. Listen for local ICE candidates and publish to Firestore hostCandidates
            launch {
                client.generatedIceCandidates.collect { cand ->
                    val model = IceCandidateModel.fromWebRtc(cand, senderUid = hostUid)
                    Log.d("ZYVO_SIGNALING", "ICE candidate published: ${model.id}")
                    signalingRepository.publishHostIceCandidate(roomId, model)
                }
            }

            // 4. Create SDP offer
            val offerDesc = client.createOffer()
            if (offerDesc == null) {
                Log.e("ZYVO_SIGNALING", "Failed to create host SDP offer")
                _signalingStatus.value = "FAILED"
                return@launch
            }
            Log.d("ZYVO_SIGNALING", "offer created")

            // 5. Publish Offer to Firestore
            val offerModel = SessionDescriptionModel.fromWebRtc(offerDesc, senderUid = hostUid)
            val published = signalingRepository.publishOffer(roomId, offerModel)
            if (published) {
                Log.d("ZYVO_SIGNALING", "offer published")
                _signalingStatus.value = "OFFER_SENT"
            } else {
                Log.e("ZYVO_SIGNALING", "Failed to publish offer to Firestore")
                _signalingStatus.value = "FAILED"
            }

            // 6. Observe Answer from Viewer
            launch {
                signalingRepository.observeAnswer(roomId).collect { answerModel ->
                    if (answerModel != null && answerModel.toWebRtc() != null) {
                        val answerDesc = answerModel.toWebRtc()!!
                        Log.d("ZYVO_SIGNALING", "answer received")
                        val setOk = client.setRemoteDescription(answerDesc)
                        if (setOk) {
                            _signalingStatus.value = "ANSWER_RECEIVED"
                        }
                    }
                }
            }

            // 7. Observe Viewer ICE Candidates
            launch {
                signalingRepository.observeViewerIceCandidates(roomId).collect { candModel ->
                    val cand = candModel.toWebRtc()
                    if (cand != null) {
                        Log.d("ZYVO_SIGNALING", "ICE candidate received: ${candModel.id}")
                        client.addIceCandidate(cand)
                    }
                }
            }
        }
    }

    fun startViewerSession(roomId: String) {
        audioRouteManager?.startLiveAudioSession()
        hostSignalingJob?.cancel()
        viewerSignalingJob?.cancel()
        heartbeatJob?.cancel()
        _signalingStatus.value = "VIEWER_CONNECTING"
        viewerSignalingJob = viewModelScope.launch {
            val client = getOrCreateWebRtcClient() ?: run {
                Log.e("ZYVO_RTC", "WebRtcClient unavailable for viewer")
                _signalingStatus.value = "FAILED"
                return@launch
            }
            val currentUser = authCurrentUser.value
            val viewerUid = currentUser?.uid ?: currentUserIdentity

            Log.d("ZYVO_ROOM", "room joined: $roomId (viewer: $viewerUid)")

            // Track viewer presence & count in Firestore
            launch {
                val profile = currentUserProfile.value
                signalingRepository.addParticipant(roomId, viewerUid, profile.displayName, profile.avatarEmoji)
                signalingRepository.incrementViewerCount(roomId)
            }

            // 1. Create PeerConnection
            val pc = client.createPeerConnection()
            if (pc == null) {
                Log.e("ZYVO_RTC", "Failed to create PeerConnection for viewer")
                _signalingStatus.value = "FAILED"
                return@launch
            }

            // 2. Listen for local ICE candidates and publish to Firestore viewerCandidates
            launch {
                client.generatedIceCandidates.collect { cand ->
                    val model = IceCandidateModel.fromWebRtc(cand, senderUid = viewerUid)
                    Log.d("ZYVO_SIGNALING", "ICE candidate published: ${model.id}")
                    signalingRepository.publishViewerIceCandidate(roomId, model)
                }
            }

            // 3. Observe Host ICE Candidates
            launch {
                signalingRepository.observeHostIceCandidates(roomId).collect { candModel ->
                    val cand = candModel.toWebRtc()
                    if (cand != null) {
                        Log.d("ZYVO_SIGNALING", "ICE candidate received: ${candModel.id}")
                        client.addIceCandidate(cand)
                    }
                }
            }

            // 4. Observe Room status (detect host ending room)
            launch {
                signalingRepository.observeRoom(roomId).collect { updatedRoom ->
                    if (updatedRoom != null && (!updatedRoom.isLive || updatedRoom.status == FirestoreSignalingRepository.STATUS_ENDED)) {
                        Log.d("ZYVO_ROOM", "Room ended by host: $roomId")
                        _signalingStatus.value = "ROOM_ENDED"
                    }
                }
            }

            // 5. Observe Host SDP Offer
            var hasAnswered = false
            launch {
                signalingRepository.observeOffer(roomId).collect { offerModel ->
                    if (offerModel != null && !hasAnswered) {
                        val offerDesc = offerModel.toWebRtc()
                        if (offerDesc != null) {
                            Log.d("ZYVO_SIGNALING", "offer received")
                            val setOk = client.setRemoteDescription(offerDesc)
                            if (setOk) {
                                val answerDesc = client.createAnswer()
                                if (answerDesc != null) {
                                    hasAnswered = true
                                    Log.d("ZYVO_SIGNALING", "answer created")
                                    val answerModel = SessionDescriptionModel.fromWebRtc(answerDesc, senderUid = viewerUid)
                                    val pubOk = signalingRepository.publishAnswer(roomId, answerModel)
                                    if (pubOk) {
                                        Log.d("ZYVO_SIGNALING", "answer published")
                                        _signalingStatus.value = "ANSWER_SENT"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun leaveRoomSession(isHost: Boolean) {
        val room = currentRoom.value
        val roomId = room?.id

        Log.d("ZYVO_ROOM", "room exited: ${roomId ?: "none"}")
        Log.d("ZYVO_SIGNALING", "signaling listeners removed")

        hostSignalingJob?.cancel()
        hostSignalingJob = null
        viewerSignalingJob?.cancel()
        viewerSignalingJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        _signalingStatus.value = "IDLE"

        if (roomId != null) {
            viewModelScope.launch {
                if (isHost) {
                    signalingRepository.updateRoomStatus(roomId, FirestoreSignalingRepository.STATUS_ENDED)
                    signalingRepository.clearSignaling(roomId)
                } else {
                    val viewerUid = authCurrentUser.value?.uid ?: currentUserIdentity
                    signalingRepository.removeParticipant(roomId, viewerUid)
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

    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            _signalingStatus.value = "VIEWER_CONNECTING"
            // Fetch/validate room from Firestore before starting session
            signalingRepository.observeRoom(roomId).firstOrNull()?.let { roomObj ->
                val now = System.currentTimeMillis()
                val lastHeartbeat = roomObj.lastHeartbeatAt ?: roomObj.createdAt ?: now
                val isFreshlyCreated = (now - roomObj.createdAt) in -600_000L..600_000L
                val isStale = !isFreshlyCreated && ((now - lastHeartbeat) > 10 * 60 * 1000L)
                if (roomObj.isLive && roomObj.status.equals(FirestoreSignalingRepository.STATUS_LIVE, ignoreCase = true) && !isStale && !roomObj.hostId.isNullOrEmpty()) {
                    repository.setCurrentRoom(roomObj)
                    repository.joinRoom(roomId)
                } else {
                    Log.w("ZYVO_ROOM", "Join validation failed: room $roomId is ended or stale (isLive=${roomObj.isLive}, status=${roomObj.status}, isStale=$isStale)")
                    _signalingStatus.value = "ROOM_ENDED"
                }
            } ?: run {
                Log.w("ZYVO_ROOM", "Join validation failed: room $roomId not found")
                _signalingStatus.value = "ROOM_ENDED"
            }
        }
    }

    fun leaveRoom() {
        val room = currentRoom.value
        val isHost = room?.creatorIdentity == currentUserIdentity || room?.hostId == currentUserIdentity
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
            Log.w("ZYVO_ROOM_CREATE", "Duplicate createRoom call ignored")
            return
        }
        isCreatingRoom = true
        repository.createRoom(title, roomType, category, tags, isPrivate, password)
        val room = repository.currentRoom.value
        val hostUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: authCurrentUser.value?.uid
            ?: currentUserIdentity

        Log.d("ZYVO_ROOM_CREATE", "Initiating live room create: id=${room?.id}, hostUid=$hostUid, title=$title")
        if (room != null) {
            viewModelScope.launch {
                try {
                    val success = signalingRepository.createLiveRoom(room, hostUid)
                    Log.d("ZYVO_ROOM_CREATE", "signalingRepository.createLiveRoom returned $success for ${room.id}")
                } catch (e: Exception) {
                    Log.e("ZYVO_ROOM_CREATE", "Failed to create live room in Firestore: ${e.message}", e)
                } finally {
                    isCreatingRoom = false
                }
            }
        } else {
            isCreatingRoom = false
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
        _webRtcClient?.setMicrophoneEnabled(newEnabled)
        repository.toggleMic()
    }
    fun toggleVideo() {
        val newEnabled = !_isCameraEnabled.value
        _webRtcClient?.setCameraEnabled(newEnabled)
        repository.toggleVideo()
    }
    fun flipCamera() {
        _webRtcClient?.switchCamera()
        repository.flipCamera()
    }
    fun playSfx(sfx: String) = repository.playSfx(sfx)

    override fun onCleared() {
        super.onCleared()
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

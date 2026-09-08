package com.example.zyvo.data

import android.content.Context
import android.util.Log
import com.example.zyvo.auth.AuthRepository
import com.example.zyvo.auth.AuthState
import com.example.zyvo.data.model.User
import com.example.zyvo.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import com.example.zyvo.data.firebase.FirestoreSignalingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ZyvoRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    val authRepository: AuthRepository = AuthRepository()
) {

    init {
        scope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    val currentProf = _currentUserProfile.value
                    val updatedProf = user.toUserProfile(
                        bio = if (currentProf.bio.isNotBlank() && currentProf.bio != "Official Zyvo Streamer ⚡") currentProf.bio else "Official ZYVO Broadcaster 🎙️ Live on ZYVO!",
                        gender = currentProf.gender,
                        location = currentProf.location,
                        userLevel = currentProf.userLevel,
                        userXp = currentProf.userXp,
                        followersCount = currentProf.followersCount,
                        followingCount = currentProf.followingCount,
                        likesCount = currentProf.likesCount,
                        vipTier = currentProf.vipTier
                    )
                    _currentUserProfile.value = updatedProf
                    _userProfiles.update { map -> map + (user.uid to updatedProf) }
                    _isLoggedIn.value = true
                    saveSessionToPrefs()
                    signalingRepositoryRef?.let { startActiveRoomsObservation(it) }
                } else if (authRepository.authState.value is AuthState.Unauthenticated) {
                    _isLoggedIn.value = false
                }
            }
        }
    }

    val currentUserIdentity: String
        get() = _currentUserProfile.value.userId

    val currentUserName: String
        get() = _currentUserProfile.value.displayName

    val currentUserAvatar: String
        get() = _currentUserProfile.value.avatarEmoji

    // Auth state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private var sharedPreferences: android.content.SharedPreferences? = null

    fun initPersistence(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("zyvo_user_session_v2", Context.MODE_PRIVATE)
        }
        val prefs = sharedPreferences ?: return

        scope.launch {
            authRepository.checkSession()
        }

        val isLoggedInSaved = prefs.getBoolean("is_logged_in", false)
        if (isLoggedInSaved) {
            val userJson = prefs.getString("current_user_profile_json", null)
            if (!userJson.isNullOrBlank()) {
                val restoredProfile = jsonToUserProfile(userJson)
                if (restoredProfile != null) {
                    _currentUserProfile.value = restoredProfile
                    _userProfiles.update { map -> map + (restoredProfile.userId to restoredProfile) }
                    _userCoinBalance.value = prefs.getInt("user_coin_balance", 500)
                    _userBeansBalance.value = prefs.getInt("user_beans_balance", 0)

                    val savedFollowing = prefs.getStringSet("following_user_ids", null)
                    if (savedFollowing != null) {
                        _followingUserIds.value = savedFollowing
                    }

                    _isLoggedIn.value = true
                }
            }
        }

        // Restore any extra custom created user profiles saved locally
        val customProfilesJson = prefs.getString("all_custom_profiles_json", null)
        if (!customProfilesJson.isNullOrBlank()) {
            val customMap = jsonToUserProfilesMap(customProfilesJson)
            if (customMap.isNotEmpty()) {
                _userProfiles.update { map -> map + customMap }
            }
        }
    }

    private fun saveSessionToPrefs() {
        val prefs = sharedPreferences ?: return
        try {
            val profileJson = userProfileToJson(_currentUserProfile.value)
            val customProfilesJson = userProfilesMapToJson(_userProfiles.value)
            prefs.edit()
                .putBoolean("is_logged_in", _isLoggedIn.value)
                .putString("current_user_profile_json", profileJson)
                .putInt("user_coin_balance", _userCoinBalance.value)
                .putInt("user_beans_balance", _userBeansBalance.value)
                .putStringSet("following_user_ids", _followingUserIds.value)
                .putString("all_custom_profiles_json", customProfilesJson)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearSessionFromPrefs() {
        val prefs = sharedPreferences ?: return
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("current_user_profile_json")
            .apply()
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
        val sanitizedUsername = (if (username.isBlank()) email.substringBefore("@") else username)
            .replace(".", "_")
            .lowercase()
            .filter { it.isLetterOrDigit() || it == '_' }
        val finalUsername = if (sanitizedUsername.isBlank()) "broadcaster" else sanitizedUsername

        scope.launch {
            val result = if (!password.isNullOrBlank() && email.contains("@")) {
                authRepository.signUpWithEmail(
                    email = email,
                    password = password,
                    displayName = displayName.ifBlank { "ZYVO Broadcaster" },
                    username = finalUsername,
                    avatar = if (!avatarUrl.isNullOrBlank()) avatarUrl else avatarEmoji.ifBlank { "👑" }
                )
            } else {
                authRepository.signInAnonymously(
                    displayName = displayName.ifBlank { "ZYVO Broadcaster" },
                    username = finalUsername,
                    avatar = if (!avatarUrl.isNullOrBlank()) avatarUrl else avatarEmoji.ifBlank { "👑" }
                )
            }

            result.onSuccess { user ->
                val newProfile = user.toUserProfile(
                    bio = bio.ifBlank { "Official ZYVO Broadcaster 🎙️ Live on ZYVO!" },
                    gender = gender,
                    location = location
                )
                _currentUserProfile.value = newProfile
                _userProfiles.update { map -> map + (user.uid to newProfile) }
                _isLoggedIn.value = true
                saveSessionToPrefs()
            }.onFailure {
                val fallbackUid = "user_${finalUsername}_" + UUID.randomUUID().toString().take(6)
                val newProfile = UserProfile(
                    userId = fallbackUid,
                    username = finalUsername,
                    displayName = displayName.ifBlank { "ZYVO Broadcaster" },
                    avatarEmoji = avatarEmoji.ifBlank { "🚀" },
                    avatarUrl = avatarUrl,
                    coverGradientIndex = 0,
                    bio = bio.ifBlank { "Official ZYVO Broadcaster 🎙️ Live on ZYVO!" },
                    gender = gender,
                    location = location,
                    userLevel = 1,
                    userXp = 0,
                    nextLevelXp = 1000,
                    wealthLevel = 1,
                    hostLevel = 1,
                    vipTier = VipTier.NONE,
                    vipExpiresTimestamp = 0L,
                    followersCount = 0,
                    followingCount = 3,
                    followingUserIds = listOf("ceo_rayan", "co_founder_alpha", "ansharah_gahni"),
                    likesCount = 0,
                    diamondsEarnedTotal = 0,
                    giftsReceivedTotal = 0,
                    liveStreamsCount = 0,
                    badges = listOf("Verified Broadcaster", "New Creator"),
                    isLiveNow = false
                )
                _currentUserProfile.value = newProfile
                _userProfiles.update { map -> map + (fallbackUid to newProfile) }
                _isLoggedIn.value = true
                saveSessionToPrefs()
            }
        }
    }

    fun loginWithGoogle(
        idToken: String? = null,
        displayName: String,
        email: String,
        avatarEmoji: String,
        avatarUrl: String? = null
    ) {
        val sanitizedUsername = email.substringBefore("@")
            .replace(".", "_")
            .lowercase()
            .filter { it.isLetterOrDigit() || it == '_' }
            .ifBlank { "broadcaster" }

        scope.launch {
            if (!idToken.isNullOrBlank()) {
                val result = authRepository.signInWithGoogle(
                    idToken = idToken,
                    displayName = displayName.ifBlank { "ZYVO Broadcaster" },
                    username = sanitizedUsername,
                    avatar = if (!avatarUrl.isNullOrBlank()) avatarUrl else avatarEmoji.ifBlank { "👑" }
                )
                result.onSuccess { user ->
                    val newProfile = user.toUserProfile(
                        bio = "Official ZYVO Broadcaster & Creator 🎙️ Live on ZYVO!",
                        gender = "Not Specified",
                        location = "Global HQ 🌍"
                    )
                    _currentUserProfile.value = newProfile
                    _userProfiles.update { map -> map + (user.uid to newProfile) }
                    _isLoggedIn.value = true
                    saveSessionToPrefs()
                }.onFailure {
                    createCustomProfileAndLogin(
                        displayName = displayName,
                        username = sanitizedUsername,
                        email = email,
                        avatarEmoji = avatarEmoji,
                        avatarUrl = avatarUrl,
                        bio = "Official ZYVO Broadcaster & Creator 🎙️ Live on ZYVO!",
                        gender = "Not Specified",
                        location = "Global HQ 🌍"
                    )
                }
            } else {
                createCustomProfileAndLogin(
                    displayName = displayName,
                    username = sanitizedUsername,
                    email = email,
                    avatarEmoji = avatarEmoji,
                    avatarUrl = avatarUrl,
                    bio = "Official ZYVO Broadcaster & Creator 🎙️ Live on ZYVO!",
                    gender = "Not Specified",
                    location = "Global HQ 🌍"
                )
            }
        }
    }

    fun logout() {
        authRepository.signOut()
        _isLoggedIn.value = false
        clearSessionFromPrefs()
    }

    // Current User Profile
    private val _currentUserProfile = MutableStateFlow(
        UserProfile(
            userId = "user_me",
            username = "streamer",
            displayName = "New Streamer",
            avatarEmoji = "🚀",
            coverGradientIndex = 0,
            bio = "Official Zyvo Streamer ⚡",
            gender = "Unspecified",
            location = "Global HQ 🌍",
            userLevel = 1,
            userXp = 0,
            nextLevelXp = 1000,
            wealthLevel = 1,
            hostLevel = 1,
            vipTier = VipTier.NONE,
            vipExpiresTimestamp = 0L,
            followersCount = 0,
            followingCount = 3,
            followingUserIds = listOf("ceo_rayan", "co_founder_alpha", "ansharah_gahni"),
            likesCount = 0,
            diamondsEarnedTotal = 0,
            giftsReceivedTotal = 0,
            liveStreamsCount = 0,
            badges = listOf("Verified User"),
            isLiveNow = false
        )
    )
    val currentUserProfile: StateFlow<UserProfile> = _currentUserProfile.asStateFlow()

    // All Users Registry
    private val _userProfiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val userProfiles: StateFlow<Map<String, UserProfile>> = _userProfiles.asStateFlow()

    // Wallet balances
    private val _userCoinBalance = MutableStateFlow(500)
    val userCoinBalance: StateFlow<Int> = _userCoinBalance.asStateFlow()

    private val _userBeansBalance = MutableStateFlow(0)
    val userBeansBalance: StateFlow<Int> = _userBeansBalance.asStateFlow()

    // Transaction History
    private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

    // Following & Blocked User IDs
    private val _followingUserIds = MutableStateFlow<Set<String>>(setOf("ceo_rayan", "co_founder_alpha", "ansharah_gahni"))
    val followingUserIds: StateFlow<Set<String>> = _followingUserIds.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    // Direct Messages & Conversations
    private val _conversations = MutableStateFlow<List<ConversationSummary>>(emptyList())
    val conversations: StateFlow<List<ConversationSummary>> = _conversations.asStateFlow()

    private val _directMessages = MutableStateFlow<Map<String, List<DirectMessage>>>(emptyMap())
    val directMessages: StateFlow<Map<String, List<DirectMessage>>> = _directMessages.asStateFlow()

    // Live Rooms & Stream state
    private val _rooms = MutableStateFlow<List<LiveRoom>>(emptyList())
    val rooms: StateFlow<List<LiveRoom>> = _rooms.asStateFlow()

    private val _currentRoom = MutableStateFlow<LiveRoom?>(null)
    val currentRoom: StateFlow<LiveRoom?> = _currentRoom.asStateFlow()

    private var activeRoomsJob: Job? = null
    private var signalingRepositoryRef: FirestoreSignalingRepository? = null

    fun startActiveRoomsObservation(signalingRepo: FirestoreSignalingRepository) {
        signalingRepositoryRef = signalingRepo
        activeRoomsJob?.cancel()
        Log.d("ZYVO_DISCOVERY", "ZyvoRepository: Starting active rooms observation")
        activeRoomsJob = scope.launch {
            signalingRepo.observeActiveLiveRooms().collect { activeRooms ->
                Log.d("ZYVO_DISCOVERY", "ZyvoRepository: Emitting ${activeRooms.size} active rooms to UI StateFlow: ${activeRooms.map { "${it.id}(${it.title})" }}")
                _rooms.value = activeRooms
            }
        }
    }

    fun setCurrentRoom(room: LiveRoom?) {
        _currentRoom.value = room
    }

    private val _chatMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val chatMessages: StateFlow<Map<String, List<ChatMessage>>> = _chatMessages.asStateFlow()

    private val _participants = MutableStateFlow<Map<String, List<Participant>>>(emptyMap())
    val participants: StateFlow<Map<String, List<Participant>>> = _participants.asStateFlow()

    private val _activeFloatingGifts = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeFloatingGifts: StateFlow<List<ChatMessage>> = _activeFloatingGifts.asStateFlow()

    private val _currentFilter = MutableStateFlow(BeautifyFilter.ORIGINAL)
    val currentFilter: StateFlow<BeautifyFilter> = _currentFilter.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isVideoMuted = MutableStateFlow(false)
    val isVideoMuted: StateFlow<Boolean> = _isVideoMuted.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _lastPlayedSfx = MutableStateFlow<String?>(null)
    val lastPlayedSfx: StateFlow<String?> = _lastPlayedSfx.asStateFlow()

    init {
        initializeInitialUsers()
        initializeInitialTransactions()
        initializeInitialRooms()
        initializeInitialDms()
    }

    private fun initializeInitialUsers() {
        val initialUsers = mapOf(
            "ceo_rayan" to UserProfile(
                userId = "ceo_rayan",
                username = "rayan_mirza",
                displayName = "RAYAN MIRZA",
                avatarEmoji = "👑",
                avatarUrl = "https://cdn.phototourl.com/free/2026-09-01-f3e014af-6987-41b0-8bcf-732294379e68.png",
                coverGradientIndex = 0,
                bio = "👑 Founder & Chief Executive Officer (CEO) of ZYVO. Empowering millions of creators worldwide. Contact executive desk via WhatsApp: +44 7868 713315",
                gender = "Male",
                location = "London, UK 🇬🇧 / Global HQ 🌍",
                userLevel = 99,
                userXp = 9999999,
                nextLevelXp = 10000000,
                wealthLevel = 99,
                hostLevel = 99,
                vipTier = VipTier.VIP_9,
                vipExpiresTimestamp = System.currentTimeMillis() + (365L * 24 * 3600 * 1000),
                followersCount = 9850000,
                followingCount = 99,
                likesCount = 158000000,
                diamondsEarnedTotal = 99999999,
                giftsReceivedTotal = 99999,
                liveStreamsCount = 999,
                badges = listOf("👑 Founder & CEO", "💎 VIP 9 SUPREME", "⭐ Lv.99 Sovereign", "Official Verified", "God Tier Creator"),
                isFollowedByCurrentUser = true,
                isLiveNow = true,
                currentRoomId = "room_ceo_999",
                executiveRole = "FOUNDER & CEO",
                whatsappNumber = "+44 7868 713315",
                whatsappDirectUrl = "https://wa.me/447868713315"
            ),
            "co_founder_alpha" to UserProfile(
                userId = "co_founder_alpha",
                username = "alpha_rajpoot",
                displayName = "ALPHA RAJPOOT",
                avatarEmoji = "🦁",
                avatarUrl = "https://cdn.phototourl.com/free/2026-09-01-4aa927e1-ee25-497a-ae9e-4201e9d81679.jpg",
                coverGradientIndex = 2,
                bio = "🔥 Co-Founder & Executive Director at ZYVO. Head of Global PK Arenas, Creator Growth & Strategic Partnerships. WhatsApp Executive Desk: +447366 387620",
                gender = "Male",
                location = "London, UK 🇬🇧 / Global Operations 🌍",
                userLevel = 99,
                userXp = 9999999,
                nextLevelXp = 10000000,
                wealthLevel = 99,
                hostLevel = 99,
                vipTier = VipTier.VIP_9,
                vipExpiresTimestamp = System.currentTimeMillis() + (365L * 24 * 3600 * 1000),
                followersCount = 8420000,
                followingCount = 88,
                likesCount = 132000000,
                diamondsEarnedTotal = 88888888,
                giftsReceivedTotal = 88888,
                liveStreamsCount = 888,
                badges = listOf("🛡️ Co-Founder", "💎 VIP 9 SUPREME", "⭐ Lv.99 Sovereign", "Official Verified", "PK Grandmaster"),
                isFollowedByCurrentUser = true,
                isLiveNow = true,
                currentRoomId = "room_alpha_888",
                executiveRole = "CO-FOUNDER",
                whatsappNumber = "+447366 387620",
                whatsappDirectUrl = "https://wa.me/447366387620"
            ),
            "ansharah_gahni" to UserProfile(
                userId = "ansharah_gahni",
                username = "ansharah_gahni",
                displayName = "ANSHARAH GAHNI",
                avatarEmoji = "👸",
                avatarUrl = "https://mp3tourl.com/images/1788287833535-dc94ba6e-5e98-4349-b949-cd1521ff4618.jpg",
                coverGradientIndex = 3,
                bio = "✨ TOP HOST & ZYVO GLOBAL QUEEN 👑 Level 89 Superstar • SVIP 7 • Receiving Millions Daily 💖 Officially Following Founder Rayan Mirza & Co-Founder Alpha Rajpoot 🌍",
                gender = "Female",
                location = "Dubai, UAE 🇦🇪 / Global Host Stage 🌍",
                userLevel = 89,
                userXp = 8900000,
                nextLevelXp = 9000000,
                wealthLevel = 89,
                hostLevel = 89,
                vipTier = VipTier.SVIP_7,
                vipExpiresTimestamp = System.currentTimeMillis() + (365L * 24 * 3600 * 1000),
                followersCount = 7650000,
                followingCount = 2,
                followingUserIds = listOf("ceo_rayan", "co_founder_alpha"),
                likesCount = 98500000,
                diamondsEarnedTotal = 78500000,
                giftsReceivedTotal = 75800,
                liveStreamsCount = 740,
                badges = listOf("💎 SVIP 7 QUEEN", "👑 Top Host Queen", "⭐ Lv.89 Superstar", "Official Verified Broadcaster", "Million Receiving Elite"),
                isFollowedByCurrentUser = true,
                isLiveNow = true,
                currentRoomId = "room_ansharah_777",
                isTopHost = true
            )
        )
        _userProfiles.value = initialUsers
    }

    private fun initializeInitialTransactions() {
        _walletTransactions.value = listOf(
            WalletTransaction(
                id = "tx_101",
                title = "Coin Recharge Package",
                detail = "7,500 Coins + 2,000 Bonus Coins via Google Pay",
                coinAmount = 9500,
                usdAmount = 49.99,
                type = TransactionType.COIN_RECHARGE,
                status = TransactionStatus.COMPLETED,
                timestampFormatted = "Today, 14:32",
                iconEmoji = "💳"
            ),
            WalletTransaction(
                id = "tx_102",
                title = "Sent Golden Dragon Gift",
                detail = "To Kai Sterling in Cyberpunk Beats DJ Session",
                coinAmount = -2500,
                type = TransactionType.GIFT_SENT,
                status = TransactionStatus.COMPLETED,
                timestampFormatted = "Yesterday, 21:15",
                iconEmoji = "🐉"
            ),
            WalletTransaction(
                id = "tx_103",
                title = "Purchased VIP 3 Pass",
                detail = "1 Month VIP 3 Membership Renewal",
                coinAmount = -2000,
                type = TransactionType.VIP_PURCHASE,
                status = TransactionStatus.COMPLETED,
                timestampFormatted = "3 days ago",
                iconEmoji = "👑"
            ),
            WalletTransaction(
                id = "tx_104",
                title = "Host Earnings Payout",
                detail = "Withdrawal of $150.00 USD via PayPal Payout",
                beanAmount = -15000,
                usdAmount = 150.00,
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.COMPLETED,
                timestampFormatted = "5 days ago",
                iconEmoji = "💸"
            )
        )
    }

    private fun initializeInitialRooms() {
        val ceoRoom = LiveRoom(
            id = "room_ceo_999",
            title = "👑 FOUNDER RAYAN MIRZA | Official Global Keynote & Creator Summit",
            description = "Welcome to ZYVO HQ Live Stream! Discussing new features, massive creator rewards, and global partnership opportunities. Direct WhatsApp available.",
            creatorIdentity = "ceo_rayan",
            hostName = "RAYAN MIRZA (FOUNDER & CEO)",
            hostAvatar = "👑",
            hostAvatarUrl = "https://cdn.phototourl.com/free/2026-09-01-f3e014af-6987-41b0-8bcf-732294379e68.png",
            hostGender = "Male",
            roomCoverUrl = "https://cdn.phototourl.com/free/2026-09-01-f3e014af-6987-41b0-8bcf-732294379e68.png",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.SINGLE_LIVE,
            category = "Official",
            tags = listOf("Founder", "CEO", "Official", "Summit", "VIP9", "Lv99"),
            viewerCount = 148900,
            likesCount = 2850000,
            enableChat = true
        )

        val alphaRoom = LiveRoom(
            id = "room_alpha_888",
            title = "⚔️ CO-FOUNDER ALPHA RAJPOOT | Zyvo Executive PK Battle & 10M Gem Drop",
            description = "High stakes Executive Battle Stage! Dropping millions of Gems and golden gifts for the community.",
            creatorIdentity = "co_founder_alpha",
            hostName = "ALPHA RAJPOOT (CO-FOUNDER)",
            hostAvatar = "🦁",
            hostAvatarUrl = "https://cdn.phototourl.com/free/2026-09-01-4aa927e1-ee25-497a-ae9e-4201e9d81679.jpg",
            hostGender = "Male",
            roomCoverUrl = "https://cdn.phototourl.com/free/2026-09-01-4aa927e1-ee25-497a-ae9e-4201e9d81679.jpg",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.PK_BATTLE,
            category = "PK Arena",
            tags = listOf("CoFounder", "Executive", "PK", "HighStakes", "VIP9", "Lv99"),
            viewerCount = 112400,
            likesCount = 1950000,
            pkState = PkState(
                isActive = true,
                targetHostName = "Apex Arenas",
                targetHostAvatar = "⚔️",
                myScore = 890000,
                targetScore = 650000,
                remainingSeconds = 240
            )
        )

        val ansharahRoom = LiveRoom(
            id = "room_ansharah_777",
            title = "🔮 ANSHARAH GAHNI | Top Host Queen Live & 50M Gem Gala 👑",
            description = "Welcome to Ansharah Gahni's Official Live Stage! Level 89 Top Host • SVIP 7 • Dropping massive rewards with Founder & Co-Founder backing!",
            creatorIdentity = "ansharah_gahni",
            hostName = "ANSHARAH GAHNI",
            hostAvatar = "👸",
            hostAvatarUrl = "https://mp3tourl.com/images/1788287833535-dc94ba6e-5e98-4349-b949-cd1521ff4618.jpg",
            roomCoverUrl = "https://mp3tourl.com/images/1788287833535-dc94ba6e-5e98-4349-b949-cd1521ff4618.jpg",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.SINGLE_LIVE,
            category = "Top Host",
            tags = listOf("TopHost", "SVIP7", "Lv89", "Queen", "Official"),
            viewerCount = 98400,
            likesCount = 1650000,
            enableChat = true
        )

        val nusratRoom = LiveRoom(
            id = "room_nusrat_01",
            title = "❤️ Let's Talk & Relax Together | Q&A Night",
            description = "Good vibes, music requests, and fan shoutouts! Tap the screen to send likes.",
            creatorIdentity = "nusrat_jahan",
            hostName = "Nusrat Jahan",
            hostAvatar = "🌸",
            hostAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80",
            roomCoverUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.SINGLE_LIVE,
            category = "Let's Talk",
            tags = listOf("Chat", "Talk", "Verified", "Chill"),
            viewerCount = 12500,
            likesCount = 245000,
            enableChat = true
        )

        val maishaRoom = LiveRoom(
            id = "room_maisha_02",
            title = "🌊 Good Vibes ✨ | Acoustic Chill & Stories",
            description = "Late night chill session with live guitar and chats.",
            creatorIdentity = "maisha",
            hostName = "Maisha",
            hostAvatar = "🌊",
            hostAvatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&auto=format&fit=crop&q=80",
            roomCoverUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&auto=format&fit=crop&q=80",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.SINGLE_LIVE,
            category = "Good Vibes",
            tags = listOf("Vibes", "Acoustic", "Verified"),
            viewerCount = 8700,
            likesCount = 189000,
            enableChat = true
        )

        val ayeshaRoom = LiveRoom(
            id = "room_ayesha_03",
            title = "🎵 Music Live | Singing Your Favorite Songs Live 🎤",
            description = "Drop your song requests in the comments! Gift senders get priority queue.",
            creatorIdentity = "ayesha_live",
            hostName = "Ayesha Live",
            hostAvatar = "🎵",
            hostAvatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&auto=format&fit=crop&q=80",
            roomCoverUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&auto=format&fit=crop&q=80",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.SINGLE_LIVE,
            category = "Music Live",
            tags = listOf("Music", "Singing", "LiveConcert"),
            viewerCount = 9200,
            likesCount = 210000,
            enableChat = true
        )

        val cuteAngelRoom = LiveRoom(
            id = "room_angel_04",
            title = "⭐ Happy Time | Gaming & Cozy Talk 🎮",
            description = "Playing community games and chatting with VIP followers.",
            creatorIdentity = "cute_angel",
            hostName = "Cute Angel",
            hostAvatar = "⭐",
            hostAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&auto=format&fit=crop&q=80",
            roomCoverUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&auto=format&fit=crop&q=80",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.SINGLE_LIVE,
            category = "Entertainment",
            tags = listOf("Happy", "Gaming", "Fun"),
            viewerCount = 7100,
            likesCount = 154000,
            enableChat = true
        )

        val kingRoom = LiveRoom(
            id = "room_king_05",
            title = "👑 King Of King's | Elite Battle Championship 🔥",
            description = "Defending our #1 Global Leaderboard rank in high-energy battles!",
            creatorIdentity = "king_of_kings",
            hostName = "King Of King's",
            hostAvatar = "👑",
            hostAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80",
            hostGender = "Male",
            roomCoverUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.PK_BATTLE,
            category = "PK Arena",
            tags = listOf("PK", "Champion", "GoldRank"),
            viewerCount = 45200,
            likesCount = 980000,
            enableChat = true
        )

        val dramaQueenRoom = LiveRoom(
            id = "room_drama_06",
            title = "🎭 Drama Queen | Theatre, Acting & Roast Party 🎉",
            description = "Hilarious live interactions and creative skits with the chat!",
            creatorIdentity = "drama_queen",
            hostName = "Drama Queen",
            hostAvatar = "🎭",
            hostAvatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=500&auto=format&fit=crop&q=80",
            roomCoverUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=500&auto=format&fit=crop&q=80",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.MULTI_GUEST,
            category = "Multi-Guest",
            tags = listOf("Party", "Drama", "MultiGuest"),
            viewerCount = 38900,
            likesCount = 750000,
            enableChat = true
        )

        val jannatRoom = LiveRoom(
            id = "room_jannat_07",
            title = "💬 Jannatul Islam | Late Night Podcast & Advice ☕",
            description = "Real talk, listener stories, and community advice circle.",
            creatorIdentity = "jannatul_islam",
            hostName = "Jannatul Islam",
            hostAvatar = "☕",
            hostAvatarUrl = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=500&auto=format&fit=crop&q=80",
            roomCoverUrl = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=500&auto=format&fit=crop&q=80",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.AUDIO_STAGE,
            category = "Audio Stage",
            tags = listOf("Podcast", "Audio", "LateNight"),
            viewerCount = 28400,
            likesCount = 610000,
            enableChat = true
        )

        val husnatRoom = LiveRoom(
            id = "room_husnat_08",
            title = "✨ Husnat Smita | Global Talent & Dance Spotlight 💃",
            description = "High energy dance performances and freestyle live stream!",
            creatorIdentity = "husnat_smita",
            hostName = "Husnat Smita",
            hostAvatar = "💃",
            hostAvatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=500&auto=format&fit=crop&q=80",
            roomCoverUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=500&auto=format&fit=crop&q=80",
            coverStyle = "FULL_BACKDROP",
            roomType = RoomType.SINGLE_LIVE,
            category = "Dance",
            tags = listOf("Dance", "Talent", "Energy"),
            viewerCount = 21900,
            likesCount = 490000,
            enableChat = true
        )

        // Production rule: Firestore liveRooms is the single source of truth for active rooms
        _rooms.value = emptyList()

        // Pre-fill initial chat messages
        _chatMessages.value = mapOf(
            "room_ceo_999" to listOf(
                ChatMessage("c01", "system", "ZYVO HQ", senderAvatar = "👑", text = "👑 WELCOME TO CEO & FOUNDER RAYAN MIRZA OFFICIAL LIVE! VIP 9 SUPREME ACTIVE.", type = MessageType.SYSTEM),
                ChatMessage("c02", "vip_fan", "Lord_Vanguard", "💎", "Glory to Founder Rayan Mirza! Sent 50x Golden Dragons! 🐉"),
                ChatMessage("c03", "creator_1", "Mia_Vocal", "🎤", "Thank you for the creator fund upgrade! Zyvo is #1 🔥")
            ),
            "room_alpha_888" to listOf(
                ChatMessage("c04", "system", "ZYVO HQ", senderAvatar = "🛡️", text = "⚔️ WELCOME TO CO-FOUNDER ALPHA RAJPOOT EXECUTIVE PK ARENA! VIP 9 ACTIVE.", type = MessageType.SYSTEM),
                ChatMessage("c05", "pk_titan", "Gamer_Rex", "🦁", "Alpha Rajpoot crushing the arena! 100k combo!! 💥")
            ),
            "room_ansharah_777" to listOf(
                ChatMessage("ca1", "system", "ZYVO HQ", senderAvatar = "💎", text = "👑 WELCOME TO TOP HOST ANSHARAH GAHNI OFFICIAL LIVE! SVIP 7 ACTIVE.", type = MessageType.SYSTEM),
                ChatMessage("ca2", "ceo_rayan", "RAYAN MIRZA (CEO)", "👑", "Welcome Ansharah to the Top Host Spotlight! 🔮 Sent 100,000 Gems!"),
                ChatMessage("ca3", "co_founder_alpha", "ALPHA RAJPOOT", "🦁", "Keep shining Queen Ansharah! Top Host power! 🔥"),
                ChatMessage("ca4", "vip_fan", "CrownPrince_99", "💎", "Sent 10x Galactic Dragon Palace to Queen Ansharah! 👸")
            )
        )

        // Pre-fill participants
        _participants.value = mapOf(
            "room_ceo_999" to listOf(
                Participant("ceo_rayan", "RAYAN MIRZA (CEO)", "👑", role = ParticipantRole.HOST),
                Participant("co_founder_alpha", "ALPHA RAJPOOT", "🦁", role = ParticipantRole.ADMIN),
                Participant("ansharah_gahni", "ANSHARAH GAHNI", "👸", role = ParticipantRole.ADMIN)
            ),
            "room_alpha_888" to listOf(
                Participant("co_founder_alpha", "ALPHA RAJPOOT", "🦁", role = ParticipantRole.HOST),
                Participant("ceo_rayan", "RAYAN MIRZA (CEO)", "👑", role = ParticipantRole.ADMIN)
            ),
            "room_ansharah_777" to listOf(
                Participant("ansharah_gahni", "ANSHARAH GAHNI", "👸", role = ParticipantRole.HOST),
                Participant("ceo_rayan", "RAYAN MIRZA (CEO)", "👑", role = ParticipantRole.ADMIN)
            )
        )
    }

    private fun initializeInitialDms() {
        val initialConversations = listOf(
            ConversationSummary(
                peerUserId = "ceo_rayan",
                peerDisplayName = "RAYAN MIRZA (CEO)",
                peerUsername = "@rayan_mirza",
                peerAvatarEmoji = "👑",
                peerVipTier = VipTier.VIP_9,
                lastMessageText = "Welcome to ZYVO Live! Reach out anytime on WhatsApp for official creator backing.",
                lastMessageTime = "12:00",
                unreadCount = 1,
                isPeerLive = true
            ),
            ConversationSummary(
                peerUserId = "co_founder_alpha",
                peerDisplayName = "ALPHA RAJPOOT",
                peerUsername = "@alpha_rajpoot",
                peerAvatarEmoji = "🦁",
                peerVipTier = VipTier.VIP_9,
                lastMessageText = "Welcome to the family! Join the PK Arenas and climb the global leaderboards.",
                lastMessageTime = "Yesterday",
                unreadCount = 0,
                isPeerLive = true
            ),
            ConversationSummary(
                peerUserId = "ansharah_gahni",
                peerDisplayName = "ANSHARAH GAHNI",
                peerUsername = "@ansharah_gahni",
                peerAvatarEmoji = "👸",
                peerVipTier = VipTier.SVIP_7,
                lastMessageText = "Hello darling! Welcome to Zyvo Live! Let me know if you need any hosting tips ✨",
                lastMessageTime = "2 days ago",
                unreadCount = 0,
                isPeerLive = true
            )
        )
        _conversations.value = initialConversations

        _directMessages.value = mapOf(
            "ceo_rayan" to listOf(
                DirectMessage("m1", "ceo_rayan", "RAYAN MIRZA (CEO)", "👑", "Welcome to ZYVO Live! Reach out anytime on WhatsApp for official creator backing.", "12:00", false)
            ),
            "co_founder_alpha" to listOf(
                DirectMessage("m2", "co_founder_alpha", "ALPHA RAJPOOT", "🦁", "Welcome to the family! Join the PK Arenas and climb the global leaderboards.", "Yesterday", false)
            ),
            "ansharah_gahni" to listOf(
                DirectMessage("m3", "ansharah_gahni", "ANSHARAH GAHNI", "👸", "Hello darling! Welcome to Zyvo Live! Let me know if you need any hosting tips ✨", "2 days ago", false)
            )
        )
    }

    // === USER PROFILE & ACTIONS ===

    fun getUserProfile(userId: String): UserProfile {
        if (userId == currentUserIdentity || userId == _currentUserProfile.value.userId) {
            return _currentUserProfile.value
        }
        return _userProfiles.value[userId] ?: UserProfile(
            userId = userId,
            username = "user_$userId",
            displayName = "User $userId",
            avatarEmoji = "👤"
        )
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
        _currentUserProfile.update { profile ->
            profile.copy(
                displayName = displayName.ifBlank { profile.displayName },
                username = username?.ifBlank { profile.username } ?: profile.username,
                bio = bio,
                gender = gender,
                location = location,
                avatarEmoji = avatarEmoji,
                avatarUrl = avatarUrl ?: profile.avatarUrl,
                coverGradientIndex = coverGradientIndex ?: profile.coverGradientIndex
            )
        }
        // Update user in directory as well
        val updated = _currentUserProfile.value
        _userProfiles.update { map ->
            map + (updated.userId to updated)
        }
        saveSessionToPrefs()
    }

    fun followUser(userId: String) {
        _followingUserIds.update { it + userId }
        _userProfiles.update { map ->
            val p = map[userId]
            if (p != null) {
                map + (userId to p.copy(
                    isFollowedByCurrentUser = true,
                    followersCount = p.followersCount + 1
                ))
            } else map
        }
        _currentUserProfile.update { it.copy(followingCount = it.followingCount + 1) }
        saveSessionToPrefs()
    }

    fun unfollowUser(userId: String) {
        _followingUserIds.update { it - userId }
        _userProfiles.update { map ->
            val p = map[userId]
            if (p != null) {
                map + (userId to p.copy(
                    isFollowedByCurrentUser = false,
                    followersCount = maxOf(0, p.followersCount - 1)
                ))
            } else map
        }
        _currentUserProfile.update { it.copy(followingCount = maxOf(0, it.followingCount - 1)) }
        saveSessionToPrefs()
    }

    fun blockUser(userId: String) {
        _blockedUserIds.update { it + userId }
        _userProfiles.update { map ->
            val p = map[userId]
            if (p != null) {
                map + (userId to p.copy(isBlocked = true))
            } else map
        }
    }

    fun unblockUser(userId: String) {
        _blockedUserIds.update { it - userId }
        _userProfiles.update { map ->
            val p = map[userId]
            if (p != null) {
                map + (userId to p.copy(isBlocked = false))
            } else map
        }
    }

    fun reportUser(userId: String, reason: String) {
        // Log report simulation
    }

    // === WALLET & FINANCES ===

    fun rechargeCoins(packageTitle: String, coinAmount: Int, bonusCoins: Int, usdPrice: Double, paymentMethod: String) {
        val totalAdded = coinAmount + bonusCoins
        _userCoinBalance.update { it + totalAdded }

        // Increase user XP & Wealth level
        val addedXp = totalAdded * 2
        _currentUserProfile.update { p ->
            val newXp = p.userXp + addedXp
            val newLevel = if (newXp >= p.nextLevelXp) p.userLevel + 1 else p.userLevel
            val nextXp = if (newXp >= p.nextLevelXp) p.nextLevelXp + 3000 else p.nextLevelXp
            val newWealth = p.wealthLevel + (totalAdded / 1000)
            p.copy(
                userXp = newXp,
                userLevel = newLevel,
                nextLevelXp = nextXp,
                wealthLevel = maxOf(p.wealthLevel, newWealth)
            )
        }

        // Add transaction log
        val tx = WalletTransaction(
            id = "tx_" + UUID.randomUUID().toString().take(6),
            title = "Recharged $packageTitle",
            detail = "+$totalAdded Coins via $paymentMethod",
            coinAmount = totalAdded,
            usdAmount = usdPrice,
            type = TransactionType.COIN_RECHARGE,
            status = TransactionStatus.COMPLETED,
            timestampFormatted = "Just now",
            iconEmoji = "💳"
        )
        _walletTransactions.update { listOf(tx) + it }
    }

    fun buyVipPackage(tier: VipTier, months: Int, coinCost: Int) {
        if (_userCoinBalance.value < coinCost) return

        _userCoinBalance.update { it - coinCost }

        val expiry = System.currentTimeMillis() + (months * 30L * 24 * 3600 * 1000)
        _currentUserProfile.update { p ->
            p.copy(
                vipTier = tier,
                vipExpiresTimestamp = expiry,
                badges = (p.badges + tier.badge).distinct()
            )
        }

        val tx = WalletTransaction(
            id = "tx_" + UUID.randomUUID().toString().take(6),
            title = "Purchased ${tier.displayName}",
            detail = "$months Month Membership Pass",
            coinAmount = -coinCost,
            type = TransactionType.VIP_PURCHASE,
            status = TransactionStatus.COMPLETED,
            timestampFormatted = "Just now",
            iconEmoji = "👑"
        )
        _walletTransactions.update { listOf(tx) + it }
    }

    fun requestWithdrawal(beansAmount: Int, payoutMethod: String, accountDetail: String) {
        if (_userBeansBalance.value < beansAmount) return

        val usdAmount = beansAmount / 100.0
        _userBeansBalance.update { it - beansAmount }

        val tx = WalletTransaction(
            id = "tx_" + UUID.randomUUID().toString().take(6),
            title = "Withdrawal Request ($${String.format("%.2f", usdAmount)})",
            detail = "Cashout of $beansAmount Beans to $payoutMethod ($accountDetail)",
            beanAmount = -beansAmount,
            usdAmount = usdAmount,
            type = TransactionType.WITHDRAWAL,
            status = TransactionStatus.COMPLETED,
            timestampFormatted = "Just now",
            iconEmoji = "💸"
        )
        _walletTransactions.update { listOf(tx) + it }
    }

    // === DIRECT MESSAGES ===

    fun sendDirectMessage(peerUserId: String, text: String, gift: Gift? = null) {
        if (text.isBlank() && gift == null) return

        val peer = getUserProfile(peerUserId)
        val msg = DirectMessage(
            id = UUID.randomUUID().toString(),
            senderId = currentUserIdentity,
            senderName = currentUserName,
            senderAvatar = currentUserAvatar,
            text = text.ifBlank { "Sent a ${gift?.name} ${gift?.iconEmoji}" },
            timestampFormatted = "Just now",
            isFromCurrentUser = true,
            giftAttached = gift
        )

        // Update messages map
        _directMessages.update { map ->
            val list = map[peerUserId] ?: emptyList()
            map + (peerUserId to (list + msg))
        }

        // Update conversation summary
        _conversations.update { list ->
            val existing = list.find { it.peerUserId == peerUserId }
            val updatedSummary = ConversationSummary(
                peerUserId = peerUserId,
                peerDisplayName = peer.displayName,
                peerUsername = "@${peer.username}",
                peerAvatarEmoji = peer.avatarEmoji,
                peerVipTier = peer.vipTier,
                lastMessageText = msg.text,
                lastMessageTime = "Just now",
                unreadCount = 0,
                isPeerLive = peer.isLiveNow
            )
            if (existing != null) {
                list.map { if (it.peerUserId == peerUserId) updatedSummary else it }
            } else {
                listOf(updatedSummary) + list
            }
        }
    }

    // === ROOMS & LIVE SIMULATION ===

    fun joinRoom(roomId: String, fallbackRoom: LiveRoom? = null) {
        val room = fallbackRoom ?: _rooms.value.find { it.id == roomId } ?: _currentRoom.value
        if (room != null) {
            _currentRoom.value = room
        }

        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: authRepository.currentUser.value?.uid
            ?: currentUserIdentity

        // Add user as participant
        _participants.update { map ->
            val list = map[roomId] ?: emptyList()
            val exists = list.any { it.identity == authUid || it.identity == currentUserIdentity }
            if (!exists) {
                map + (roomId to (list + Participant(authUid, currentUserName, currentUserAvatar, ParticipantRole.VIEWER)))
            } else map
        }

        sendSystemMessage(roomId, "$currentUserName joined the live stream ✨")
    }

    fun leaveRoom() {
        val room = _currentRoom.value ?: return
        sendSystemMessage(room.id, "$currentUserName left the room")
        _currentRoom.value = null
    }

    fun createRoomWithId(
        roomId: String,
        title: String,
        roomType: RoomType,
        category: String,
        tags: List<String>,
        hostUid: String,
        isPrivate: Boolean = false,
        password: String? = null
    ): LiveRoom {
        val initialSeats = if (roomType == RoomType.MULTI_GUEST || roomType == RoomType.AUDIO_STAGE) {
            listOf(
                Seat(id = 1, occupied = true, assignedParticipant = hostUid, participantName = "$currentUserName (Host)", avatarEmoji = currentUserAvatar, role = "HOST"),
                Seat(id = 2, occupied = false, locked = false),
                Seat(id = 3, occupied = false, locked = false),
                Seat(id = 4, occupied = false, locked = false),
                Seat(id = 5, occupied = false, locked = false),
                Seat(id = 6, occupied = false, locked = false)
            )
        } else emptyList()

        val currentProfile = _currentUserProfile.value
        val now = System.currentTimeMillis()
        val newRoom = LiveRoom(
            id = roomId,
            title = title,
            creatorIdentity = hostUid,
            hostId = hostUid,
            hostName = currentUserName,
            hostAvatar = currentUserAvatar,
            hostAvatarUrl = currentProfile.avatarUrl,
            hostGender = currentProfile.gender,
            roomCoverUrl = currentProfile.avatarUrl,
            coverStyle = "FULL_BACKDROP",
            roomType = roomType,
            category = category,
            tags = tags,
            isPrivate = isPrivate,
            password = password,
            viewerCount = 1,
            likesCount = 0,
            isLive = true,
            status = "LIVE",
            createdAt = now,
            lastHeartbeatAt = now,
            seats = initialSeats
        )

        _rooms.update { listOf(newRoom) + (it.filterNot { r -> r.id == roomId }) }
        _currentRoom.value = newRoom
        _participants.value = mapOf(
            roomId to listOf(Participant(hostUid, currentUserName, currentUserAvatar, ParticipantRole.HOST))
        )
        sendSystemMessage(roomId, "Broadcast Studio live stream initiated! 🔴")
        return newRoom
    }

    fun createRoom(
        title: String,
        roomType: RoomType,
        category: String,
        tags: List<String>,
        isPrivate: Boolean = false,
        password: String? = null
    ) {
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: authRepository.currentUser.value?.uid
            ?: currentUserIdentity
        val newRoomId = "room_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        createRoomWithId(
            roomId = newRoomId,
            title = title,
            roomType = roomType,
            category = category,
            tags = tags,
            hostUid = authUid,
            isPrivate = isPrivate,
            password = password
        )
    }

    fun updateRoomCover(roomId: String, coverUrl: String, coverStyle: String = "FULL_BACKDROP") {
        _rooms.update { list ->
            list.map { room ->
                if (room.id == roomId) {
                    room.copy(roomCoverUrl = coverUrl, coverStyle = coverStyle)
                } else room
            }
        }
        if (_currentRoom.value?.id == roomId) {
            _currentRoom.value = _currentRoom.value?.copy(roomCoverUrl = coverUrl, coverStyle = coverStyle)
        }
    }

    fun updateBroadcasterProfilePic(userId: String, newAvatarUrl: String) {
        if (userId == currentUserIdentity) {
            _currentUserProfile.update { it.copy(avatarUrl = newAvatarUrl) }
        }
        _userProfiles.update { map ->
            val existing = map[userId]
            if (existing != null) {
                map + (userId to existing.copy(avatarUrl = newAvatarUrl))
            } else map
        }
        // Also update any live rooms hosted by this broadcaster
        _rooms.update { list ->
            list.map { room ->
                if (room.creatorIdentity == userId) {
                    room.copy(
                        hostAvatarUrl = newAvatarUrl,
                        roomCoverUrl = if (room.roomCoverUrl == room.hostAvatarUrl || room.roomCoverUrl.isNullOrBlank()) newAvatarUrl else room.roomCoverUrl
                    )
                } else room
            }
        }
        if (_currentRoom.value?.creatorIdentity == userId) {
            _currentRoom.value = _currentRoom.value?.copy(
                hostAvatarUrl = newAvatarUrl,
                roomCoverUrl = if (_currentRoom.value?.roomCoverUrl == _currentRoom.value?.hostAvatarUrl || _currentRoom.value?.roomCoverUrl.isNullOrBlank()) newAvatarUrl else _currentRoom.value?.roomCoverUrl
            )
        }
    }

    fun sendTextMessage(roomId: String, text: String, mention: String? = null) {
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderIdentity = currentUserIdentity,
            senderName = currentUserName,
            senderAvatar = currentUserAvatar,
            text = if (mention != null) "@$mention $text" else text,
            type = MessageType.TEXT
        )

        _chatMessages.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to (list + msg))
        }
    }

    fun sendGift(roomId: String, gift: Gift, count: Int = 1) {
        val totalCost = gift.coinCost * count
        if (_userCoinBalance.value < totalCost) return

        _userCoinBalance.update { it - totalCost }

        // Host receives beans (100% of coin cost added to host beans)
        val room = _rooms.value.find { it.id == roomId }
        if (room != null && room.creatorIdentity == currentUserIdentity) {
            _userBeansBalance.update { it + totalCost }
        }

        // Increase user XP & Wealth
        _currentUserProfile.update { p ->
            val addedXp = totalCost * 3
            val newXp = p.userXp + addedXp
            val newLevel = if (newXp >= p.nextLevelXp) p.userLevel + 1 else p.userLevel
            val nextXp = if (newXp >= p.nextLevelXp) p.nextLevelXp + 3000 else p.nextLevelXp
            val newWealth = p.wealthLevel + maxOf(1, totalCost / 500)
            p.copy(
                userXp = newXp,
                userLevel = newLevel,
                nextLevelXp = nextXp,
                wealthLevel = newWealth
            )
        }

        // Add Transaction
        val tx = WalletTransaction(
            id = "tx_" + UUID.randomUUID().toString().take(6),
            title = "Sent ${count}x ${gift.name}",
            detail = "In stream: ${room?.title ?: "Live Room"}",
            coinAmount = -totalCost,
            type = TransactionType.GIFT_SENT,
            status = TransactionStatus.COMPLETED,
            timestampFormatted = "Just now",
            iconEmoji = gift.iconEmoji
        )
        _walletTransactions.update { listOf(tx) + it }

        val giftMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderIdentity = currentUserIdentity,
            senderName = currentUserName,
            senderAvatar = currentUserAvatar,
            text = "Sent $count x ${gift.name} ${gift.iconEmoji}!",
            type = MessageType.GIFT,
            gift = gift,
            giftCount = count
        )

        _chatMessages.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to (list + giftMessage))
        }

        // Floating animation queue
        _activeFloatingGifts.update { it + giftMessage }
        scope.launch {
            delay(4000)
            _activeFloatingGifts.update { list -> list.filter { it.id != giftMessage.id } }
        }

        // Update room score & PK state
        _rooms.update { roomsList ->
            roomsList.map { r ->
                if (r.id == roomId) {
                    val updatedPk = if (r.pkState.isActive) {
                        r.pkState.copy(myScore = r.pkState.myScore + totalCost)
                    } else r.pkState
                    val updatedTeam = if (r.teamState.isActive) {
                        r.teamState.copy(myTeamScore = r.teamState.myTeamScore + totalCost)
                    } else r.teamState

                    r.copy(
                        likesCount = r.likesCount + (totalCost * 2),
                        pkState = updatedPk,
                        teamState = updatedTeam
                    )
                } else r
            }
        }

        _currentRoom.update { current ->
            if (current?.id == roomId) {
                val updatedPk = if (current.pkState.isActive) {
                    current.pkState.copy(myScore = current.pkState.myScore + totalCost)
                } else current.pkState
                val updatedTeam = if (current.teamState.isActive) {
                    current.teamState.copy(myTeamScore = current.teamState.myTeamScore + totalCost)
                } else current.teamState
                current.copy(
                    likesCount = current.likesCount + (totalCost * 2),
                    pkState = updatedPk,
                    teamState = updatedTeam
                )
            } else current
        }
    }

    fun sendLike(roomId: String) {
        _rooms.update { list ->
            list.map { r -> if (r.id == roomId) r.copy(likesCount = r.likesCount + 1) else r }
        }
        _currentRoom.update { current ->
            if (current?.id == roomId) current.copy(likesCount = current.likesCount + 1) else current
        }
    }

    fun toggleChatEnabled(roomId: String, enabled: Boolean) {
        _rooms.update { list ->
            list.map { r -> if (r.id == roomId) r.copy(enableChat = enabled) else r }
        }
        _currentRoom.update { current ->
            if (current?.id == roomId) current.copy(enableChat = enabled) else current
        }
        sendSystemMessage(roomId, if (enabled) "Host enabled the chat" else "Host paused the chat")
    }

    fun requestToPresent(roomId: String, seatId: Int = -1) {
        _participants.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to list.map { p ->
                if (p.identity == currentUserIdentity) p.copy(isReqToPresent = true, isRequestedToCall = true)
                else p
            })
        }
        sendSystemMessage(roomId, "$currentUserName raised hand to join stage ✋")
    }

    fun cancelRequestToPresent(roomId: String) {
        _participants.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to list.map { p ->
                if (p.identity == currentUserIdentity) p.copy(isReqToPresent = false, isRequestedToCall = false)
                else p
            })
        }
    }

    fun inviteParticipantToStage(roomId: String, participantIdentity: String, targetSeatId: Int = -1) {
        val room = _rooms.value.find { it.id == roomId } ?: return
        val participant = (_participants.value[roomId] ?: emptyList()).find { it.identity == participantIdentity } ?: return

        val availableSeat = if (targetSeatId != -1) {
            room.seats.find { it.id == targetSeatId && !it.occupied && !it.locked }
        } else {
            room.seats.find { !it.occupied && !it.locked }
        }

        if (availableSeat != null) {
            val updatedSeats = room.seats.map { s ->
                if (s.id == availableSeat.id) {
                    s.copy(
                        occupied = true,
                        assignedParticipant = participantIdentity,
                        participantName = participant.name,
                        avatarEmoji = participant.avatar,
                        role = "SPEAKER"
                    )
                } else s
            }

            _rooms.update { list ->
                list.map { r -> if (r.id == roomId) r.copy(seats = updatedSeats) else r }
            }
            _currentRoom.update { current ->
                if (current?.id == roomId) current.copy(seats = updatedSeats) else current
            }

            _participants.update { map ->
                val list = map[roomId] ?: emptyList()
                map + (roomId to list.map { p ->
                    if (p.identity == participantIdentity) {
                        p.copy(
                            role = ParticipantRole.STAGE_SPEAKER,
                            seatId = availableSeat.id,
                            isReqToPresent = false,
                            isRequestedToCall = false
                        )
                    } else p
                })
            }

            sendSystemMessage(roomId, "${participant.name} joined stage seat #${availableSeat.id} 🎤")
        }
    }

    fun removeParticipantFromStage(roomId: String, participantIdentity: String) {
        val room = _rooms.value.find { it.id == roomId } ?: return
        val updatedSeats = room.seats.map { s ->
            if (s.assignedParticipant == participantIdentity) {
                s.copy(occupied = false, assignedParticipant = null, participantName = null, avatarEmoji = "👤", role = "GUEST")
            } else s
        }

        _rooms.update { list ->
            list.map { r -> if (r.id == roomId) r.copy(seats = updatedSeats) else r }
        }
        _currentRoom.update { current ->
            if (current?.id == roomId) current.copy(seats = updatedSeats) else current
        }

        _participants.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to list.map { p ->
                if (p.identity == participantIdentity) {
                    p.copy(role = ParticipantRole.VIEWER, seatId = -1)
                } else p
            })
        }
    }

    fun toggleSeatLock(roomId: String, seatId: Int) {
        val room = _rooms.value.find { it.id == roomId } ?: return
        val updatedSeats = room.seats.map { s ->
            if (s.id == seatId) s.copy(locked = !s.locked) else s
        }
        _rooms.update { list ->
            list.map { r -> if (r.id == roomId) r.copy(seats = updatedSeats) else r }
        }
        _currentRoom.update { current ->
            if (current?.id == roomId) current.copy(seats = updatedSeats) else current
        }
    }

    fun makeAdmin(roomId: String, participantIdentity: String) {
        _participants.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to list.map { p ->
                if (p.identity == participantIdentity) p.copy(role = ParticipantRole.ADMIN) else p
            })
        }
        sendSystemMessage(roomId, "Participant made room moderator 🛡️")
    }

    fun removeAdmin(roomId: String, participantIdentity: String) {
        _participants.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to list.map { p ->
                if (p.identity == participantIdentity) p.copy(role = ParticipantRole.VIEWER) else p
            })
        }
    }

    fun muteParticipantAudio(roomId: String, participantIdentity: String, muted: Boolean) {
        _participants.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to list.map { p ->
                if (p.identity == participantIdentity) p.copy(isMutedAudio = muted) else p
            })
        }
    }

    fun blockParticipant(roomId: String, participantIdentity: String) {
        _participants.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to list.map { p ->
                if (p.identity == participantIdentity) p.copy(isBlocked = true) else p
            })
        }
        sendSystemMessage(roomId, "Participant blocked from room")
    }

    fun setFilter(filter: BeautifyFilter) {
        _currentFilter.value = filter
    }

    fun toggleMic() { _isMicMuted.value = !_isMicMuted.value }
    fun toggleVideo() { _isVideoMuted.value = !_isVideoMuted.value }
    fun flipCamera() { _isFrontCamera.value = !_isFrontCamera.value }
    fun playSfx(sfx: String) { _lastPlayedSfx.value = sfx }

    private fun sendSystemMessage(roomId: String, text: String) {
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderIdentity = "system",
            senderName = "System",
            text = text,
            type = MessageType.SYSTEM
        )
        _chatMessages.update { map ->
            val list = map[roomId] ?: emptyList()
            map + (roomId to (list + msg))
        }
    }

    private fun startLiveSimulation() {
        scope.launch {
            val randomSenders = listOf(
                Pair("Aurora_Glow", "✨"),
                Pair("CyberKnight", "⚡"),
                Pair("VibeMaster", "🎧"),
                Pair("PixelNova", "👾"),
                Pair("DragonSlayer", "🐉"),
                Pair("TokyoDreamer", "🌸")
            )

            val randomComments = listOf(
                "Incredible vibe right here! 💖",
                "Let's win this battle!! 🚀",
                "Audio is so soothing ☕",
                "Send more gifts everyone!!",
                "Can you play that synth track again? 🔥",
                "Greeting from Tokyo! 🗾",
                "This 60fps feed is buttery smooth ✨",
                "Team Alpha for the win! 🛡️"
            )

            while (true) {
                delay(3500)
                val current = _currentRoom.value
                if (current != null && current.isLive && current.enableChat) {
                    val sender = randomSenders.random()
                    val isGiftEvent = (1..5).random() == 1

                    if (isGiftEvent) {
                        val gift = PredefinedGifts.ALL_GIFTS.take(5).random()
                        val count = listOf(1, 3, 5, 10).random()
                        val msg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            senderIdentity = "sim_${sender.first}",
                            senderName = sender.first,
                            senderAvatar = sender.second,
                            text = "Sent $count x ${gift.name} ${gift.iconEmoji}!",
                            type = MessageType.GIFT,
                            gift = gift,
                            giftCount = count
                        )
                        _chatMessages.update { map ->
                            val list = map[current.id] ?: emptyList()
                            map + (current.id to (list + msg))
                        }
                        if (current.pkState.isActive) {
                            val rivalDelta = (100..400).random()
                            _currentRoom.update { r ->
                                r?.copy(
                                    pkState = r.pkState.copy(
                                        myScore = r.pkState.myScore + (gift.coinCost * count),
                                        targetScore = r.pkState.targetScore + rivalDelta
                                    )
                                )
                            }
                        }
                    } else {
                        val comment = randomComments.random()
                        val msg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            senderIdentity = "sim_${sender.first}",
                            senderName = sender.first,
                            senderAvatar = sender.second,
                            text = comment,
                            type = MessageType.TEXT
                        )
                        _chatMessages.update { map ->
                            val list = map[current.id] ?: emptyList()
                            map + (current.id to (list + msg))
                        }
                    }

                    _currentRoom.update { r ->
                        r?.copy(
                            viewerCount = r.viewerCount + (-2..5).random(),
                            likesCount = r.likesCount + (1..8).random()
                        )
                    }
                }
            }
        }
    }

    // JSON Helper Methods for Session Persistence
    private fun userProfileToJson(profile: UserProfile): String {
        val json = JSONObject()
        json.put("userId", profile.userId)
        json.put("username", profile.username)
        json.put("displayName", profile.displayName)
        json.put("avatarEmoji", profile.avatarEmoji)
        json.put("avatarUrl", profile.avatarUrl ?: "")
        json.put("coverGradientIndex", profile.coverGradientIndex)
        json.put("bio", profile.bio)
        json.put("gender", profile.gender)
        json.put("location", profile.location)
        json.put("userLevel", profile.userLevel)
        json.put("userXp", profile.userXp)
        json.put("nextLevelXp", profile.nextLevelXp)
        json.put("wealthLevel", profile.wealthLevel)
        json.put("hostLevel", profile.hostLevel)
        json.put("vipTier", profile.vipTier.name)
        json.put("vipExpiresTimestamp", profile.vipExpiresTimestamp ?: 0L)
        json.put("followersCount", profile.followersCount)
        json.put("followingCount", profile.followingCount)
        json.put("likesCount", profile.likesCount)
        json.put("diamondsEarnedTotal", profile.diamondsEarnedTotal)
        json.put("giftsReceivedTotal", profile.giftsReceivedTotal)
        json.put("liveStreamsCount", profile.liveStreamsCount)
        json.put("badges", JSONArray(profile.badges))
        json.put("isFollowedByCurrentUser", profile.isFollowedByCurrentUser)
        json.put("isBlocked", profile.isBlocked)
        json.put("isLiveNow", profile.isLiveNow)
        json.put("currentRoomId", profile.currentRoomId ?: "")
        json.put("executiveRole", profile.executiveRole ?: "")
        json.put("whatsappNumber", profile.whatsappNumber ?: "")
        json.put("whatsappDirectUrl", profile.whatsappDirectUrl ?: "")
        json.put("followingUserIds", JSONArray(profile.followingUserIds))
        json.put("isTopHost", profile.isTopHost)
        return json.toString()
    }

    private fun jsonToUserProfile(jsonStr: String): UserProfile? {
        return try {
            val json = JSONObject(jsonStr)
            val badgesArr = json.optJSONArray("badges")
            val badgesList = mutableListOf<String>()
            if (badgesArr != null) {
                for (i in 0 until badgesArr.length()) {
                    badgesList.add(badgesArr.getString(i))
                }
            }
            val followingArr = json.optJSONArray("followingUserIds")
            val followingList = mutableListOf<String>()
            if (followingArr != null) {
                for (i in 0 until followingArr.length()) {
                    followingList.add(followingArr.getString(i))
                }
            }
            val vipTierStr = json.optString("vipTier", "NONE")
            val vipTierEnum = try { VipTier.valueOf(vipTierStr) } catch (e: Exception) { VipTier.NONE }

            UserProfile(
                userId = json.optString("userId", "user_me"),
                username = json.optString("username", "user"),
                displayName = json.optString("displayName", "User"),
                avatarEmoji = json.optString("avatarEmoji", "🚀"),
                avatarUrl = json.optString("avatarUrl").ifEmpty { null },
                coverGradientIndex = json.optInt("coverGradientIndex", 0),
                bio = json.optString("bio", "Live streaming enthusiast 🚀"),
                gender = json.optString("gender", "Unspecified"),
                location = json.optString("location", "Global 🌍"),
                userLevel = json.optInt("userLevel", 1),
                userXp = json.optInt("userXp", 0),
                nextLevelXp = json.optInt("nextLevelXp", 1000),
                wealthLevel = json.optInt("wealthLevel", 1),
                hostLevel = json.optInt("hostLevel", 1),
                vipTier = vipTierEnum,
                vipExpiresTimestamp = if (json.optLong("vipExpiresTimestamp", 0L) > 0) json.optLong("vipExpiresTimestamp") else null,
                followersCount = json.optInt("followersCount", 0),
                followingCount = json.optInt("followingCount", 3),
                likesCount = json.optInt("likesCount", 0),
                diamondsEarnedTotal = json.optInt("diamondsEarnedTotal", 0),
                giftsReceivedTotal = json.optInt("giftsReceivedTotal", 0),
                liveStreamsCount = json.optInt("liveStreamsCount", 0),
                badges = if (badgesList.isNotEmpty()) badgesList else listOf("Verified User"),
                isFollowedByCurrentUser = json.optBoolean("isFollowedByCurrentUser", false),
                isBlocked = json.optBoolean("isBlocked", false),
                isLiveNow = json.optBoolean("isLiveNow", false),
                currentRoomId = json.optString("currentRoomId").ifEmpty { null },
                executiveRole = json.optString("executiveRole").ifEmpty { null },
                whatsappNumber = json.optString("whatsappNumber").ifEmpty { null },
                whatsappDirectUrl = json.optString("whatsappDirectUrl").ifEmpty { null },
                followingUserIds = followingList,
                isTopHost = json.optBoolean("isTopHost", false)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun userProfilesMapToJson(map: Map<String, UserProfile>): String {
        val jsonObj = JSONObject()
        map.forEach { (key, profile) ->
            jsonObj.put(key, JSONObject(userProfileToJson(profile)))
        }
        return jsonObj.toString()
    }

    private fun jsonToUserProfilesMap(jsonStr: String): Map<String, UserProfile> {
        val resultMap = mutableMapOf<String, UserProfile>()
        try {
            val jsonObj = JSONObject(jsonStr)
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val profileObj = jsonObj.optJSONObject(key)
                if (profileObj != null) {
                    val profile = jsonToUserProfile(profileObj.toString())
                    if (profile != null) {
                        resultMap[key] = profile
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultMap
    }
}

package com.example.zyvo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zyvo.ui.components.*
import com.example.zyvo.ui.screens.*
import com.example.zyvo.ui.theme.*
import com.example.zyvo.ui.viewmodel.LiveStreamViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZyvoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    ZyvoApp()
                }
            }
        }
    }
}

@Composable
fun ZyvoApp(
    viewModel: LiveStreamViewModel = viewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentRoom by viewModel.currentRoom.collectAsState()
    val filteredRooms by viewModel.filteredRooms.collectAsState()
    val followedRooms by viewModel.followedRooms.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val userProfiles by viewModel.userProfiles.collectAsState()
    val userCoinBalance by viewModel.userCoinBalance.collectAsState()
    val userBeansBalance by viewModel.userBeansBalance.collectAsState()
    val walletTransactions by viewModel.walletTransactions.collectAsState()
    val followingUserIds by viewModel.followingUserIds.collectAsState()
    val blockedUserIds by viewModel.blockedUserIds.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val directMessages by viewModel.directMessages.collectAsState()

    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedUserProfile by viewModel.selectedUserProfile.collectAsState()
    val activeDmPeerUserId by viewModel.activeDmPeerUserId.collectAsState()

    val showVipStoreDialog by viewModel.showVipStoreDialog.collectAsState()
    val showRechargeDialog by viewModel.showRechargeDialog.collectAsState()
    val showWithdrawalDialog by viewModel.showWithdrawalDialog.collectAsState()
    val showTransactionsDialog by viewModel.showTransactionsDialog.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showCreateRoomSheet by viewModel.showCreateRoomSheet.collectAsState()

    val authErrorMessage by viewModel.authErrorMessage.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    var activeSubView by remember { mutableStateOf<String?>(null) } // "wallet", "vip_center", "rankings"

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.initPersistence(context.applicationContext)
    }

    val followingProfiles = remember(userProfiles, followingUserIds) {
        userProfiles.values.filter { followingUserIds.contains(it.userId) }
    }

    val blockedProfiles = remember(userProfiles, blockedUserIds) {
        userProfiles.values.filter { blockedUserIds.contains(it.userId) }
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { name, email, avatar, photoUrl ->
                viewModel.loginWithGoogle(null, name, email, avatar, photoUrl)
            },
            onGoogleSignIn = { idToken, name, email, avatar, photoUrl ->
                viewModel.loginWithGoogle(idToken, name, email, avatar, photoUrl)
            },
            onCreateCustomProfile = { name, username, email, avatar, photoUrl, bio, gender, location ->
                viewModel.createCustomProfileAndLogin(name, username, email, avatar, photoUrl, bio, gender, location)
            },
            onEmailSignIn = { email, password ->
                viewModel.signInWithEmail(email, password)
            },
            onEmailSignUp = { email, password, displayName, username, avatar ->
                viewModel.signUpWithEmail(email, password, displayName, username, avatar)
            },
            onAnonymousSignIn = { displayName, username, avatar ->
                viewModel.signInAnonymously(displayName, username, avatar)
            },
            authErrorMessage = authErrorMessage,
            isLoading = isAuthLoading
        )
    } else {
        BackHandler(
            enabled = currentRoom != null || activeSubView != null || activeDmPeerUserId != null || selectedUserProfile != null || showCreateRoomSheet || showVipStoreDialog || showRechargeDialog || showWithdrawalDialog || showTransactionsDialog || showSettingsDialog || selectedTab != 0
        ) {
            when {
                activeDmPeerUserId != null -> viewModel.closeDmChat()
                selectedUserProfile != null -> viewModel.closeUserProfile()
                showCreateRoomSheet -> viewModel.setShowCreateRoomSheet(false)
                showVipStoreDialog -> viewModel.setShowVipStoreDialog(false)
                showRechargeDialog -> viewModel.setShowRechargeDialog(false)
                showWithdrawalDialog -> viewModel.setShowWithdrawalDialog(false)
                showTransactionsDialog -> viewModel.setShowTransactionsDialog(false)
                showSettingsDialog -> viewModel.setShowSettingsDialog(false)
                currentRoom != null -> viewModel.leaveRoom()
                activeSubView != null -> activeSubView = null
                selectedTab != 0 -> viewModel.setSelectedTab(0)
            }
        }

        Scaffold(
            containerColor = DarkBackground,
        bottomBar = {
            if (currentRoom == null && activeSubView == null) {
                ZyvoBottomBar(
                    selectedTab = selectedTab,
                    onSelectTab = {
                        activeSubView = null
                        viewModel.setSelectedTab(it)
                    },
                    onGoLiveClick = { viewModel.setShowCreateRoomSheet(true) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentRoom != null) {
                // Active Live Room Studio / Audience view
                LiveRoomScreen(
                    room = currentRoom!!,
                    viewModel = viewModel,
                    onLeaveRoom = { viewModel.leaveRoom() }
                )
            } else if (activeSubView == "wallet") {
                WalletScreen(
                    coinBalance = userCoinBalance,
                    beansBalance = userBeansBalance,
                    onBack = { activeSubView = null },
                    onOpenRecharge = { viewModel.setShowRechargeDialog(true) },
                    onOpenVip = { activeSubView = "vip_center" },
                    onOpenWithdrawal = { viewModel.setShowWithdrawalDialog(true) },
                    onOpenTransactions = { viewModel.setShowTransactionsDialog(true) }
                )
            } else if (activeSubView == "vip_center") {
                VipCenterScreen(
                    currentVipTier = currentUserProfile.vipTier,
                    coinBalance = userCoinBalance,
                    onBack = { activeSubView = null },
                    onBuyVip = { tier, months, cost -> viewModel.buyVipPackage(tier, months, cost) },
                    onOpenRecharge = { viewModel.setShowRechargeDialog(true) }
                )
            } else if (activeSubView == "rankings") {
                RankingsScreen(
                    onUserClick = { userId -> viewModel.openUserProfile(userId) },
                    onFollowUser = { userId -> viewModel.followUser(userId) },
                    onUnfollowUser = { userId -> viewModel.unfollowUser(userId) }
                )
            } else if (selectedUserProfile != null) {
                val profile = selectedUserProfile!!
                val isSelf = profile.userId == currentUserProfile.userId
                UserProfileScreen(
                    userProfile = profile,
                    coinBalance = if (isSelf) userCoinBalance else profile.diamondsEarnedTotal,
                    beansBalance = if (isSelf) userBeansBalance else 85230,
                    followingCount = profile.followingCount,
                    isOwnProfile = isSelf,
                    isFollowing = profile.isFollowedByCurrentUser,
                    onFollowToggle = {
                        if (profile.isFollowedByCurrentUser) {
                            viewModel.unfollowUser(profile.userId)
                        } else {
                            viewModel.followUser(profile.userId)
                        }
                    },
                    onOpenDm = {
                        val uid = profile.userId
                        viewModel.closeUserProfile()
                        activeSubView = null
                        viewModel.openDmChat(uid)
                    },
                    onBack = { viewModel.closeUserProfile() },
                    onOpenVipStore = { activeSubView = "vip_center" },
                    onOpenRecharge = { activeSubView = "wallet" },
                    onOpenWithdrawal = { viewModel.setShowWithdrawalDialog(true) },
                    onOpenTransactions = { viewModel.setShowTransactionsDialog(true) },
                    onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                    onOpenAnalytics = { activeSubView = "rankings" },
                    onOpenUserDetail = { userId -> viewModel.openUserProfile(userId) },
                    onLogout = if (isSelf) { { viewModel.logout() } } else null
                )
            } else {
                when (selectedTab) {
                    0 -> HomeScreen(
                        rooms = filteredRooms,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        coinBalance = userCoinBalance,
                        ceoProfile = userProfiles["ceo_rayan"],
                        coFounderProfile = userProfiles["co_founder_alpha"],
                        ansharahProfile = userProfiles["ansharah_gahni"],
                        onSelectCategory = { viewModel.setSelectedCategory(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onRoomClick = { room -> viewModel.joinRoom(room.id) },
                        onGoLiveClick = { viewModel.setShowCreateRoomSheet(true) },
                        onOpenAnalyticsClick = { activeSubView = "rankings" },
                        onOpenUserDetail = { userId -> viewModel.openUserProfile(userId) },
                        onNavigateToLive = { viewModel.setSelectedTab(1) },
                        onOpenDm = { userId -> viewModel.openDmChat(userId) },
                        onRefreshRooms = { viewModel.refreshRoomsObservation() }
                    )
                    1 -> FollowingScreen(
                        followedRooms = followedRooms,
                        followingProfiles = followingProfiles,
                        conversations = conversations,
                        onRoomClick = { room -> viewModel.joinRoom(room.id) },
                        onUserClick = { userId -> viewModel.openUserProfile(userId) },
                        onOpenDm = { userId -> viewModel.openDmChat(userId) }
                    )
                    3 -> ChatScreen(
                        conversations = conversations,
                        followingProfiles = followingProfiles,
                        onOpenDm = { userId -> viewModel.openDmChat(userId) },
                        onUserClick = { userId -> viewModel.openUserProfile(userId) }
                    )
                    4 -> UserProfileScreen(
                        userProfile = currentUserProfile,
                        coinBalance = userCoinBalance,
                        beansBalance = userBeansBalance,
                        followingCount = followingUserIds.size,
                        isOwnProfile = true,
                        onOpenVipStore = { activeSubView = "vip_center" },
                        onOpenRecharge = { activeSubView = "wallet" },
                        onOpenWithdrawal = { viewModel.setShowWithdrawalDialog(true) },
                        onOpenTransactions = { viewModel.setShowTransactionsDialog(true) },
                        onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                        onOpenAnalytics = { activeSubView = "rankings" },
                        onOpenUserDetail = { userId -> viewModel.openUserProfile(userId) },
                        onLogout = { viewModel.logout() }
                    )
                    else -> HomeScreen(
                        rooms = filteredRooms,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        coinBalance = userCoinBalance,
                        ceoProfile = userProfiles["ceo_rayan"],
                        coFounderProfile = userProfiles["co_founder_alpha"],
                        ansharahProfile = userProfiles["ansharah_gahni"],
                        onSelectCategory = { viewModel.setSelectedCategory(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onRoomClick = { room -> viewModel.joinRoom(room.id) },
                        onGoLiveClick = { viewModel.setShowCreateRoomSheet(true) },
                        onOpenAnalyticsClick = { activeSubView = "rankings" },
                        onOpenUserDetail = { userId -> viewModel.openUserProfile(userId) },
                        onOpenDm = { userId -> viewModel.openDmChat(userId) }
                    )
                }
            }

            // Global Dialogs & Sheets
            if (showCreateRoomSheet) {
                GoLiveScreen(
                    onDismiss = { viewModel.setShowCreateRoomSheet(false) },
                    onStartLive = { title, type, category, tags ->
                        viewModel.createRoom(title, type, category, tags, false)
                    }
                )
            }



            if (showVipStoreDialog) {
                VipStoreDialog(
                    userCoinBalance = userCoinBalance,
                    currentVipTier = currentUserProfile.vipTier,
                    onDismiss = { viewModel.setShowVipStoreDialog(false) },
                    onBuyVip = { tier, months, cost -> viewModel.buyVipPackage(tier, months, cost) },
                    onOpenRecharge = {
                        viewModel.setShowVipStoreDialog(false)
                        viewModel.setShowRechargeDialog(true)
                    }
                )
            }

            if (showRechargeDialog) {
                RechargeDialog(
                    userCoinBalance = userCoinBalance,
                    onDismiss = { viewModel.setShowRechargeDialog(false) },
                    onRecharge = { title, coins, bonus, priceUsd, method ->
                        viewModel.rechargeCoins(title, coins, bonus, priceUsd, method)
                    }
                )
            }

            if (showWithdrawalDialog) {
                WithdrawalDialog(
                    userBeansBalance = userBeansBalance,
                    onDismiss = { viewModel.setShowWithdrawalDialog(false) },
                    onWithdraw = { beans, method, account ->
                        viewModel.requestWithdrawal(beans, method, account)
                    }
                )
            }

            if (showTransactionsDialog) {
                TransactionsDialog(
                    transactions = walletTransactions,
                    onDismiss = { viewModel.setShowTransactionsDialog(false) }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    currentUserProfile = currentUserProfile,
                    blockedUsers = blockedProfiles,
                    onDismiss = { viewModel.setShowSettingsDialog(false) },
                    onSaveProfile = { name, bio, gender, loc, avatar ->
                        viewModel.updateProfile(name, bio, gender, loc, avatar)
                    },
                    onUnblockUser = { userId -> viewModel.unblockUser(userId) },
                    onLogout = { viewModel.logout() }
                )
            }

            if (activeDmPeerUserId != null) {
                val peerProfile = userProfiles[activeDmPeerUserId]
                    ?: currentUserProfile
                val peerMessages = directMessages[activeDmPeerUserId] ?: emptyList()

                DirectMessageDialog(
                    peerUser = peerProfile,
                    messages = peerMessages,
                    onDismiss = { viewModel.closeDmChat() },
                    onSendMessage = { text -> viewModel.sendDirectMessage(activeDmPeerUserId!!, text) }
                )
            }
        }
    }
}
}

@Composable
fun ZyvoBottomBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onGoLiveClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "go_live_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        color = Color(0xFF0C0A17).copy(alpha = 0.96f),
        tonalElevation = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .testTag("zyvo_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Home
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = selectedTab == 0,
                onClick = { onSelectTab(0) }
            )

            // Tab 1: Live
            BottomNavItem(
                icon = Icons.Default.Videocam,
                label = "Live",
                isSelected = selectedTab == 1,
                onClick = { onSelectTab(1) }
            )

            // Center: Signature Elevated Glowing "Go Live" Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-4).dp),
                contentAlignment = Alignment.Center
            ) {
                // Ambient Glow Aura
                Box(
                    modifier = Modifier
                        .size((52f * pulseScale).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFF007A).copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )

                // Main Central Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF007A), Color(0xFF7209B7), Color(0xFF4CC9F0))
                            )
                        )
                        .border(2.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color.White)), CircleShape)
                        .clickable { onGoLiveClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Go Live",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Tab 3: Chat
            BottomNavItem(
                icon = Icons.Default.Chat,
                label = "Chat",
                isSelected = selectedTab == 3,
                onClick = { onSelectTab(3) },
                badgeText = "12"
            )

            // Tab 4: Profile
            BottomNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = selectedTab == 4,
                onClick = { onSelectTab(4) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeText: String? = null
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFFFF007A) else Color(0xFF8E8A9F),
                modifier = Modifier.size(24.dp)
            )
            if (!badgeText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF007A))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFFFF007A) else Color(0xFF8E8A9F)
        )
    }
}

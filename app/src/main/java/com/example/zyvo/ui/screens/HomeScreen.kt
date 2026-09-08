package com.example.zyvo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.ui.components.AnimatedHostAvatar
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.zyvo.model.LiveRoom
import com.example.zyvo.model.RoomType
import com.example.zyvo.model.UserProfile
import com.example.zyvo.model.VipTier
import com.example.zyvo.ui.components.ExecutiveAvatar
import com.example.zyvo.ui.components.openWhatsAppChat
import com.example.zyvo.ui.theme.*

// Data Models for Home Page Reference Content
data class TopHostItem(
    val id: String,
    val name: String,
    val isVerified: Boolean = true,
    val category: String,
    val viewerCount: String,
    val imageUrl: String,
    val gender: String = "Female",
    val roomType: RoomType = RoomType.SINGLE_LIVE
)

data class PopularHostItem(
    val id: String,
    val name: String,
    val popularity: String,
    val imageUrl: String,
    val isGoldCrown: Boolean = true,
    val hasRose: Boolean = false,
    val gender: String = "Female"
)

@Composable
fun HomeScreen(
    rooms: List<LiveRoom>,
    selectedCategory: RoomType?,
    searchQuery: String,
    coinBalance: Int,
    ceoProfile: UserProfile? = null,
    coFounderProfile: UserProfile? = null,
    ansharahProfile: UserProfile? = null,
    onSelectCategory: (RoomType?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRoomClick: (LiveRoom) -> Unit,
    onGoLiveClick: () -> Unit,
    onOpenAnalyticsClick: () -> Unit,
    onOpenUserDetail: ((String) -> Unit)? = null,
    onNavigateToLive: (() -> Unit)? = null,
    onOpenDm: ((String) -> Unit)? = null,
    onRefreshRooms: (() -> Unit)? = null
) {
    LaunchedEffect(Unit) {
        onRefreshRooms?.invoke()
    }

    var showExecutiveSheet by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showDailyTaskDialog by remember { mutableStateOf(false) }
    var showTopLiveExplorer by remember { mutableStateOf(false) }
    var showPkExplorer by remember { mutableStateOf(false) }
    var showAudioExplorer by remember { mutableStateOf(false) }
    var showNewExplorer by remember { mutableStateOf(false) }

    if (showNewExplorer) {
        TopLiveExplorerScreen(
            rooms = rooms,
            initialCategory = "New",
            headerBadgeText = "NEW CREATORS",
            screenTitle = "NEW CREATOR ROOMS",
            ceoProfile = ceoProfile,
            coFounderProfile = coFounderProfile,
            ansharahProfile = ansharahProfile,
            onBack = { showNewExplorer = false },
            onRoomClick = { room ->
                showNewExplorer = false
                onRoomClick(room)
            },
            onOpenUserDetail = { uid ->
                showNewExplorer = false
                onOpenUserDetail?.invoke(uid)
            },
            onOpenAnalyticsClick = onOpenAnalyticsClick,
            onGoLiveClick = onGoLiveClick,
            onOpenDm = onOpenDm
        )
        return
    }

    if (showAudioExplorer) {
        TopLiveExplorerScreen(
            rooms = rooms,
            initialCategory = "Audio",
            headerBadgeText = "AUDIO STAGE",
            screenTitle = "AUDIO & MULTI ROOMS",
            ceoProfile = ceoProfile,
            coFounderProfile = coFounderProfile,
            ansharahProfile = ansharahProfile,
            onBack = { showAudioExplorer = false },
            onRoomClick = { room ->
                showAudioExplorer = false
                onRoomClick(room)
            },
            onOpenUserDetail = { uid ->
                showAudioExplorer = false
                onOpenUserDetail?.invoke(uid)
            },
            onOpenAnalyticsClick = onOpenAnalyticsClick,
            onGoLiveClick = onGoLiveClick,
            onOpenDm = onOpenDm
        )
        return
    }

    if (showPkExplorer) {
        TopLiveExplorerScreen(
            rooms = rooms,
            initialCategory = "PK Battle",
            headerBadgeText = "PK ARENA",
            screenTitle = "PK BATTLE ARENAS",
            ceoProfile = ceoProfile,
            coFounderProfile = coFounderProfile,
            ansharahProfile = ansharahProfile,
            onBack = { showPkExplorer = false },
            onRoomClick = { room ->
                showPkExplorer = false
                onRoomClick(room)
            },
            onOpenUserDetail = { uid ->
                showPkExplorer = false
                onOpenUserDetail?.invoke(uid)
            },
            onOpenAnalyticsClick = onOpenAnalyticsClick,
            onGoLiveClick = onGoLiveClick,
            onOpenDm = onOpenDm
        )
        return
    }

    if (showTopLiveExplorer) {
        TopLiveExplorerScreen(
            rooms = rooms,
            initialCategory = "All",
            headerBadgeText = "TOP LIVE",
            screenTitle = "TOP LIVE ROOMS",
            ceoProfile = ceoProfile,
            coFounderProfile = coFounderProfile,
            ansharahProfile = ansharahProfile,
            onBack = { showTopLiveExplorer = false },
            onRoomClick = { room ->
                showTopLiveExplorer = false
                onRoomClick(room)
            },
            onOpenUserDetail = { uid ->
                showTopLiveExplorer = false
                onOpenUserDetail?.invoke(uid)
            },
            onOpenAnalyticsClick = onOpenAnalyticsClick,
            onGoLiveClick = onGoLiveClick,
            onOpenDm = onOpenDm
        )
        return
    }

    // Reference Data items matching the design source of truth
    val topLiveHosts = remember {
        listOf(
            TopHostItem(
                id = "nusrat_jahan",
                name = "Nusrat Jahan",
                category = "❤️ Let's Talk",
                viewerCount = "12.5K",
                imageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80"
            ),
            TopHostItem(
                id = "maisha",
                name = "Maisha",
                category = "🌊 Good Vibes ✨",
                viewerCount = "8.7K",
                imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&auto=format&fit=crop&q=80"
            ),
            TopHostItem(
                id = "ayesha_live",
                name = "Ayesha Live",
                category = "🎵 Music Live",
                viewerCount = "9.2K",
                imageUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&auto=format&fit=crop&q=80"
            ),
            TopHostItem(
                id = "cute_angel",
                name = "Cute Angel",
                category = "⭐ Happy Time",
                viewerCount = "7.1K",
                imageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&auto=format&fit=crop&q=80"
            )
        )
    }

    val popularHosts = remember {
        listOf(
            PopularHostItem(
                id = "king_of_kings",
                name = "King Of King's",
                popularity = "127.5M",
                imageUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80",
                isGoldCrown = true,
                gender = "Male"
            ),
            PopularHostItem(
                id = "drama_queen",
                name = "Drama Queen",
                popularity = "98.7M",
                imageUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=500&auto=format&fit=crop&q=80",
                isGoldCrown = true
            ),
            PopularHostItem(
                id = "jannatul_islam",
                name = "Jannatul Islam",
                popularity = "75.2M",
                imageUrl = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=500&auto=format&fit=crop&q=80",
                isGoldCrown = true
            ),
            PopularHostItem(
                id = "husnat_smita",
                name = "Husnat Smita",
                popularity = "64.1M",
                imageUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=500&auto=format&fit=crop&q=80",
                isGoldCrown = false
            ),
            PopularHostItem(
                id = "send_rose",
                name = "Send Rose",
                popularity = "58.3M",
                imageUrl = "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=500&auto=format&fit=crop&q=80",
                isGoldCrown = true,
                hasRose = true
            )
        )
    }

    val allSquareRooms = remember(rooms) {
        rooms.map { r ->
            SquareRoomItem(
                id = r.id,
                name = r.hostName,
                category = r.category,
                viewerCount = "${r.viewerCount}",
                imageUrl = r.roomCoverUrl ?: r.hostAvatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
                gender = r.hostGender,
                liveRoomObj = r
            )
        }
    }

    Scaffold(
        containerColor = Color(0xFF090712) // Deep space black/navy
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP HEADER
            item {
                Spacer(modifier = Modifier.height(6.dp))
                HomeTopHeader(
                    onOpenAnalytics = onOpenAnalyticsClick,
                    onSearchClick = { showSearchDialog = true },
                    onNotificationsClick = { showNotificationsDialog = true }
                )
            }

            // 2. HERO PROMOTIONAL BANNER
            item {
                HeroPromoBanner(onClick = { showExecutiveSheet = true })
            }

            // 3. QUICK ACCESS HORIZONTAL CARD
            item {
                QuickAccessCard(
                    selectedCategory = selectedCategory,
                    onSelectCategory = onSelectCategory,
                    onTopLiveClick = { showTopLiveExplorer = true },
                    onPkBattleClick = { showPkExplorer = true },
                    onAudioLiveClick = { showAudioExplorer = true },
                    onNewClick = { showNewExplorer = true }
                )
            }

            // 4. TOP LIVE SECTION
            // 4. TOP LIVE SECTION (REAL-TIME FIRESTORE ACTIVE ROOMS)
            item {
                SectionHeader(
                    icon = "🔥",
                    title = "Live Now",
                    onViewAllClick = { showTopLiveExplorer = true }
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (rooms.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(rooms, key = { r -> r.id }) { room ->
                            DynamicRoomCard(
                                room = room,
                                onClick = { onRoomClick(room) }
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGoLiveClick() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B162B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔴", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("No One Is Live Right Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Tap here to start a live room and broadcast!", color = Color.Gray, fontSize = 12.sp)
                            }
                            Button(
                                onClick = onGoLiveClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Go Live", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 5. POPULAR HOSTS SECTION
            item {
                SectionHeader(
                    icon = "👑",
                    title = "Popular Hosts",
                    onViewAllClick = { onOpenAnalyticsClick() }
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(popularHosts) { host ->
                        PopularHostCrownItem(
                            host = host,
                            onOpenUserDetail = onOpenUserDetail
                        )
                    }
                }
            }

            // 6. ALL LIVE ROOMS (1:1 SQUARE GRID - REAL ACTIVE FIRESTORE ROOMS)
            if (rooms.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SectionHeader(
                        icon = "🔥",
                        title = "All Live Streams",
                        onViewAllClick = { showTopLiveExplorer = true }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(allSquareRooms.chunked(2), key = { pair -> pair.first().id }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { roomItem ->
                            Box(modifier = Modifier.weight(1f)) {
                                SquareRoomCard(
                                    item = roomItem,
                                    onClick = {
                                        if (roomItem.liveRoomObj != null) {
                                            onRoomClick(roomItem.liveRoomObj)
                                        } else {
                                            val match = rooms.find { it.id == roomItem.id }
                                            if (match != null) onRoomClick(match)
                                        }
                                    }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Bottom Spacing for Fixed Navigation Bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Executive Founders Leadership Bottom Sheet
    if (showExecutiveSheet) {
        ExecutiveFoundersModal(
            ceoProfile = ceoProfile,
            coFounderProfile = coFounderProfile,
            ansharahProfile = ansharahProfile,
            onDismiss = { showExecutiveSheet = false },
            onOpenProfile = { userId ->
                showExecutiveSheet = false
                onOpenUserDetail?.invoke(userId)
            },
            onOpenLiveRoom = { roomId ->
                showExecutiveSheet = false
                val matchRoom = rooms.find { it.id == roomId }
                if (matchRoom != null) {
                    onRoomClick(matchRoom)
                } else {
                    onGoLiveClick()
                }
            },
            onOpenChat = { userId ->
                showExecutiveSheet = false
                onOpenDm?.invoke(userId)
            }
        )
    }

    // Search Full Screen Window
    if (showSearchDialog) {
        HomeSearchModal(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            topHosts = topLiveHosts,
            popularHosts = popularHosts,
            rooms = rooms,
            onRoomClick = { room ->
                showSearchDialog = false
                onRoomClick(room)
            },
            onHostClick = { hostId ->
                showSearchDialog = false
                onOpenUserDetail?.invoke(hostId)
            },
            onDismiss = { showSearchDialog = false }
        )
    }

    // Notifications Modal
    if (showNotificationsDialog) {
        HomeNotificationsModal(
            rooms = rooms,
            onRoomClick = { room ->
                showNotificationsDialog = false
                onRoomClick(room)
            },
            onDismiss = { showNotificationsDialog = false },
            onOpenCeoProfile = {
                showNotificationsDialog = false
                onOpenUserDetail?.invoke("ceo_rayan")
            }
        )
    }

    // Daily Tasks Modal
    if (showDailyTaskDialog) {
        HomeDailyTasksModal(
            onDismiss = { showDailyTaskDialog = false },
            onGoLiveClick = {
                showDailyTaskDialog = false
                onGoLiveClick()
            }
        )
    }
}

// --------------------------------------------------------------------------------
// 1. TOP HEADER COMPOSABLE
// --------------------------------------------------------------------------------
@Composable
fun HomeTopHeader(
    onOpenAnalytics: () -> Unit,
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ZYVO Logo & Tagline
        Column {
            Text(
                text = "ZYVO",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFF007A), Color(0xFF9D4EDD), Color(0xFF00F0FF))
                    )
                ),
                letterSpacing = 1.sp
            )
            Text(
                text = "WATCH • CONNECT • SHINE",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 1.5.sp
            )
        }

        // Top-Right Action Icons (Search, Trophy, Notifications with badge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCircleIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                onClick = onSearchClick
            )
            HeaderCircleIconButton(
                icon = Icons.Default.EmojiEvents,
                contentDescription = "Leaderboard",
                onClick = onOpenAnalytics
            )
            HeaderCircleIconButton(
                icon = Icons.Default.Notifications,
                contentDescription = "Notifications",
                hasBadge = true,
                onClick = onNotificationsClick
            )
        }
    }
}

@Composable
fun HeaderCircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    hasBadge: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF19142A))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        if (hasBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF007A))
                    .border(1.5.dp, Color(0xFF19142A), CircleShape)
            )
        }
    }
}

// --------------------------------------------------------------------------------
// 2. HERO PROMOTIONAL BANNER COMPOSABLE
// --------------------------------------------------------------------------------
@Composable
fun HeroPromoBanner(onClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(185.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF090712))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFFF00AA).copy(alpha = 0.5f), Color(0xFF00F0FF).copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .testTag("hero_promo_banner")
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(com.example.zyvo.R.drawable.hero_banner)
                .crossfade(true)
                .error(com.example.zyvo.R.drawable.hero_banner)
                .fallback(com.example.zyvo.R.drawable.hero_banner)
                .build(),
            contentDescription = "BE A STAR BE ON TOP",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun MascotGraphic(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Glowing aura behind mascot
        Box(
            modifier = Modifier
                .size(75.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF00AA).copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )
        // Cute cartoon mascot composite
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🐱", fontSize = 42.sp)
            Box(
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF007A), Color(0xFF9D4EDD))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CrownTrophyGraphic(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Golden radiance aura
        Box(
            modifier = Modifier
                .size(75.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFD700).copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
        // Golden Crown & Wings Composite
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "👑", fontSize = 44.sp)
            Text(
                text = "🏆",
                fontSize = 24.sp,
                modifier = Modifier.offset(y = (-14).dp)
            )
        }
    }
}

// --------------------------------------------------------------------------------
// 3. QUICK ACCESS HORIZONTAL CARD
// --------------------------------------------------------------------------------
@Composable
fun QuickAccessCard(
    selectedCategory: RoomType?,
    onSelectCategory: (RoomType?) -> Unit,
    onTopLiveClick: () -> Unit,
    onPkBattleClick: () -> Unit = { onSelectCategory(RoomType.PK_BATTLE) },
    onAudioLiveClick: () -> Unit = { onSelectCategory(RoomType.AUDIO_STAGE) },
    onNewClick: () -> Unit = { onSelectCategory(null) }
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161224))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickAccessItem(
                imageRes = com.example.zyvo.R.drawable.ic_top_live_custom,
                label = "Top Live",
                isSelected = false,
                onClick = onTopLiveClick
            )
            QuickAccessItem(
                imageRes = com.example.zyvo.R.drawable.ic_pk_battle_custom,
                iconBgGradient = listOf(Color(0xFF7209B7), Color(0xFFB5179E)),
                label = "PK Battle",
                isSelected = selectedCategory == RoomType.PK_BATTLE,
                onClick = onPkBattleClick
            )
            QuickAccessItem(
                imageRes = com.example.zyvo.R.drawable.ic_audio_live_custom,
                iconBgGradient = listOf(Color(0xFF3A0CA3), Color(0xFF480CA8)),
                label = "Audio Live",
                isSelected = selectedCategory == RoomType.AUDIO_STAGE || selectedCategory == RoomType.MULTI_GUEST,
                onClick = onAudioLiveClick
            )
            QuickAccessItem(
                imageRes = com.example.zyvo.R.drawable.ic_new_custom,
                iconBgGradient = listOf(Color(0xFF4361EE), Color(0xFF4CC9F0)),
                label = "New",
                isSelected = false,
                onClick = onNewClick
            )
        }
    }
}

@Composable
fun QuickAccessItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    imageRes: Int? = null,
    iconBgGradient: List<Color> = listOf(Color(0xFFFF007A), Color(0xFFFF529A)),
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("quick_access_${label.lowercase().replace(" ", "_")}")
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .then(
                    if (imageRes != null) {
                        Modifier.border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(13.dp)
                        )
                    } else {
                        Modifier
                            .background(Brush.linearGradient(iconBgGradient))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(13.dp)
                            )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (imageRes != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageRes)
                        .crossfade(true)
                        .build(),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(13.dp))
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFFB0ACC0)
        )
    }
}

// --------------------------------------------------------------------------------
// SECTION HEADER COMPOSABLE
// --------------------------------------------------------------------------------
@Composable
fun SectionHeader(
    icon: String,
    title: String,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onViewAllClick() }
        ) {
            Text(
                text = "View all",
                fontSize = 12.sp,
                color = Color(0xFFA09BAC),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFA09BAC),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// --------------------------------------------------------------------------------
// 4. TOP LIVE HOST CARDS
// --------------------------------------------------------------------------------
@Composable
fun TopLiveCard(
    item: TopHostItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1B162B))
        ) {
            // High quality portrait image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x66000000),
                                Color.Transparent,
                                Color(0xDD090712)
                            )
                        )
                    )
            )

            // LIVE Pill Badge (Top Left)
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF007A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.TopStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(com.example.zyvo.R.drawable.ic_live_custom)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Viewers Pill Overlay (Bottom Left on image)
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.BottomStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.viewerCount,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Host Name + Verified Checkmark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = getVerifiedTickColor(item.gender, item.name),
                modifier = Modifier.size(12.dp)
            )
        }

        // Category Tag
        Text(
            text = item.category,
            fontSize = 10.sp,
            color = Color(0xFFB0ACC0),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class SquareRoomItem(
    val id: String,
    val name: String,
    val category: String,
    val viewerCount: String,
    val imageUrl: String,
    val gender: String = "Female",
    val liveRoomObj: LiveRoom? = null
)

@Composable
fun SquareRoomCard(
    item: SquareRoomItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1B162B))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x44000000),
                                Color.Transparent,
                                Color(0xDD090712)
                            )
                        )
                    )
            )

            // LIVE Pill Badge (Top-Left)
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF007A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.TopStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(com.example.zyvo.R.drawable.ic_live_custom)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Viewers Count Pill (Bottom-Left on image)
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.BottomStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.viewerCount,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Host Name + Verified Tick Checkmark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = getVerifiedTickColor(item.gender, item.name),
                modifier = Modifier.size(12.dp)
            )
        }

        // Category Tag
        Text(
            text = "#${item.category}",
            fontSize = 10.sp,
            color = Color(0xFFB0ACC0),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DynamicRoomCard(
    room: LiveRoom,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverPic = room.roomCoverUrl ?: room.hostAvatarUrl
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1B162B))
        ) {
            if (!coverPic.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverPic)
                        .crossfade(true)
                        .build(),
                    contentDescription = room.hostName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF381463), Color(0xFF120524)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = room.hostAvatar, fontSize = 38.sp)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Color(0xDD090712)))
                    )
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF007A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.TopStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(com.example.zyvo.R.drawable.ic_live_custom)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "LIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.BottomStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "${room.viewerCount}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = room.hostName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(3.dp))
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = getVerifiedTickColor(room.hostGender, room.hostName), modifier = Modifier.size(12.dp))
        }
        Text(text = "#${room.category}", fontSize = 10.sp, color = Color(0xFFB0ACC0), maxLines = 1)
    }
}

// --------------------------------------------------------------------------------
// 5. POPULAR HOSTS CROWN AVATARS
// --------------------------------------------------------------------------------
@Composable
fun PopularHostCrownItem(
    host: PopularHostItem,
    onOpenUserDetail: ((String) -> Unit)?
) {
    val hostGender = if (host.gender.isNotBlank()) host.gender else if (host.isGoldCrown) "Male" else "Female"
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(105.dp)
            .clickable { onOpenUserDetail?.invoke(host.id) }
            .padding(vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            AnimatedHostAvatar(
                imageUrl = host.imageUrl,
                gender = hostGender,
                name = host.name,
                rank = when (host.id) {
                    "king_of_kings" -> 1
                    "drama_queen" -> 2
                    "jannatul_islam" -> 3
                    else -> null
                },
                size = 96.dp,
                isLive = false
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = host.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = com.example.zyvo.ui.theme.getVerifiedTickColor(hostGender, host.name),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
fun CrownFrameHeader(isGold: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = if (isGold) "👑" else "👑",
        fontSize = 20.sp,
        modifier = modifier
    )
}

// --------------------------------------------------------------------------------
// 6. VIP PROMOTION BANNER
// --------------------------------------------------------------------------------
@Composable
fun VipUpgradeBanner(onUpgradeClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF22073A),
                        Color(0xFF420E64),
                        Color(0xFF1C0630)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFFF00AA).copy(alpha = 0.5f), Color(0xFFFFD700).copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onUpgradeClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // VIP 7 Shield Emblem Visual
            VipShield7Emblem(modifier = Modifier.size(46.dp))

            Spacer(modifier = Modifier.width(10.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade to VIP",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Enjoy exclusive perks and rewards",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Upgrade Now Pill Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF007A), Color(0xFF9D4EDD))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Upgrade Now",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VipShield7Emblem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Gold Shield Icon representation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF8C00))
                    )
                )
                .border(1.dp, Color.White, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "7",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4A0E00)
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------
// 7. FEATURE CARDS GRID (2x2 GRID)
// --------------------------------------------------------------------------------
enum class FeatureVisualType {
    PK_BATTLE, GO_LIVE, TOP_GIFTING, DAILY_TASK
}

@Composable
fun FeatureCardsGrid(
    onSelectCategory: (RoomType?) -> Unit,
    onGoLiveClick: () -> Unit,
    onOpenAnalyticsClick: () -> Unit,
    onDailyTaskClick: () -> Unit = onOpenAnalyticsClick,
    onPkBattleClick: () -> Unit = { onSelectCategory(RoomType.PK_BATTLE) }
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1: PK Battle & Go Live
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "PK Battle",
                subtitle = "Live Competition",
                buttonText = "Join Now",
                cardGradient = listOf(Color(0xFF1E0A38), Color(0xFF3C0E5A)),
                buttonGradient = listOf(Color(0xFF4361EE), Color(0xFF3A0CA3)),
                visualType = FeatureVisualType.PK_BATTLE,
                onClick = onPkBattleClick
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "Go Live",
                subtitle = "Share your talent",
                buttonText = "Start Live",
                cardGradient = listOf(Color(0xFF2E083D), Color(0xFF5A0C6B)),
                buttonGradient = listOf(Color(0xFF7209B7), Color(0xFFFF007A)),
                visualType = FeatureVisualType.GO_LIVE,
                onClick = onGoLiveClick
            )
        }

        // Row 2: Top Gifting & Daily Task
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "Top Gifting",
                subtitle = "Support your favorite",
                buttonText = "Send Gift",
                cardGradient = listOf(Color(0xFF2A0930), Color(0xFF4A0A48)),
                buttonGradient = listOf(Color(0xFFFF007A), Color(0xFF7209B7)),
                visualType = FeatureVisualType.TOP_GIFTING,
                onClick = onOpenAnalyticsClick
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "Daily Task",
                subtitle = "Complete & Earn",
                buttonText = "Check Now",
                cardGradient = listOf(Color(0xFF1F083B), Color(0xFF3E0F66)),
                buttonGradient = listOf(Color(0xFF7209B7), Color(0xFF4361EE)),
                visualType = FeatureVisualType.DAILY_TASK,
                onClick = onDailyTaskClick
            )
        }
    }
}

@Composable
fun FeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    buttonText: String,
    cardGradient: List<Color>,
    buttonGradient: List<Color>,
    visualType: FeatureVisualType,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(cardGradient))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Text & Button Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(buttonGradient))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Right Visual Graphic according to feature card type
            Box(
                modifier = Modifier
                    .size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                when (visualType) {
                    FeatureVisualType.PK_BATTLE -> PkBattleVisualGraphic()
                    FeatureVisualType.GO_LIVE -> GoLiveVisualGraphic()
                    FeatureVisualType.TOP_GIFTING -> TopGiftingVisualGraphic()
                    FeatureVisualType.DAILY_TASK -> DailyTaskVisualGraphic()
                }
            }
        }
    }
}

@Composable
fun PkBattleVisualGraphic() {
    val context = LocalContext.current
    Box(
        modifier = Modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(com.example.zyvo.R.drawable.ic_pk_battle_custom)
                .crossfade(true)
                .build(),
            contentDescription = "PK Battle",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun GoLiveVisualGraphic() {
    val context = LocalContext.current
    Box(
        modifier = Modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(com.example.zyvo.R.drawable.ic_live_custom)
                .crossfade(true)
                .build(),
            contentDescription = "Go Live",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun TopGiftingVisualGraphic() {
    Box(
        modifier = Modifier.size(75.dp),
        contentAlignment = Alignment.Center
    ) {
        // 3D Gift Box illustration with floating hearts
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF9D4EDD), Color(0xFF7209B7))
                    )
                )
                .border(1.5.dp, Color(0xFFFF529A), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🎁", fontSize = 28.sp)
        }
        Text(
            text = "💖",
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-4).dp, y = 2.dp)
        )
        Text(
            text = "💕",
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 4.dp, y = (-2).dp)
        )
    }
}

@Composable
fun DailyTaskVisualGraphic() {
    Box(
        modifier = Modifier.size(75.dp),
        contentAlignment = Alignment.Center
    ) {
        // 3D Task Clipboard & Coins illustration
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF8B5CF6))
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📋", fontSize = 26.sp)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-2).dp, y = 2.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD700))
                .padding(3.dp)
        ) {
            Text(text = "⭐", fontSize = 10.sp)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD700))
                .padding(3.dp)
        ) {
            Text(text = "🪙", fontSize = 10.sp)
        }
    }
}

@Composable
fun LiveRoomCard(
    room: LiveRoom,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCardElevated)
                    .border(1.5.dp, NeonPurple, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = room.hostAvatar, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.hostName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (room.roomType) {
                                    RoomType.SINGLE_LIVE -> NeonPurpleDark
                                    RoomType.MULTI_GUEST -> CyberBlue.copy(alpha = 0.3f)
                                    RoomType.AUDIO_STAGE -> EmeraldGreen.copy(alpha = 0.3f)
                                    RoomType.PK_BATTLE -> PkRed.copy(alpha = 0.3f)
                                    RoomType.TEAM_MODE -> GoldAccent.copy(alpha = 0.3f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${room.roomType.icon} ${room.roomType.badge}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = room.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${room.viewerCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = ElectricMagenta,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${room.likesCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (room.tags.isNotEmpty()) {
                        Text(
                            text = "#${room.tags.first()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPurpleLight
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// 8. EXECUTIVE FOUNDERS MODAL (CEO & CO-FOUNDER SPOTLIGHT)
// --------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutiveFoundersModal(
    ceoProfile: UserProfile?,
    coFounderProfile: UserProfile?,
    ansharahProfile: UserProfile? = null,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenLiveRoom: (String) -> Unit,
    onOpenChat: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF130924),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(GoldAccent.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Crown & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👑", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ZYVO EXECUTIVE LEADERSHIP",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldAccent,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Meet the Founders & Official Backers",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. FOUNDER & CEO CARD: RAYAN MIRZA
            FounderSpotlightCard(
                name = "RAYAN MIRZA",
                roleTitle = "FOUNDER & CHIEF EXECUTIVE OFFICER",
                avatarUrl = ceoProfile?.avatarUrl ?: "https://cdn.phototourl.com/free/2026-09-01-f3e014af-6987-41b0-8bcf-732294379e68.png",
                avatarEmoji = "👑",
                phoneNumber = "+44 7868 713315",
                gemsText = "99.9M Gems",
                followersText = "9.8M Fans",
                bioText = "Managing Director of Global Operations. Directing executive platform governance, creator backing, and strategic expansion.",
                isCeo = true,
                onViewProfile = { onOpenProfile("ceo_rayan") },
                onJoinLive = { onOpenLiveRoom("ceo_rayan_room") },
                onOpenChat = { onOpenChat?.invoke("ceo_rayan") }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. CO-FOUNDER CARD: ALPHA RAJPOOT
            FounderSpotlightCard(
                name = "ALPHA RAJPOOT",
                roleTitle = "CO-FOUNDER & CHIEF OPERATING OFFICER",
                avatarUrl = coFounderProfile?.avatarUrl ?: "https://cdn.phototourl.com/free/2026-09-01-4aa927e1-ee25-497a-ae9e-4201e9d81679.jpg",
                avatarEmoji = "🦁",
                phoneNumber = "+447366 387620",
                gemsText = "88.8M Gems",
                followersText = "8.4M Fans",
                bioText = "Director of Global Expansion. Managing strategic partnerships, PK Arena operations, and regional ecosystem development.",
                isCeo = false,
                onViewProfile = { onOpenProfile("co_founder_alpha") },
                onJoinLive = { onOpenLiveRoom("co_founder_alpha_room") },
                onOpenChat = { onOpenChat?.invoke("co_founder_alpha") }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. TOP HOST & FIRST HOST CARD: ANSHARAH GAHNI
            FounderSpotlightCard(
                name = "ANSHARAH GAHNI",
                roleTitle = "TOP HOST & PLATFORM AMBASSADOR",
                avatarUrl = ansharahProfile?.avatarUrl ?: "https://mp3tourl.com/images/1788287833535-dc94ba6e-5e98-4349-b949-cd1521ff4618.jpg",
                avatarEmoji = "👸",
                phoneNumber = "+44 7868 713315",
                gemsText = "78.5M Gems",
                followersText = "7.6M Fans",
                bioText = "✨ Elite Creator & Zyvo Global Icon 👑 Level 89 Superstar • SVIP 9 • Celebrated as the very first official host of Zyvo Live, inspiring millions daily 💖",
                isCeo = false,
                onViewProfile = { onOpenProfile("ansharah_gahni") },
                onJoinLive = { onOpenLiveRoom("ansharah_gahni_room_1") },
                onOpenChat = { onOpenChat?.invoke("ansharah_gahni") }
            )
        }
    }
}

@Composable
fun FounderSpotlightCard(
    name: String,
    roleTitle: String,
    avatarUrl: String,
    avatarEmoji: String,
    phoneNumber: String,
    gemsText: String,
    followersText: String,
    bioText: String,
    isCeo: Boolean,
    onViewProfile: () -> Unit,
    onJoinLive: () -> Unit,
    onOpenChat: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = if (isCeo) GoldAccent else if (name == "ANSHARAH GAHNI") Color(0xFFFF007A) else NeonCyan
    val borderGradient = if (isCeo) {
        listOf(GoldAccent, Color(0xFFFF007A), GoldAccent)
    } else if (name == "ANSHARAH GAHNI") {
        listOf(Color(0xFFFF007A), Color(0xFFE040FB), Color(0xFFFF007A))
    } else {
        listOf(NeonCyan, NeonPurple, NeonCyan)
    }

    // Dynamic, smooth continuous rotating/shifting border gradient animation
    val infiniteTransition = rememberInfiniteTransition(label = "borderTransition")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientShift"
    )

    val animatedBrush = Brush.linearGradient(
        colors = borderGradient,
        start = Offset(gradientShift, 0f),
        end = Offset(gradientShift + 400f, 400f),
        tileMode = TileMode.Mirror
    )

    // Dynamic shimmering metallic glint cover sweep animation
    val shimmerTransition = rememberInfiniteTransition(label = "cardShimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, animatedBrush, RoundedCornerShape(20.dp))
            .drawWithContent {
                drawContent()
                val width = size.width
                val height = size.height
                val xPosition = width * shimmerProgress
                val shimmerWidth = 140.dp.toPx()
                
                val sweepBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    start = Offset(xPosition, 0f),
                    end = Offset(xPosition + shimmerWidth, height)
                )
                drawRect(brush = sweepBrush)
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D0E33))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Avatar + Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Use the gorgeous AnimatedHostAvatar (popular host frames)
                // This has the premium custom crown and colored borders, but has NO level/vip badges on it
                AnimatedHostAvatar(
                    imageUrl = avatarUrl,
                    gender = if (name == "ANSHARAH GAHNI") "Female" else "Male",
                    name = name,
                    size = 72.dp,
                    isLive = false,
                    onClick = onViewProfile
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = getVerifiedTickColor(if (name == "ANSHARAH GAHNI") "Female" else "Male", name),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Sleek, highly professional inline badges for SVIP and Level
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // SVIP 9 Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(GoldAccent, Color(0xFFFF9800), GoldAccent)
                                    )
                                )
                                .border(0.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SVIP 9",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        // Level Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF7B1FA2), Color(0xFFFF007A))
                                    )
                                )
                                .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Lv.${if (name == "ANSHARAH GAHNI") 89 else 99}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Role Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCeo) Color(0xFF5A3900)
                                else if (name == "ANSHARAH GAHNI") Color(0xFF4A0E2E)
                                else Color(0xFF00445E)
                            )
                            .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = roleTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$gemsText • $followersText",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = bioText,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.75f),
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: WhatsApp Direct (or Live), View Profile, Chat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (name == "ANSHARAH GAHNI") {
                    // "🔴 Live" button (Only for Ansharah Ghani - instead of WhatsApp)
                    Button(
                        onClick = onJoinLive,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(text = "🔴", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Live",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // WhatsApp Official Button (Rayan & Alpha only)
                    Button(
                        onClick = { openWhatsAppChat(context, phoneNumber, name) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(text = "💬", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "WhatsApp",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // View Profile Button
                OutlinedButton(
                    onClick = onViewProfile,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text(
                        text = "View Profile",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                // Chat Button
                Button(
                    onClick = { onOpenChat?.invoke() ?: onJoinLive() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCeo) Color(0xFFFF9800) else if (name == "ANSHARAH GAHNI") Color(0xFFFF007A) else Color(0xFF00E5FF)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "💬 Chat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// 9. HOME SEARCH FULL SCREEN WINDOW
// --------------------------------------------------------------------------------
data class SearchHostModel(
    val id: String,
    val name: String,
    val category: String,
    val viewerOrPopularity: String,
    val imageUrl: String,
    val gender: String = "Female",
    val isCrown: Boolean = false
)

@Composable
fun HomeSearchModal(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    topHosts: List<TopHostItem> = emptyList(),
    popularHosts: List<PopularHostItem> = emptyList(),
    rooms: List<LiveRoom> = emptyList(),
    onRoomClick: ((LiveRoom) -> Unit)? = null,
    onHostClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilterCategory by remember { mutableStateOf("All") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler {
            onDismiss()
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F041D),
                            Color(0xFF150A26),
                            Color(0xFF0D0218)
                        )
                    )
                ),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. TOP HEADER WITH BACK BUTTON & SEARCH INPUT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                "Search live rooms, top hosts, IDs...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color(0xFF22113D),
                            unfocusedContainerColor = Color(0xFF1C0D33)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp)
                    )
                }

                // 2. FILTER CATEGORY CHIPS
                val filterCategories = listOf("All", "🔥 Live Rooms", "👑 Top Hosts", "⚔️ PK Arena", "🎙️ Audio Stage")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(filterCategories) { cat ->
                        val isSelected = selectedFilterCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(listOf(Color(0xFFFF007A), Color(0xFF7928CA)))
                                    else Brush.horizontalGradient(listOf(Color(0xFF22113D), Color(0xFF1C0D33)))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedFilterCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. MAIN SCROLLABLE CONTENT: SUGGESTIONS or SEARCH RESULTS
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    val isQueryActive = searchQuery.isNotBlank()

                    if (!isQueryActive) {
                        // ==========================================
                        // A. SUGGESTED LIVE ROOMS (1:1 Square Cards)
                        // ==========================================
                        if (selectedFilterCategory == "All" || selectedFilterCategory == "🔥 Live Rooms" || selectedFilterCategory == "⚔️ PK Arena" || selectedFilterCategory == "🎙️ Audio Stage") {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🔥", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Suggested Live Rooms",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = "${rooms.size} Active",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )
                                    }

                                    // 1:1 SQUARE CARDS LAZY ROW
                                    val filteredRooms = rooms.filter { r ->
                                        when (selectedFilterCategory) {
                                            "⚔️ PK Arena" -> r.roomType == RoomType.PK_BATTLE || r.category.contains("PK", ignoreCase = true)
                                            "🎙️ Audio Stage" -> r.roomType == RoomType.AUDIO_STAGE || r.roomType == RoomType.MULTI_GUEST
                                            else -> true
                                        }
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(filteredRooms) { room ->
                                            SquareLiveRoomCard(
                                                room = room,
                                                onClick = { onRoomClick?.invoke(room) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // B. SUGGESTED TOP HOSTS & STARS (1:1 Square Cards)
                        // ==========================================
                        if (selectedFilterCategory == "All" || selectedFilterCategory == "👑 Top Hosts") {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👑", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Top Hosts & Stars",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent
                                            )
                                        }
                                        Text(
                                            text = "Trending Now",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF007A)
                                        )
                                    }

                                    // Combine topHosts and popularHosts into 1:1 Square Host Cards
                                    val combinedHosts = (topHosts.map { host ->
                                        SearchHostModel(
                                            id = host.id,
                                            name = host.name,
                                            category = host.category,
                                            viewerOrPopularity = "👀 ${host.viewerCount}",
                                            imageUrl = host.imageUrl,
                                            gender = "Female"
                                        )
                                    } + popularHosts.map { pop ->
                                        SearchHostModel(
                                            id = pop.id,
                                            name = pop.name,
                                            category = "Popularity ${pop.popularity}",
                                            viewerOrPopularity = "💎 ${pop.popularity}",
                                            imageUrl = pop.imageUrl,
                                            gender = pop.gender,
                                            isCrown = pop.isGoldCrown
                                        )
                                    }).distinctBy { it.id }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(combinedHosts) { hostItem ->
                                            SquareHostCard(
                                                host = hostItem,
                                                onClick = { onHostClick(hostItem.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ==========================================
                        // C. SEARCH ACTIVE RESULTS (1:1 SQUARE CARDS)
                        // ==========================================
                        val matchingRooms = rooms.filter { r ->
                            r.title.contains(searchQuery, ignoreCase = true) ||
                            r.hostName.contains(searchQuery, ignoreCase = true) ||
                            r.category.contains(searchQuery, ignoreCase = true) ||
                            r.id.contains(searchQuery, ignoreCase = true)
                        }

                        val matchingTopHosts = topHosts.filter { h ->
                            h.name.contains(searchQuery, ignoreCase = true) ||
                            h.category.contains(searchQuery, ignoreCase = true) ||
                            h.id.contains(searchQuery, ignoreCase = true)
                        }.map { h ->
                            SearchHostModel(
                                id = h.id,
                                name = h.name,
                                category = h.category,
                                viewerOrPopularity = "👀 ${h.viewerCount}",
                                imageUrl = h.imageUrl,
                                gender = "Female"
                            )
                        }

                        val matchingPopularHosts = popularHosts.filter { p ->
                            p.name.contains(searchQuery, ignoreCase = true) ||
                            p.id.contains(searchQuery, ignoreCase = true)
                        }.map { p ->
                            SearchHostModel(
                                id = p.id,
                                name = p.name,
                                category = "Popularity ${p.popularity}",
                                viewerOrPopularity = "💎 ${p.popularity}",
                                imageUrl = p.imageUrl,
                                gender = p.gender,
                                isCrown = p.isGoldCrown
                            )
                        }

                        val allMatchingHosts = (matchingTopHosts + matchingPopularHosts).distinctBy { it.id }

                        if (matchingRooms.isEmpty() && allMatchingHosts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔍", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No results found for \"$searchQuery\"",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Try searching for names like 'Nusrat', 'Ansharah', 'Alpha', or 'PK'",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            // Render Matching Live Rooms
                            if (matchingRooms.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Matching Live Rooms (${matchingRooms.size})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                items(matchingRooms.chunked(2)) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (room in pair) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                SquareLiveRoomCard(
                                                    room = room,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    onClick = { onRoomClick?.invoke(room) }
                                                )
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            // Render Matching Hosts
                            if (allMatchingHosts.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Matching Hosts (${allMatchingHosts.size})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                                    )
                                }

                                items(allMatchingHosts.chunked(2)) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (host in pair) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                SquareHostCard(
                                                    host = host,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    onClick = { onHostClick(host.id) }
                                                )
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1:1 Square Live Room Card Component
 */
@Composable
fun SquareLiveRoomCard(
    room: LiveRoom,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = room.roomCoverUrl ?: room.hostAvatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80"

    Card(
        modifier = modifier
            .width(148.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFF007A).copy(alpha = 0.6f),
                        Color(0xFF00F0FF).copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0E38))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = room.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF0055))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "👀 ${room.viewerCount}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (room.roomType == RoomType.PK_BATTLE) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFF007A), Color(0xFF7928CA))))
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(com.example.zyvo.R.drawable.ic_pk_battle_custom)
                            .crossfade(true)
                            .build(),
                        contentDescription = "PK",
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = room.hostName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = getVerifiedTickColor(room.hostGender, room.hostName),
                        modifier = Modifier.size(11.dp)
                    )
                }

                Text(
                    text = room.title,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 1:1 Square Host / Star Card Component
 */
@Composable
fun SquareHostCard(
    host: SearchHostModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .width(148.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        GoldAccent.copy(alpha = 0.7f),
                        Color(0xFFFF8800).copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0E38))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(host.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = host.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (host.gender.equals("Male", ignoreCase = true)) listOf(GoldAccent, Color(0xFFFF8800))
                            else listOf(Color(0xFFFF007A), Color(0xFF7928CA))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (host.gender.equals("Male", ignoreCase = true)) "👑 KING" else "👸 QUEEN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = host.viewerOrPopularity,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = host.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = getVerifiedTickColor(host.gender, host.name),
                        modifier = Modifier.size(11.dp)
                    )
                }

                Text(
                    text = host.category,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------
// 10. HOME NOTIFICATIONS MODAL (UPDATES PANEL - RIGHT SIDE SLIDING DRAWER)
// --------------------------------------------------------------------------------
data class UpdateNotificationItem(
    val id: String,
    val title: String,
    val username: String,
    val avatarUrl: String,
    val gender: String = "Female",
    val badgeText: String,
    val badgeColor: Color,
    val message: String,
    val time: String,
    val isUnread: Boolean = false,
    val actionType: String = "NONE",
    val categoryGroup: String = "ALL", // "LIVE", "OFFICIAL", "REWARDS"
    val roomCoverUrl: String? = null,
    val roomId: String? = null
)

@Composable
fun HomeNotificationsModal(
    rooms: List<LiveRoom> = emptyList(),
    onRoomClick: ((LiveRoom) -> Unit)? = null,
    onOpenCeoProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("ALL") } // "ALL", "LIVE", "OFFICIAL", "REWARDS"
    var isAllRead by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    val coroutineScope = rememberCoroutineScope()
    val handleClose = {
        if (animateIn) {
            animateIn = false
            coroutineScope.launch {
                delay(220)
                onDismiss()
            }
        }
    }

    // Android System Back Button handling: closes drawer first, doesn't exit app or navigate away
    BackHandler(enabled = true) {
        handleClose()
    }

    Dialog(
        onDismissRequest = { handleClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF130926))
        ) {
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(tween(220)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(220)),
                exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = Color(0xFF130926)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Vertical list of update items
                        val updateItemsList = remember {
                            listOf(
                                UpdateNotificationItem(
                                    id = "ceo_announcement",
                                    title = "CEO Rayan Mirza",
                                    username = "RAYAN MIRZA (FOUNDER)",
                                    avatarUrl = "https://cdn.phototourl.com/free/2026-09-01-f3e014af-6987-41b0-8bcf-732294379e68.png",
                                    gender = "Male",
                                    badgeText = "OFFICIAL",
                                    badgeColor = GoldAccent,
                                    message = "Welcome to ZYVO Live! Platform partnerships, SVIP rewards & founder desk perks are active.",
                                    time = "Just now",
                                    isUnread = true,
                                    actionType = "OPEN_CEO",
                                    categoryGroup = "OFFICIAL"
                                ),
                                UpdateNotificationItem(
                                    id = "pk_live",
                                    title = "King Of King's",
                                    username = "King Of King's",
                                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80",
                                    gender = "Male",
                                    badgeText = "LIVE PK",
                                    badgeColor = Color(0xFFFF0055),
                                    message = "1v1 PK Battle is LIVE against Drama Queen! 245.8K viewers online.",
                                    time = "5m ago",
                                    isUnread = true,
                                    categoryGroup = "LIVE",
                                    roomCoverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80",
                                    roomId = "1"
                                ),
                                UpdateNotificationItem(
                                    id = "stream_ansharah",
                                    title = "Ansharah Gahni",
                                    username = "Ansharah Gahni",
                                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80",
                                    gender = "Female",
                                    badgeText = "HOT STREAM",
                                    badgeColor = ElectricMagenta,
                                    message = "Broadcasted 'Acoustic Chill & Midnight Music Lounge'!",
                                    time = "12m ago",
                                    isUnread = true,
                                    categoryGroup = "LIVE",
                                    roomCoverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&auto=format&fit=crop&q=80",
                                    roomId = "2"
                                ),
                                UpdateNotificationItem(
                                    id = "gift_nusrat",
                                    title = "Nusrat Jahan",
                                    username = "Nusrat Jahan",
                                    avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&auto=format&fit=crop&q=80",
                                    gender = "Female",
                                    badgeText = "GIFT REWARD",
                                    badgeColor = Color(0xFFFFB800),
                                    message = "Sent you a Lucky Rose +50 Coins bonus in the live stream!",
                                    time = "25m ago",
                                    categoryGroup = "REWARDS"
                                ),
                                UpdateNotificationItem(
                                    id = "vip_alpha",
                                    title = "Alpha Rajpoot",
                                    username = "ALPHA RAJPOOT",
                                    avatarUrl = "https://cdn.phototourl.com/free/2026-09-01-4aa927e1-ee25-497a-ae9e-4201e9d81679.jpg",
                                    gender = "Male",
                                    badgeText = "SVIP 9",
                                    badgeColor = Color(0xFF00F0FF),
                                    message = "Unlocked SVIP Tier 9 Supreme badge privileges & Golden Dragon frame.",
                                    time = "1h ago",
                                    categoryGroup = "OFFICIAL"
                                ),
                                UpdateNotificationItem(
                                    id = "audio_ayesha",
                                    title = "Ayesha Live",
                                    username = "Ayesha Live",
                                    avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&auto=format&fit=crop&q=80",
                                    gender = "Female",
                                    badgeText = "AUDIO LOUNGE",
                                    badgeColor = Color(0xFF9D4EDD),
                                    message = "Opened Audio Live Room 'Late Night Storytelling & Music'!",
                                    time = "2h ago",
                                    categoryGroup = "LIVE",
                                    roomCoverUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&auto=format&fit=crop&q=80",
                                    roomId = "3"
                                )
                            )
                        }

                        val unreadCount = if (isAllRead) 0 else updateItemsList.count { it.isUnread }

                        val filteredItems = remember(selectedCategory, isAllRead) {
                            updateItemsList.map { item ->
                                if (isAllRead) item.copy(isUnread = false) else item
                            }.filter { item ->
                                if (selectedCategory == "ALL") true else item.categoryGroup == selectedCategory
                            }
                        }

                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Updates",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(ElectricMagenta)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$unreadCount NEW",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (unreadCount > 0) {
                                    Text(
                                        text = "Mark Read",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { isAllRead = true }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // Close X Button
                                IconButton(
                                    onClick = { handleClose() },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Updates Panel",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                        // Organized Category Filter Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "ALL" to "All",
                                "LIVE" to "🔴 Live Rooms",
                                "OFFICIAL" to "👑 Official & SVIP",
                                "REWARDS" to "🎁 Rewards"
                            ).forEach { (catId, label) ->
                                val isSelected = selectedCategory == catId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) ElectricMagenta else Color.White.copy(alpha = 0.08f))
                                        .border(
                                            1.dp,
                                            if (isSelected) GoldAccent else Color.Transparent,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedCategory = catId }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Section: 1:1 Live Rooms Header & Row
                            if (selectedCategory == "ALL" || selectedCategory == "LIVE") {
                                item {
                                    val liveRoomsList = if (rooms.isNotEmpty()) rooms else remember {
                                        listOf(
                                            LiveRoom(
                                                id = "1",
                                                title = "1v1 PK Battle Live",
                                                creatorIdentity = "king_of_kings",
                                                hostName = "King Of King's",
                                                hostGender = "Male",
                                                viewerCount = 245800,
                                                roomCoverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80"
                                            ),
                                            LiveRoom(
                                                id = "2",
                                                title = "Acoustic Chill Music",
                                                creatorIdentity = "ansharah_gahni",
                                                hostName = "Ansharah Gahni",
                                                hostGender = "Female",
                                                viewerCount = 12500,
                                                roomCoverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&auto=format&fit=crop&q=80"
                                            ),
                                            LiveRoom(
                                                id = "3",
                                                title = "Late Night Audio Lounge",
                                                creatorIdentity = "ayesha_live",
                                                hostName = "Ayesha Live",
                                                hostGender = "Female",
                                                viewerCount = 8200,
                                                roomCoverUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&auto=format&fit=crop&q=80"
                                            )
                                        )
                                    }

                                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                        ) {
                                            Text(
                                                text = "🔥 Live Rooms in Updates",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent
                                            )
                                            Text(
                                                text = "1:1 Live Cards",
                                                fontSize = 9.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            contentPadding = PaddingValues(end = 4.dp)
                                        ) {
                                            items(liveRoomsList, key = { it.id }) { room ->
                                                // 1:1 Square Room Card
                                                Card(
                                                    modifier = Modifier
                                                        .size(105.dp) // 1:1 Square
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .border(
                                                            1.dp,
                                                            Brush.linearGradient(
                                                                listOf(
                                                                    ElectricMagenta.copy(alpha = 0.6f),
                                                                    GoldAccent.copy(alpha = 0.4f)
                                                                )
                                                            ),
                                                            RoundedCornerShape(14.dp)
                                                        )
                                                        .clickable {
                                                            handleClose()
                                                            onRoomClick?.invoke(room)
                                                        },
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B0F33))
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        AsyncImage(
                                                            model = room.roomCoverUrl ?: room.hostAvatarUrl,
                                                            contentDescription = room.title,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        // Dark Bottom Gradient
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(
                                                                    Brush.verticalGradient(
                                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                                                                        startY = 40f
                                                                    )
                                                                )
                                                        )
                                                        // Top Live Badge
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopStart)
                                                                .padding(4.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color.Red)
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "1:1 LIVE",
                                                                fontSize = 7.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color.White
                                                            )
                                                        }

                                                        // Viewer Badge
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(4.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color.Black.copy(alpha = 0.65f))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "👀 ${room.viewerCount}",
                                                                fontSize = 7.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }

                                                        // Bottom info
                                                        Column(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .padding(6.dp)
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = room.hostName,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                )
                                                                Spacer(modifier = Modifier.width(2.dp))
                                                                Icon(
                                                                    imageVector = Icons.Default.Verified,
                                                                    contentDescription = null,
                                                                    tint = getVerifiedTickColor(room.hostGender, room.hostName),
                                                                    modifier = Modifier.size(9.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Notification Updates List
                            items(filteredItems, key = { it.id }) { item ->
                                UpdateNotificationCard(
                                    item = item,
                                    onClick = {
                                        if (item.actionType == "OPEN_CEO") {
                                            handleClose()
                                            onOpenCeoProfile()
                                        } else if (item.categoryGroup == "LIVE") {
                                            val targetRoom = rooms.find { it.id == item.roomId } ?: LiveRoom(
                                                id = item.roomId ?: "1",
                                                title = item.message,
                                                creatorIdentity = item.username.lowercase().replace(" ", "_"),
                                                hostName = item.username,
                                                viewerCount = 12500,
                                                hostGender = item.gender,
                                                roomCoverUrl = item.roomCoverUrl ?: item.avatarUrl
                                            )
                                            handleClose()
                                            onRoomClick?.invoke(targetRoom)
                                        }
                                    },
                                    onJoinRoom = { roomId ->
                                        val targetRoom = rooms.find { it.id == roomId } ?: LiveRoom(
                                            id = roomId,
                                            title = item.message,
                                            creatorIdentity = item.username.lowercase().replace(" ", "_"),
                                            hostName = item.username,
                                            viewerCount = 12500,
                                            hostGender = item.gender,
                                            roomCoverUrl = item.roomCoverUrl ?: item.avatarUrl
                                        )
                                        handleClose()
                                        onRoomClick?.invoke(targetRoom)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateNotificationCard(
    item: UpdateNotificationItem,
    onClick: () -> Unit,
    onJoinRoom: ((String) -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (item.isUnread) Color(0xFF1F1138) else Color(0xFF170D2B)
            )
            .border(
                1.dp,
                if (item.isUnread) item.badgeColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Thumbnail / Avatar
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = item.avatarUrl,
                        contentDescription = item.username,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, item.badgeColor, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    if (item.isUnread) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ElectricMagenta)
                                .border(1.5.dp, Color(0xFF130926), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = item.username,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = getVerifiedTickColor(item.gender, item.username),
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = item.time,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(item.badgeColor)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = item.badgeText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (item.badgeColor == GoldAccent || item.badgeColor == Color(0xFFFFB800)) Color.Black else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.message,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 1:1 Square Room Cover Preview Card if roomCoverUrl is present!
            if (!item.roomCoverUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, ElectricMagenta.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable {
                            item.roomId?.let { roomId ->
                                onJoinRoom?.invoke(roomId)
                            } ?: onClick()
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1:1 Square Image Thumbnail
                    Box(
                        modifier = Modifier
                            .size(56.dp) // 1:1 Square
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = item.roomCoverUrl,
                            contentDescription = "1:1 Room Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Red)
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text("1:1 LIVE", fontSize = 6.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Stream Room",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Text(
                            text = "Tap to enter stream now",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(ElectricMagenta, Color(0xFFFF0055))))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "JOIN ▶",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// 11. HOME DAILY TASKS MODAL
// --------------------------------------------------------------------------------
@Composable
fun HomeDailyTasksModal(
    onDismiss: () -> Unit,
    onGoLiveClick: () -> Unit
) {
    var checkInClaimed by remember { mutableStateOf(false) }
    var watchClaimed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NeonPurpleLight.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF160929))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎯", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Daily Tasks & Rewards",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Complete tasks to earn coins & EXP",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Task 1: Daily Login
                DailyTaskItem(
                    title = "Daily Check-in",
                    rewardText = "+500 Coins",
                    progressText = "1/1",
                    isCompleted = true,
                    isClaimed = checkInClaimed,
                    onClaim = { checkInClaimed = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Task 2: Watch Live
                DailyTaskItem(
                    title = "Watch Live for 5 mins",
                    rewardText = "+300 Coins",
                    progressText = "5/5 mins",
                    isCompleted = true,
                    isClaimed = watchClaimed,
                    onClaim = { watchClaimed = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Task 3: Start Live Stream
                DailyTaskItem(
                    title = "Go Live as Host",
                    rewardText = "+1,000 Coins + VIP EXP",
                    progressText = "0/1",
                    isCompleted = false,
                    isClaimed = false,
                    onClaim = onGoLiveClick,
                    actionButtonLabel = "Go Live"
                )
            }
        }
    }
}

@Composable
fun DailyTaskItem(
    title: String,
    rewardText: String,
    progressText: String,
    isCompleted: Boolean,
    isClaimed: Boolean,
    onClaim: () -> Unit,
    actionButtonLabel: String = "Claim"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF251342))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🪙 $rewardText",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = progressText,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        if (isClaimed) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Claimed ✓",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        } else {
            Button(
                onClick = onClaim,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) Color(0xFFFF007A) else Color(0xFF7209B7)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = actionButtonLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}


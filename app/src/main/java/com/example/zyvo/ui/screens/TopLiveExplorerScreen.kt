package com.example.zyvo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.R
import com.example.zyvo.model.LiveRoom
import com.example.zyvo.model.RoomType
import com.example.zyvo.model.UserProfile
import com.example.zyvo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLiveExplorerScreen(
    rooms: List<LiveRoom>,
    initialCategory: String? = "All",
    headerBadgeText: String = "TOP LIVE",
    screenTitle: String = "TOP LIVE ROOMS",
    ceoProfile: UserProfile? = null,
    coFounderProfile: UserProfile? = null,
    ansharahProfile: UserProfile? = null,
    onBack: () -> Unit,
    onRoomClick: (LiveRoom) -> Unit,
    onOpenUserDetail: ((String) -> Unit)? = null,
    onOpenAnalyticsClick: (() -> Unit)? = null,
    onGoLiveClick: (() -> Unit)? = null,
    onOpenDm: ((String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember(initialCategory) { mutableStateOf(initialCategory ?: "All") }
    var showExecutiveSheet by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    // Sort rooms so official accounts are strictly on top
    val officialIdentities = setOf("ceo_rayan", "co_founder_alpha", "ansharah_gahni")
    val sortedRooms = remember(rooms, searchQuery, selectedFilterCategory) {
        val filtered = rooms.filter { room ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                room.title.contains(searchQuery, ignoreCase = true) ||
                        room.hostName.contains(searchQuery, ignoreCase = true) ||
                        room.category.contains(searchQuery, ignoreCase = true)
            }
            val matchesCat = when (selectedFilterCategory) {
                "All", null -> true
                "Official" -> room.creatorIdentity in officialIdentities || room.category.contains("Official", ignoreCase = true)
                "PK Battle" -> room.roomType == RoomType.PK_BATTLE || room.category.contains("PK", ignoreCase = true)
                "Audio" -> room.roomType == RoomType.AUDIO_STAGE || room.roomType == RoomType.MULTI_GUEST || room.category.contains("Audio", ignoreCase = true) || room.category.contains("Multi", ignoreCase = true)
                "New" -> room.category.contains("New", ignoreCase = true) || room.creatorIdentity !in officialIdentities || room.tags.any { it.contains("New", ignoreCase = true) || it.contains("Rising", ignoreCase = true) || it.contains("Talent", ignoreCase = true) }
                "Music" -> room.category.contains("Music", ignoreCase = true) || room.category.contains("Talk", ignoreCase = true)
                else -> true
            }
            matchesQuery && matchesCat
        }

        filtered.sortedWith(
            compareByDescending<LiveRoom> { it.creatorIdentity in officialIdentities }
                .thenByDescending { it.viewerCount }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopLiveHeader(
                badgeText = when (selectedFilterCategory) {
                    "PK Battle" -> "PK ARENA"
                    "Audio" -> "AUDIO STAGE"
                    "New" -> "NEW CREATORS"
                    else -> headerBadgeText
                },
                onBack = onBack,
                onSearchClick = { showSearchDialog = true },
                onNotificationsClick = { showNotificationsDialog = true },
                onOpenAnalytics = { onOpenAnalyticsClick?.invoke() }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // 1. HERO BANNER AT THE TOP (Span 2 columns)
            item(span = { GridItemSpan(2) }) {
                HeroPromoBanner(onClick = { showExecutiveSheet = true })
            }

            // 2. FILTER CHIPS & LIVE COUNT HEADER (Span 2 columns)
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedFilterCategory == "PK Battle") {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(R.drawable.ic_pk_battle_custom)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "PK Battle",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            } else {
                                Text(
                                    text = when (selectedFilterCategory) {
                                        "Audio" -> "🎙️"
                                        "New" -> "🌟"
                                        else -> "🔥"
                                    },
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (selectedFilterCategory) {
                                    "PK Battle" -> "PK BATTLE ARENAS"
                                    "Audio" -> "AUDIO & MULTI ROOMS"
                                    "New" -> "NEW USER ROOMS"
                                    else -> screenTitle
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Live Pulse Indicator with count
                        Surface(
                            shape = CircleShape,
                            color = LiveRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LivePulsingDot()
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${sortedRooms.size} Live Now",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LiveRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick category pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = when (initialCategory) {
                            "PK Battle" -> listOf("PK Battle", "All", "New", "Official", "Audio", "Music")
                            "Audio" -> listOf("Audio", "All", "New", "Official", "PK Battle", "Music")
                            "New" -> listOf("New", "All", "Official", "PK Battle", "Audio", "Music")
                            else -> listOf("All", "New", "PK Battle", "Audio", "Official", "Music")
                        }
                        categories.forEach { cat ->
                            val isSelected = selectedFilterCategory == cat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ElectricMagenta else Color(0xFF1E1730),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedFilterCategory = cat }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (cat == "PK Battle") {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(R.drawable.ic_pk_battle_custom)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "PK Battle",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // 3. 1:1 SQUARE SIZE LIVE ROOMS (2 items per row)
            items(sortedRooms, key = { it.id }) { room ->
                val isOfficial = room.creatorIdentity in officialIdentities ||
                        room.category.contains("Official", ignoreCase = true) ||
                        room.category.contains("Top Host", ignoreCase = true)

                SquareLiveRoomCard(
                    room = room,
                    isOfficial = isOfficial,
                    onClick = { onRoomClick(room) }
                )
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
                    onGoLiveClick?.invoke()
                }
            },
            onOpenChat = { userId ->
                showExecutiveSheet = false
                onOpenDm?.invoke(userId)
            }
        )
    }

    // Search Modal
    if (showSearchDialog) {
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
                )
            )
        }

        HomeSearchModal(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            topHosts = topLiveHosts,
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
}

// --------------------------------------------------------------------------------
// TOP LIVE APP HEADER
// --------------------------------------------------------------------------------
@Composable
fun TopLiveHeader(
    badgeText: String = "TOP LIVE",
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenAnalytics: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1730))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ZYVO",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF007A), Color(0xFF7209B7))))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = if (badgeText.contains("PK", ignoreCase = true)) "EXPLORE LIVE PK ARENA BATTLES" else "EXPLORE ALL LIVE BROADCASTS",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Action Buttons: Search, Trophy, Notification
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
                contentDescription = "Rankings",
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

// --------------------------------------------------------------------------------
// 1:1 SQUARE SIZE LIVE ROOM CARD (2-COLUMN GRID ITEM)
// --------------------------------------------------------------------------------
@Composable
fun SquareLiveRoomCard(
    room: LiveRoom,
    isOfficial: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val cardBorder = if (isOfficial) {
        androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.sweepGradient(listOf(GoldAccent, Color(0xFFFF007A), GoldAccent))
        )
    } else {
        androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.12f)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // EXACT 1:1 SQUARE SIZE
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("square_live_room_${room.id}"),
        shape = RoundedCornerShape(18.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF160F2B))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Room Cover Photo / Avatar Image
            val imageUrl = room.roomCoverUrl ?: room.hostAvatarUrl
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .error(R.drawable.hero_banner)
                    .fallback(R.drawable.hero_banner)
                    .build(),
                contentDescription = room.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Gradient Overlays (Top Scrim + Bottom Scrim for crisp readability)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.55f),
                            0.35f to Color.Transparent,
                            0.6f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.92f)
                        )
                    )
            )

            // Top Badges Row: [LIVE PULSE / OFFICIAL] + [VIEWERS COUNT]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOfficial) {
                    // Golden Official Crown Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8800))))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👑", fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (room.creatorIdentity == "ceo_rayan") "FOUNDER & CEO" else if (room.creatorIdentity == "co_founder_alpha") "CO-FOUNDER" else "OFFICIAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    // Live Red Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LiveRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(R.drawable.ic_live_custom)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
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

                // Viewer Count Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "👁️ ${formatViewerCount(room.viewerCount)}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom Info Column: Host Name + Verified, Title, and Category Tag
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                // Host Name + Verified Check
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = room.hostName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfficial) GoldAccent else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = getVerifiedTickColor(room.hostGender, room.hostName),
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Room Title
                Text(
                    text = room.title,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Category Tag Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isOfficial) GoldAccent.copy(alpha = 0.2f)
                            else ElectricMagenta.copy(alpha = 0.25f)
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = room.category,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfficial) GoldAccent else Color(0xFFFF85C0)
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// HELPER COMPOSABLES & FUNCTIONS
// --------------------------------------------------------------------------------
@Composable
fun LivePulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(LiveRed)
    )
}

fun formatViewerCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

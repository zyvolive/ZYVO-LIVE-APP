package com.example.zyvo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.R
import com.example.zyvo.model.LeaderboardCategory
import com.example.zyvo.model.LeaderboardItem
import com.example.zyvo.model.LeaderboardTimeframe
import com.example.zyvo.model.VipTier
import com.example.zyvo.ui.components.ExecutiveAvatar
import com.example.zyvo.ui.components.openWhatsAppChat
import com.example.zyvo.ui.theme.*

@Composable
fun RankingsScreen(
    onUserClick: (String) -> Unit,
    onFollowUser: (String) -> Unit,
    onUnfollowUser: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(LeaderboardCategory.TOP_HOSTS) }
    var selectedTimeframe by remember { mutableStateOf(LeaderboardTimeframe.DAILY) }

    val mockLeaderboardItems = remember(selectedCategory, selectedTimeframe) {
        listOf(
            LeaderboardItem(
                rank = 1,
                userId = "ceo_rayan",
                username = "rayan_mirza",
                displayName = "RAYAN MIRZA (FOUNDER & CEO)",
                avatarEmoji = "👑",
                avatarUrl = "https://cdn.phototourl.com/free/2026-09-01-f3e014af-6987-41b0-8bcf-732294379e68.png",
                scorePoints = 99999999,
                metricLabel = "Gems",
                vipTier = VipTier.VIP_9,
                userLevel = 99,
                isLiveNow = true,
                isFollowing = true,
                executiveRole = "FOUNDER & CEO",
                whatsappDirectUrl = "https://wa.me/447868713315"
            ),
            LeaderboardItem(
                rank = 2,
                userId = "co_founder_alpha",
                username = "alpha_rajpoot",
                displayName = "ALPHA RAJPOOT (CO-FOUNDER)",
                avatarEmoji = "🦁",
                avatarUrl = "https://cdn.phototourl.com/free/2026-09-01-4aa927e1-ee25-497a-ae9e-4201e9d81679.jpg",
                scorePoints = 88888888,
                metricLabel = "Gems",
                vipTier = VipTier.VIP_9,
                userLevel = 99,
                isLiveNow = true,
                isFollowing = true,
                executiveRole = "CO-FOUNDER",
                whatsappDirectUrl = "https://wa.me/447366387620"
            ),
            LeaderboardItem(
                rank = 3,
                userId = "ansharah_gahni",
                username = "ansharah_gahni",
                displayName = "ANSHARAH GAHNI",
                avatarEmoji = "👸",
                avatarUrl = "https://mp3tourl.com/images/1788287833535-dc94ba6e-5e98-4349-b949-cd1521ff4618.jpg",
                scorePoints = 78500000,
                metricLabel = "Gems",
                vipTier = VipTier.SVIP_7,
                userLevel = 89,
                isLiveNow = true,
                isFollowing = true,
                executiveRole = "TOP HOST QUEEN"
            ),
            LeaderboardItem(4, "pixel_queen", "pixel_queen", "Elena 'PixelQueen'", "🎮", null, 690000, "Gems", VipTier.SVIP_2, 35, true, true),
            LeaderboardItem(4, "apex_arenas", "apex_arenas", "Apex Arenas", "⚔️", null, 520000, "Gems", VipTier.SVIP_1, 31, true, false),
            LeaderboardItem(5, "dj_kai", "kai_sterling", "Kai Sterling", "🎧", null, 284000, "Gems", VipTier.VIP_5, 28, true, true),
            LeaderboardItem(6, "marcus_voice", "marcus_vance", "Marcus Vance", "☕", null, 145000, "Gems", VipTier.VIP_2, 22, true, true),
            LeaderboardItem(7, "user_me", "alex_vance", "Alex Vance (You)", "🚀", null, 34250, "Gems", VipTier.VIP_3, 14, false, false)
        )
    }

    Scaffold(
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Leaderboard & Hall of Fame 🏆",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            // Category Selector Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        LeaderboardCategory.TOP_HOSTS to "🏆 Hosts",
                        LeaderboardCategory.TOP_GIVERS to "💎 Givers",
                        LeaderboardCategory.PK_CHAMPIONS to "⚔️ PK",
                        LeaderboardCategory.GAINED_FOLLOWERS to "📈 Stars"
                    ).forEach { (cat, label) ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonPurple else DarkSurface)
                                .border(1.dp, if (isSelected) NeonCyan else OverlayLight, RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (cat == LeaderboardCategory.PK_CHAMPIONS) {
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
                                    Text(
                                        text = "PK",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                } else {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Timeframe Selector Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LeaderboardTimeframe.entries.forEach { timeframe ->
                        val isSelected = selectedTimeframe == timeframe
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) NeonPurpleDark else DarkSurface)
                                .border(1.dp, if (isSelected) NeonCyan else OverlayLight, RoundedCornerShape(20.dp))
                                .clickable { selectedTimeframe = timeframe }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = timeframe.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TextPrimary else TextMuted
                            )
                        }
                    }
                }
            }

            // Top 3 Podium Cards
            item {
                val top1 = mockLeaderboardItems.find { it.rank == 1 }
                val top2 = mockLeaderboardItems.find { it.rank == 2 }
                val top3 = mockLeaderboardItems.find { it.rank == 3 }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Rank 2 (Left)
                    if (top2 != null) {
                        PodiumCard(
                            item = top2,
                            badgeColor = Color(0xFFC0C0C0),
                            crownEmoji = "🥈",
                            height = 160.dp,
                            modifier = Modifier.weight(1f),
                            onClick = { onUserClick(top2.userId) }
                        )
                    }

                    // Rank 1 (Center - Taller & Glowing)
                    if (top1 != null) {
                        PodiumCard(
                            item = top1,
                            badgeColor = GoldAccent,
                            crownEmoji = "👑",
                            height = 190.dp,
                            modifier = Modifier.weight(1.1f),
                            onClick = { onUserClick(top1.userId) }
                        )
                    }

                    // Rank 3 (Right)
                    if (top3 != null) {
                        PodiumCard(
                            item = top3,
                            badgeColor = Color(0xFFCD7F32),
                            crownEmoji = "🥉",
                            height = 145.dp,
                            modifier = Modifier.weight(1f),
                            onClick = { onUserClick(top3.userId) }
                        )
                    }
                }
            }

            // Ranks 4-50 List
            itemsIndexed(mockLeaderboardItems.drop(3)) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
                        .clickable { onUserClick(item.userId) }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rank Number Badge
                            Text(
                                text = "#${item.rank}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = TextMuted,
                                modifier = Modifier.width(32.dp)
                            )

                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(DarkCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = item.avatarEmoji, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = item.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (item.vipTier != VipTier.NONE) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = item.vipTier.badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                    }
                                }
                                Text(text = "@${item.username}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "${item.scorePoints}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldAccent)
                            Text(text = item.metricLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun PodiumCard(
    item: LeaderboardItem,
    badgeColor: Color,
    crownEmoji: String,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        badgeColor.copy(alpha = 0.25f),
                        DarkSurface
                    )
                )
            )
            .border(1.5.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = crownEmoji, fontSize = 20.sp)
                if (item.whatsappDirectUrl != null) {
                    val phone = if (item.userId == "ceo_rayan") "+44 7868 713315" else "+447366 387620"
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366))
                            .clickable {
                                openWhatsAppChat(context, phone, item.displayName)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💬", fontSize = 12.sp)
                    }
                }
            }

            if (item.vipTier == VipTier.VIP_9 || !item.avatarUrl.isNullOrBlank()) {
                ExecutiveAvatar(
                    avatarUrl = item.avatarUrl,
                    avatarEmoji = item.avatarEmoji,
                    size = 50.dp,
                    userLevel = item.userLevel,
                    vipTier = item.vipTier,
                    showCrown = false,
                    showLevelBadge = true
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkBackground)
                        .border(2.dp, badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item.avatarEmoji, fontSize = 24.sp)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${item.scorePoints / 1000}k",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = badgeColor
                )
            }
        }
    }
}

package com.example.zyvo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioAnalyticsScreen(
    coinBalance: Int,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Studio Analytics & SDK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Broadcaster Profile Overview Card
                BroadcasterProfileCard(coinBalance = coinBalance)
            }

            item {
                Text(
                    text = "LIFETIME BROADCAST STATS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccessTime,
                        iconTint = NeonPurple,
                        title = "128.5 hrs",
                        subtitle = "Stream Time"
                    )
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingUp,
                        iconTint = ElectricMagenta,
                        title = "14.2k",
                        subtitle = "Peak Viewers"
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Diamond,
                        iconTint = GoldAccent,
                        title = "48,920",
                        subtitle = "Gems Earned"
                    )
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PeopleAlt,
                        iconTint = NeonCyan,
                        title = "8,430",
                        subtitle = "Followers"
                    )
                }
            }

            item {
                // LiveKit / WebRTC SDK Server Status Card
                Text(
                    text = "LIVEKIT / WEBRTC SDK STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SdkStatusCard()
            }

            item {
                // Stream History Log
                Text(
                    text = "RECENT SESSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                StreamHistoryCard(
                    title = "⚡ Cyberpunk Synthwave DJ Set",
                    mode = "Single Live",
                    duration = "2h 45m",
                    viewers = "4,280 peak",
                    coins = "+3,850 🪙"
                )
                Spacer(modifier = Modifier.height(8.dp))
                StreamHistoryCard(
                    title = "⚔️ PK Duel vs Neon Phoenix",
                    mode = "PK Battle",
                    duration = "1h 12m",
                    viewers = "7,890 peak",
                    coins = "+8,420 🪙"
                )
                Spacer(modifier = Modifier.height(8.dp))
                StreamHistoryCard(
                    title = "🎙️ Midnight Chill Podcast",
                    mode = "Audio Stage",
                    duration = "3h 20m",
                    viewers = "1,850 peak",
                    coins = "+2,100 🪙"
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun BroadcasterProfileCard(coinBalance: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        DarkSurface,
                        DarkCard
                    )
                )
            )
            .border(1.dp, OverlayLight, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPurple, ElectricMagenta)))
                    .border(2.dp, GoldAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚀", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Alex Rivera",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonPurpleDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "PRO BROADCASTER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@alex_rivera • Level 42 Studio Creator",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Wallet Balance:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "$coinBalance Coins 🪙", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldAccent)
                }
            }
        }
    }
}

@Composable
fun MetricBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = subtitle,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
fun SdkStatusCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LiveKit Server Service",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Operational",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Protocol: WebRTC + WebSocket Signaling",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = "Supported Modules: Single Live, Multi-Guest (6 slots), Audio Stage (9 seats), PK Battle 1v1, Squad 4v4",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun StreamHistoryCard(
    title: String,
    mode: String,
    duration: String,
    viewers: String,
    coins: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$mode • $duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewers,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
            }
            Text(
                text = coins,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
        }
    }
}

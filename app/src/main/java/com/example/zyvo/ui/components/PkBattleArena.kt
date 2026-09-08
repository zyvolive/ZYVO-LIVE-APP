package com.example.zyvo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.model.BeautifyFilter
import com.example.zyvo.model.PkState
import com.example.zyvo.ui.theme.*

@Composable
fun PkBattleArena(
    modifier: Modifier = Modifier,
    pkState: PkState,
    hostName: String,
    hostAvatar: String,
    hostAvatarUrl: String? = null,
    hostCoverUrl: String? = null,
    targetHostAvatarUrl: String? = null,
    filter: BeautifyFilter = BeautifyFilter.ORIGINAL,
    hostPreview: (@Composable () -> Unit)? = null,
    isHostSpeaking: Boolean = true,
    isHostMuted: Boolean = false,
    isHostVideoOff: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pk_pulse")
    val vsScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vs_scale"
    )

    val totalScore = (pkState.myScore + pkState.targetScore).coerceAtLeast(1)
    val myRatio = (pkState.myScore.toFloat() / totalScore).coerceIn(0.1f, 0.9f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // PK Tug-of-War Score Bar & Timer Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkBackground)
                .border(1.dp, OverlayLight, RoundedCornerShape(12.dp))
                .padding(6.dp)
        ) {
            Column {
                // Scores & Timer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Score (My Team)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pkState.myScore}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = CyberBlue
                        )
                    }

                    // Center PK Timer Badge
                    val minutes = pkState.remainingSeconds / 60
                    val seconds = pkState.remainingSeconds % 60
                    val timeStr = String.format("%02d:%02d", minutes, seconds)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(PkRed, NeonPurple)))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PK $timeStr",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Right Score (Rival Team)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${pkState.targetScore}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = PkRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "⚡", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tug of War Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(myRatio)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CyberBlue, NeonCyan)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - myRatio)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PkRed, ElectricMagenta)
                                )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Split-Screen Dual Video Feeds
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Host Video (Left)
                VideoFeedTile(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    hostName = hostName,
                    avatarEmoji = hostAvatar,
                    avatarUrl = hostAvatarUrl,
                    roomCoverUrl = hostCoverUrl,
                    isSpeaking = isHostSpeaking,
                    isMuted = isHostMuted,
                    isVideoOff = isHostVideoOff,
                    filter = filter,
                    badgeText = "MY STREAM",
                    badgeColor = CyberBlue,
                    localCameraPreview = hostPreview
                )

                // Rival Video (Right)
                VideoFeedTile(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    hostName = pkState.targetHostName,
                    avatarEmoji = pkState.targetHostAvatar,
                    avatarUrl = targetHostAvatarUrl,
                    isSpeaking = false,
                    badgeText = "RIVAL STREAM",
                    badgeColor = PkRed
                )
            }

            // Center Glowing VS Emblem
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(ElectricMagenta, PkRed, DarkBackground)
                        )
                    )
                    .border(2.dp, GoldAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VS",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

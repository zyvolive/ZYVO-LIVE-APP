package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.R
import com.example.zyvo.ui.theme.*

@Composable
fun BroadcasterToolbar(
    modifier: Modifier = Modifier,
    isHost: Boolean,
    isMicMuted: Boolean,
    isVideoMuted: Boolean,
    onFlipCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleVideo: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenSoundboard: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenParticipants: () -> Unit,
    onOpenRoomCover: () -> Unit = {},
    onTogglePkMode: () -> Unit = {},
    onRaiseHand: () -> Unit,
    isHandRaised: Boolean = false,
    onShare: () -> Unit = {}
) {
    if (isHost) {
        // HOST ONLY BROADCAST CONTROLS
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(OverlayBackground)
                .border(1.dp, OverlayLight, RoundedCornerShape(20.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Room Cover & Profile Pic Studio
            ToolbarIconButton(
                icon = Icons.Default.Wallpaper,
                label = "Cover/Pic",
                tint = ElectricMagenta,
                onClick = onOpenRoomCover,
                testTag = "toolbar_cover_button"
            )

            // PK Battle Host Option Button
            ToolbarDrawableIconButton(
                drawableRes = R.drawable.ic_pk_battle_custom,
                label = "PK Battle",
                onClick = onTogglePkMode,
                testTag = "toolbar_pk_battle_button"
            )

            // Mic Toggle
            ToolbarIconButton(
                icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMicMuted) "Muted" else "Mic",
                tint = if (isMicMuted) PkRed else TextPrimary,
                onClick = onToggleMic,
                testTag = "toolbar_mic_button"
            )

            // Video Toggle
            ToolbarIconButton(
                icon = if (isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam,
                label = if (isVideoMuted) "Cam Off" else "Camera",
                tint = if (isVideoMuted) PkRed else TextPrimary,
                onClick = onToggleVideo,
                testTag = "toolbar_video_button"
            )

            // Flip Camera
            ToolbarIconButton(
                icon = Icons.Default.FlipCameraAndroid,
                label = "Flip",
                tint = NeonCyan,
                onClick = onFlipCamera,
                testTag = "toolbar_flip_button"
            )

            // Beautify Filters
            ToolbarIconButton(
                icon = Icons.Default.AutoFixHigh,
                label = "Filters",
                tint = NeonPurpleLight,
                onClick = onOpenFilters,
                testTag = "toolbar_filter_button"
            )

            // Soundboard SFX
            ToolbarIconButton(
                icon = Icons.Default.GraphicEq,
                label = "SFX",
                tint = GoldAccent,
                onClick = onOpenSoundboard,
                testTag = "toolbar_sfx_button"
            )

            // Manage Viewers & Moderation
            ToolbarIconButton(
                icon = Icons.Default.People,
                label = "Manage",
                tint = TextPrimary,
                onClick = onOpenParticipants,
                testTag = "toolbar_users_button"
            )

            // Host Telemetry & Stats
            ToolbarIconButton(
                icon = Icons.Default.BarChart,
                label = "Stats",
                tint = EmeraldGreen,
                onClick = onOpenStats,
                testTag = "toolbar_stats_button"
            )
        }
    } else {
        // VIEWER ONLY TOOLBAR (Zero Host Controls)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(OverlayBackground)
                .border(1.dp, OverlayLight, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Viewers list
            ToolbarIconButton(
                icon = Icons.Default.People,
                label = "Viewers",
                tint = TextPrimary,
                onClick = onOpenParticipants,
                testTag = "viewer_toolbar_users_button"
            )

            // Room Cover View / Details
            ToolbarIconButton(
                icon = Icons.Default.Wallpaper,
                label = "Cover",
                tint = ElectricMagenta,
                onClick = onOpenRoomCover,
                testTag = "viewer_toolbar_cover_button"
            )

            // Request Stage / Raise Hand
            ToolbarIconButton(
                icon = Icons.Default.PanTool,
                label = if (isHandRaised) "Raised" else "Guest Mic",
                tint = if (isHandRaised) GoldAccent else TextPrimary,
                onClick = onRaiseHand,
                testTag = "viewer_toolbar_hand_button"
            )

            // Share Stream
            ToolbarIconButton(
                icon = Icons.Default.Share,
                label = "Share",
                tint = NeonCyan,
                onClick = onShare,
                testTag = "viewer_toolbar_share_button"
            )
        }
    }
}

@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DarkCardElevated.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ToolbarDrawableIconButton(
    drawableRes: Int,
    label: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DarkCardElevated.copy(alpha = 0.8f))
                .border(1.dp, PkRed.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(drawableRes)
                    .crossfade(true)
                    .build(),
                contentDescription = label,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = PkRed,
            fontWeight = FontWeight.Bold
        )
    }
}

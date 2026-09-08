package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.model.BeautifyFilter
import com.example.zyvo.model.Seat
import com.example.zyvo.ui.theme.*

@Composable
fun MultiGuestVideoGrid(
    modifier: Modifier = Modifier,
    seats: List<Seat>,
    hostName: String,
    hostAvatar: String,
    hostAvatarUrl: String? = null,
    hostCoverUrl: String? = null,
    filter: BeautifyFilter = BeautifyFilter.ORIGINAL,
    hostPreview: (@Composable () -> Unit)? = null,
    isHostSpeaking: Boolean = true,
    isHostMuted: Boolean = false,
    isHostVideoOff: Boolean = false,
    onSeatClick: (Seat) -> Unit = {}
) {
    val displaySeats = if (seats.size >= 5) seats.take(5) else {
        seats + (seats.size + 1..5).map { Seat(id = it) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Host Video Tile (Left) + Guest 1 Video Tile (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                badgeText = "👑 HOST",
                badgeColor = NeonPurple,
                filter = filter,
                localCameraPreview = hostPreview
            )

            val seat1 = displaySeats.getOrNull(0)
            if (seat1 != null && seat1.occupied) {
                VideoFeedTile(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    hostName = seat1.participantName ?: "Guest 1",
                    avatarEmoji = seat1.avatarEmoji,
                    isSpeaking = seat1.isSpeaking,
                    badgeText = "GUEST 1",
                    badgeColor = ElectricMagenta
                )
            } else {
                EmptySeatVideoBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    seatId = 1,
                    isLocked = seat1?.locked == true,
                    onClick = { if (seat1 != null) onSeatClick(seat1) }
                )
            }
        }

        // Row 2: Guest 2, 3, 4 Small Video Tiles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..3).forEach { index ->
                val guestSeat = displaySeats.getOrNull(index)
                if (guestSeat != null && guestSeat.occupied) {
                    VideoFeedTile(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        hostName = guestSeat.participantName ?: "Guest ${index + 1}",
                        avatarEmoji = guestSeat.avatarEmoji,
                        isSpeaking = guestSeat.isSpeaking,
                        badgeText = "GUEST ${index + 1}",
                        badgeColor = NeonCyan
                    )
                } else {
                    EmptySeatVideoBox(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        seatId = index + 1,
                        isLocked = guestSeat?.locked == true,
                        onClick = { if (guestSeat != null) onSeatClick(guestSeat) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySeatVideoBox(
    modifier: Modifier = Modifier,
    seatId: Int,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(
                1.dp,
                if (isLocked) OverlayLight else NeonPurple.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Add,
                    contentDescription = if (isLocked) "Locked Slot" else "Join Stage",
                    tint = if (isLocked) TextMuted else NeonPurpleLight,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isLocked) "Seat $seatId (Locked)" else "+ Join Slot $seatId",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (isLocked) TextMuted else NeonCyan,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

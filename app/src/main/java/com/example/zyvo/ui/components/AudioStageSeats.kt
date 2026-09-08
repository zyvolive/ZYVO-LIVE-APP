package com.example.zyvo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.model.Seat
import com.example.zyvo.ui.theme.*

@Composable
fun AudioStageSeats(
    modifier: Modifier = Modifier,
    seats: List<Seat>,
    isHost: Boolean = false,
    onSeatClick: (Seat) -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkSurface.copy(alpha = 0.95f),
                        DarkCard.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, OverlayLight, RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Stage Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎙️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Audio Stage (${seats.count { it.occupied }}/9 Speaking)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonPurpleDark.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Clubhouse Vibe",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3x3 Grid of 9 Seats
            val displaySeats = if (seats.size >= 9) seats.take(9) else {
                seats + (seats.size + 1..9).map { Seat(id = it) }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(280.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(displaySeats) { seat ->
                    AudioSeatItem(
                        seat = seat,
                        isHostSeat = seat.id == 1,
                        onClick = { onSeatClick(seat) }
                    )
                }
            }
        }
    }
}

@Composable
fun AudioSeatItem(
    seat: Seat,
    isHostSeat: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_ripple_${seat.id}")
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ripple_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                seat.locked -> {
                    // Locked Seat
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DarkBackground)
                            .border(1.dp, OverlayLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked Seat",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                seat.occupied -> {
                    // Occupied Seat
                    if (seat.isSpeaking) {
                        // Speaking Wave Ring
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = NeonCyan.copy(alpha = rippleAlpha),
                                    shape = CircleShape
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isHostSeat) Brush.linearGradient(listOf(NeonPurple, ElectricMagenta))
                                else Brush.linearGradient(listOf(DarkCardElevated, DarkCard))
                            )
                            .border(
                                width = if (isHostSeat) 2.dp else 1.dp,
                                color = if (isHostSeat) GoldAccent else OverlayLight,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = seat.avatarEmoji,
                            fontSize = 24.sp
                        )
                    }

                    // Host Crown / Seat Indicator
                    if (isHostSeat) {
                        Text(
                            text = "👑",
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }

                    // Mute Badge
                    if (seat.isMuted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(PkRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = "Muted",
                                tint = TextPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
                else -> {
                    // Empty Open Seat (Tap to take seat)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DarkBackground.copy(alpha = 0.6f))
                            .border(
                                width = 1.dp,
                                color = NeonPurple.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Take Seat",
                            tint = NeonPurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Seat Name / Number Label
        Text(
            text = when {
                seat.locked -> "Seat ${seat.id}"
                seat.occupied -> seat.participantName ?: "User"
                else -> "Seat ${seat.id}"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (seat.occupied) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                seat.occupied && isHostSeat -> GoldAccent
                seat.occupied -> TextPrimary
                else -> TextMuted
            },
            maxLines = 1,
            textAlign = TextAlign.Center,
            fontSize = 11.sp
        )
    }
}

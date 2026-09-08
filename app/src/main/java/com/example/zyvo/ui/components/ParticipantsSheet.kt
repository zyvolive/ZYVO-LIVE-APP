package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.model.Participant
import com.example.zyvo.model.ParticipantRole
import com.example.zyvo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsSheet(
    participants: List<Participant>,
    currentUserId: String,
    isHostOrAdmin: Boolean,
    onDismiss: () -> Unit,
    onInviteToStage: (String) -> Unit,
    onRemoveFromStage: (String) -> Unit,
    onMakeAdmin: (String) -> Unit,
    onRemoveAdmin: (String) -> Unit,
    onMuteAudio: (String, Boolean) -> Unit,
    onBlockParticipant: (String) -> Unit
) {
    val handRaiseQueue = participants.filter { it.isRequestedToCall || it.isReqToPresent }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = OverlayDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👥", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Participants & Moderation (${participants.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (handRaiseQueue.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricMagenta)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${handRaiseQueue.size} Hands Raised ✋",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stage Hand Raise Approval Queue (if host/admin)
            if (isHostOrAdmin && handRaiseQueue.isNotEmpty()) {
                Text(
                    text = "STAGE REQUESTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    handRaiseQueue.forEach { requester ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = requester.avatar, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = requester.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Requested speaking slot",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onInviteToStage(requester.identity) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonPurple,
                                        contentColor = TextPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Full Participant List
            Text(
                text = "ALL AUDIENCE & SPEAKERS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(participants, key = { it.identity }) { participant ->
                    ParticipantRow(
                        participant = participant,
                        isCurrentUser = participant.identity == currentUserId,
                        canModerate = isHostOrAdmin && participant.identity != currentUserId,
                        onInviteToStage = { onInviteToStage(participant.identity) },
                        onRemoveFromStage = { onRemoveFromStage(participant.identity) },
                        onMakeAdmin = { onMakeAdmin(participant.identity) },
                        onRemoveAdmin = { onRemoveAdmin(participant.identity) },
                        onMuteAudio = { onMuteAudio(participant.identity, !participant.isMutedAudio) },
                        onBlock = { onBlockParticipant(participant.identity) }
                    )
                }
            }
        }
    }
}

@Composable
fun ParticipantRow(
    participant: Participant,
    isCurrentUser: Boolean,
    canModerate: Boolean,
    onInviteToStage: () -> Unit,
    onRemoveFromStage: () -> Unit,
    onMakeAdmin: () -> Unit,
    onRemoveAdmin: () -> Unit,
    onMuteAudio: () -> Unit,
    onBlock: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Avatar + Name + Role Badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(text = participant.avatar, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = participant.name + if (isCurrentUser) " (You)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    RoleBadge(role = participant.role)
                }
                Text(
                    text = "${participant.giftPoints} Gift points",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent
                )
            }
        }

        // Moderation Action Menu Trigger
        if (canModerate) {
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "User Options",
                        tint = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    modifier = Modifier.background(DarkCardElevated)
                ) {
                    if (participant.role == ParticipantRole.STAGE_SPEAKER) {
                        DropdownMenuItem(
                            text = { Text("Move to Audience", color = TextPrimary) },
                            onClick = {
                                onRemoveFromStage()
                                expandedMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = TextMuted) }
                        )
                    } else if (participant.role == ParticipantRole.VIEWER) {
                        DropdownMenuItem(
                            text = { Text("Invite to Stage", color = NeonCyan) },
                            onClick = {
                                onInviteToStage()
                                expandedMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = NeonCyan) }
                        )
                    }

                    if (participant.role != ParticipantRole.ADMIN) {
                        DropdownMenuItem(
                            text = { Text("Promote to Admin", color = NeonPurpleLight) },
                            onClick = {
                                onMakeAdmin()
                                expandedMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = NeonPurpleLight) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Demote from Admin", color = TextSecondary) },
                            onClick = {
                                onRemoveAdmin()
                                expandedMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.RemoveModerator, contentDescription = null, tint = TextSecondary) }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(if (participant.isMutedAudio) "Unmute Mic" else "Mute Mic", color = TextPrimary) },
                        onClick = {
                            onMuteAudio()
                            expandedMenu = false
                        },
                        leadingIcon = { Icon(if (participant.isMutedAudio) Icons.Default.Mic else Icons.Default.MicOff, contentDescription = null, tint = PkRed) }
                    )

                    DropdownMenuItem(
                        text = { Text("Block / Kick", color = PkRed) },
                        onClick = {
                            onBlock()
                            expandedMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = PkRed) }
                    )
                }
            }
        }
    }
}

@Composable
fun RoleBadge(role: ParticipantRole) {
    val (text, bgColor, textColor) = when (role) {
        ParticipantRole.HOST -> Triple("HOST", GoldAccent, DarkBackground)
        ParticipantRole.ADMIN -> Triple("ADMIN", NeonPurple, TextPrimary)
        ParticipantRole.STAGE_SPEAKER, ParticipantRole.STAGE_GUEST -> Triple("STAGE", NeonCyan, DarkBackground)
        ParticipantRole.VIEWER -> Triple("VIEWER", DarkBackground, TextMuted)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

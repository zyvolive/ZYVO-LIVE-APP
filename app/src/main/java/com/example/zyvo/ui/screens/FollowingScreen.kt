package com.example.zyvo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.model.ConversationSummary
import com.example.zyvo.model.LiveRoom
import com.example.zyvo.model.UserProfile
import com.example.zyvo.ui.theme.*

@Composable
fun FollowingScreen(
    followedRooms: List<LiveRoom>,
    followingProfiles: List<UserProfile>,
    conversations: List<ConversationSummary>,
    onRoomClick: (LiveRoom) -> Unit,
    onUserClick: (String) -> Unit,
    onOpenDm: (String) -> Unit
) {
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
                    text = "Following & Direct Messages 👥",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            // Live Now Followed Creators Section
            item {
                Text(
                    text = "LIVE NOW (${followedRooms.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = LiveRed
                )
            }

            if (followedRooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "None of your followed creators are live right now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(followedRooms, key = { it.id }) { room ->
                    LiveRoomCard(room = room, onClick = { onRoomClick(room) })
                }
            }

            // Direct Messages & Recent Chats
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DIRECT MESSAGES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            if (conversations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No message history yet. Tap any user profile to send a message!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(conversations, key = { it.peerUserId }) { conv ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
                            .clickable { onOpenDm(conv.peerUserId) }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(DarkCardElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = conv.peerAvatarEmoji, fontSize = 24.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = conv.peerDisplayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = getVerifiedTickColor(null, conv.peerDisplayName),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        if (conv.isPeerLive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(LiveRed)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(com.example.zyvo.R.drawable.ic_live_custom)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("LIVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                                                }
                                            }
                                        }
                                    }
                                    Text(text = conv.lastMessageText, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = conv.lastMessageTime, fontSize = 10.sp, color = TextMuted)
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Followed Creators Cards List
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FOLLOWED CREATORS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurpleLight
                )
            }

            items(followingProfiles, key = { it.userId }) { user ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
                        .clickable { onUserClick(user.userId) }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DarkCardElevated)
                                    .border(1.dp, NeonPurple, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = user.avatarEmoji, fontSize = 24.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(text = user.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "@${user.username} • ${user.followersCount} followers", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }

                        IconButton(onClick = { onOpenDm(user.userId) }) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Message", tint = NeonCyan)
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

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import com.example.zyvo.model.ConversationSummary
import com.example.zyvo.model.UserProfile
import com.example.zyvo.ui.theme.*

@Composable
fun ChatScreen(
    conversations: List<ConversationSummary>,
    followingProfiles: List<UserProfile>,
    onOpenDm: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Messages, 1: Friends, 2: Visitors

    Scaffold(
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sub-tabs: Messages, Friends, Visitors
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val tabs = listOf("Messages", "Friends", "Visitors")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedSubTab == index
                        Column(
                            modifier = Modifier
                                .clickable { selectedSubTab = index }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextMuted
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(ElectricMagenta)
                                )
                            }
                        }
                    }
                }

                // Add / New Chat button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(1.dp, OverlayLight, CircleShape)
                        .clickable {
                            if (followingProfiles.isNotEmpty()) {
                                onOpenDm(followingProfiles.first().userId)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Message",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tab Content
            when (selectedSubTab) {
                0 -> {
                    // MESSAGES TAB
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Default ZYVO System Announcement Item
                        item {
                            ChatItemRow(
                                avatar = "🔮",
                                displayName = "Official ZYVO",
                                messagePreview = "New event is coming! Join the PK Tournament 🏆",
                                timestamp = "09:30",
                                unreadCount = 3,
                                isSystem = true,
                                onClick = { /* System notification modal */ }
                            )
                        }

                        items(conversations, key = { it.peerUserId }) { conv ->
                            ChatItemRow(
                                avatar = conv.peerAvatarEmoji,
                                displayName = conv.peerDisplayName,
                                messagePreview = conv.lastMessageText,
                                timestamp = conv.lastMessageTime,
                                unreadCount = conv.unreadCount,
                                isLive = conv.isPeerLive,
                                onClick = { onOpenDm(conv.peerUserId) }
                            )
                        }

                        // Add mock/initial social conversations matching reference
                        if (conversations.none { it.peerUserId == "king_of_kings" }) {
                            item {
                                ChatItemRow(
                                    avatar = "👑",
                                    displayName = "King Of King's",
                                    messagePreview = "Sent you a gift 🌹",
                                    timestamp = "08:45",
                                    unreadCount = 1,
                                    onClick = { onOpenDm("king_of_kings") }
                                )
                            }
                        }

                        if (conversations.none { it.peerUserId == "maisha" }) {
                            item {
                                ChatItemRow(
                                    avatar = "🌸",
                                    displayName = "Maisha",
                                    messagePreview = "Let's PK today! 🔥",
                                    timestamp = "08:20",
                                    unreadCount = 2,
                                    onClick = { onOpenDm("maisha") }
                                )
                            }
                        }

                        if (conversations.none { it.peerUserId == "drama_queen" }) {
                            item {
                                ChatItemRow(
                                    avatar = "🦁",
                                    displayName = "Drama Queen",
                                    messagePreview = "How are you doing today?",
                                    timestamp = "Yesterday",
                                    unreadCount = 0,
                                    onClick = { onOpenDm("drama_queen") }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
                1 -> {
                    // FRIENDS TAB
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "FRIENDS (${followingProfiles.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        items(followingProfiles, key = { it.userId }) { friend ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
                                    .clickable { onUserClick(friend.userId) }
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
                                            Text(text = friend.avatarEmoji, fontSize = 24.sp)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = friend.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "@${friend.username} • ${friend.followersCount} followers",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onOpenDm(friend.userId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleDark),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Message", fontSize = 12.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
                2 -> {
                    // VISITORS TAB
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "👀", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Profile Visitors",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upgrade to VIP 7 to see complete profile visitor analytics and secret fans!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItemRow(
    avatar: String,
    displayName: String,
    messagePreview: String,
    timestamp: String,
    unreadCount: Int = 0,
    isLive: Boolean = false,
    isSystem: Boolean = false,
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
            // Avatar with online / live indicator
            Box {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isSystem) NeonPurpleDark else DarkCardElevated)
                        .border(
                            1.5.dp,
                            if (isSystem) GoldAccent else NeonPurple,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatar, fontSize = 26.sp)
                }

                if (isLive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(4.dp))
                            .background(LiveRed)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("LIVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = timestamp,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = messagePreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(ElectricMagenta),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$unreadCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.zyvo.model.UserProfile
import com.example.zyvo.ui.theme.*

@Composable
fun SettingsDialog(
    currentUserProfile: UserProfile,
    blockedUsers: List<UserProfile>,
    onDismiss: () -> Unit,
    onSaveProfile: (displayName: String, bio: String, gender: String, location: String, avatarEmoji: String) -> Unit,
    onUnblockUser: (userId: String) -> Unit,
    onLogout: (() -> Unit)? = null
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Edit Profile, 1: Preferences, 2: Blocked Users, 3: About

    var displayName by remember { mutableStateOf(currentUserProfile.displayName) }
    var bio by remember { mutableStateOf(currentUserProfile.bio) }
    var location by remember { mutableStateOf(currentUserProfile.location) }
    var gender by remember { mutableStateOf(currentUserProfile.gender) }
    var avatarEmoji by remember { mutableStateOf(currentUserProfile.avatarEmoji) }

    var notifyLiveAlerts by remember { mutableStateOf(true) }
    var notifyDmAlerts by remember { mutableStateOf(true) }
    var notifyGiftAlerts by remember { mutableStateOf(true) }

    val avatarOptions = listOf("🚀", "🎧", "🎮", "👑", "🌟", "🔥", "⚡", "👾", "☕", "🐉")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚙️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "App Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Edit Profile", "Preferences", "Blocked", "About").forEachIndexed { index, label ->
                        val isSelected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonPurple else DarkCardElevated)
                                .border(1.dp, if (isSelected) NeonCyan else OverlayLight, RoundedCornerShape(10.dp))
                                .clickable { activeTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) TextPrimary else TextSecondary)
                        }
                    }
                }

                when (activeTab) {
                    0 -> {
                        // Edit Profile Form
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "AVATAR EMOJI", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                avatarOptions.take(5).forEach { emoji ->
                                    val isSelected = avatarEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) NeonPurple else DarkCardElevated)
                                            .border(1.dp, if (isSelected) GoldAccent else OverlayLight, RoundedCornerShape(10.dp))
                                            .clickable { avatarEmoji = emoji },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 20.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan, unfocusedBorderColor = OverlayLight, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Profile Bio", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan, unfocusedBorderColor = OverlayLight, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Location / Country", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan, unfocusedBorderColor = OverlayLight, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )
                        }
                    }

                    1 -> {
                        // Notifications & Security
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Live Stream Notifications", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = notifyLiveAlerts, onCheckedChange = { notifyLiveAlerts = it })
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Direct Message Alerts", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = notifyDmAlerts, onCheckedChange = { notifyDmAlerts = it })
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Gift & Coin Notifications", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = notifyGiftAlerts, onCheckedChange = { notifyGiftAlerts = it })
                            }
                        }
                    }

                    2 -> {
                        // Blocked Users
                        if (blockedUsers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "No blocked users.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(blockedUsers, key = { it.userId }) { u ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarkCardElevated)
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = u.avatarEmoji, fontSize = 22.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = u.displayName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                        }

                                        TextButton(onClick = { onUnblockUser(u.userId) }) {
                                            Text("Unblock", color = NeonCyan)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // About
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "⚡ Zyvo Live Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Version 3.4.0 (Build 8892)", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Real-time Live Streaming, PK Battle Arena, Multi-Guest Video, Audio Stage, & Social Wallet Platform.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Privacy Policy • Terms of Service • License", fontSize = 11.sp, color = TextMuted)
                            
                            if (onLogout != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onLogout()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCardElevated),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(LiveRed, ElectricMagenta)))
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = "Log Out", tint = LiveRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign Out / Logout Account", color = LiveRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (activeTab == 0) {
                Button(
                    onClick = {
                        onSaveProfile(displayName, bio, gender, location, avatarEmoji)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = NeonCyan)
                }
            }
        }
    )
}

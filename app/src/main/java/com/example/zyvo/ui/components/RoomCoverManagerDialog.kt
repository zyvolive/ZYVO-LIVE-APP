package com.example.zyvo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.model.LiveRoom
import com.example.zyvo.model.UserProfile
import com.example.zyvo.ui.theme.*

data class CoverPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val imageUrl: String,
    val accentColor: Color
)

val LIVE_ROOM_COVER_PRESETS = listOf(
    CoverPreset(
        id = "ceo_exec_suite",
        title = "Executive Presidential Suite",
        subtitle = "Rayan Mirza CEO Desk",
        iconEmoji = "👑",
        imageUrl = "https://cdn.phototourl.com/free/2026-09-01-f3e014af-6987-41b0-8bcf-732294379e68.png",
        accentColor = GoldAccent
    ),
    CoverPreset(
        id = "pk_arena_cyber",
        title = "PK Arena & Cyberpunk Stadium",
        subtitle = "Alpha Rajpoot Battle Stage",
        iconEmoji = "⚔️",
        imageUrl = "https://cdn.phototourl.com/free/2026-09-01-4aa927e1-ee25-497a-ae9e-4201e9d81679.jpg",
        accentColor = NeonCyan
    ),
    CoverPreset(
        id = "top_host_gala",
        title = "Top Host Gala & Gem Runway",
        subtitle = "Ansharah Gahni Super Stage",
        iconEmoji = "🔮",
        imageUrl = "https://mp3tourl.com/images/1788287833535-dc94ba6e-5e98-4349-b949-cd1521ff4618.jpg",
        accentColor = ElectricMagenta
    ),
    CoverPreset(
        id = "dubai_skyline",
        title = "Dubai Penthouse Skyline",
        subtitle = "VIP Global Horizon Lounge",
        iconEmoji = "🏙️",
        imageUrl = "https://cdn.phototourl.com/free/2026-09-01-f3e014af-6987-41b0-8bcf-732294379e68.png",
        accentColor = EmeraldGreen
    ),
    CoverPreset(
        id = "cyber_neon_grid",
        title = "Holographic Cyber Neon Grid",
        subtitle = "Ultra DJ & EDM Stream",
        iconEmoji = "🎧",
        imageUrl = "https://cdn.phototourl.com/free/2026-09-01-4aa927e1-ee25-497a-ae9e-4201e9d81679.jpg",
        accentColor = NeonPurpleLight
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomCoverManagerDialog(
    room: LiveRoom,
    currentUserProfile: UserProfile,
    onDismiss: () -> Unit,
    onApplyRoomCover: (coverUrl: String, coverStyle: String) -> Unit,
    onUpdateBroadcasterProfilePic: (newAvatarUrl: String) -> Unit
) {
    var selectedCoverUrl by remember { mutableStateOf(room.roomCoverUrl ?: currentUserProfile.avatarUrl ?: "") }
    var selectedStyle by remember { mutableStateOf(room.coverStyle) }
    var customUrlInput by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    var showUpdateProfilePicInput by remember { mutableStateOf(false) }
    var newProfilePicInput by remember { mutableStateOf(currentUserProfile.avatarUrl ?: "") }
    var successToast by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = OverlayDark,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ElectricMagenta, NeonPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = "Room Cover",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Live Room Cover & Photo Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Customize live stream backdrop & broadcaster photo",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toast / Confirmation Pill
            if (successToast != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldGreen.copy(alpha = 0.2f))
                        .border(1.dp, EmeraldGreen, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✨", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successToast ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Real-Time Live Room Cover Preview Card
            Text(
                text = "LIVE BROADCAST PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkBackground)
                    .border(2.dp, Brush.linearGradient(listOf(GoldAccent, NeonCyan)), RoundedCornerShape(16.dp))
            ) {
                if (selectedCoverUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selectedCoverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.radialGradient(listOf(NeonPurpleDark, DarkBackground))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌟", fontSize = 54.sp)
                    }
                }

                // Overlay gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x99000000),
                                    Color.Transparent,
                                    Color(0xCC000000)
                                )
                            )
                        )
                )

                // Top Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LiveRed)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "🔴 LIVE BROADCAST", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xAA000000))
                            .border(1.dp, GoldAccent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "✨ $selectedStyle", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Center Spotlight Avatar if in STUDIO_STAGE style
                if (selectedStyle == "STUDIO_STAGE" && selectedCoverUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, GoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(selectedCoverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar Spotlight",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Bottom Host Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentUserProfile.avatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(currentUserProfile.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Host Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, GoldAccent, CircleShape)
                            )
                        } else {
                            Text(text = currentUserProfile.avatarEmoji, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = room.hostName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "Cover Active for ${room.viewerCount} Viewers",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action 1: 1-Tap Sync with Profile Picture
            val isUsingProfilePic = selectedCoverUrl == currentUserProfile.avatarUrl && !currentUserProfile.avatarUrl.isNullOrBlank()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val picUrl = currentUserProfile.avatarUrl
                        if (!picUrl.isNullOrBlank()) {
                            selectedCoverUrl = picUrl
                            onApplyRoomCover(picUrl, selectedStyle)
                            successToast = "✅ Broadcaster Profile Picture Applied as Room Cover!"
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isUsingProfilePic) DarkCardElevated else DarkCard),
                border = if (isUsingProfilePic) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(GoldAccent, ElectricMagenta))) else CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!currentUserProfile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(currentUserProfile.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Pic",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, GoldAccent, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = currentUserProfile.avatarEmoji, fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Use My Profile Photo as Cover",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isUsingProfilePic) "Currently Active on Live Stream" else "Tap to sync room cover with your avatar",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUsingProfilePic) EmeraldGreen else TextMuted
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val picUrl = currentUserProfile.avatarUrl
                            if (!picUrl.isNullOrBlank()) {
                                selectedCoverUrl = picUrl
                                onApplyRoomCover(picUrl, selectedStyle)
                                successToast = "✅ Broadcaster Profile Picture Applied as Room Cover!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isUsingProfilePic) EmeraldGreen else ElectricMagenta),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isUsingProfilePic) "Active ✓" else "Use Pic",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action 2: High-Definition Live Studio Backdrops Library
            Text(
                text = "CURATED LIVE STUDIO BACKDROPS",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(LIVE_ROOM_COVER_PRESETS) { preset ->
                    val isSelected = selectedCoverUrl == preset.imageUrl
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable {
                                selectedCoverUrl = preset.imageUrl
                                onApplyRoomCover(preset.imageUrl, selectedStyle)
                                successToast = "✅ Switched to ${preset.title}!"
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(preset.accentColor, GoldAccent))) else CardDefaults.outlinedCardBorder()
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(preset.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = preset.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xBB000000))
                                        .padding(4.dp)
                                ) {
                                    Text(text = preset.iconEmoji, fontSize = 12.sp)
                                }
                            }
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action 3: Cover Presentation Style Selector
            Text(
                text = "COVER PRESENTATION STYLE",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val styles = listOf(
                    "FULL_BACKDROP" to "Full Frame",
                    "STUDIO_STAGE" to "Spotlight Stage",
                    "CINEMATIC" to "Cinematic Ambient"
                )
                styles.forEach { (styleKey, styleLabel) ->
                    val isSelected = selectedStyle == styleKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonPurple else DarkCard)
                            .border(1.dp, if (isSelected) NeonCyan else OverlayLight, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedStyle = styleKey
                                onApplyRoomCover(selectedCoverUrl, styleKey)
                                successToast = "✅ Cover style changed to $styleLabel"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = styleLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action 4: Custom Cover Photo URL & Profile Pic Update Tools
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showCustomInput = !showCustomInput },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = "URL", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Custom URL", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { showUpdateProfilePicInput = !showUpdateProfilePicInput },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                ) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Profile Pic", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Update Avatar", fontSize = 11.sp)
                }
            }

            // Collapsible Custom Cover URL Input
            AnimatedVisibility(visible = showCustomInput) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Paste Custom Live Room Cover Photo URL:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        placeholder = { Text("https://example.com/cover.jpg", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (customUrlInput.isNotBlank()) {
                                selectedCoverUrl = customUrlInput
                                onApplyRoomCover(customUrlInput, selectedStyle)
                                successToast = "✅ Custom Room Cover Applied!"
                                showCustomInput = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply Custom Cover", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Collapsible Update Broadcaster Profile Picture
            AnimatedVisibility(visible = showUpdateProfilePicInput) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Update Broadcaster Profile Picture URL:",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newProfilePicInput,
                        onValueChange = { newProfilePicInput = it },
                        placeholder = { Text("https://example.com/my-photo.jpg", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newProfilePicInput.isNotBlank()) {
                                onUpdateBroadcasterProfilePic(newProfilePicInput)
                                selectedCoverUrl = newProfilePicInput
                                onApplyRoomCover(newProfilePicInput, selectedStyle)
                                successToast = "✅ Profile Picture Updated & Synced to Live Room Cover!"
                                showUpdateProfilePicInput = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save & Apply Everywhere", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Final Apply & Done Button
            Button(
                onClick = {
                    onApplyRoomCover(selectedCoverUrl, selectedStyle)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_room_cover_done"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricMagenta
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Done")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm & Apply Live Room Cover",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

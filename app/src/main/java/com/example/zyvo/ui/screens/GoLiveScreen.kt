package com.example.zyvo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.model.RoomType
import com.example.zyvo.ui.theme.*

@Composable
fun GoLiveScreen(
    onDismiss: () -> Unit,
    onStartLive: (title: String, roomType: RoomType, category: String, tags: List<String>) -> Unit
) {
    val context = LocalContext.current
    var titleText by remember { mutableStateOf("Let's have fun together ♥") }
    var selectedCategory by remember { mutableStateOf("Music") }
    var selectedRoomType by remember { mutableStateOf(RoomType.SINGLE_LIVE) }
    var allowGuest by remember { mutableStateOf(true) }
    var enablePk by remember { mutableStateOf(true) }
    var saveToAlbum by remember { mutableStateOf(false) }

    val categories = remember { listOf("Music", "Gaming", "Chitchat", "Dance", "Talent", "Esports") }
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }

    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val cameraGranted = results[Manifest.permission.CAMERA] ?: false
        val audioGranted = results[Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted && audioGranted) {
            onStartLive(titleText, selectedRoomType, selectedCategory, listOf(selectedCategory, "Live"))
        } else {
            // Proceed even if user granted at least one or allow entry with visual note
            onStartLive(titleText, selectedRoomType, selectedCategory, listOf(selectedCategory, "Live"))
        }
    }

    val requestPermissionsAndStart: () -> Unit = {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasCamera && hasAudio) {
            onStartLive(titleText, selectedRoomType, selectedCategory, listOf(selectedCategory, "Live"))
        } else {
            val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                Text(
                    text = "Go Live",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Select Stream Mode (Video Live, Audio Stage, Multi-Guest, PK, Team)
            Column {
                Text(
                    text = "Broadcast Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val roomTypes = listOf(
                    Triple(RoomType.SINGLE_LIVE, "Video Live", "📹"),
                    Triple(RoomType.AUDIO_STAGE, "Audio Live", "🎧"),
                    Triple(RoomType.MULTI_GUEST, "Multi-Guest", "👥"),
                    Triple(RoomType.PK_BATTLE, "PK Battle", "⚔️"),
                    Triple(RoomType.TEAM_MODE, "Team Battle", "🛡️")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    roomTypes.forEach { (type, label, icon) ->
                        val isSelected = selectedRoomType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFF38084A) else DarkSurface)
                                .border(
                                    1.5.dp,
                                    if (isSelected) ElectricMagenta else OverlayLight,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedRoomType = type }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (type == RoomType.SINGLE_LIVE) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(com.example.zyvo.R.drawable.ic_live_custom)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = label,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else if (type == RoomType.PK_BATTLE) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(com.example.zyvo.R.drawable.ic_pk_battle_custom)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = label,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else if (type == RoomType.AUDIO_STAGE) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(com.example.zyvo.R.drawable.ic_audio_live_custom)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = label,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Text(text = icon, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Live Title Input Field
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Live Title", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(text = "${titleText.length}/100", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { if (it.length <= 100) titleText = it },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("go_live_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricMagenta,
                        unfocusedBorderColor = OverlayLight,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }

            // Category Selection Dropdown
            Column {
                Text(text = "Category", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurface)
                        .border(1.dp, OverlayLight, RoundedCornerShape(14.dp))
                        .clickable { isCategoryMenuExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedCategory, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select", tint = TextMuted)
                    }

                    DropdownMenu(
                        expanded = isCategoryMenuExpanded,
                        onDismissRequest = { isCategoryMenuExpanded = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = TextPrimary) },
                                onClick = {
                                    selectedCategory = cat
                                    isCategoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Cover Thumbnails Section
            Column {
                Text(text = "Cover", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val covers = listOf("🎤", "🎧", "🌸")
                    covers.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurface)
                                .border(1.5.dp, NeonPurple, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 28.sp)
                        }
                    }

                    // Upload Button (+)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkCardElevated)
                            .border(1.dp, OverlayLight, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Cover", tint = TextMuted)
                    }
                }
            }

            // Mode Toggles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Allow Guest Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Allow Guest", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Switch(
                        checked = allowGuest,
                        onCheckedChange = { allowGuest = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = ElectricMagenta
                        )
                    )
                }

                // PK Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "PK Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Switch(
                        checked = enablePk,
                        onCheckedChange = { enablePk = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = ElectricMagenta
                        )
                    )
                }

                // Save to Album Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Save to Album", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Switch(
                        checked = saveToAlbum,
                        onCheckedChange = { saveToAlbum = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = ElectricMagenta
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start Live Gradient Button
            Button(
                onClick = requestPermissionsAndStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_live_submit_btn"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(ElectricMagenta, NeonPurple)
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start Live",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

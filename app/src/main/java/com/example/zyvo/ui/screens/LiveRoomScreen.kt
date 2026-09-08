package com.example.zyvo.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.model.*
import com.example.zyvo.rtc.MediaPermissionStatus
import com.example.zyvo.ui.components.*
import com.example.zyvo.ui.theme.*
import com.example.zyvo.ui.viewmodel.LiveStreamViewModel

@Composable
fun LiveRoomScreen(
    room: LiveRoom,
    viewModel: LiveStreamViewModel,
    onLeaveRoom: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val localMediaManager = viewModel.localMediaManager
    val localMediaState by viewModel.localMediaState.collectAsState()

    val currentRoomChat by viewModel.currentRoomChat.collectAsState()
    val currentRoomParticipants by viewModel.currentRoomParticipants.collectAsState()
    val activeFloatingGifts by viewModel.activeFloatingGifts.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isVideoMuted by viewModel.isVideoMuted.collectAsState()
    val userCoinBalance by viewModel.userCoinBalance.collectAsState()
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()

    val localVideoTrack by viewModel.localVideoTrack.collectAsState()
    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsState()
    val remoteAudioTrack by viewModel.remoteAudioTrack.collectAsState()
    val isFrontCamera by viewModel.cameraFacing.collectAsState()

    val signalingStatus by viewModel.signalingStatus.collectAsState()
    val iceConnectionState by viewModel.iceConnectionState.collectAsState()
    val peerConnectionState by viewModel.peerConnectionState.collectAsState()
    val iceCandidatesSentCount by viewModel.iceCandidatesSentCount.collectAsState()
    val iceCandidatesReceivedCount by viewModel.iceCandidatesReceivedCount.collectAsState()
    val pendingCandidatesCount by viewModel.pendingCandidatesCount.collectAsState()
    val webRtcError by viewModel.webRtcError.collectAsState()
    val authUser by viewModel.authCurrentUser.collectAsState()
    val currentUid = authUser?.uid ?: viewModel.currentUserIdentity
    var showDiagnosticPanel by remember { mutableStateOf(false) }

    val isHost = room.creatorIdentity == viewModel.currentUserIdentity

    var pendingPermissionCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val camGranted = permissions[Manifest.permission.CAMERA] ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        if (camGranted && micGranted && isHost) {
            viewModel.startHostSession(room.id)
        }
        pendingPermissionCallback?.invoke()
        pendingPermissionCallback = null
    }

    DisposableEffect(isHost, lifecycleOwner, room.id) {
        if (isHost) {
            val camGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!camGranted || !micGranted) {
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            } else {
                viewModel.startHostSession(room.id)
            }
        } else {
            viewModel.startViewerSession(room.id)
        }

        onDispose {
            viewModel.leaveRoomSession(isHost = isHost)
        }
    }

    val safeToggleMic: () -> Unit = {
        if (isMicMuted) {
            val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasMic) {
                viewModel.toggleMic()
            } else {
                pendingPermissionCallback = { viewModel.toggleMic() }
                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            }
        } else {
            viewModel.toggleMic()
        }
    }

    val safeToggleVideo: () -> Unit = {
        if (isVideoMuted) {
            val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (hasCam) {
                viewModel.toggleVideo()
            } else {
                pendingPermissionCallback = { viewModel.toggleVideo() }
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        } else {
            viewModel.toggleVideo()
        }
    }

    // Dialog states
    val showGiftDialog by viewModel.showGiftDialog.collectAsState()
    val showParticipantsSheet by viewModel.showParticipantsSheet.collectAsState()
    val showFilterSheet by viewModel.showFilterSheet.collectAsState()
    val showSoundboard by viewModel.showSoundboard.collectAsState()
    val showStreamStats by viewModel.showStreamStats.collectAsState()
    val showRoomCoverSheet by viewModel.showRoomCoverSheet.collectAsState()

    val currentUserParticipant = currentRoomParticipants.find { it.identity == viewModel.currentUserIdentity }
    val isHandRaised = currentUserParticipant?.isRequestedToCall == true || currentUserParticipant?.isReqToPresent == true

    val activeVideoComposable: (@Composable () -> Unit)? = if (isHost) {
        if (!isVideoMuted && localVideoTrack != null) {
            {
                WebRtcVideoView(
                    videoTrack = localVideoTrack,
                    isMirror = isFrontCamera,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else null
    } else {
        if (remoteVideoTrack != null) {
            {
                WebRtcVideoView(
                    videoTrack = remoteVideoTrack,
                    isMirror = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else null
    }

    Scaffold(
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                // Top Live Room Bar
                LiveRoomTopBar(
                    room = room,
                    isHost = isHost,
                    onLeave = onLeaveRoom,
                    onOpenParticipants = { viewModel.setShowParticipantsSheet(true) },
                    onOpenHostProfile = { viewModel.openUserProfile(room.creatorIdentity) },
                    onOpenRoomCover = { viewModel.setShowRoomCoverSheet(true) },
                    onToggleDiagnostics = { showDiagnosticPanel = !showDiagnosticPanel }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mode-Specific Stage Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (room.roomType) {
                        RoomType.SINGLE_LIVE -> {
                            VideoFeedTile(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                hostName = room.hostName,
                                avatarEmoji = room.hostAvatar,
                                avatarUrl = room.hostAvatarUrl,
                                roomCoverUrl = room.roomCoverUrl,
                                coverStyle = room.coverStyle,
                                isSpeaking = true,
                                isMuted = if (isHost) isMicMuted else false,
                                isVideoOff = if (isHost) isVideoMuted else false,
                                filter = currentFilter,
                                badgeText = "🔴 LIVE BROADCAST",
                                badgeColor = LiveRed,
                                localCameraPreview = activeVideoComposable
                            )
                        }
                        RoomType.MULTI_GUEST -> {
                            MultiGuestVideoGrid(
                                modifier = Modifier.fillMaxSize(),
                                seats = room.seats,
                                hostName = room.hostName,
                                hostAvatar = room.hostAvatar,
                                hostAvatarUrl = room.hostAvatarUrl,
                                hostCoverUrl = room.roomCoverUrl,
                                filter = currentFilter,
                                hostPreview = activeVideoComposable,
                                isHostSpeaking = true,
                                isHostMuted = if (isHost) isMicMuted else false,
                                isHostVideoOff = if (isHost) isVideoMuted else false,
                                onSeatClick = { seat ->
                                    if (!seat.occupied && !seat.locked) {
                                        viewModel.inviteParticipantToStage(viewModel.currentUserIdentity, seat.id)
                                    } else if (isHost) {
                                        viewModel.toggleSeatLock(seat.id)
                                    }
                                }
                            )
                        }
                        RoomType.AUDIO_STAGE -> {
                            AudioStageSeats(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                seats = room.seats,
                                isHost = isHost,
                                onSeatClick = { seat ->
                                    if (!seat.occupied && !seat.locked) {
                                        viewModel.requestToPresent(seat.id)
                                    } else if (isHost) {
                                        viewModel.toggleSeatLock(seat.id)
                                    }
                                }
                            )
                        }
                        RoomType.PK_BATTLE -> {
                            PkBattleArena(
                                modifier = Modifier.fillMaxSize(),
                                pkState = room.pkState,
                                hostName = room.hostName,
                                hostAvatar = room.hostAvatar,
                                hostAvatarUrl = room.hostAvatarUrl,
                                hostCoverUrl = room.roomCoverUrl,
                                filter = currentFilter,
                                hostPreview = activeVideoComposable,
                                isHostSpeaking = if (isHost) localMediaState.isSpeaking else true,
                                isHostMuted = if (isHost) localMediaState.isMicMuted else false,
                                isHostVideoOff = if (isHost) !localMediaState.isCameraEnabled else false
                            )
                        }
                        RoomType.TEAM_MODE -> {
                            TeamBattleArena(
                                modifier = Modifier.fillMaxSize(),
                                teamState = room.teamState,
                                hostName = room.hostName,
                                hostAvatar = room.hostAvatar,
                                hostAvatarUrl = room.hostAvatarUrl,
                                hostCoverUrl = room.roomCoverUrl,
                                filter = currentFilter,
                                hostPreview = activeVideoComposable,
                                isHostSpeaking = if (isHost) localMediaState.isSpeaking else true,
                                isHostMuted = if (isHost) localMediaState.isMicMuted else false,
                                isHostVideoOff = if (isHost) !localMediaState.isCameraEnabled else false
                            )
                        }
                    }

                    // Host Permissions Resolution Card (shown if permissions not granted)
                    val hasCamPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    val hasMicPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (isHost && (!hasCamPerm || !hasMicPerm)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                border = BorderStroke(1.dp, OverlayLight),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = ElectricMagenta,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Text(
                                        text = "Camera & Microphone Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "To broadcast live to your audience, ZYVO requires permission to access your camera and microphone.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = {
                                                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Grant Permissions", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", context.packageName, null)
                                                }
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, OverlayLight)
                                        ) {
                                            Text("Settings", color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Media Error Toast/Pill (dismissible)
                    if (webRtcError != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PkRed.copy(alpha = 0.9f))
                                .clickable { }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = webRtcError ?: "", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Real-time Chat & Gifting Overlay
                LiveChatOverlay(
                    modifier = Modifier.fillMaxWidth(),
                    messages = currentRoomChat,
                    floatingGifts = activeFloatingGifts,
                    enableChat = room.enableChat,
                    onSendMessage = { text -> viewModel.sendTextMessage(text) },
                    onSendLike = { viewModel.sendLike() },
                    onOpenGiftDialog = { viewModel.setShowGiftDialog(true) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Broadcaster & Viewer Toolbar
                BroadcasterToolbar(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    isHost = isHost,
                    isMicMuted = isMicMuted,
                    isVideoMuted = isVideoMuted,
                    onFlipCamera = { viewModel.flipCamera() },
                    onToggleMic = safeToggleMic,
                    onToggleVideo = safeToggleVideo,
                    onOpenFilters = { viewModel.setShowFilterSheet(true) },
                    onOpenSoundboard = { viewModel.setShowSoundboard(true) },
                    onOpenStats = { viewModel.setShowStreamStats(true) },
                    onOpenParticipants = { viewModel.setShowParticipantsSheet(true) },
                    onOpenRoomCover = { viewModel.setShowRoomCoverSheet(true) },
                    onRaiseHand = {
                        if (isHandRaised) viewModel.cancelRequestToPresent()
                        else viewModel.requestToPresent()
                    },
                    isHandRaised = isHandRaised
                )
            }

            // Bottom Sheets
            if (showRoomCoverSheet) {
                RoomCoverManagerDialog(
                    room = room,
                    currentUserProfile = currentUserProfile,
                    onDismiss = { viewModel.setShowRoomCoverSheet(false) },
                    onApplyRoomCover = { coverUrl, style ->
                        viewModel.updateRoomCover(room.id, coverUrl, style)
                    },
                    onUpdateBroadcasterProfilePic = { newPic ->
                        viewModel.updateBroadcasterProfilePic(room.creatorIdentity, newPic)
                    }
                )
            }

            if (showGiftDialog) {
                GiftDialog(
                    userCoinBalance = userCoinBalance,
                    onDismiss = { viewModel.setShowGiftDialog(false) },
                    onSendGift = { gift, count -> viewModel.sendGift(gift, count) }
                )
            }

            if (showParticipantsSheet) {
                ParticipantsSheet(
                    participants = currentRoomParticipants,
                    currentUserId = viewModel.currentUserIdentity,
                    isHostOrAdmin = isHost,
                    onDismiss = { viewModel.setShowParticipantsSheet(false) },
                    onInviteToStage = { id -> viewModel.inviteParticipantToStage(id) },
                    onRemoveFromStage = { id -> viewModel.removeParticipantFromStage(id) },
                    onMakeAdmin = { id -> viewModel.makeAdmin(id) },
                    onRemoveAdmin = { id -> viewModel.removeAdmin(id) },
                    onMuteAudio = { id, muted -> viewModel.muteParticipantAudio(id, muted) },
                    onBlockParticipant = { id -> viewModel.blockParticipant(id) }
                )
            }

            if (showFilterSheet) {
                BeautifyFilterSheet(
                    activeFilter = currentFilter,
                    onSelectFilter = { viewModel.setFilter(it) },
                    onDismiss = { viewModel.setShowFilterSheet(false) }
                )
            }

            if (showSoundboard) {
                AudioSoundboardDialog(
                    onDismiss = { viewModel.setShowSoundboard(false) },
                    onPlaySfx = { viewModel.playSfx(it) }
                )
            }

            if (showStreamStats) {
                StreamStatsDialog(
                    stats = room.streamStats,
                    onDismiss = { viewModel.setShowStreamStats(false) }
                )
            }

            // WebRTC Development Diagnostic Panel
            if (showDiagnosticPanel) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.95f)),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth()
                        .testTag("rtc_diagnostic_panel")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WebRTC & Signaling Diagnostics",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = NeonCyan
                            )
                            IconButton(
                                onClick = { showDiagnosticPanel = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Diagnostics", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        HorizontalDivider(color = OverlayLight, thickness = 0.5.dp)
                        DiagnosticRow("Room ID", room.id)
                        DiagnosticRow("Current UID", currentUid)
                        DiagnosticRow("Role", if (isHost) "HOST" else "VIEWER")
                        DiagnosticRow("Signaling state", signalingStatus)
                        DiagnosticRow("PeerConnection state", peerConnectionState)
                        DiagnosticRow("ICE connection state", iceConnectionState)
                        DiagnosticRow("LOCAL VIDEO TRACK", if (localVideoTrack != null) "YES" else "NO")
                        DiagnosticRow("LOCAL AUDIO TRACK", if (viewModel.localAudioTrack != null) "YES" else "NO")
                        DiagnosticRow("REMOTE VIDEO TRACK", if (remoteVideoTrack != null) "YES" else "NO")
                        DiagnosticRow("REMOTE AUDIO TRACK", if (remoteAudioTrack != null) "YES" else "NO")
                        DiagnosticRow("AUDIO MUTED", if (isMicMuted) "YES" else "NO")
                        DiagnosticRow("AUDIO SENDER", if (viewModel.isAudioSenderPresent) "PRESENT" else "MISSING")
                        DiagnosticRow("AUDIO TRANSCEIVER", if (viewModel.isAudioTransceiverPresent) "PRESENT" else "MISSING")
                        DiagnosticRow("REMOTE AUDIO RECEIVER", if (viewModel.isRemoteAudioReceiverPresent) "PRESENT" else "MISSING")
                        DiagnosticRow("ICE candidates sent", "$iceCandidatesSentCount")
                        DiagnosticRow("ICE candidates received", "$iceCandidatesReceivedCount")
                        DiagnosticRow("Pending ICE candidates", "$pendingCandidatesCount")
                        DiagnosticRow("WebRTC error", webRtcError ?: "none", isError = webRtcError != null)
                    }
                }
            }

            // Room Ended Overlay (when Host terminates broadcast)
            if (signalingStatus == "ROOM_ENDED" && !isHost) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = BorderStroke(1.dp, OverlayLight),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(text = "🎙️", fontSize = 36.sp)
                            Text(
                                text = "Broadcast Ended",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "The host has ended this live stream session. Thank you for joining!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onLeaveRoom,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("leave_ended_room_button")
                            ) {
                                Text("Leave Room", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveRoomTopBar(
    room: LiveRoom,
    isHost: Boolean,
    onLeave: () -> Unit,
    onOpenParticipants: () -> Unit,
    onOpenHostProfile: () -> Unit = {},
    onOpenRoomCover: () -> Unit = {},
    onToggleDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val hostPic = room.roomCoverUrl ?: room.hostAvatarUrl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Host Info Pill or Broadcast Status Pill
        if (isHost) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(LiveRed.copy(alpha = 0.85f), ElectricMagenta.copy(alpha = 0.85f))))
                    .border(1.dp, GoldAccent, RoundedCornerShape(24.dp))
                    .clickable { onOpenRoomCover() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!hostPic.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(hostPic)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Broadcaster Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "LIVE STUDIO 📸",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface.copy(alpha = 0.9f))
                    .border(1.dp, OverlayLight, RoundedCornerShape(24.dp))
                    .clickable { onOpenHostProfile() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!hostPic.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(hostPic)
                                .crossfade(true)
                                .build(),
                            contentDescription = room.hostName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, GoldAccent, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = room.hostAvatar, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = room.hostName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = getVerifiedTickColor(room.hostGender, room.hostName),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = "${room.likesCount} ❤️",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricMagenta,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Right Actions: RTC Diagnostic Toggle + Viewers Count + Close Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // RTC Diagnostics Pill Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface.copy(alpha = 0.9f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable { onToggleDiagnostics() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("rtc_debug_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "RTC Diagnostics",
                        tint = NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "RTC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }

            // Viewers Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface.copy(alpha = 0.9f))
                    .clickable { onOpenParticipants() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Viewers",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${room.viewerCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Close / End / Leave Stream Button
            if (isHost) {
                Button(
                    onClick = onLeave,
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("end_broadcast_button")
                ) {
                    Text(
                        text = "End Live",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                IconButton(
                    onClick = onLeave,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .testTag("leave_room_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Leave Stream",
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun TeamBattleArena(
    modifier: Modifier = Modifier,
    teamState: TeamState,
    hostName: String,
    hostAvatar: String,
    hostAvatarUrl: String? = null,
    hostCoverUrl: String? = null,
    filter: BeautifyFilter = BeautifyFilter.ORIGINAL,
    hostPreview: (@Composable () -> Unit)? = null,
    isHostSpeaking: Boolean = true,
    isHostMuted: Boolean = false,
    isHostVideoOff: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Team Tug-of-War Score Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkBackground)
                .border(1.dp, OverlayLight, RoundedCornerShape(12.dp))
                .padding(6.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛡️ ${teamState.teamName} (${teamState.myTeamScore})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberBlue
                    )
                    Text(
                        text = "${teamState.enemyTeamScore} (${teamState.enemyTeamName}) ⚔️",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PkRed
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (teamState.myTeamScore.toFloat() / (teamState.myTeamScore + teamState.enemyTeamScore).coerceAtLeast(1)).coerceIn(0.1f, 0.9f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = CyberBlue,
                    trackColor = PkRed
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Team Split Arena
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                badgeText = "MY SQUAD",
                badgeColor = CyberBlue,
                filter = filter,
                localCameraPreview = hostPreview
            )
            VideoFeedTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                hostName = "Titan Prime",
                avatarEmoji = "🛡️",
                isSpeaking = false,
                badgeText = "ENEMY SQUAD",
                badgeColor = PkRed
            )
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isError) PkRed else TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


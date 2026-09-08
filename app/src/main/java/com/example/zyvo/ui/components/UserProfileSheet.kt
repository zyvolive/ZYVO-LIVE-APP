package com.example.zyvo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.model.UserProfile
import com.example.zyvo.model.VipTier
import com.example.zyvo.ui.components.AnimatedHostAvatar
import com.example.zyvo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSheet(
    user: UserProfile,
    isSelf: Boolean = false,
    onDismiss: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onOpenDm: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onJoinLive: ((String) -> Unit)? = null,
    onOpenUserDetail: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedBadgeDetail by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableIntStateOf(0) }
    var showGiftSheetModal by remember { mutableStateOf(false) }

    fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format(java.util.Locale.US, "%.2fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied: $text", Toast.LENGTH_SHORT).show()
    }

    // Dynamic animations for profile aura & shimmer
    val infiniteTransition = rememberInfiniteTransition(label = "profile_sheet_anim")
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val haloRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotate"
    )

    // Themed Colors for Cover & Accents
    val isCeo = user.userId == "ceo_rayan" || user.executiveRole?.contains("CEO", ignoreCase = true) == true
    val isCoFounder = user.userId == "co_founder_alpha" || user.executiveRole?.contains("CO-FOUNDER", ignoreCase = true) == true
    val isTopQueen = user.userId == "ansharah_gahni" || user.isTopHost

    val coverGradient = when {
        isCeo -> listOf(
            Color(0xFF4A0E63),
            Color(0xFF260538),
            Color(0xFF140220),
            Color(0xFF330948)
        )
        isCoFounder -> listOf(
            Color(0xFF0F2042),
            Color(0xFF25073F),
            Color(0xFF3A0033),
            Color(0xFF09142E)
        )
        isTopQueen -> listOf(
            Color(0xFF570633),
            Color(0xFF340727),
            Color(0xFF1F0317),
            Color(0xFF420829)
        )
        user.vipTier == VipTier.VIP_9 || user.vipTier == VipTier.SVIP_7 -> listOf(
            Color(0xFF3D2702),
            Color(0xFF241501),
            Color(0xFF140C00),
            Color(0xFF3B2403)
        )
        else -> listOf(
            Color(0xFF1E0C3A),
            Color(0xFF110724),
            Color(0xFF090314),
            Color(0xFF190B30)
        )
    }

    val primaryAccent = when {
        isCeo -> GoldAccent
        isCoFounder -> NeonCyan
        isTopQueen -> ElectricMagenta
        user.vipTier != VipTier.NONE -> GoldAccent
        else -> NeonPurpleLight
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12031E),
        scrimColor = Color.Black.copy(alpha = 0.82f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .testTag("user_profile_sheet")
        ) {
            // ==========================================
            // 1. DYNAMIC LUXURY ANIMATED COVER BANNER
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .background(Brush.verticalGradient(coverGradient))
            ) {
                // Cyber Particle & Grid Backdrop
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Ambient light rays
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryAccent.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(w * 0.5f, h * 0.3f),
                            radius = w * 0.6f
                        )
                    )
                    // Decorative grid lines
                    drawLine(
                        color = primaryAccent.copy(alpha = 0.15f),
                        start = Offset(0f, h * 0.75f),
                        end = Offset(w, h * 0.75f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = primaryAccent.copy(alpha = 0.1f),
                        start = Offset(0f, h * 0.9f),
                        end = Offset(w, h * 0.9f),
                        strokeWidth = 1.5f
                    )
                }

                // Decorative Watermark Branding on Cover
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                isCeo -> "👑 ZYVO EXECUTIVE SUITE"
                                isCoFounder -> "🦁 GLOBAL ARENAS & PK"
                                isTopQueen -> "💎 TOP HOST QUEEN STAGE"
                                user.vipTier != VipTier.NONE -> "✨ VIP PRESTIGE LOUNGE"
                                else -> "⚡ ZYVO LIVE CREATOR"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryAccent,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Text(
                        text = when {
                            isCeo -> "Founder & CEO • Sovereign Desk"
                            isCoFounder -> "Co-Founder • Executive Director"
                            isTopQueen -> "Superstar Broadcaster • 78.5M+"
                            else -> "Verified Broadcaster Profile"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary.copy(alpha = 0.8f)
                    )
                }

                // Top Controls Bar on Cover (Live Radar, WhatsApp, Share, More, Close)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LIVE RADAR PILL ON COVER (If host is live right now!)
                    if (user.isLiveNow && user.currentRoomId != null && onJoinLive != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.horizontalGradient(listOf(LiveRed, ElectricMagenta)))
                                .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                                .clickable {
                                    onDismiss()
                                    onJoinLive(user.currentRoomId)
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(com.example.zyvo.R.drawable.ic_live_custom)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .scale(auraPulse)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Direct WhatsApp Button on Cover for Executives
                    if (user.whatsappNumber != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366))
                                .border(1.dp, Color.White, CircleShape)
                                .clickable {
                                    openWhatsAppChat(context, user.whatsappNumber, user.displayName)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", fontSize = 16.sp)
                        }
                    }

                    // Share Profile Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, OverlayLight, CircleShape)
                            .clickable {
                                copyToClipboard("Profile Link", "https://zyvo.live/user/${user.userId}")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // More Options (Report / Block)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, OverlayLight, CircleShape)
                            .clickable { showReportDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Dismiss Sheet Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, OverlayLight, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 2. OVERLAPPING DELUXE HOLOGRAPHIC AVATAR
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Floating Avatar Stack
                Box(
                    modifier = Modifier
                        .offset(y = (-45).dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedHostAvatar(
                        imageUrl = user.avatarUrl ?: "",
                        gender = user.gender ?: if (isCeo || isCoFounder) "Male" else "Female",
                        name = user.displayName,
                        rank = if (isCeo || isTopQueen) 1 else if (isCoFounder) 2 else null,
                        size = 128.dp,
                        isLive = user.isLiveNow
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height((-38).dp))

                // ==========================================
                // 3. EXECUTIVE ROLE & VIP TITLES
                // ==========================================
                if (user.executiveRole != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        GoldAccent,
                                        Color(0xFFFF007A),
                                        GoldAccent
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⭐", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = user.executiveRole.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "⭐", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                } else if (isTopQueen) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ElectricMagenta, GoldAccent, ElectricMagenta)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "👑 ZYVO TOP HOST QUEEN • SVIP 7 👑",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // ==========================================
                // 4. DISPLAY NAME & VERIFIED STATUS
                // ==========================================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Verified Check Badge
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(getVerifiedTickColor(user.gender, user.displayName)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Verified",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (user.vipTier != VipTier.NONE) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(GoldAccent, Color(0xFFFF8800)))
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = user.vipTier.badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Copyable Username & ID Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, OverlayLight, RoundedCornerShape(12.dp))
                        .clickable { copyToClipboard("User ID", user.userId) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "@${user.username} • ID: ${user.userId}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy ID",
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 5. TRIPLE PRESTIGE LEVEL BADGES ROW
                // ==========================================
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Account Level
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))
                                )
                            )
                            .border(1.dp, Color(0xFFBA68C8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lv.${user.userLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                    }

                    // 2. Broadcaster / Host Level
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF880E4F), Color(0xFFC2185B))
                                )
                            )
                            .border(1.dp, ElectricMagenta.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎙️", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Host Lv.${user.hostLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF80AB)
                            )
                        }
                    }

                    // 3. Wealth / Gifter Level
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF5D4037), Color(0xFFFF8F00))
                                )
                            )
                            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💎", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Wealth Lv.${user.wealthLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 6. BIO & LOCATION
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurface.copy(alpha = 0.7f))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("📍", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = user.location, fontSize = 11.sp, color = TextMuted)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("•", color = TextMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("🛡️ ${user.gender}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 7. INTERACTIVE STATS HUB (Followers, Following, Likes, Diamonds)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, OverlayLight, RoundedCornerShape(18.dp))
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCount(user.followersCount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(text = "Followers", fontSize = 10.sp, color = TextMuted)
                    }

                    Box(modifier = Modifier.height(26.dp).width(1.dp).background(OverlayLight))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${user.followingCount}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(text = "Following", fontSize = 10.sp, color = TextMuted)
                    }

                    Box(modifier = Modifier.height(26.dp).width(1.dp).background(OverlayLight))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCount(user.likesCount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(text = "Likes", fontSize = 10.sp, color = TextMuted)
                    }

                    Box(modifier = Modifier.height(26.dp).width(1.dp).background(OverlayLight))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCount(user.diamondsEarnedTotal),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldAccent
                        )
                        Text(text = "🔮 Gems", fontSize = 10.sp, color = GoldAccent.copy(alpha = 0.8f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // 8. PRIMARY ACTION COMMAND BAR
                // ==========================================
                if (!isSelf) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Follow / Unfollow Button
                        Button(
                            onClick = {
                                if (user.isFollowedByCurrentUser) onUnfollow() else onFollow()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.isFollowedByCurrentUser) DarkCardElevated else primaryAccent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("profile_sheet_follow_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (user.isFollowedByCurrentUser) Icons.Default.Check else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = if (user.isFollowedByCurrentUser) TextPrimary else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (user.isFollowedByCurrentUser) "Following" else "Follow",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.isFollowedByCurrentUser) TextPrimary else Color.Black
                                )
                            }
                        }

                        // 2. Direct Message Button
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenDm()
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, NeonCyan),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("profile_sheet_dm_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        // 3. Send Gift / Tip Button
                        Button(
                            onClick = {
                                showGiftSheetModal = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricMagenta
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(46.dp)
                                .testTag("profile_sheet_gift_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎁", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send Gift", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // WhatsApp Direct Contact Button for Executive Profiles
                    if (user.whatsappNumber != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                openWhatsAppChat(context, user.whatsappNumber, user.displayName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF25D366))
                                .testTag("whatsapp_executive_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "💬", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Direct WhatsApp Executive Desk: ${user.whatsappNumber}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    // Own Profile Actions for Bottom Sheet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                Toast.makeText(context, "Edit Profile", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("profile_sheet_edit_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Profile Link", "https://zyvo.live/user/${user.userId}"))
                                Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, NeonCyan),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("profile_sheet_share_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Link", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // 9. MULTI-TAB DEEP PROFILE NAVIGATION
                // ==========================================
                val tabs = listOf("🌟 Overview", "🏆 Badges", "👑 Top Supporters", "🎬 Highlights")
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = DarkSurface,
                    contentColor = primaryAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = primaryAccent,
                            height = 3.dp
                        )
                    },
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == index) FontWeight.Black else FontWeight.Bold,
                                    color = if (activeTab == index) primaryAccent else TextMuted
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 10. TAB CONTENT DISPLAY
                // ==========================================
                when (activeTab) {
                    // TAB 0: OVERVIEW & VIP PRIVILEGES
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Honorary Badges Showcase Rack
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Badges & Honorifics 🎖️",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${user.badges.size} Earned",
                                        fontSize = 11.sp,
                                        color = primaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(user.badges) { badge ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF2A0845), Color(0xFF6441A5))
                                                    )
                                                )
                                                .border(1.dp, primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                .clickable { selectedBadgeDetail = badge }
                                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("✨", fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = badge,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // VIP Supreme Privileges (For VIP & Executive Profiles)
                            if (user.vipTier != VipTier.NONE || isCeo || isCoFounder) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF331E00), Color(0xFF1F1200))
                                            )
                                        )
                                        .border(1.5.dp, GoldAccent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("👑", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "VIP 9 Supreme Privileges Active",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = GoldAccent
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val perks = listOf(
                                        "🌟 Sovereign Entrance Halo Animation across all Live Rooms",
                                        "🛡️ Permanent Anti-Kick & Anti-Mute Protection Shield",
                                        "💬 Golden Highlighted Chat Bubble with Animated Crown",
                                        "💎 Priority PK Arena Matchmaking & Stage Dominance",
                                        "⚡ 100% Boosted Charm & Gifting Multipliers"
                                    )

                                    perks.forEach { perk ->
                                        Text(
                                            text = perk,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Following Highlight (If following Rayan & Alpha)
                            if (user.followingUserIds.isNotEmpty() || isTopQueen) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Officially Following (Founder & Co-Founder)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonCyan
                                        )
                                        Text("👑", fontSize = 14.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Rayan Mirza
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkBackground)
                                            .clickable {
                                                onDismiss()
                                                onOpenUserDetail?.invoke("ceo_rayan")
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("👑", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("RAYAN MIRZA", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldAccent)
                                            Text("Founder & CEO • Lv.99 Sovereign", fontSize = 10.sp, color = TextMuted)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GoldAccent)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Alpha Rajpoot
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkBackground)
                                            .clickable {
                                                onDismiss()
                                                onOpenUserDetail?.invoke("co_founder_alpha")
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🦁", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ALPHA RAJPOOT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricMagenta)
                                            Text("Co-Founder & Exec Director • Lv.99", fontSize = 10.sp, color = TextMuted)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ElectricMagenta)
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: ALL PRESTIGE BADGES GALLERY
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val allAchievements = listOf(
                                Triple("👑 Founder & Sovereign", "Highest executive leadership badge", "Mythic"),
                                Triple("💎 VIP 9 Supreme Tier", "Unlocks golden broadcast auras", "Legendary"),
                                Triple("⭐ Level 99 Hall of Fame", "Maximum platform experience tier", "Mythic"),
                                Triple("⚔️ PK Arena Grandmaster", "Over 500+ PK Arena victories", "Legendary"),
                                Triple("💖 Millionaire Gifter", "Contributed over 10M+ Gems", "Epic"),
                                Triple("🎙️ Top 1% Broadcaster", "Streamed over 1,000+ live hours", "Epic"),
                                Triple("🕊️ Peace Ambassador", "Community guardian & mentor", "Rare")
                            )

                            allAchievements.forEach { (title, desc, rarity) ->
                                val rarityColor = when (rarity) {
                                    "Mythic" -> GoldAccent
                                    "Legendary" -> ElectricMagenta
                                    "Epic" -> NeonPurpleLight
                                    else -> NeonCyan
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, rarityColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(rarityColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = rarity, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = rarityColor)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(text = desc, fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Icon(Icons.Default.LockOpen, contentDescription = "Unlocked", tint = rarityColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // TAB 2: TOP SUPPORTERS PODIUM
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Hall of Fame Top Gifters 👑",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldAccent
                            )

                            val supporters = listOf(
                                Triple("1. King Of Kings", "12.5M Gems", "👑 VIP 9"),
                                Triple("2. Drama Queen", "8.9M Gems", "🦁 SVIP 7"),
                                Triple("3. DJ Kai", "5.4M Gems", "🎧 VIP 5"),
                                Triple("4. Pixel Queen", "3.2M Gems", "👾 VIP 3")
                            )

                            supporters.forEachIndexed { index, (name, diamonds, vip) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (index == 0) Color(0xFF2A1B02) else DarkSurface)
                                        .border(
                                            1.dp,
                                            if (index == 0) GoldAccent else OverlayLight,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (index == 0) "🥇" else if (index == 1) "🥈" else if (index == 2) "🥉" else "🏅",
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(text = vip, fontSize = 10.sp, color = GoldAccent)
                                        }
                                    }

                                    Text(
                                        text = diamonds,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GoldAccent
                                    )
                                }
                            }
                        }
                    }

                    // TAB 3: STREAM HIGHLIGHTS & PK RECORDS
                    3 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // PK Battles Record Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF4A0028), Color(0xFF1E004A))
                                        )
                                    )
                                    .border(1.dp, ElectricMagenta.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⚔️ 99.4%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                                    Text("PK Win Rate", fontSize = 10.sp, color = TextSecondary)
                                }
                                Box(modifier = Modifier.height(30.dp).width(1.dp).background(OverlayLight))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔥 99 Wins", fontSize = 18.sp, fontWeight = FontWeight.Black, color = ElectricMagenta)
                                    Text("Win Streak", fontSize = 10.sp, color = TextSecondary)
                                }
                                Box(modifier = Modifier.height(30.dp).width(1.dp).background(OverlayLight))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎬 ${user.liveStreamsCount}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                                    Text("Live Streams", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // BADGE DETAIL TOOLTIP MODAL
        if (selectedBadgeDetail != null) {
            AlertDialog(
                onDismissRequest = { selectedBadgeDetail = null },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎖️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = selectedBadgeDetail!!, color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = "Official honorary insignia awarded for top contribution, broadcaster supremacy, and community leadership on ZYVO Live.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { selectedBadgeDetail = null },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent)
                    ) {
                        Text("Awesome!", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // REPORT & BLOCK DIALOG
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(20.dp),
                title = { Text(text = "Profile Actions", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                onReport()
                                showReportDialog = false
                                Toast.makeText(context, "Report submitted to Safety Team", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = LiveRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Report Account for Policy Violation", color = LiveRed)
                            }
                        }
                        TextButton(
                            onClick = {
                                onBlock()
                                showReportDialog = false
                                onDismiss()
                                Toast.makeText(context, "@${user.username} blocked", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = TextMuted)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Block @${user.username}", color = TextPrimary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("Close", color = NeonCyan)
                    }
                }
            )
        }

        // QUICK SEND GIFT SHEET
        if (showGiftSheetModal) {
            AlertDialog(
                onDismissRequest = { showGiftSheetModal = false },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎁", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Send Gift to ${user.displayName}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "Select a luxury gift to boost charm & support:", fontSize = 12.sp, color = TextSecondary)
                        
                        val quickGifts = listOf(
                            Triple("👑 Royal Crown", "9,999 🪙", "👑"),
                            Triple("🚀 Super Rocket", "4,999 🪙", "🚀"),
                            Triple("🔮 Luxury Gem Ring", "1,999 🪙", "🔮"),
                            Triple("🌹 Love Rose", "99 🪙", "🌹")
                        )

                        quickGifts.forEach { (name, cost, icon) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBackground)
                                    .border(1.dp, OverlayLight, RoundedCornerShape(12.dp))
                                    .clickable {
                                        showGiftSheetModal = false
                                        Toast.makeText(context, "Sent $name to ${user.displayName}! 💖", Toast.LENGTH_LONG).show()
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                }
                                Text(cost, fontWeight = FontWeight.Black, color = GoldAccent, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGiftSheetModal = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }
    }
}

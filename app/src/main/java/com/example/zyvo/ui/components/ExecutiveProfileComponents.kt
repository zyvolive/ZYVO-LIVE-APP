package com.example.zyvo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.model.UserProfile
import com.example.zyvo.model.VipTier
import com.example.zyvo.ui.theme.*

/**
 * Official WhatsApp launcher helper.
 * Redirects directly to WhatsApp chat using https://wa.me/<number>
 */
fun openWhatsAppChat(context: Context, phoneNumber: String, executiveName: String) {
    val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
    val url = "https://wa.me/$cleanNumber"
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        Toast.makeText(context, "Opening WhatsApp chat with $executiveName...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        // Fallback: Copy to clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WhatsApp Number", phoneNumber)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$phoneNumber copied to clipboard", Toast.LENGTH_LONG).show()
    }
}

/**
 * Heavy Animated Executive Avatar with rotating golden/mythic halo and floating VIP 9 crown.
 */
@Composable
fun ExecutiveAvatar(
    avatarUrl: String?,
    avatarEmoji: String = "👑",
    size: Dp = 80.dp,
    userLevel: Int = 99,
    vipTier: VipTier = VipTier.VIP_9,
    showCrown: Boolean = true,
    showLevelBadge: Boolean = true,
    accentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "executive_aura")
    
    // Rotating gradient border angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_rotation"
    )

    // Pulsing aura glow
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    val primaryColor = accentColor ?: if (vipTier == VipTier.VIP_9) GoldAccent else NeonCyan
    val secondaryColor = if (accentColor != null) accentColor.copy(alpha = 0.5f) else Color(0xFFFF007A)
    val executiveBrush = Brush.sweepGradient(
        colors = listOf(
            primaryColor,
            secondaryColor,
            Color(0xFFFFD700),
            secondaryColor,
            primaryColor
        )
    )

    Box(
        modifier = modifier.size(size + 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Pulsing Neon Glow Ring
        Box(
            modifier = Modifier
                .size(size * auraScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GoldAccent.copy(alpha = auraAlpha * 0.6f),
                            Color(0xFFFF007A).copy(alpha = auraAlpha * 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Rotating Rainbow/Gold Halo Border
        Box(
            modifier = Modifier
                .size(size + 6.dp)
                .rotate(rotationAngle)
                .clip(CircleShape)
                .background(executiveBrush)
        )

        // Inner Avatar Container
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(DarkBackground)
                .border(2.dp, Color(0xFF1F0B2E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Executive Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = avatarEmoji,
                    fontSize = (size.value * 0.5f).sp
                )
            }
        }

        // Floating Crown on Top
        if (showCrown) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .shadow(8.dp, CircleShape)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(GoldAccent, Color(0xFFFF8800), GoldAccent)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "👑 SVIP 9",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }

        // Floating Level 99 Badge on Bottom
        if (showLevelBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (6).dp)
                    .shadow(6.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF7B1FA2), Color(0xFFFF007A))
                        )
                    )
                    .border(1.dp, GoldAccent, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "⭐ Lv.$userLevel",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
        }
    }
}

/**
 * Dedicated WhatsApp Icon Button with official green branding and ripple.
 */
@Composable
fun WhatsAppDirectButton(
    phoneNumber: String,
    executiveName: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val context = LocalContext.current
    val whatsAppGreen = Color(0xFF25D366)

    if (isCompact) {
        // Icon-only round button for headers/cards
        Box(
            modifier = modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF25D366), Color(0xFF128C7E))
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                .clickable {
                    openWhatsAppChat(context, phoneNumber, executiveName)
                }
                .testTag("whatsapp_btn_$executiveName"),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "💬", fontSize = 18.sp)
        }
    } else {
        // Full pill button with WhatsApp logo text
        Button(
            onClick = {
                openWhatsAppChat(context, phoneNumber, executiveName)
            },
            colors = ButtonDefaults.buttonColors(containerColor = whatsAppGreen),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = modifier
                .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = whatsAppGreen)
                .testTag("whatsapp_full_btn_$executiveName")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "💬", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "WhatsApp",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Luxury Showcase Banner for CEO Rayan Mirza & Co-Founder Alpha Rajpoot.
 * Rendered prominently on the Home Screen & Rankings.
 */
@Composable
fun ExecutiveGrandBanner(
    ceoProfile: UserProfile?,
    coFounderProfile: UserProfile?,
    onOpenProfile: (String) -> Unit,
    onOpenLiveRoom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "banner_shimmer")
    
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3B064A),
                        Color(0xFF20032B),
                        Color(0xFF12011A)
                    )
                )
            )
            .border(
                2.dp,
                Brush.horizontalGradient(
                    listOf(
                        GoldAccent.copy(alpha = shimmerAlpha),
                        Color(0xFFFF007A),
                        NeonCyan.copy(alpha = shimmerAlpha),
                        GoldAccent.copy(alpha = shimmerAlpha)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        // Banner Header: Title & Supreme Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "👑", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ZYVO FOUNDERS & EXECUTIVE DESK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = GoldAccent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Level 99 Sovereigns • VIP 9 Supreme Tiers",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GoldAccent)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "SUPREME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Executive Profiles Row (CEO & Co-Founder side by side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. FOUNDER & CEO RAYAN MIRZA CARD
            ExecutiveMiniCard(
                name = "RAYAN MIRZA",
                roleTitle = "FOUNDER & CEO",
                avatarUrl = ceoProfile?.avatarUrl ?: "https://cdn.phototourl.com/free/2026-09-01-f3e014af-6987-41b0-8bcf-732294379e68.png",
                avatarEmoji = "👑",
                phoneNumber = "+44 7868 713315",
                gemsText = "99.9M Gems",
                followersText = "9.8M Fans",
                isCeo = true,
                onCardClick = { onOpenProfile("ceo_rayan") },
                onLiveClick = { onOpenLiveRoom("room_ceo_999") },
                modifier = Modifier.weight(1f)
            )

            // 2. CO-FOUNDER ALPHA RAJPOOT CARD
            ExecutiveMiniCard(
                name = "ALPHA RAJPOOT",
                roleTitle = "CO-FOUNDER",
                avatarUrl = coFounderProfile?.avatarUrl ?: "https://cdn.phototourl.com/free/2026-09-01-4aa927e1-ee25-497a-ae9e-4201e9d81679.jpg",
                avatarEmoji = "🦁",
                phoneNumber = "+447366 387620",
                gemsText = "88.8M Gems",
                followersText = "8.4M Fans",
                isCeo = false,
                onCardClick = { onOpenProfile("co_founder_alpha") },
                onLiveClick = { onOpenLiveRoom("room_alpha_888") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual Card for CEO or Co-Founder in the Banner.
 */
@Composable
fun ExecutiveMiniCard(
    name: String,
    roleTitle: String,
    avatarUrl: String,
    avatarEmoji: String,
    phoneNumber: String,
    gemsText: String,
    followersText: String,
    isCeo: Boolean,
    onCardClick: () -> Unit,
    onLiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = if (isCeo) GoldAccent else NeonCyan
    val borderGradient = if (isCeo) {
        listOf(GoldAccent, Color(0xFFFF007A))
    } else {
        listOf(NeonCyan, NeonPurple)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCardElevated)
            .border(1.5.dp, Brush.linearGradient(borderGradient), RoundedCornerShape(18.dp))
            .clickable { onCardClick() }
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Role Tag Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCeo) Color(0xFF523300) else Color(0xFF00384D))
                    .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = roleTitle,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Animated Avatar
            ExecutiveAvatar(
                avatarUrl = avatarUrl,
                avatarEmoji = avatarEmoji,
                size = 56.dp,
                userLevel = 99,
                vipTier = VipTier.VIP_9,
                showCrown = true,
                showLevelBadge = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = gemsText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: WhatsApp Icon Direct & Live Room Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp Icon Button (Direct Redirect)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366))
                        .border(1.dp, Color.White, CircleShape)
                        .clickable {
                            openWhatsAppChat(context, phoneNumber, name)
                        }
                        .testTag("wa_btn_$name"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💬", fontSize = 16.sp)
                }

                // Live Button
                Button(
                    onClick = onLiveClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCeo) LiveRed else ElectricMagenta
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(com.example.zyvo.R.drawable.ic_live_custom)
                                .crossfade(true)
                                .build(),
                            contentDescription = "LIVE",
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

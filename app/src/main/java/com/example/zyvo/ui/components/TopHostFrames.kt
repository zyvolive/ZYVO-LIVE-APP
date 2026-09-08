package com.example.zyvo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.R
import com.example.zyvo.model.LiveRoom
import com.example.zyvo.ui.theme.ElectricMagenta
import com.example.zyvo.ui.theme.GoldAccent
import com.example.zyvo.ui.theme.NeonPurple
import com.example.zyvo.ui.theme.getVerifiedTickColor
import kotlin.math.cos
import kotlin.math.sin

// Gender Determination Helper
fun isMaleGender(gender: String?, name: String? = null): Boolean {
    val g = gender?.lowercase() ?: ""
    val n = name?.lowercase() ?: ""
    if (g == "male" || g == "m" || g == "boy" || g == "king") return true
    if (g == "female" || g == "f" || g == "girl" || g == "queen") return false
    return n.contains("king") || n.contains("rajpoot") || n.contains("islam") || n.contains("prince")
}

/**
 * Rank Badge Component
 */
@Composable
fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    val (badgeColors, textLabel) = when (rank) {
        1 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500)) to "TOP 1"
        2 -> listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E)) to "TOP 2"
        3 -> listOf(Color(0xFFCD7F32), Color(0xFF8B4513)) to "TOP 3"
        else -> listOf(Color(0xFFFF007A), Color(0xFF7928CA)) to "#$rank"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(badgeColors))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = textLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

/**
 * Animated Host Avatar Component
 * Reused everywhere across the app for both Male (Gold King) and Female (Pink Queen) frames.
 */
@Composable
fun AnimatedHostAvatar(
    imageUrl: String,
    gender: String = "Female",
    name: String = "",
    rank: Int? = null,
    size: Dp = 100.dp,
    isLive: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isMale = remember(gender, name) { isMaleGender(gender, name) }

    // Colors
    val frameGlowColor = if (isMale) Color(0xFFFFC107) else Color(0xFFFF4081)
    val accentColor = if (isMale) Color(0xFFFFD700) else Color(0xFFFF69B4)
    val frameDrawable = if (isMale) R.drawable.ic_gold_male_frame_custom else R.drawable.ic_gold_female_frame_custom

    // Infinite Animations
    val infiniteTransition = rememberInfiniteTransition(label = "HostFrameAnimation")
    
    // Crown Floating/Breathing Animation (-2dp to +2dp)
    val crownYOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CrownFloat"
    )

    // Outer Glow Pulse Alpha (0.55f to 0.95f)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // Metallic Light Sweep Angle (0f to 360f)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LightSweep"
    )

    // Sparkle Scale
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )

    // Rank multipliers for glow and size
    val glowRadius = when (rank) {
        1 -> size * 0.28f
        2 -> size * 0.22f
        3 -> size * 0.18f
        else -> size * 0.14f
    }

    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // 1. Soft Outer Radial Aura Glow
        Box(
            modifier = Modifier
                .size(size - 10.dp)
                .alpha(glowAlpha)
                .shadow(
                    elevation = glowRadius,
                    shape = CircleShape,
                    spotColor = frameGlowColor,
                    ambientColor = frameGlowColor
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            frameGlowColor.copy(alpha = if (rank == 1) 0.5f else 0.35f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // 2. Profile Photo (Centered, clipped perfectly inside circle)
        val photoSize = size * 0.55f
        Box(
            modifier = Modifier
                .size(photoSize)
                .clip(CircleShape)
                .background(Color(0xFF1B162B)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Metallic Light Sweep Reflection Layer
        Canvas(
            modifier = Modifier
                .size(size * 0.78f)
                .clip(CircleShape)
        ) {
            rotate(sweepAngle) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    ),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // 4. 3D King/Queen Frame Asset Layer (With Floating Crown)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(frameDrawable)
                .crossfade(true)
                .build(),
            contentDescription = if (isMale) "King Frame" else "Queen Frame",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = crownYOffset.dp)
        )

        // 5. Gemstone Particle Sparkles
        if (rank == 1 || rank == 2 || rank == 3) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.toPx() / 2, size.toPx() / 2)
                val ringR = size.toPx() * 0.38f

                // Bottom jewel sparkle
                val sparkX2 = center.x - sin(Math.toRadians(150.0)).toFloat() * ringR
                val sparkY2 = center.y + cos(Math.toRadians(150.0)).toFloat() * ringR + (8 * density)
                drawCircle(
                    color = accentColor.copy(alpha = 0.85f),
                    radius = 2.5.dp.toPx() * sparkleScale,
                    center = Offset(sparkX2, sparkY2)
                )
            }
        }

        // 6. LIVE Indicator Badge
        if (isLive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF007A))
                    .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Dedicated King Host Frame Component
 */
@Composable
fun KingHostFrame(
    imageUrl: String,
    name: String = "",
    rank: Int? = null,
    size: Dp = 100.dp,
    isLive: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    AnimatedHostAvatar(
        imageUrl = imageUrl,
        gender = "Male",
        name = name,
        rank = rank,
        size = size,
        isLive = isLive,
        onClick = onClick
    )
}

/**
 * Dedicated Queen Host Frame Component
 */
@Composable
fun QueenHostFrame(
    imageUrl: String,
    name: String = "",
    rank: Int? = null,
    size: Dp = 100.dp,
    isLive: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    AnimatedHostAvatar(
        imageUrl = imageUrl,
        gender = "Female",
        name = name,
        rank = rank,
        size = size,
        isLive = isLive,
        onClick = onClick
    )
}

/**
 * Popular Host Horizontal Card (For "👑 Popular Hosts" Section)
 */
@Composable
fun PopularHostCard(
    id: String,
    name: String,
    gender: String,
    viewerCount: String,
    imageUrl: String,
    rank: Int? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(115.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        // Frame Container
        Box(contentAlignment = Alignment.TopEnd) {
            AnimatedHostAvatar(
                imageUrl = imageUrl,
                gender = gender,
                name = name,
                rank = rank,
                size = 98.dp,
                isLive = false
            )

            // Rank Badge if Top 1, 2, 3
            if (rank != null && rank in 1..3) {
                RankBadge(
                    rank = rank,
                    modifier = Modifier.offset(x = 4.dp, y = (-2).dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Host Name + Verified Tick
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = getVerifiedTickColor(gender, name),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * Top Host Card (For Top 1 / Top 2 / Top 3 Podium Displays)
 */
@Composable
fun TopHostCard(
    rank: Int,
    name: String,
    gender: String,
    viewerCount: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    val frameSize = when (rank) {
        1 -> 130.dp
        2 -> 110.dp
        3 -> 105.dp
        else -> 95.dp
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        RankBadge(rank = rank)
        Spacer(modifier = Modifier.height(4.dp))
        
        AnimatedHostAvatar(
            imageUrl = imageUrl,
            gender = gender,
            name = name,
            rank = rank,
            size = frameSize,
            isLive = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                fontSize = if (rank == 1) 14.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = getVerifiedTickColor(gender, name),
                modifier = Modifier.size(13.dp)
            )
        }

        Text(
            text = "💎 $viewerCount",
            fontSize = 11.sp,
            color = GoldAccent,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Top Hosts Section (Horizontal Scroll on Home Screen)
 */
@Composable
fun TopHostsSection(
    hosts: List<PopularHostData>,
    onHostClick: (PopularHostData) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "👑",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Popular Hosts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "View all >",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoldAccent,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Hosts List
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(hosts) { host ->
                PopularHostCard(
                    id = host.id,
                    name = host.name,
                    gender = host.gender,
                    viewerCount = host.viewerCount,
                    imageUrl = host.imageUrl,
                    rank = host.rank,
                    onClick = { onHostClick(host) }
                )
            }
        }
    }
}

/**
 * Host Profile Header (Reuses the Exact Same King / Queen Animated Frame Component)
 */
@Composable
fun HostProfileHeader(
    name: String,
    gender: String,
    imageUrl: String,
    rank: Int? = 1,
    diamonds: String = "127.5M",
    followers: String = "482.9K",
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {},
    onSendGiftClick: () -> Unit = {}
) {
    val isMale = remember(gender, name) { isMaleGender(gender, name) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isMale) Color(0x33FFC107) else Color(0x33FF4081),
                        Color(0xFF090712)
                    )
                )
            )
            .padding(20.dp)
    ) {
        // Same Reused King/Queen Animated Frame Component
        AnimatedHostAvatar(
            imageUrl = imageUrl,
            gender = gender,
            name = name,
            rank = rank,
            size = 140.dp,
            isLive = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Name + Verified Tick
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = getVerifiedTickColor(gender, name),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Rank Title
        val rankTitle = if (isMale) "👑 KING TOP HOST #$rank" else "👑 QUEEN TOP HOST #$rank"
        Text(
            text = rankTitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isMale) GoldAccent else ElectricMagenta
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🔮 $diamonds", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                Text(text = "Gems", fontSize = 11.sp, color = Color(0xFFB0ACC0))
            }
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2D2640)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = followers, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "Followers", fontSize = 11.sp, color = Color(0xFFB0ACC0))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onFollowClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color(0xFF2D2640) else if (isMale) Color(0xFFFFB300) else Color(0xFFFF007A)
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onSendGiftClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7928CA)
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Send Gift", fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class PopularHostData(
    val id: String,
    val name: String,
    val gender: String,
    val viewerCount: String,
    val imageUrl: String,
    val rank: Int? = null,
    val liveRoomObj: LiveRoom? = null
)

package com.example.zyvo.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zyvo.model.UserProfile
import com.example.zyvo.model.VipTier
import com.example.zyvo.ui.components.AnimatedHostAvatar
import com.example.zyvo.ui.theme.*

// Exact ZYVO Dark Theme Palette
private val GalaxyDarkBg = Color(0xFF090712)
private val DarkCardBg = Color(0xFF140E22)
private val GlassBorderColor = Color(0xFF281E3B)
private val BrandPinkPrimary = Color(0xFFFF007A)
private val BrandPinkGradientEnd = Color(0xFFE01E5A)
private val NeonCyanColor = Color(0xFF00F0FF)
private val GoldAccentColor = Color(0xFFFFD700)
private val TextMutedColor = Color(0xFFA1A0B5)

@Composable
fun UserProfileScreen(
    userProfile: UserProfile,
    coinBalance: Int,
    beansBalance: Int,
    followingCount: Int,
    isOwnProfile: Boolean = true,
    isFollowing: Boolean = false,
    onFollowToggle: () -> Unit = { },
    onOpenDm: () -> Unit = { },
    onShareProfile: () -> Unit = { },
    onBack: (() -> Unit)? = null,
    onOpenVipStore: () -> Unit,
    onOpenRecharge: () -> Unit,
    onOpenWithdrawal: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenUserDetail: (String) -> Unit,
    onLogout: (() -> Unit)? = null,
    // Optional callbacks with default fallbacks for complete navigation coverage
    onOpenWallet: () -> Unit = onOpenRecharge,
    onOpenMyLevel: () -> Unit = { },
    onOpenMyFans: () -> Unit = { },
    onOpenHistory: () -> Unit = onOpenTransactions,
    onOpenMyPosts: () -> Unit = { },
    onOpenMyVideos: () -> Unit = { },
    onOpenMyMoments: () -> Unit = { },
    onOpenInviteFriends: () -> Unit = { },
    onOpenHelpSupport: () -> Unit = { },
    onEditProfile: () -> Unit = { }
) {
    val context = LocalContext.current

    // Display values (User real authenticated data or fallback to reference values)
    val displayName = if (userProfile.displayName.isNotBlank() && userProfile.displayName != "Streamer") {
        userProfile.displayName
    } else if (userProfile.username.isNotBlank()) {
        userProfile.username
    } else {
        "ZYVO Broadcaster"
    }
    val userId = if (userProfile.userId.isNotBlank()) userProfile.userId else "user_me"
    val avatarUrl = userProfile.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80"
    
    val followersText = if (userProfile.followersCount > 1000) "98.7K" else "${userProfile.followersCount}"
    val followingText = if (followingCount > 0) "$followingCount" else "125"
    val likesText = if (userProfile.likesCount > 1000) "2.6M" else "${userProfile.likesCount}"
    val popularityText = "1.2M"

    CosmicBackground {
        Scaffold(
            containerColor = Color.Transparent
        ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // SECTION 1: PROFILE HEADER & TOP ACTIONS
                ProfileHeaderSection(
                    displayName = displayName,
                    userId = userId,
                    gender = userProfile.gender,
                    avatarUrl = avatarUrl,
                    userLevel = 48,
                    currentXp = 89560,
                    requiredXp = 120000,
                    isOwnProfile = isOwnProfile,
                    onBack = onBack,
                    onEditProfile = {
                        onEditProfile()
                        Toast.makeText(context, "Edit Profile", Toast.LENGTH_SHORT).show()
                    },
                    onOpenSettings = onOpenSettings,
                    onShareProfile = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Profile Link", "https://zyvo.live/user/$userId"))
                        Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        onShareProfile()
                    },
                    context = context
                )
            }

            item {
                // SECTION 2: USER STATISTICS CARD & ACTION BUTTONS (Own account vs Other account)
                UserStatisticsCard(
                    following = followingText,
                    followers = followersText,
                    likes = likesText,
                    popularity = popularityText,
                    isOwnProfile = isOwnProfile,
                    isFollowing = isFollowing,
                    onFollowToggle = onFollowToggle,
                    onChatClick = onOpenDm,
                    onEditProfile = {
                        onEditProfile()
                        Toast.makeText(context, "Edit Profile", Toast.LENGTH_SHORT).show()
                    },
                    onShareProfile = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Profile Link", "https://zyvo.live/user/$userId"))
                        Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        onShareProfile()
                    }
                )
            }

            item {
                // SECTION 3: PROFILE QUICK ACTIONS HORIZONTAL MENU
                QuickActionsHorizontalMenu(
                    onOpenWallet = onOpenWallet,
                    onOpenMyLevel = {
                        onOpenMyLevel()
                        Toast.makeText(context, "Level 48 Broadcaster", Toast.LENGTH_SHORT).show()
                    },
                    onOpenVipCenter = onOpenVipStore,
                    onOpenMyFans = {
                        onOpenMyFans()
                        Toast.makeText(context, "My Fans List", Toast.LENGTH_SHORT).show()
                    },
                    onOpenHistory = onOpenHistory
                )
            }

            item {
                // SECTION 4: VIP 7 PROMOTION BANNER
                VipPromotionBanner(
                    vipLevel = "VIP 7",
                    currentXp = 8750,
                    requiredXp = 10000,
                    onUpgradeClick = onOpenVipStore,
                    onViewVipClick = onOpenVipStore
                )
            }

            item {
                // SECTION 5: TOP FANS
                TopFansSection(
                    onViewAllClick = {
                        Toast.makeText(context, "Viewing All Top Fans", Toast.LENGTH_SHORT).show()
                    },
                    onUserClick = onOpenUserDetail
                )
            }

            item {
                // SECTION 6: WALLET + ASSETS
                WalletAndAssetsSection(
                    coinBalance = if (coinBalance > 0) coinBalance else 125680,
                    usdValue = "125.68",
                    coinsCount = if (coinBalance > 0) coinBalance else 125680,
                    gemsCount = 12568,
                    beansCount = if (beansBalance > 0) beansBalance else 85230,
                    medalsCount = 32,
                    badgesCount = 68,
                    framesCount = 15,
                    onTopUpClick = onOpenRecharge,
                    onViewWalletAll = onOpenWallet,
                    onViewAssetsAll = {
                        Toast.makeText(context, "My Assets (Medals, Badges, Frames)", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                // SECTION 7: PROFILE MENU LIST
                ProfileMenuListSection(
                    onOpenMyPosts = {
                        onOpenMyPosts()
                        Toast.makeText(context, "My Posts", Toast.LENGTH_SHORT).show()
                    },
                    onOpenMyVideos = {
                        onOpenMyVideos()
                        Toast.makeText(context, "My Videos", Toast.LENGTH_SHORT).show()
                    },
                    onOpenMyMoments = {
                        onOpenMyMoments()
                        Toast.makeText(context, "My Moments", Toast.LENGTH_SHORT).show()
                    },
                    onOpenInviteFriends = {
                        onOpenInviteFriends()
                        Toast.makeText(context, "Invite Friends to Earn Rewards! 🎁", Toast.LENGTH_SHORT).show()
                    },
                    onOpenHelpSupport = {
                        onOpenHelpSupport()
                        Toast.makeText(context, "Help & Support Desk", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                // EXECUTIVE DESK & LOGOUT
                if (onLogout != null) {
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("logout_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCardBg),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(LiveRed, BrandPinkPrimary))
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Logout, contentDescription = "Log Out", tint = LiveRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Sign Out / Switch Account", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LiveRed)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
}

// ============================================================================
// 1. PROFILE HEADER SECTION
// ============================================================================
@Composable
private fun ProfileHeaderSection(
    displayName: String,
    userId: String,
    gender: String = "Female",
    avatarUrl: String,
    userLevel: Int,
    currentXp: Int,
    requiredXp: Int,
    isOwnProfile: Boolean,
    onBack: (() -> Unit)?,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onShareProfile: () -> Unit,
    context: Context
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF260D42),
                        Color(0xFF130826),
                        Color(0xFF090712)
                    )
                )
            )
            .border(1.dp, Color(0xFF3B235E), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Controls Bar (Back button on left if applicable, Edit/Settings or Share on right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button if viewing another profile or if onBack provided
                if (onBack != null || !isOwnProfile) {
                    IconButton(
                        onClick = { onBack?.invoke() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1730))
                            .testTag("profile_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Right action icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwnProfile) {
                        IconButton(
                            onClick = onEditProfile,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1730))
                                .testTag("edit_profile_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Edit Profile",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1730))
                                .testTag("settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onShareProfile,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1730))
                                .testTag("share_user_profile_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                Toast.makeText(context, "User Options", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1730))
                                .testTag("more_user_profile_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Header Row: Avatar (Left), User Info (Middle), Level Card (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Left: Premium Avatar with custom crown frame matching top 100% perfect profile page!
                Box(
                    modifier = Modifier.width(105.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedHostAvatar(
                        imageUrl = avatarUrl,
                        gender = gender,
                        name = displayName,
                        size = 105.dp,
                        isLive = false
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Middle: User Info (Name, Verified Badge, ID, Bio, Badges)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        // Verified Gender-based Checkmark (Golden for Male, Pink for Female)
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(getVerifiedTickColor(gender, displayName)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Verified",
                                tint = Color.Black,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // User ID Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("User ID", userId))
                            Toast.makeText(context, "ID: $userId copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(
                            text = "ID: $userId",
                            fontSize = 11.sp,
                            color = TextMutedColor,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy ID",
                            tint = TextMutedColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Live with passion 💜",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Let's spread love and positivity ✨",
                        fontSize = 10.sp,
                        color = TextMutedColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pill Badges (VIP 7, SVIP 3, Zodiac)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // VIP 7 Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFE6AC00), Color(0xFFB37400))
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⭐", fontSize = 8.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("VIP 7", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        // SVIP 3 Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF8A2BE2), Color(0xFFB026FF))
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⭐", fontSize = 8.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("SVIP 3", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        // Zodiac Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF381559))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("♑ Taurus", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD8B4FE))
                        }
                    }
                }

                // Right: Level Card (Lotus icon, Lv. 48, XP Bar)
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D0E38))
                        .border(1.dp, Color(0xFF3D236E), RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌺", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Lv.$userLevel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandPinkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Orange XP Progress Bar
                        LinearProgressIndicator(
                            progress = { (currentXp.toFloat() / requiredXp.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFFF9F1C),
                            trackColor = Color(0xFF2E1A4E)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "$currentXp/$requiredXp",
                            fontSize = 8.sp,
                            color = TextMutedColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. USER STATISTICS CARD & ACTION BUTTONS
// ============================================================================
@Composable
private fun UserStatisticsCard(
    following: String,
    followers: String,
    likes: String,
    popularity: String,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    onChatClick: () -> Unit,
    onEditProfile: () -> Unit,
    onShareProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCardBg)
            .border(1.dp, GlassBorderColor, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatColumn(number = following, label = "Following")
                DividerVertical()
                StatColumn(number = followers, label = "Followers")
                DividerVertical()
                StatColumn(number = likes, label = "Likes")
                DividerVertical()
                StatColumn(number = popularity, label = "Popularity")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Adapts depending on whether viewing own profile or another person's profile)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isOwnProfile) {
                    // OWN ACCOUNT: "Edit Profile" (Pink Gradient) & "Share Profile" (Dark Glass)
                    Button(
                        onClick = onEditProfile,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("edit_profile_main_btn"),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(BrandPinkPrimary, BrandPinkGradientEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Button(
                        onClick = onShareProfile,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .border(1.dp, Color(0xFF3B295A), RoundedCornerShape(21.dp))
                            .testTag("share_profile_btn"),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1329))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    // OTHER PERSON'S ACCOUNT: "+ Follow" / "Following ✓" & "Chat"
                    Button(
                        onClick = onFollowToggle,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("follow_user_btn"),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color(0xFF1E1730) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isFollowing) Brush.horizontalGradient(listOf(Color(0xFF26183D), Color(0xFF1D1230)))
                                    else Brush.horizontalGradient(listOf(BrandPinkPrimary, BrandPinkGradientEnd))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFollowing) "Following" else "+ Follow",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onChatClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .border(1.dp, NeonCyanColor.copy(alpha = 0.6f), RoundedCornerShape(21.dp))
                            .testTag("chat_user_btn"),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1329))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = NeonCyanColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = number, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = TextMutedColor)
    }
}

@Composable
private fun DividerVertical() {
    Box(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(Color(0xFF2D2342))
    )
}

// ============================================================================
// 3. PROFILE QUICK ACTIONS HORIZONTAL MENU
// ============================================================================
@Composable
private fun QuickActionsHorizontalMenu(
    onOpenWallet: () -> Unit,
    onOpenMyLevel: () -> Unit,
    onOpenVipCenter: () -> Unit,
    onOpenMyFans: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCardBg)
            .border(1.dp, GlassBorderColor, RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickMenuItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Wallet",
                gradient = listOf(Color(0xFF8E24AA), Color(0xFF5E35B1)),
                onClick = onOpenWallet
            )
            QuickMenuItem(
                icon = Icons.Default.Stars,
                label = "My Level",
                gradient = listOf(Color(0xFFFFB300), Color(0xFFFB8C00)),
                onClick = onOpenMyLevel
            )
            QuickMenuItem(
                icon = Icons.Default.Shield,
                label = "VIP Center",
                gradient = listOf(Color(0xFFD4AF37), Color(0xFF996515)),
                onClick = onOpenVipCenter
            )
            QuickMenuItem(
                icon = Icons.Default.Favorite,
                label = "My Fans",
                gradient = listOf(Color(0xFFFF007A), Color(0xFFD81B60)),
                onClick = onOpenMyFans
            )
            QuickMenuItem(
                icon = Icons.Default.AccessTime,
                label = "History",
                gradient = listOf(Color(0xFF3F51B5), Color(0xFF1E88E5)),
                onClick = onOpenHistory
            )
        }
    }
}

@Composable
private fun QuickMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

// ============================================================================
// 4. VIP PROMOTION BANNER
// ============================================================================
@Composable
private fun VipPromotionBanner(
    vipLevel: String,
    currentXp: Int,
    requiredXp: Int,
    onUpgradeClick: () -> Unit,
    onViewVipClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF3B0B66),
                        Color(0xFF210540),
                        Color(0xFF15022B)
                    )
                )
            )
            .border(1.dp, Color(0xFF6B2FB8), RoundedCornerShape(20.dp))
            .clickable { onViewVipClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Banner Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vipLevel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Enjoy exclusive VIP privileges",
                    fontSize = 11.sp,
                    color = Color(0xFFD8B4FE)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar & Upgrade Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandPinkPrimary)
                            .padding(2.dp)
                    ) {
                        Text("⭐", fontSize = 8.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "$currentXp/$requiredXp",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPinkPrimary)
                            .clickable { onUpgradeClick() }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text("Upgrade", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Indicator Line
                LinearProgressIndicator(
                    progress = { (currentXp.toFloat() / requiredXp.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(180.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = BrandPinkPrimary,
                    trackColor = Color(0xFF381559)
                )
            }

            // Right: Winged Gold Shield Emblem 7 & View VIP link
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(GoldAccentColor, Color(0xFFFF8C00))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("7", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFF3D1F00))
                    }
                }

                Text(
                    text = "View VIP >",
                    fontSize = 11.sp,
                    color = Color(0xFFD8B4FE),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onViewVipClick() }
                )
            }
        }
    }
}

// ============================================================================
// 5. TOP FANS SECTION
// ============================================================================
private data class FanItem(
    val rank: Int,
    val name: String,
    val score: String,
    val avatarUrl: String
)

@Composable
private fun TopFansSection(
    onViewAllClick: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current
    val topFans = listOf(
        FanItem(1, "Waisha", "🔥 125.7K", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&auto=format&fit=crop&q=80"),
        FanItem(2, "Drama Queen", "🔥 98.7K", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&auto=format&fit=crop&q=80"),
        FanItem(3, "Jannatul Islam", "🔥 75.2K", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200&auto=format&fit=crop&q=80"),
        FanItem(4, "Husnat Smita", "🔥 64.1K", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80"),
        FanItem(5, "Ayesha Live", "🔥 58.3K", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Top Fans", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = "View all >",
                fontSize = 11.sp,
                color = TextMutedColor,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            topFans.forEach { fan ->
                FanAvatarCard(fan = fan, context = context, onClick = { onUserClick(fan.name) })
            }
        }
    }
}

@Composable
private fun FanAvatarCard(fan: FanItem, context: Context, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .width(62.dp)
    ) {
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            // Crown / Frame for Ranks 1, 2, 3
            val frameBorderColor = when (fan.rank) {
                1 -> GoldAccentColor
                2 -> Color(0xFFC0C0C0)
                3 -> Color(0xFFCD7F32)
                else -> Color(0xFF2E2442)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, frameBorderColor, CircleShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fan.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = fan.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Crown for Rank 1-3
            if (fan.rank <= 3) {
                val crownEmoji = when (fan.rank) {
                    1 -> "👑"
                    2 -> "👑"
                    else -> "👑"
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-8).dp)
                ) {
                    Text(crownEmoji, fontSize = 14.sp)
                }

                // Rank Badge at Bottom Left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(frameBorderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${fan.rank}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            } else {
                // Rank Badge at Top Right for 4 and 5
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF281C3D))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${fan.rank}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = fan.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = fan.score,
            fontSize = 8.sp,
            color = BrandPinkPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================================================
// 6. WALLET + ASSETS SECTION
// ============================================================================
@Composable
private fun WalletAndAssetsSection(
    coinBalance: Int,
    usdValue: String,
    coinsCount: Int,
    gemsCount: Int,
    beansCount: Int,
    medalsCount: Int,
    badgesCount: Int,
    framesCount: Int,
    onTopUpClick: () -> Unit,
    onViewWalletAll: () -> Unit,
    onViewAssetsAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // MY WALLET CARD
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(DarkCardBg)
                .border(1.dp, GlassBorderColor, RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Wallet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "View all >",
                        fontSize = 9.sp,
                        color = TextMutedColor,
                        modifier = Modifier.clickable { onViewWalletAll() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Balance & Top Up
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "%,d".format(coinBalance),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(text = "≈ $$usdValue", fontSize = 9.sp, color = TextMutedColor)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPinkPrimary)
                            .clickable { onTopUpClick() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("Top Up", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Coins, Gems, Beans Sub-row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F0A1A))
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AssetSubItem(icon = "🪙", label = "Coins", count = "%,d".format(coinsCount))
                        AssetSubItem(icon = "🔮", label = "Gems", count = "%,d".format(gemsCount))
                        AssetSubItem(icon = "🔮", label = "Beans", count = "%,d".format(beansCount))
                    }
                }
            }
        }

        // MY ASSETS CARD
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(DarkCardBg)
                .border(1.dp, GlassBorderColor, RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Assets", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "View all >",
                        fontSize = 9.sp,
                        color = TextMutedColor,
                        modifier = Modifier.clickable { onViewAssetsAll() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Medal, Badge, Frame Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AssetTileItem(icon = "🥇", label = "Medal", count = "$medalsCount")
                    AssetTileItem(icon = "🎖️", label = "Badge", count = "$badgesCount")
                    AssetTileItem(icon = "🖼️", label = "Frame", count = "$framesCount")
                }
            }
        }
    }
}

@Composable
private fun AssetSubItem(icon: String, label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 12.sp)
        Text(label, fontSize = 8.sp, color = TextMutedColor)
        Text(count, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun AssetTileItem(icon: String, label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1530))
                .border(1.dp, Color(0xFF332352), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, color = TextMutedColor)
        Text(count, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ============================================================================
// 7. PROFILE MENU LIST SECTION
// ============================================================================
@Composable
private fun ProfileMenuListSection(
    onOpenMyPosts: () -> Unit,
    onOpenMyVideos: () -> Unit,
    onOpenMyMoments: () -> Unit,
    onOpenInviteFriends: () -> Unit,
    onOpenHelpSupport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCardBg)
            .border(1.dp, GlassBorderColor, RoundedCornerShape(20.dp))
            .padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MenuItemRow(
                icon = Icons.Default.PostAdd,
                iconBg = Color(0xFFE91E63),
                title = "My Posts",
                onClick = onOpenMyPosts
            )
            DividerHorizontal()
            MenuItemRow(
                icon = Icons.Default.Videocam,
                iconBg = Color(0xFF9C27B0),
                title = "My Videos",
                onClick = onOpenMyVideos
            )
            DividerHorizontal()
            MenuItemRow(
                icon = Icons.Default.Chat,
                iconBg = Color(0xFF00BCD4),
                title = "My Moments",
                onClick = onOpenMyMoments
            )
            DividerHorizontal()
            MenuItemRow(
                icon = Icons.Default.People,
                iconBg = Color(0xFFFF9800),
                title = "Invite Friends",
                tagText = "Earn Rewards",
                onClick = onOpenInviteFriends
            )
            DividerHorizontal()
            MenuItemRow(
                icon = Icons.Default.HelpOutline,
                iconBg = Color(0xFF4CAF50),
                title = "Help & Support",
                onClick = onOpenHelpSupport
            )
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    tagText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tagText != null) {
                Text(
                    text = tagText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPinkPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMutedColor, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DividerHorizontal() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF1E1730))
    )
}

@Composable
private fun CosmicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090317)) // Extremely dark purple space base
    ) {
        // Draw some beautiful nebula-like glows using layered radial gradients
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Purple nebula on top left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF330954).copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.2f),
                    radius = size.width * 0.9f
                )
            )
            // Blue/Indigo nebula on middle right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF130938).copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.5f),
                    radius = size.width * 0.8f
                )
            )
            // Magenta nebula on bottom left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF570633).copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                    radius = size.width * 0.7f
                )
            )
        }
        
        // Add random twinkling stars in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val starPositions = listOf(
                Offset(0.12f, 0.08f), Offset(0.25f, 0.15f), Offset(0.72f, 0.06f),
                Offset(0.85f, 0.22f), Offset(0.05f, 0.42f), Offset(0.92f, 0.48f),
                Offset(0.18f, 0.65f), Offset(0.78f, 0.72f), Offset(0.35f, 0.88f),
                Offset(0.88f, 0.92f), Offset(0.55f, 0.28f), Offset(0.64f, 0.58f),
                Offset(0.48f, 0.76f)
            )
            starPositions.forEach { pos ->
                val x = pos.x * size.width
                val y = pos.y * size.height
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = 1.2f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = 3.5f,
                    center = Offset(x, y)
                )
            }
        }
        
        content()
    }
}

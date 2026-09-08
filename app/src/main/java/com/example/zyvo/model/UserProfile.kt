package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarEmoji: String = "👤",
    val avatarUrl: String? = null,
    val coverGradientIndex: Int = 0,
    val bio: String = "Live streaming enthusiast & gaming streamer 🚀",
    val gender: String = "Not Specified",
    val location: String = "Global 🌍",
    val userLevel: Int = 12,
    val userXp: Int = 3400,
    val nextLevelXp: Int = 5000,
    val wealthLevel: Int = 8,
    val hostLevel: Int = 15,
    val vipTier: VipTier = VipTier.NONE,
    val vipExpiresTimestamp: Long? = null,
    val followersCount: Int = 1280,
    val followingCount: Int = 240,
    val likesCount: Int = 8900,
    val diamondsEarnedTotal: Int = 45200,
    val giftsReceivedTotal: Int = 320,
    val liveStreamsCount: Int = 48,
    val badges: List<String> = listOf("Verified Broadcaster", "Top Giver", "PK Champion"),
    val isFollowedByCurrentUser: Boolean = false,
    val isBlocked: Boolean = false,
    val isLiveNow: Boolean = false,
    val currentRoomId: String? = null,
    val executiveRole: String? = null,
    val whatsappNumber: String? = null,
    val whatsappDirectUrl: String? = null,
    val followingUserIds: List<String> = emptyList(),
    val isTopHost: Boolean = false
)

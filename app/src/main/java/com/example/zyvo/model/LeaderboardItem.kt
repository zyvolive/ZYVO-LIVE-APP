package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
enum class LeaderboardCategory {
    TOP_HOSTS,
    TOP_GIVERS,
    PK_CHAMPIONS,
    GAINED_FOLLOWERS
}

@Serializable
enum class LeaderboardTimeframe {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY
}

@Serializable
data class LeaderboardItem(
    val rank: Int,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarEmoji: String,
    val avatarUrl: String? = null,
    val scorePoints: Long,
    val metricLabel: String,
    val vipTier: VipTier = VipTier.NONE,
    val userLevel: Int = 1,
    val isLiveNow: Boolean = false,
    val isFollowing: Boolean = false,
    val executiveRole: String? = null,
    val whatsappDirectUrl: String? = null
)

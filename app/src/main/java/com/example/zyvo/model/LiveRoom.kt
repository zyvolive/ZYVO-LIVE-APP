package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
data class LiveRoom(
    val id: String,
    val title: String,
    val description: String = "",
    val creatorIdentity: String,
    val hostId: String = creatorIdentity,
    val hostName: String,
    val hostAvatar: String = "🌟",
    val hostAvatarUrl: String? = null,
    val hostGender: String = "Female",
    val roomCoverUrl: String? = null,
    val coverStyle: String = "FULL_BACKDROP",
    val useProfilePicAsCover: Boolean = true,
    val roomType: RoomType = RoomType.SINGLE_LIVE,
    val category: String = "Entertainment",
    val tags: List<String> = emptyList(),
    val viewerCount: Int = 1420,
    val likesCount: Int = 8900,
    val isLive: Boolean = true,
    val status: String = "LIVE",
    val enableChat: Boolean = true,
    val allowParticipation: Boolean = true,
    val isPrivate: Boolean = false,
    val password: String? = null,
    val seats: List<Seat> = emptyList(),
    val pkState: PkState = PkState(),
    val teamState: TeamState = TeamState(),
    val streamStats: StreamStats = StreamStats(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastHeartbeatAt: Long = createdAt
)

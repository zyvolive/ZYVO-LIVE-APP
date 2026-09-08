package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
data class DirectMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val text: String,
    val timestampFormatted: String,
    val isFromCurrentUser: Boolean,
    val giftAttached: Gift? = null
)

@Serializable
data class ConversationSummary(
    val peerUserId: String,
    val peerDisplayName: String,
    val peerUsername: String,
    val peerAvatarEmoji: String,
    val peerVipTier: VipTier = VipTier.NONE,
    val lastMessageText: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val isPeerLive: Boolean = false
)

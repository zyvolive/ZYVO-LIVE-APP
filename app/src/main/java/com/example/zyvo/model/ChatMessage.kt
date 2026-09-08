package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageType {
    TEXT,
    GIFT,
    SYSTEM,
    REACTION,
    JOIN,
    STAGE_EVENT
}

@Serializable
data class ChatMessage(
    val id: String,
    val senderIdentity: String,
    val senderName: String,
    val senderAvatar: String = "👤",
    val text: String,
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val gift: Gift? = null,
    val giftCount: Int = 1,
    val mention: String? = null,
    val isHost: Boolean = false,
    val isAdmin: Boolean = false
)

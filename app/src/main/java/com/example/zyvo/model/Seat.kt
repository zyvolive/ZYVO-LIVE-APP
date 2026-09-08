package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
data class Seat(
    val id: Int,
    val occupied: Boolean = false,
    val locked: Boolean = false,
    val assignedParticipant: String? = null,
    val participantName: String? = null,
    val avatarEmoji: String = "👤",
    val isMuted: Boolean = false,
    val isVideoOn: Boolean = true,
    val isSpeaking: Boolean = false,
    val role: String = "GUEST"
)

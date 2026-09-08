package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
enum class ParticipantRole {
    HOST,
    ADMIN,
    STAGE_SPEAKER,
    STAGE_GUEST,
    VIEWER
}

@Serializable
data class Participant(
    val identity: String,
    val name: String,
    val avatar: String = "👤",
    val role: ParticipantRole = ParticipantRole.VIEWER,
    val isMutedAudio: Boolean = false,
    val isMutedVideo: Boolean = false,
    val isRequestedToCall: Boolean = false,
    val isReqToPresent: Boolean = false,
    val isBlocked: Boolean = false,
    val seatId: Int = -1,
    val giftPoints: Int = 0,
    val isSpeaking: Boolean = false
)

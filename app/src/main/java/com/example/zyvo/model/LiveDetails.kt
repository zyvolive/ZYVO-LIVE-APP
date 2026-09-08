package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
data class PkState(
    val isActive: Boolean = false,
    val targetRoomId: String = "",
    val targetHostIdentity: String = "host_rival",
    val targetHostName: String = "Aria Nova",
    val targetHostAvatar: String = "⚡",
    val myScore: Int = 1250,
    val targetScore: Int = 1100,
    val remainingSeconds: Int = 180,
    val totalSeconds: Int = 180,
    val pkType: String = "1v1 Duel",
    val winnerIdentity: String? = null,
    val isSuddenDeath: Boolean = false
)

@Serializable
data class TeamState(
    val isActive: Boolean = false,
    val teamRoomId: String = "",
    val teamAdmin: String = "",
    val teamName: String = "Alpha Wolves",
    val enemyTeamName: String = "Shadow Ninjas",
    val myTeamMembers: List<String> = emptyList(),
    val enemyTeamMembers: List<String> = emptyList(),
    val myTeamScore: Int = 3400,
    val enemyTeamScore: Int = 3120,
    val remainingSeconds: Int = 240,
    val defendingTeam: Boolean = false
)

@Serializable
data class StreamStats(
    val fps: Int = 60,
    val bitrateKbps: Int = 4500,
    val resolution: String = "1080p 60fps",
    val latencyMs: Int = 85,
    val packetLossPercent: Double = 0.02,
    val audioCodec: String = "Opus 48kHz Stereo",
    val videoCodec: String = "H.264 / VP8",
    val connectionQuality: String = "Excellent"
)

enum class BeautifyFilter(val displayName: String, val icon: String, val description: String) {
    ORIGINAL("Natural", "✨", "True-to-life studio colors"),
    WARM_GLOW("Sunset Glow", "🌅", "Warm golden hour highlights"),
    CYBERPUNK("Cyber Neon", "🌆", "Electric purple & cyan neon tones"),
    VINTAGE_FILM("Retro Film", "🎞️", "Muted 90s aesthetic grain"),
    STUDIO_PRO("Studio Light", "💡", "High contrast clean studio lighting"),
    BLACK_WHITE("Noir Velvet", "🎬", "Cinematic monochrome shadow depth"),
    VIGNETTE_90S("90s Vignette Beauty", "📸", "Nostalgic premium beauty skin and vignette filter")
}

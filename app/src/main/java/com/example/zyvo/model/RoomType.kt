package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
enum class RoomType(val title: String, val badge: String, val icon: String) {
    SINGLE_LIVE("Single Live", "LIVE", "📹"),
    MULTI_GUEST("Multi-Guest", "MULTI", "👥"),
    AUDIO_STAGE("Audio Stage", "AUDIO", "🎧"),
    PK_BATTLE("PK Battle", "PK DUEL", "⚔️"),
    TEAM_MODE("Team Battle", "TEAM 4v4", "🛡️")
}

package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
enum class VipTier(
    val displayName: String,
    val badge: String,
    val levelNumber: Int,
    val isSvip: Boolean,
    val monthlyCoinPrice: Int,
    val entranceEffect: String,
    val chatColorHex: String,
    val avatarBorderColorHex: String
) {
    NONE("Member", "👤", 0, false, 0, "", "#FFFFFF", "#3D2B56"),
    VIP_1("VIP 1", "👑 VIP 1", 1, false, 500, "⚡ Enter with Neon Glow", "#4DEEEA", "#4DEEEA"),
    VIP_2("VIP 2", "👑 VIP 2", 2, false, 1000, "🔮 Enter with Crystal Aura", "#B967FF", "#B967FF"),
    VIP_3("VIP 3", "👑 VIP 3", 3, false, 2000, "🔥 Enter on Fireburst", "#FF007A", "#FF007A"),
    VIP_4("VIP 4", "👑 VIP 4", 4, false, 3500, "🏎️ Rides in Cyber Car", "#FFD700", "#FFD700"),
    VIP_5("VIP 5", "👑 VIP 5", 5, false, 5000, "🚀 Arrives in Hyper Rocket", "#FF6B00", "#FF6B00"),
    SVIP_1("SVIP 1", "🌟 SVIP 1", 6, true, 8000, "🐉 Summons Golden Dragon Entrance", "#FFD700", "#FFD700"),
    SVIP_2("SVIP 2", "🌟 SVIP 2", 7, true, 15000, "👑 Galaxy Coronation Entrance", "#00F5D4", "#00F5D4"),
    SVIP_3("SVIP 3", "🌟 SVIP 3", 8, true, 30000, "🌌 Cosmic Supernova Entrance", "#FF0055", "#FF0055"),
    SVIP_7("SVIP 7", "💎 SVIP 7 QUEEN", 7, true, 75000, "👑 Celestial Queen Phoenix & Gem Palace Entrance", "#FF007A", "#FF00AA"),
    VIP_9("SVIP 9", "👑 SVIP 9 SUPREME", 9, true, 100000, "👑 Supreme Sovereign Cosmic Emperor Dragon Mount", "#FFD700", "#FF007A")
}

@Serializable
data class VipPackage(
    val tier: VipTier,
    val months: Int,
    val coinCost: Int,
    val originalCoinCost: Int,
    val bonusPerks: List<String>
)

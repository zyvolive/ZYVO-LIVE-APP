package com.example.zyvo.model

import kotlinx.serialization.Serializable

@Serializable
data class Gift(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val coinCost: Int,
    val animationEffect: String,
    val rarity: GiftRarity = GiftRarity.COMMON
)

enum class GiftRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

object PredefinedGifts {
    val ALL_GIFTS = listOf(
        Gift("gift_rose", "Neon Rose", "🌹", 5, "rose_shower", GiftRarity.COMMON),
        Gift("gift_heart", "Heart Flare", "💖", 20, "heart_burst", GiftRarity.COMMON),
        Gift("gift_confetti", "Party Popper", "🎉", 50, "confetti_blast", GiftRarity.COMMON),
        Gift("gift_gem", "Crystal Gem", "🔮", 100, "gem_sparkle", GiftRarity.RARE),
        Gift("gift_fire", "Supernova", "🔥", 250, "fire_wave", GiftRarity.RARE),
        Gift("gift_rocket", "Hyper Rocket", "🚀", 500, "rocket_launch", GiftRarity.EPIC),
        Gift("gift_car", "Cyber Car", "🏎️", 1200, "supercar_drift", GiftRarity.EPIC),
        Gift("gift_dragon", "Golden Dragon", "🐉", 2500, "dragon_roar", GiftRarity.LEGENDARY),
        Gift("gift_crown", "Galaxy Crown", "👑", 5000, "crown_coronation", GiftRarity.LEGENDARY)
    )
}

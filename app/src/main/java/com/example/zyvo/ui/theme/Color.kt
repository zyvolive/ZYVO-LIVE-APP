package com.example.zyvo.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Yeah! Live Brand Colors
val YeahPink = Color(0xFFFF007F)
val YeahPinkLight = Color(0xFFFF3399)
val YeahPurple = Color(0xFF7928CA)
val YeahPurpleDark = Color(0xFF531792)
val YeahCyan = Color(0xFF00F5D4)
val YeahCyanBright = Color(0xFF00E5FF)
val YeahGold = Color(0xFFFFD166)
val YeahOrange = Color(0xFFFF7A00)
val YeahRed = Color(0xFFFF2A6D)
val YeahGreen = Color(0xFF06D6A0)

// Existing Named Colors preserved for full backwards compatibility
val NeonPurple = Color(0xFF7928CA)
val NeonPurpleDark = Color(0xFF531792)
val NeonPurpleLight = Color(0xFFC77DFF)
val ElectricMagenta = Color(0xFFFF007F)
val NeonCyan = Color(0xFF00F5D4)
val CyberBlue = Color(0xFF00E5FF)
val PkRed = Color(0xFFFF2A6D)
val GoldAccent = Color(0xFFFFD166)
val EmeraldGreen = Color(0xFF06D6A0)

// Deep Obsidian Dark Surfaces (Yeah! Live UI Kit Dark Theme)
val DarkBackground = Color(0xFF080611)
val DarkSurface = Color(0xFF100C22)
val DarkCard = Color(0xFF181330)
val DarkCardElevated = Color(0xFF241C44)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFC8C2E0)
val TextMuted = Color(0xFF867EAA)

val OverlayBackground = Color(0xCC080611)
val OverlayLight = Color(0x2EFFFFFF)
val OverlayDark = Color(0x80000000)
val LiveRed = Color(0xFFFF1E44)

// Signature Yeah! Live Gradients
val YeahBrandGradient = Brush.horizontalGradient(listOf(ElectricMagenta, NeonPurple))
val YeahBrandVerticalGradient = Brush.verticalGradient(listOf(ElectricMagenta, NeonPurple))
val YeahCyanBlueGradient = Brush.horizontalGradient(listOf(NeonCyan, CyberBlue))
val YeahGoldOrangeGradient = Brush.horizontalGradient(listOf(GoldAccent, YeahOrange))
val YeahDarkGlassBrush = Brush.verticalGradient(listOf(Color(0x55241C44), Color(0x22100C22)))
val YeahCardBorderBrush = Brush.linearGradient(listOf(OverlayLight, Color.Transparent))
val YeahActiveBorderBrush = Brush.sweepGradient(listOf(ElectricMagenta, NeonCyan, GoldAccent, ElectricMagenta))

/**
 * Returns Golden tick color for male users/hosts and Pink tick color for female users/hosts.
 */
fun getVerifiedTickColor(gender: String? = null, nameOrId: String? = null): Color {
    val g = gender?.lowercase()?.trim() ?: ""
    if (g == "female" || g == "f" || g == "woman") {
        return Color(0xFFFF007A) // Pink tick for female
    }
    if (g == "male" || g == "m" || g == "man") {
        return GoldAccent // Golden tick for male
    }

    // Name/ID fallback heuristic if gender field is unspecified
    val name = (nameOrId ?: "").lowercase()
    val isFemaleName = name.contains("female") || name.contains("woman") || name.contains("queen") || 
                       name.contains("girl") || name.contains("lady") || name.contains("angel") || 
                       name.contains("ansharah") || name.contains("nusrat") || name.contains("maisha") || 
                       name.contains("ayesha") || name.contains("jannat") || name.contains("husnat") ||
                       name.contains("smita") || name.contains("rose") || name.contains("princess") ||
                       name.contains("miss") || name.contains("mrs")

    val isMaleName = name.contains("male") || name.contains("man") || name.contains("king") || 
                     name.contains("rayan") || name.contains("alpha") || name.contains("ceo") ||
                     name.contains("prince") || name.contains("boy") || name.contains("mr")

    return if (isFemaleName) Color(0xFFFF007A) else GoldAccent
}

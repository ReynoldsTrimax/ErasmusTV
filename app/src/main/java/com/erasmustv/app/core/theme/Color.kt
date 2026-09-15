package com.erasmustv.app.core.theme

import androidx.compose.ui.graphics.Color

// Erasmus Pure OLED Black Canvas & Elevated Charcoal Surfaces
val PitchBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF0D0D0D)
val SurfaceElevated = Color(0xFF141414)
val SurfaceCard = Color(0xFF121212)
val SurfaceCardBorder = Color(0xFF242424)
val SurfaceStudioCard = Color(0xFF141414)

// Crisp White Focus Tokens & Hairline Borders
val FocusWhite = Color(0xFFFFFFFF)
val BorderHairline = Color(0xFF1F1F1F)
val BorderSubtle = Color(0xFF2E2E2E)
val BorderFocused = Color(0xFFFFFFFF)
val SurfacePill = Color(0xFF1C1C1E)

// Subtle Brand & Status Tokens
val BrandAccent = Color(0xFFFFFFFF)
val ElectricBlue = Color(0xFF1D90F5)
val ElectricBlueSubtle = Color(0x1F1D90F5)
val RatingGold = Color(0xFFFBBF24)
val MatchGreen = Color(0xFF46D369)
val Top10Red = Color(0xFFE50914)
val ErrorRed = Color(0xFFEF4444)

// Typography Tokens - Editorial Hierarchy
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA1A1AA)
val TextMuted = Color(0xFF71717A)
val BadgeBorder = Color(0xFF27272A)

// Studio Palette References (neutral studio slates)
val StudioDisneyBlue = Color(0xFF0063E5)
val StudioHboPurple = Color(0xFF9933FF)
val StudioNetflixRed = Color(0xFFE50914)
val StudioHuluGreen = Color(0xFF1CE783)
val StudioPrimeBlue = Color(0xFF00A8E1)
val StudioAppleWhite = Color(0xFFFFFFFF)
val StudioHotstarCyan = Color(0xFF00D2D2)
val StudioPeacockYellow = Color(0xFFFFB800)
val StudioParamountBlue = Color(0xFF0064FF)

// Profile Avatar Gradients
val AvatarSlate = listOf(Color(0xFF334155), Color(0xFF0F172A))
val AvatarOcean = listOf(Color(0xFF0369A1), Color(0xFF082F49))
val AvatarViolet = listOf(Color(0xFF6D28D9), Color(0xFF2E1065))
val AvatarRose = listOf(Color(0xFFBE123C), Color(0xFF4C0519))
val AvatarAmber = listOf(Color(0xFFD97706), Color(0xFF451A03))
val AvatarForest = listOf(Color(0xFF047857), Color(0xFF022C22))
val AvatarCrimson = listOf(Color(0xFFE11D48), Color(0xFF4C0519))
val AvatarIndigo = listOf(Color(0xFF4338CA), Color(0xFF1E1B4B))

fun getAvatarGradient(key: String): List<Color> {
    return when (key.lowercase()) {
        "ocean" -> AvatarOcean
        "violet" -> AvatarViolet
        "rose" -> AvatarRose
        "amber" -> AvatarAmber
        "forest" -> AvatarForest
        "crimson" -> AvatarCrimson
        "indigo" -> AvatarIndigo
        else -> AvatarSlate
    }
}


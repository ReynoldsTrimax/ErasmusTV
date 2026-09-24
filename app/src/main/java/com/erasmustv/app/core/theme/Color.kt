package com.erasmustv.app.core.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// ERASMUS DESIGN SYSTEM — Color Tokens
// ============================================================================
// Premium / cinematic / minimal / dark. Layered near-black surfaces (never a
// flat pure #000000 canvas), warm off-white text hierarchy, and a restrained
// accent system that can be re-tinted per hero artwork (see DynamicAccent.kt).
// ============================================================================

// --- Layered Near-Black Surface System ---------------------------------
// Three-step canvas so depth reads even at 10-foot viewing distances.
val ErasmusCanvas = Color(0xFF050505)       // Base screen canvas (deepest layer)
val ErasmusSurface = Color(0xFF070707)      // Raised surfaces: nav rail, sheets, rows
val ErasmusSurfaceRaised = Color(0xFF090909) // Cards, tiles, modals resting above Surface

// Card-level layering (focus/elevation states build on ErasmusSurfaceRaised)
val SurfaceCardRest = Color(0xFF0D0D0E)      // Unfocused card fill
val SurfaceCardFocused = Color(0xFF141416)   // Focused / elevated card fill
val SurfaceOverlay = Color(0xFF161618)       // Modals, submenus, frosted sheets

// --- Legacy aliases (kept so existing screens/components keep compiling) ---
val PitchBlack = ErasmusCanvas            // Canvas background
val SurfaceDark = ErasmusSurface          // Navigation & base overlay
val SurfaceCard = SurfaceCardRest         // Content card surface
val SurfaceElevated = SurfaceCardFocused  // Focused / elevated card surface
val SurfaceSecondary = Color(0xFF202021) // Secondary surface & buttons
val SurfaceCardBorder = Color(0x1AFFFFFF) // Subtle card border

// --- Focus & Border Tokens -------------------------------------------------
// Focus communicates via a *restrained* outline + glow, never a heavy ring.
val FocusWhite = Color(0xFFFFFFFF)
val BorderHairline = Color(0x1FFFFFFF)
val BorderSubtle = Color(0x2EFFFFFF)
val BorderFocused = Color(0xFFFFFFFF)
val SurfacePill = Color(0xFF1C1C1E)
val SurfaceFrosted = Color(0x2E1E1E28)
val SurfaceFrostedBorder = Color(0x26FFFFFF)

// --- Accent System ----------------------------------------------------
// Erasmus does not commit to one permanent bright accent hue. `AccentNeutral`
// is the resting/default accent used when no artwork-derived accent is
// available; screens showing hero art should prefer `LocalAccentColor` from
// DynamicAccent.kt, which extracts a muted tone from the current backdrop.
val AccentNeutral = Color(0xFFD8D3C7)     // Warm neutral accent, default/fallback
val BrandAccent = Color(0xFFFFFFFF)
val ElectricBlue = Color(0xFF1D90F5)
val ElectricBlueSubtle = Color(0x1F1D90F5)
val RatingGold = Color(0xFFFBBF24)       // Signature amber/gold accent (Recommendation #22)
val MatchGreen = Color(0xFF46D369)
val Top10Red = Color(0xFFE50914)
val ErrorRed = Color(0xFFEF4444)

// --- Typography Tokens — Warm, Restrained Hierarchy ---------------------
// Primary text is a warm off-white (never stark #FFFFFF), secondary is a
// muted warm gray, tertiary recedes further for the least important labels.
val ErasmusTextPrimary = Color(0xFFF2EEE8)    // Warm off-white
val ErasmusTextSecondary = Color(0xFFA6A29B)  // Muted warm gray
val ErasmusTextTertiary = Color(0xFF6B6862)   // Dark warm gray, least emphasis

// --- Legacy aliases (kept so existing screens/components keep compiling) ---
val TextPrimary = ErasmusTextPrimary      // Crisp warm white
val TextSecondary = ErasmusTextSecondary  // Muted silver for metadata & subtitles
val TextMuted = ErasmusTextTertiary       // Subtle tertiary text
val BadgeBorder = Color(0x33FFFFFF)

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


package com.erasmustv.app.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * ERASMUS DESIGN SYSTEM — Elevation & Shadow Tokens
 *
 * Erasmus uses soft, cinematic shadow elevation rather than the hard black
 * drop shadows typical of flat web-card UI. Shadows here are intentionally
 * low-elevation and low-opacity — depth should read as a gentle lift off the
 * canvas, not a floating card with a visible hard edge.
 *
 * Ambient/spot shadow colors are near-black at low alpha so they blend into
 * the ErasmusCanvas rather than reading as a distinct halo.
 */
object ErasmusElevation {
    val Rest = 0.dp
    val Raised = 4.dp
    val Focused = 10.dp
    val Overlay = 16.dp // modals, submenus, frosted sheets

    val ShadowAmbientColor = Color(0x66000000)
    val ShadowSpotColor = Color(0x66000000)
}

/**
 * Subtle glow used to reinforce D-pad focus without resorting to a heavy
 * glowing border. The glow is a soft, low-alpha halo tinted by the active
 * accent color (see DynamicAccent.kt) rather than a fixed bright hue.
 */
object ErasmusFocusGlow {
    const val RestAlpha = 0f
    const val FocusedAlpha = 0.35f
    val BlurRadius = 24.dp
    val Spread = 2.dp
}

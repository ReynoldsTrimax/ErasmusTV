package com.erasmustv.app.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * ERASMUS DESIGN SYSTEM — Corner Radius Tokens
 *
 * Rounded geometry is a core part of the Erasmus visual language: it reads as
 * warmer and more premium than sharp rectangular cards at 10-foot viewing
 * distance. Use these tokens instead of hardcoding `RoundedCornerShape`/`dp`
 * values inline so every screen shares one radius scale.
 */
object ErasmusRadius {
    // Cards (posters, continue-watching tiles, studio tiles, grid cells)
    val CardSmall = 12.dp
    val CardMedium = 14.dp
    val CardLarge = 18.dp

    /** Studio / channel tiles — slightly softer than content cards. */
    val Tile = 20.dp

    // Buttons / chips / action pills
    val ButtonSmall = 14.dp
    val ButtonMedium = 18.dp
    val ButtonLarge = 22.dp

    // Search fields / text inputs
    val InputSmall = 16.dp
    val InputLarge = 22.dp

    // Floating navigation (the left rail, floating pills, submenus)
    val NavPill = 32.dp
    val NavPillLarge = 40.dp

    // Large hero / billboard containers, modals, sheets
    val HeroSmall = 20.dp
    val HeroLarge = 28.dp
}

/**
 * Pre-built [RoundedCornerShape]s for the most common surfaces. Prefer these
 * over calling `RoundedCornerShape(x.dp)` inline so radius changes propagate
 * from one place.
 */
object ErasmusShapes {
    val Card = RoundedCornerShape(ErasmusRadius.CardMedium)
    val CardSmall = RoundedCornerShape(ErasmusRadius.CardSmall)
    val CardLarge = RoundedCornerShape(ErasmusRadius.CardLarge)
    val Tile = RoundedCornerShape(ErasmusRadius.Tile)

    val Button = RoundedCornerShape(ErasmusRadius.ButtonMedium)
    val ButtonLarge = RoundedCornerShape(ErasmusRadius.ButtonLarge)

    val Input = RoundedCornerShape(ErasmusRadius.InputSmall)
    val InputLarge = RoundedCornerShape(ErasmusRadius.InputLarge)

    val NavPill = RoundedCornerShape(ErasmusRadius.NavPill)
    val NavPillLarge = RoundedCornerShape(ErasmusRadius.NavPillLarge)

    val Hero = RoundedCornerShape(ErasmusRadius.HeroLarge)
    val HeroSmall = RoundedCornerShape(ErasmusRadius.HeroSmall)
}

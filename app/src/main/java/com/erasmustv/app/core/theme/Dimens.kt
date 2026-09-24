package com.erasmustv.app.core.theme

import androidx.compose.ui.unit.dp

/**
 * ERASMUS DESIGN SYSTEM — Layout Dimension Tokens
 *
 * Centralizes poster/card/hero sizing so it is defined once instead of being
 * hardcoded independently inside every card/row component. Existing
 * components (MediaPosterCard, ContinueWatchingRow, HeroBillboard, etc.)
 * should migrate their default parameter values to reference these tokens
 * during the per-screen redesign passes, rather than repeating raw dp/int
 * literals.
 */
object ErasmusDimens {
    // --- Poster cards (2:3 aspect ratio artwork) ---
    // 138dp ≈ 276px at density 2.0. With the 64dp/48dp row gutters and a 16dp
    // gap that yields ~5.5 posters across a 1920x1080 panel, per spec (5-6).
    val PosterCardWidth = 138.dp
    val PosterCardHeight = 207.dp
    val PosterCardWidthLarge = 160.dp

    /** Gap between cards inside a horizontal rail. */
    val CardSpacing = 16.dp

    /** Gap between a card's artwork and its title block. */
    val CardMetadataGap = 8.dp

    /** Reserved height for the title + metadata block beneath poster artwork. */
    val CardMetadataHeight = 38.dp

    // --- Landscape / continue-watching cards (16:9 artwork) ---
    // 200dp ≈ 400px wide / 225px tall at density 2.0 — the upper end of the
    // 340-400px spec, so the Continue Watching rail reads as distinct from
    // the portrait shelves without dominating the page.
    val LandscapeCardWidth = 200.dp
    val LandscapeCardHeight = 113.dp // 16:9 of 200dp width

    /** Thin, elegant resume progress indicator height. */
    val ProgressBarHeight = 3.dp

    // --- Ranked (Top 10) cards ---
    /** Horizontal room reserved to the left of the poster for the numeral. */
    val RankNumeralGutter = 34.dp

    // --- Poster grids (Watch List, Search results, Categories) ---
    val GridItemSpacing = 20.dp
    val GridRowSpacing = 28.dp

    // --- Studio tiles ---
    val StudioTileSize = 104.dp
    val StudioTileMinWidth = 150.dp

    /**
     * Studio tile height. Sized so a 3x3 grid of the nine studios, plus the
     * page heading and safe margins, fills a 540dp-tall viewport without either
     * leaving a dead band at the bottom or pushing the last row off-screen.
     */
    val StudioTileHeight = 118.dp

    /** Wide label tiles on the Categories page (Browse, Studios, Languages). */
    val CategoryTileWidth = 190.dp
    val CategoryTileHeight = 92.dp

    // --- Content rail rhythm ---
    /** Left gutter for rail titles and the first card in a rail. */
    val RailStartGutter = 64.dp

    /** Right gutter so the last card never sits flush against the bezel. */
    val RailEndGutter = 48.dp

    /** Gap between a rail's heading and its cards. */
    val RailTitleGap = 14.dp

    /**
     * Headroom reserved above and below cards *inside* the rail's own clip
     * bounds, so a focused card's scale lift and shadow halo render fully. Sized
     * to clear the ~1.04 scale spill plus the soft shadow at [ErasmusElevation.Focused].
     */
    val RailFocusHeadroom = 16.dp

    /**
     * Vertical rhythm between two content rails. Combined with
     * [RailFocusHeadroom] on each side, the visible gap between one shelf's
     * cards and the next shelf's heading lands at roughly 54dp — inside the
     * 48-72dp spec band — without the page feeling like a dashboard.
     */
    val RailSpacing = 44.dp

    /** Slightly wider gap after the visually heavier Continue Watching rail. */
    val RailSpacingLarge = 56.dp

    // --- Hero / billboard ---
    // 360dp maps to 720px at density 2.0 (spec: 620-760px at 1920x1080).
    val HeroHeight = 360.dp
    val HeroHeightCompact = 310.dp

    /**
     * Detail-page hero. Taller than a feed hero because it carries synopsis and
     * actions, but held short of the full 540dp viewport so the section below it
     * is partly visible and the page reads as scrollable.
     */
    val DetailHeroHeight = 430.dp

    /** How far the hero's atmospheric wash bleeds below the artwork edge. */
    val HeroAmbientBleed = 260.dp

    /** Left inset for hero copy — 48dp ≈ 96px (spec: 80-120px). */
    val HeroContentStartInset = 48.dp

    /** Max width of the hero copy column — 300dp ≈ 600px (spec: 500-620px). */
    val HeroContentMaxWidth = 300.dp

    val HeroLogoMaxHeight = 82.dp
    val HeroLogoMinHeight = 48.dp
    val HeroButtonHeight = 48.dp

    // --- Navigation rail (legacy left sidebar, superseded by the floating nav) ---
    val NavRailCollapsedWidth = 52.dp
    val NavRailExpandedWidth = 154.dp

    // --- Floating pill navigation ---
    // Sized in dp against a 1080p TV reporting ~960x540dp (density 2.0), so
    // these map to roughly 800-1000px wide / 64-76px tall / 28-40px top margin
    // on real hardware, per the Erasmus navigation spec.
    val NavPillHeight = 38.dp            // ~76px @ density 2.0
    val NavPillHeightCompact = 32.dp     // shrunken state while scrolling
    val NavPillTopMargin = 16.dp         // ~32px @ density 2.0
    val NavPillTopMarginCompact = 10.dp
    val NavPillMaxWidth = 500.dp         // ~1000px @ density 2.0
    val NavPillHorizontalPadding = 14.dp
    val NavPillItemSpacing = 2.dp
    val NavPillIconSize = 17.dp

    /** Vertical space a screen should reserve so content clears the floating nav. */
    val NavPillContentClearance = 68.dp

    /**
     * Bottom clearance the vertical scroller keeps below a focused card, so its
     * focus lift (scale 1.04 + soft shadow) renders fully instead of being
     * clipped flat by the screen edge. Covers the ~8px scale spill plus the
     * ~20px shadow plus comfortable breathing room, per the focus spec's
     * "40-80px" guidance.
     */
    val FocusScrollClearance = 30.dp

    // --- Focus elevation depth (used alongside ErasmusElevation) ---
    val CardRestElevation = 0.dp
    val CardFocusedElevation = 8.dp
}

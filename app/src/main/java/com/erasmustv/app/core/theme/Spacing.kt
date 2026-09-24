package com.erasmustv.app.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ERASMUS DESIGN SYSTEM — Spacing Tokens
 *
 * A single spacing scale used across every screen so gaps, paddings, and
 * gutters stay consistent. Values are TV-calibrated (larger than typical
 * mobile spacing) to remain legible at 10-foot viewing distance.
 */
object ErasmusSpacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val MediumSmall = 12.dp
    val Medium = 16.dp
    val Large = 24.dp
    val XLarge = 32.dp

    /** Vertical rhythm between major page sections (shelves, hero → first row, etc). */
    val Section = 56.dp
    val SectionLarge = 72.dp
}

/**
 * TV-safety: keep primary content roughly 5% of screen width away from the
 * physical screen edge so nothing critical sits under an overscan mask or
 * bezel on real TV hardware.
 */
object ErasmusSafeArea {
    const val HORIZONTAL_MARGIN_FRACTION = 0.05f
    const val VERTICAL_MARGIN_FRACTION = 0.04f

    /** Fallback margin for contexts without a configuration (e.g. previews). */
    val FallbackHorizontalMargin = 48.dp
    val FallbackVerticalMargin = 24.dp
}

/**
 * Returns the horizontal screen-safe margin (~5% of screen width) to apply to
 * top-level screen content so text/action never sits flush against the edge.
 */
@Composable
fun rememberScreenHorizontalMargin(): Dp {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        (configuration.screenWidthDp * ErasmusSafeArea.HORIZONTAL_MARGIN_FRACTION).dp
    }
}

/**
 * Returns the vertical screen-safe margin (~4% of screen height) for top/bottom
 * safe insets on hero content, submenus, and overlays.
 */
@Composable
fun rememberScreenVerticalMargin(): Dp {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenHeightDp) {
        (configuration.screenHeightDp * ErasmusSafeArea.VERTICAL_MARGIN_FRACTION).dp
    }
}

/**
 * Number of poster columns that fit a full-width grid page (Watch List,
 * Categories, studio catalogues) at the current screen size.
 *
 * Column count is derived rather than fixed so the same code yields a
 * comfortable grid at 720p and 1080p. 4K TVs report the same dp dimensions as
 * 1080p at 4x density, so they inherit the 1080p layout automatically — which
 * is exactly right, and is why nothing here is expressed in pixels.
 *
 * @param targetItemWidth ideal poster width; the real cell width flexes around
 *   it so rows always divide the available space exactly.
 */
@Composable
fun rememberPosterGridColumns(
    targetItemWidth: Dp = 132.dp,
    itemSpacing: Dp = 20.dp,
    minColumns: Int = 3,
    maxColumns: Int = 8
): Int {
    val configuration = LocalConfiguration.current
    val horizontalMargin = rememberScreenHorizontalMargin()
    return remember(configuration.screenWidthDp, horizontalMargin, targetItemWidth) {
        posterGridColumnsFor(
            availableWidthDp = configuration.screenWidthDp - (horizontalMargin.value * 2f),
            targetItemWidthDp = targetItemWidth.value,
            itemSpacingDp = itemSpacing.value,
            minColumns = minColumns,
            maxColumns = maxColumns
        )
    }
}

/**
 * Pure column-count maths behind [rememberPosterGridColumns], separated so the
 * responsive behaviour is unit-testable without a composition or a device.
 *
 * Solves `n * item + (n - 1) * spacing <= available` for the largest whole `n`,
 * then clamps it so a very narrow panel never collapses to one giant poster and
 * a very wide one never produces a wall of thumbnails.
 */
fun posterGridColumnsFor(
    availableWidthDp: Float,
    targetItemWidthDp: Float,
    itemSpacingDp: Float,
    minColumns: Int = 3,
    maxColumns: Int = 8
): Int {
    if (targetItemWidthDp <= 0f) return minColumns
    val perColumn = targetItemWidthDp + itemSpacingDp
    return ((availableWidthDp + itemSpacingDp) / perColumn)
        .toInt()
        .coerceIn(minColumns, maxColumns)
}

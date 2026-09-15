package com.erasmustv.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec

/**
 * Android TV Center-Anchored BringIntoViewSpec (Netflix / Prime Video / Hotstar style).
 * Keeps the focused element anchored at [pivotFraction] of the viewport (default 0.5f = exact middle).
 *
 * Behavior:
 * - When moving through items (vertical rows or horizontal cards), the focused item stays anchored
 *   in the middle of the screen, and the content glides smoothly up/down or left/right to meet the selector.
 * - When at the start of the list (e.g. top of screen or first cards), the container stops at 0,
 *   allowing the selector to naturally reach the top/start edge.
 * - When at the end of the list, the container stops at maxScroll,
 *   allowing the selector to naturally reach the bottom/end edge.
 */
@OptIn(ExperimentalFoundationApi::class)
class TvPivotBringIntoViewSpec(
    private val pivotFraction: Float = 0.5f,
    private val deadbandPx: Float = 40f
) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        if (containerSize <= 0f) return 0f

        // If the item itself is as large as or larger than the container, align to top/start
        if (size >= containerSize) {
            return offset
        }

        // Center of the focused child in container coordinates
        val childCenter = offset + (size / 2f)

        // Target center position (e.g. 50% = middle of screen/viewport)
        val targetCenter = containerSize * pivotFraction

        val delta = childCenter - targetCenter

        // If the item's center is already within the tolerance deadband of the target center,
        // do not scroll. This prevents jitter and vertical screen shaking during horizontal navigation!
        if (kotlin.math.abs(delta) <= deadbandPx) {
            return 0f
        }

        return delta
    }
}


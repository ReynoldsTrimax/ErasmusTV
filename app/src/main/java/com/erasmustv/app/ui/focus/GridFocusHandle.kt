package com.erasmustv.app.ui.focus

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

/**
 * ERASMUS GRID FOCUS HANDLE — the runtime half of the spatial focus engine,
 * for a two-dimensional grid (Watch List, Studios, Search results).
 *
 * Same principle as [RailFocusHandle] — a requester per index, geometry from
 * `layoutInfo`, focus granted in the same frame as the key press — but resolved
 * in two dimensions, so it can answer all four directions itself rather than
 * delegating vertical movement to a screen-level coordinator.
 *
 * ## Why not rely on Compose's own traversal
 *
 * Default focus traversal in a `LazyVerticalGrid` follows the order nodes were
 * composed in, which is row-major. That is coincidentally right for LEFT/RIGHT
 * and reliably wrong at the edges: RIGHT on the last cell of a row wraps to the
 * start of the next row, and DOWN from a cell whose column does not exist in a
 * ragged final row either does nothing or jumps to an unrelated cell. Both are
 * exactly the "why did pressing DOWN take me there" moments the spec forbids.
 *
 * Resolving against measured bounds makes the ragged-last-row case fall out for
 * free: [SpatialFocus.resolveDirectional] ranks by column alignment, so DOWN
 * from column 5 into a final row of three cells lands on cell 3 — the nearest
 * thing actually below-ish — rather than nothing at all.
 */
@Stable
class GridFocusHandle(val zoneKey: String) {

    private val requesters = mutableMapOf<Int, FocusRequester>()

    internal var gridState: LazyGridState? = null

    /** The grid's top-left corner in window pixels. */
    internal var windowLeft: Float = 0f
    internal var windowTop: Float = 0f

    internal var itemCount: Int = 0

    fun requesterFor(index: Int): FocusRequester =
        requesters.getOrPut(index) { FocusRequester() }

    /** Window-space bounds of every cell currently on screen. */
    fun visibleBounds(): List<FocusBounds> {
        val info = gridState?.layoutInfo ?: return emptyList()
        return info.visibleItemsInfo.map { item ->
            FocusBounds(
                index = item.index,
                left = windowLeft + item.offset.x,
                top = windowTop + item.offset.y,
                width = item.size.width.toFloat(),
                height = item.size.height.toFloat()
            )
        }
    }

    fun boundsOf(index: Int): FocusBounds? =
        visibleBounds().firstOrNull { it.index == index }

    fun centerXOf(index: Int): Float? = boundsOf(index)?.centerX

    fun isReady(): Boolean = visibleBounds().isNotEmpty()

    /**
     * Resolves a directional move from [fromIndex] and takes focus.
     *
     * @return true when focus moved. False means there is nothing in that
     *   direction *on screen*; the caller decides whether that means "scroll
     *   and retry" (more rows exist below) or "consume the key and stay put"
     *   (this really is the edge of the grid).
     */
    fun moveFocus(fromIndex: Int, direction: SpatialDirection): Boolean {
        val bounds = visibleBounds()
        val current = bounds.firstOrNull { it.index == fromIndex } ?: return false
        // Walking the full ranking is what makes grids with non-focusable items
        // work. A span-width group header ("Movies", "Series & Shows") occupies a
        // real index with real bounds but has no requester, so it wins the
        // geometric ranking for DOWN out of the last movie row and then refuses
        // focus. Falling through to the next candidate lands on the first series
        // card, which is what the viewer was pointing at.
        return SpatialFocus.rankDirectional(current, bounds, direction).any { focusIndex(it) }
    }

    /**
     * True when a move in [direction] from [fromIndex] has no on-screen target
     * but the grid does continue that way — i.e. the caller should scroll rather
     * than treat this as the edge.
     */
    fun needsScrollToContinue(fromIndex: Int, direction: SpatialDirection): Boolean {
        if (direction == SpatialDirection.Down) {
            val lastVisible = visibleBounds().maxOfOrNull { it.index } ?: return false
            return lastVisible < itemCount - 1
        }
        if (direction == SpatialDirection.Up) {
            val firstVisible = visibleBounds().minOfOrNull { it.index } ?: return false
            return firstVisible > 0
        }
        return false
    }

    /** Focuses the cell nearest [desiredCenterPx] horizontally, in the topmost visible row. */
    fun focusNearestInFirstVisibleRow(desiredCenterPx: Float?): Boolean {
        val bounds = visibleBounds()
        if (bounds.isEmpty()) return false
        val topRowY = bounds.minOf { it.top }
        val topRow = bounds.filter { it.top - topRowY < it.height * 0.5f }
        val spans = topRow.map { FocusSpan(it.index, it.left, it.width) }
        return SpatialFocus.rankByNearest(spans, desiredCenterPx).any { focusIndex(it) }
    }

    fun focusIndexOrNearest(index: Int): Boolean {
        if (focusIndex(index)) return true
        val bounds = visibleBounds()
        val fallback = bounds.minByOrNull { kotlin.math.abs(it.index - index) } ?: return false
        return focusIndex(fallback.index)
    }

    fun focusIndex(index: Int): Boolean {
        val requester = requesters[index] ?: return false
        return try {
            requester.requestFocus()
            true
        } catch (_: IllegalStateException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Remembers a [GridFocusHandle]. Keyed on the zone key only, so it survives
 * content refreshes — a watch-list removal must not invalidate the focus
 * plumbing of the cells around it.
 */
@Composable
fun rememberGridFocusHandle(zoneKey: String): GridFocusHandle =
    remember(zoneKey) { GridFocusHandle(zoneKey) }

package com.erasmustv.app.ui.focus

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

/**
 * ERASMUS FOCUS MODIFIERS — the wiring between a card and the focus engine.
 *
 * These are the only place D-pad key codes are interpreted for content. Every
 * rail and grid in the app routes through one of them, which is what makes the
 * navigation *consistent* rather than a per-screen reinvention: a fix here is a
 * fix everywhere.
 */

/**
 * Attaches a rail's `LazyRow` to its [RailFocusHandle].
 *
 * Captures the rail's left edge in window coordinates so item offsets from
 * `layoutInfo` can be made window-absolute, which is what lets one rail reason
 * about another rail's columns.
 *
 * Measured once per layout pass on the *container*, not per card. A per-card
 * `onGloballyPositioned` would fire for every visible card on every frame of
 * every scroll; this fires once and is read only when a directional key is
 * actually pressed.
 */
fun Modifier.railFocusContainer(
    handle: RailFocusHandle,
    listState: LazyListState,
    itemCount: Int
): Modifier {
    handle.listState = listState
    handle.itemCount = itemCount
    return this.onGloballyPositioned { coordinates ->
        handle.windowLeft = coordinates.positionInWindow().x
    }
}

/** Attaches a grid's `LazyVerticalGrid` to its [GridFocusHandle]. */
fun Modifier.gridFocusContainer(
    handle: GridFocusHandle,
    gridState: LazyGridState,
    itemCount: Int
): Modifier {
    handle.gridState = gridState
    handle.itemCount = itemCount
    return this.onGloballyPositioned { coordinates ->
        val position = coordinates.positionInWindow()
        handle.windowLeft = position.x
        handle.windowTop = position.y
    }
}

/**
 * The D-pad contract for a card inside a horizontal rail.
 *
 * - **LEFT / RIGHT** are left to the `LazyRow`'s own traversal, which is
 *   correct inside a row, except at the two edges. On the first card LEFT is
 *   handed to [onLeftEdge] (normally the floating nav) or consumed; on the last
 *   card RIGHT is consumed, so focus can never fall sideways out of the rail
 *   into an unrelated element.
 *
 * - **UP / DOWN** are claimed and handed to the [FeedFocusCoordinator], which
 *   resolves them against the *travelling column* rather than letting Compose
 *   pick whichever card happens to be geometrically nearest. This is the
 *   difference between "DOWN from card 5 lands under card 5" and the diagonal
 *   drift the spec rules out.
 *
 * - On focus, the card's window centre is recorded. That single number is the
 *   entire column-memory mechanism: it is what the next rail down is asked to
 *   match, and it is correct across rails with different card widths because it
 *   is a position, not an index.
 */
fun Modifier.railFocusItem(
    handle: RailFocusHandle,
    coordinator: FeedFocusCoordinator,
    index: Int,
    isFirstItem: Boolean,
    onLeftEdge: (() -> Boolean)? = null,
    onItemFocused: ((Int) -> Unit)? = null
): Modifier = this
    .focusRequester(handle.requesterFor(index))
    .onFocusChanged { state ->
        if (state.isFocused) {
            // Only the *index* is trusted from here — it is what a return from a
            // detail page restores. The card's centre is deliberately not
            // recorded as the travelling column at this point, because the rail
            // has not yet scrolled this card to its pivot; see the note on
            // FeedFocusCoordinator.moveVertical.
            coordinator.onFocusLanded(handle.zoneKey, index, null)
            onItemFocused?.invoke(index)
        }
    }
    .onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        when (event.key) {
            // The column is measured here, at press time, from settled layout.
            Key.DirectionUp ->
                coordinator.moveVertical(handle.zoneKey, -1, handle.centerOf(index))
            Key.DirectionDown ->
                coordinator.moveVertical(handle.zoneKey, +1, handle.centerOf(index))
            Key.DirectionLeft -> {
                // A horizontal press is the user choosing a new column, so the
                // next vertical move should adopt wherever they end up.
                coordinator.unlockColumn()
                if (!isFirstItem) false
                else onLeftEdge?.invoke() ?: true
            }
            // Consume RIGHT only at the true end of the rail. Anywhere else the
            // LazyRow must keep it, so RIGHT scrolls to the next card rather
            // than moving focus vertically just because the card is off screen.
            Key.DirectionRight -> {
                coordinator.unlockColumn()
                handle.isLastIndex(index)
            }
            else -> false
        }
    }

/**
 * The D-pad contract for a cell inside a grid.
 *
 * All four directions are resolved spatially by [GridFocusHandle] against the
 * measured bounds of the cells on screen, which is what makes UP and DOWN
 * column-preserving and makes a ragged final row behave — DOWN from column 5
 * into a three-cell last row lands on cell 3 instead of nowhere.
 *
 * Keys are consumed even when resolution finds no target, so the edges of a grid
 * are walls rather than leaks into whatever Compose would have picked next.
 * [onEscapeUp] and [onEscapeLeft] are the deliberate exits.
 */
fun Modifier.gridFocusItem(
    handle: GridFocusHandle,
    index: Int,
    onFocused: ((Int) -> Unit)? = null,
    onEscapeUp: (() -> Boolean)? = null,
    onEscapeLeft: (() -> Boolean)? = null,
    onScrollRequest: ((SpatialDirection) -> Unit)? = null
): Modifier = this
    .focusRequester(handle.requesterFor(index))
    .onFocusChanged { state ->
        if (state.isFocused) onFocused?.invoke(index)
    }
    .onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        val direction = when (event.key) {
            Key.DirectionUp -> SpatialDirection.Up
            Key.DirectionDown -> SpatialDirection.Down
            Key.DirectionLeft -> SpatialDirection.Left
            Key.DirectionRight -> SpatialDirection.Right
            else -> return@onKeyEvent false
        }

        if (handle.moveFocus(index, direction)) return@onKeyEvent true

        // Nothing on screen in that direction. Either the grid continues that
        // way and needs scrolling, or this is a genuine edge with a defined exit.
        if (handle.needsScrollToContinue(index, direction)) {
            onScrollRequest?.invoke(direction)
            return@onKeyEvent true
        }

        when (direction) {
            SpatialDirection.Up -> onEscapeUp?.invoke() ?: true
            SpatialDirection.Left -> onEscapeLeft?.invoke() ?: true
            // Bottom and right edges of a grid are walls. Consuming is what
            // stops focus from jumping to an unrelated element off the grid.
            else -> true
        }
    }

/**
 * Holds the measured centre of a single focusable between its layout pass and
 * its focus event.
 *
 * Exists because a `Modifier` factory runs again on every recomposition: a
 * `var` captured inside one would be reset each time, so the position has to
 * live in something the caller remembers.
 */
class FocusColumnProbe {
    internal var centerX: Float = -1f
}

/**
 * Records the window centre of a focusable that is not a rail or grid item — a
 * hero action, a season chip — so it still contributes to the travelling column
 * instead of leaving it stale.
 */
fun Modifier.trackFocusColumn(
    memory: FocusZoneMemory,
    probe: FocusColumnProbe
): Modifier = this
    .onGloballyPositioned { coordinates ->
        val position = coordinates.positionInWindow()
        probe.centerX = position.x + coordinates.size.width / 2f
    }
    .onFocusChanged { state ->
        if (state.isFocused && probe.centerX >= 0f) memory.recordColumn(probe.centerX)
    }

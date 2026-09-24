package com.erasmustv.app.ui.focus

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

/**
 * ERASMUS RAIL FOCUS HANDLE — the runtime half of the spatial focus engine,
 * for one horizontal shelf.
 *
 * ## The problem this solves
 *
 * To move focus *immediately* to a spatially-chosen card you need a
 * [FocusRequester] already attached to that card. The previous architecture
 * gave each rail exactly one requester, pinned to a single index decided at
 * composition time, so the only card a rail could ever hand focus to was that
 * one — which is why every vertical move landed on index 0. Changing the pinned
 * index meant recomposing first and focusing later, which is where the
 * `delay(50)` came from, and with it the rapid-input races.
 *
 * ## The approach
 *
 * Keep a requester *per index*, created lazily. A `LazyRow` only composes the
 * items in and near its viewport — roughly eight cards — so at any instant only
 * those requesters are attached to live nodes. Combined with
 * [LazyListState.layoutInfo], which tells us exactly which indices are on
 * screen and where, that means:
 *
 *   - the set of legal targets is known synchronously,
 *   - every legal target already has an attached requester,
 *   - so `requestFocus()` on the chosen one lands **in the same frame**, with
 *     no scroll animation and no delay in front of it.
 *
 * The scroll that follows is then a *consequence* of focus rather than a
 * precondition for it: `focusable()` raises a bring-into-view request, and the
 * rail's `BringIntoViewSpec` animates it. Focus is instant; only the pixels
 * take time. That is the inversion the spec asks for in "animations must not
 * delay actual navigation".
 *
 * ## Geometry source
 *
 * Item positions come from `layoutInfo` plus the rail's own window offset,
 * captured once per layout pass by the rail container. Deliberately *not*
 * `onGloballyPositioned` per card: that fires for every card on every frame of
 * every scroll, whereas `layoutInfo` is read only at the instant a directional
 * key is pressed.
 */
@Stable
class RailFocusHandle(val zoneKey: String) {

    private val requesters = mutableMapOf<Int, FocusRequester>()

    /** Set by the rail container each layout pass. */
    internal var listState: LazyListState? = null

    /** The rail's left edge in window pixels, so item offsets become window-absolute. */
    internal var windowLeft: Float = 0f

    /** Total item count, so edge consumption knows where the last card is. */
    internal var itemCount: Int = 0

    /**
     * The requester for [index]. Stable across recompositions, so the same card
     * keeps the same requester identity even as the rail scrolls.
     */
    fun requesterFor(index: Int): FocusRequester =
        requesters.getOrPut(index) { FocusRequester() }

    /**
     * Window-space horizontal spans of every card currently on screen.
     *
     * Only visible items are returned, and that is the point: the spatially
     * correct destination for a vertical move is something the viewer can
     * actually see. Offering an off-screen card as a candidate would be both
     * unfocusable and wrong.
     */
    fun visibleSpans(): List<FocusSpan> {
        val info = listState?.layoutInfo ?: return emptyList()
        return info.visibleItemsInfo.map { item ->
            FocusSpan(
                index = item.index,
                start = windowLeft + item.offset,
                size = item.size.toFloat()
            )
        }
    }

    /** Window-space horizontal centre of [index], or null if it is not on screen. */
    fun centerOf(index: Int): Float? =
        visibleSpans().firstOrNull { it.index == index }?.center

    /** True when this rail currently has at least one composed, focusable card. */
    fun isReady(): Boolean = visibleSpans().isNotEmpty()

    /**
     * Focuses the on-screen card nearest [desiredCenterPx], immediately.
     *
     * @return true when focus was taken. False means the rail has nothing
     *   composed yet, and the caller should fall back to its scroll-then-retry
     *   path.
     */
    fun focusNearest(desiredCenterPx: Float?): Boolean {
        val ranked = SpatialFocus.rankByNearest(visibleSpans(), desiredCenterPx)
        // Walk the ranking rather than taking only the winner: a card can be
        // measured but momentarily detached mid-scroll, and falling through to
        // the next best target is always better than the key doing nothing.
        return ranked.any { focusIndex(it) }
    }

    /**
     * Focuses [index] if it is composed; otherwise the nearest composed card to
     * it, so a remembered index that has scrolled out of the window degrades to
     * the closest thing on screen instead of silently doing nothing.
     */
    fun focusIndexOrNearest(index: Int): Boolean {
        if (focusIndex(index)) return true
        val spans = visibleSpans()
        val fallback = spans.minByOrNull { kotlin.math.abs(it.index - index) } ?: return false
        return focusIndex(fallback.index)
    }

    /**
     * Requests focus on exactly [index].
     *
     * The try/catch is load-bearing rather than defensive noise:
     * `requestFocus()` throws when the requester is not attached to a live
     * node, which is the normal state of affairs for any card the `LazyRow` has
     * scrolled out of composition. A false return is information the caller
     * uses, not an error to log.
     */
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

    /** Index of the first card, and of the last — used for edge key consumption. */
    fun isLastIndex(index: Int): Boolean = itemCount > 0 && index == itemCount - 1
}

/**
 * Remembers a [RailFocusHandle] for [zoneKey].
 *
 * Keyed on the zone key alone, *not* on the item list, so the handle — and
 * therefore every requester identity — survives content refreshes. An API
 * response arriving must not invalidate the focus plumbing of a rail the user
 * is standing in.
 */
@Composable
fun rememberRailFocusHandle(zoneKey: String): RailFocusHandle =
    remember(zoneKey) { RailFocusHandle(zoneKey) }

/**
 * A screen-scoped cache of [RailFocusHandle]s keyed by zone.
 *
 * Needed because a feed's shelf count is data-driven: calling
 * [rememberRailFocusHandle] inside a `map` over the shelves would tie handle
 * identity to a varying number of call sites, so appending a shelf could
 * reshuffle which handle belongs to which rail. Looking handles up by key
 * instead means a rail keeps its handle — and therefore the user keeps their
 * focus — no matter how the feed grows or reorders around it.
 *
 * Held at screen scope so it also outlives the loading/feed branch switch.
 */
@Stable
class RailFocusHandleStore {
    private val handles = mutableMapOf<String, RailFocusHandle>()

    fun handleFor(zoneKey: String): RailFocusHandle =
        handles.getOrPut(zoneKey) { RailFocusHandle(zoneKey) }
}

@Composable
fun rememberRailFocusHandleStore(): RailFocusHandleStore =
    remember { RailFocusHandleStore() }

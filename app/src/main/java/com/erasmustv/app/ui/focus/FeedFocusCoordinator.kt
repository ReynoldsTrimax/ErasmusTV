package com.erasmustv.app.ui.focus

import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A vertical band of a feed that can accept focus at a given horizontal
 * position: one content rail, or the hero's action row.
 */
interface FocusableZone {
    val zoneKey: String

    /** Index of this zone's item in the screen's `LazyColumn`. */
    val rowIndex: Int

    /** True when this zone currently has composed, focusable content. */
    fun isReady(): Boolean

    /**
     * Takes focus at the horizontal position nearest [desiredCenterPx].
     * Returns false when nothing in this zone is composed yet.
     */
    fun takeFocus(desiredCenterPx: Float?): Boolean

    /** Takes focus at a specific item index, degrading to the nearest composed one. */
    fun takeFocusAt(index: Int): Boolean
}

/** Adapts a horizontal shelf to [FocusableZone]. */
class RailZone(
    override val rowIndex: Int,
    private val handle: RailFocusHandle
) : FocusableZone {
    override val zoneKey: String get() = handle.zoneKey
    override fun isReady(): Boolean = handle.isReady()
    override fun takeFocus(desiredCenterPx: Float?): Boolean = handle.focusNearest(desiredCenterPx)
    override fun takeFocusAt(index: Int): Boolean = handle.focusIndexOrNearest(index)
}

/**
 * Adapts a zone with one canonical entry point — the hero's primary action, a
 * page's single heading control — to [FocusableZone].
 *
 * Horizontal position is ignored on purpose: a hero has two or three buttons,
 * and landing on "Details" because it happened to align with the column the
 * user came from would be worse than always landing on "Watch Now".
 */
class SingleTargetZone(
    override val zoneKey: String,
    override val rowIndex: Int,
    private val requester: FocusRequester
) : FocusableZone {
    override fun isReady(): Boolean = true

    override fun takeFocus(desiredCenterPx: Float?): Boolean = takeFocusAt(0)

    override fun takeFocusAt(index: Int): Boolean = try {
        requester.requestFocus()
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * ERASMUS FEED FOCUS COORDINATOR.
 *
 * Owns vertical movement for a screen built as a `LazyColumn` of zones, and is
 * the single place that knows the answer to "the user pressed DOWN — what now".
 *
 * ## Focus first, scroll second
 *
 * The previous per-screen implementation was:
 *
 *     animateScrollToItem(target) → delay(50) → requestFocus()
 *
 * which has three failure modes the spec calls out individually. Focus lags the
 * key press by a scroll animation plus 50ms (§27). Holding DOWN launches
 * overlapping coroutines whose `catch (_: Exception)` swallows the
 * `CancellationException` and then focuses a stale row, so focus and scroll
 * desynchronise (§26). And because focus only moves after the animation, a
 * second press during the animation is effectively dropped (§39).
 *
 * This coordinator inverts it. [moveVertical] resolves the target and calls
 * `requestFocus()` synchronously — the destination's cards are already composed,
 * because `LazyColumn` composes past its viewport, and [RailFocusHandle] keeps a
 * requester per index precisely so any composed card is immediately focusable.
 * The scroll then happens as a *consequence*: `focusable()` raises a
 * bring-into-view request which [rememberFeedBringIntoViewSpec] animates.
 *
 * So the focus state is correct within the same frame as the key press, and the
 * pixels catch up afterwards. Pressing DOWN five times quickly moves five zones
 * down and animates once, to the right place.
 *
 * The scroll-then-retry coroutine still exists, but only as the fallback for a
 * zone far enough off screen that `LazyColumn` has not composed it. It is a
 * single cancellable [Job] so the newest press always wins.
 */
@Stable
class FeedFocusCoordinator internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    val memory: FocusZoneMemory
) {
    private val zones = mutableMapOf<String, FocusableZone>()
    private var zoneOrder: List<String> = emptyList()
    private var pendingJob: Job? = null

    /**
     * The zone focus is currently in. Used to send focus back where it came from
     * when the user drops out of the floating nav, rather than to a fixed row.
     */
    var activeZoneKey: String? by mutableStateOf(null)
        private set

    /**
     * True once a vertical run has fixed the travelling column.
     *
     * Vertical movement *follows* a column; it does not redefine one. Without
     * this lock, every landing re-anchored the column to the card it happened to
     * land on, so a run of DOWN presses drifted sideways — each hop could shift
     * by up to half a card, and eight hops walked several columns across the
     * screen. Only a horizontal press means "I am choosing a new column", which
     * is what [unlockColumn] records.
     */
    private var columnLocked: Boolean = false

    /**
     * Declares the screen's zones top to bottom. Called from composition on
     * every pass; zones absent from [orderedKeys] stop being navigation targets,
     * which is how a shrinking feed avoids leaving stale destinations behind.
     */
    fun setZoneOrder(orderedKeys: List<String>) {
        zoneOrder = orderedKeys
    }

    fun register(zone: FocusableZone) {
        zones[zone.zoneKey] = zone
    }

    /**
     * Records that the user moved horizontally, so the next vertical move adopts
     * a fresh column from wherever they end up.
     */
    fun unlockColumn() {
        columnLocked = false
    }

    /** Records that focus landed on [index] of [zoneKey], at window centre [centerPx]. */
    fun onFocusLanded(zoneKey: String, index: Int, centerPx: Float?) {
        activeZoneKey = zoneKey
        memory.record(zoneKey, index, centerPx)
    }

    /** Records focus landing on a zone with no meaningful item index (the hero). */
    fun onZoneFocused(zoneKey: String) {
        activeZoneKey = zoneKey
    }

    /**
     * Moves focus one zone up or down from [fromZoneKey], preserving the
     * traveling column.
     *
     * @param delta -1 for UP, +1 for DOWN.
     * @param columnOverride the source item's horizontal centre measured *now*,
     *   at the instant the key was pressed. Supplied by the caller because a
     *   position recorded back when focus first landed is not yet settled: a
     *   rail scrolls the newly focused card toward its pivot *after* the focus
     *   event, so a centre captured in `onFocusChanged` is the card's pre-scroll
     *   position — shifted by up to a full card pitch in the direction of
     *   travel. Measuring at press time reads the layout the viewer is actually
     *   looking at.
     *
     *   It is adopted only at the *start* of a vertical run (see [columnLocked]);
     *   subsequent presses reuse the established column so a run of DOWN presses
     *   travels straight down instead of drifting sideways.
     * @return true when the key was handled. Returning true at the last zone is
     *   deliberate: the key is consumed so focus cannot escape the feed into an
     *   unrelated element, and the user simply stays put.
     */
    fun moveVertical(fromZoneKey: String, delta: Int, columnOverride: Float? = null): Boolean {
        val currentPosition = zoneOrder.indexOf(fromZoneKey)
        if (currentPosition < 0) return false

        if (columnOverride != null && !columnLocked) {
            memory.recordColumn(columnOverride)
        }
        columnLocked = true
        val column = memory.columnOrNull()

        val range = if (delta > 0) {
            (currentPosition + 1) until zoneOrder.size
        } else {
            (currentPosition - 1) downTo 0
        }

        // Walk outward one zone at a time. Skipping an empty zone rather than
        // stopping on it is what lets a rail that resolved to zero items sit in
        // the feed without becoming a vertical dead end.
        for (position in range) {
            val zone = zones[zoneOrder[position]] ?: continue
            if (zone.takeFocus(column)) {
                pendingJob?.cancel()
                return true
            }
        }

        // Nothing composed in that direction. Scroll the nearest registered zone
        // into view and retry once it has been laid out.
        val fallback = range.asSequence().mapNotNull { zones[zoneOrder[it]] }.firstOrNull()
            ?: return true // genuinely the edge of the feed — consume and stay.

        scheduleScrollAndFocus(fallback, column)
        return true
    }

    /**
     * Sends focus into content from the floating navigation.
     *
     * Prefers the zone the user actually left, then the remembered zone, then
     * the first zone — so dropping out of the nav never dumps focus on an
     * unrelated row.
     */
    fun enterContent(): Boolean {
        // Coming back in from the navigation is a fresh start, not a continuation
        // of whatever vertical run preceded it.
        columnLocked = false
        val column = memory.columnOrNull()
        val candidates = listOfNotNull(activeZoneKey, memory.lastZoneKey) + zoneOrder
        for (key in candidates) {
            val zone = zones[key] ?: continue
            if (zone.takeFocus(column)) return true
        }
        val fallback = zoneOrder.firstNotNullOfOrNull { zones[it] } ?: return false
        scheduleScrollAndFocus(fallback, column)
        return true
    }

    /**
     * Restores focus to the exact card the user left from — the BACK-from-detail
     * path. Returns false when there is nothing remembered, so the caller can
     * fall back to its intentional first-entry target.
     */
    fun restoreRememberedFocus(): Boolean {
        columnLocked = false
        val key = memory.lastZoneKey ?: return false
        val zone = zones[key] ?: return false
        val index = memory.indexFor(key) ?: 0
        if (zone.takeFocusAt(index)) return true
        scheduleScrollAndFocus(zone, memory.columnOrNull(), preferredIndex = index)
        return true
    }

    /** Focuses a named zone, used for intentional page-entry targets. */
    fun focusZone(zoneKey: String): Boolean {
        val zone = zones[zoneKey] ?: return false
        if (zone.takeFocus(memory.columnOrNull())) return true
        scheduleScrollAndFocus(zone, memory.columnOrNull())
        return true
    }

    /**
     * The off-screen fallback: scroll [zone] into view and focus it as soon as it
     * exists.
     *
     * Only reached when the destination is far enough away that `LazyColumn` has
     * not composed it — a long detail page whose hero has scrolled entirely out,
     * for instance. Everything adjacent takes the synchronous path above.
     *
     * A single [Job], cancelled on every new request, so holding a direction
     * cannot stack retries that land out of order.
     *
     * The retry is a bounded loop rather than a fixed wait. A fixed
     * "scroll, wait two frames, focus once" is a guess about how long
     * composition takes, and it was wrong often enough that UP off the bottom of
     * a detail page did nothing at all. Polling each frame instead means focus
     * lands on the first frame the zone is ready — usually while the scroll
     * animation is still running, so the movement reads as one gesture — and
     * gives up after a fixed budget instead of hanging.
     */
    private fun scheduleScrollAndFocus(
        zone: FocusableZone,
        column: Float?,
        preferredIndex: Int? = null
    ) {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            fun tryLand(): Boolean =
                if (preferredIndex != null) zone.takeFocusAt(preferredIndex)
                else zone.takeFocus(column)

            // Kick the scroll off without waiting for it: the zone becomes
            // composed well before the animation settles, and focus should not
            // wait for pixels.
            val scrollJob = launch {
                runCatching { listState.animateScrollToItem(zone.rowIndex) }
            }

            repeat(FOCUS_RETRY_FRAMES) {
                withFrameNanos { }
                if (tryLand()) {
                    return@launch
                }
            }
            // Last resort: let the scroll finish, then try once more.
            scrollJob.join()
            withFrameNanos { }
            tryLand()
        }
    }

    private companion object {
        /**
         * Frames to keep retrying an off-screen focus request — about a third of
         * a second at 60Hz, comfortably longer than a `LazyColumn` needs to
         * compose an item, and short enough that a genuinely impossible request
         * does not leave a job running.
         */
        const val FOCUS_RETRY_FRAMES = 20
    }
}

@Composable
fun rememberFeedFocusCoordinator(
    listState: LazyListState,
    scope: CoroutineScope,
    memory: FocusZoneMemory
): FeedFocusCoordinator = remember(listState, memory) {
    FeedFocusCoordinator(listState, scope, memory)
}

/**
 * Vertical bring-into-view behaviour for a feed sitting under the floating
 * navigation pill.
 *
 * Its whole job is to define a *usable band* — the viewport minus the nav pill
 * at the top and a focus-clearance strip at the bottom — and to keep the
 * focused item inside it with the least scroll possible.
 *
 * ## Why the bottom inset exists
 *
 * A card's focus lift (scale 1.04 plus a soft shadow) is a *visual* transform:
 * it grows the card by a handful of pixels below its laid-out bounds, but the
 * bring-into-view request is raised for the laid-out bounds only. So a card
 * whose measured bottom sits flush at the viewport edge looks "fully visible"
 * to a naive spec, which then never scrolls — and the lifted, shadowed bottom
 * edge spills past the screen and is clipped to a flat line. Reserving
 * [bottomInsetPx] means a card is only considered settled once its lift has room
 * to render, so the focus border is never cut.
 *
 * ## Why it is edge-based, not pivot-based
 *
 * The previous version centred the focused item and returned 0 the instant the
 * item's raw bounds fit. The two rules disagreed near the edges, so a partially
 * off-screen card produced a large centring scroll while a flush card produced
 * none — and, worse, moving LEFT/RIGHT inside a rail re-raised a request whose
 * answer flip-flopped, nudging the page vertically on nearly every horizontal
 * press. That is the jerk.
 *
 * Edge-based with a [deadbandPx] tolerance fixes both: an item already inside
 * the band scrolls by zero (so horizontal traversal never moves the page), and
 * one outside it is nudged by exactly the amount needed to clear the nearest
 * inset — a small, smooth, single scroll per vertical step.
 */
@OptIn(ExperimentalFoundationApi::class)
class FeedBringIntoViewSpec(
    private val topInsetPx: Float,
    private val bottomInsetPx: Float,
    private val deadbandPx: Float = 2f
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float
    ): Float {
        if (containerSize <= 0f) return 0f

        val topBound = topInsetPx
        val bottomBound = containerSize - bottomInsetPx
        val band = bottomBound - topBound

        // An item taller than the usable band can never sit inside it; align its
        // top under the nav so its heading and first cards are what shows.
        if (size >= band) {
            val delta = offset - topBound
            return if (kotlin.math.abs(delta) <= deadbandPx) 0f else delta
        }

        // Above the band (or tucked under the nav): pull it down to the top inset.
        if (offset < topBound - deadbandPx) {
            return offset - topBound
        }

        // Below the band: pull it up so its lifted bottom clears the bottom inset.
        if (offset + size > bottomBound + deadbandPx) {
            return offset + size - bottomBound
        }

        // Comfortably inside the band — hold. Returning zero here is what keeps
        // horizontal traversal from disturbing the vertical position.
        return 0f
    }
}

@Composable
fun rememberFeedBringIntoViewSpec(
    topInsetPx: Float,
    bottomInsetPx: Float
): FeedBringIntoViewSpec =
    remember(topInsetPx, bottomInsetPx) {
        FeedBringIntoViewSpec(topInsetPx, bottomInsetPx)
    }

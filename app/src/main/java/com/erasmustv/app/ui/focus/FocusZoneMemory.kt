package com.erasmustv.app.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * ERASMUS FOCUS MEMORY.
 *
 * Two distinct kinds of "where was I" are tracked here, and conflating them is
 * what made the previous single-slot implementation ineffective:
 *
 *  1. **Per-zone index** — for every rail or grid the user has visited on this
 *     screen, which item they were on. This is what makes leaving a shelf and
 *     coming back to it land on the same card, and what makes BACK from a
 *     detail page land on the poster the user opened.
 *
 *  2. **The travelling column** — the horizontal window position of the card
 *     the user was last on, carried *across* zones. This is what makes
 *     pressing DOWN four rails in a row stay in the same visual column instead
 *     of collapsing to the left edge.
 *
 * The old design stored a single (sectionKey, itemIndex) pair, so the moment
 * the user focused a card in rail B, rail A's remembered index was gone — and
 * because the key no longer matched, `entryIndexFor` returned 0 for everything.
 * Every vertical move therefore landed on column 0. Keeping a map fixes the
 * first problem; keeping the column centre separately fixes the second.
 *
 * ## Why saveable, and why primitives
 *
 * Navigation Compose disposes a destination's composition when you navigate
 * away, so anything in a plain `remember` is gone by the time the user presses
 * BACK. Only `rememberSaveable` state is retained on the back stack entry,
 * which is why the whole thing has to flatten to a list of primitives.
 */
@Stable
class FocusZoneMemory internal constructor(
    zoneIndices: Map<String, Int>,
    lastZoneKey: String?,
    columnCenterPx: Float
) {
    private val indices = zoneIndices.toMutableMap()

    /**
     * Key of the zone the user was in most recently. Distinct from the index
     * map: on re-entry we restore focus *to this zone*, then let the map decide
     * which item inside it.
     */
    var lastZoneKey: String? by mutableStateOf(lastZoneKey)
        private set

    /**
     * Horizontal centre, in window pixels, of the last focused item — the
     * user's implicit column. Negative means "not established yet".
     */
    var columnCenterPx: Float by mutableFloatStateOf(columnCenterPx)
        private set

    /** True once the user has focused anything, i.e. there is a target to restore. */
    val hasTarget: Boolean get() = lastZoneKey != null

    /**
     * Records a focus landing.
     *
     * @param itemCenterPx window-space horizontal centre of the focused item,
     *   or null when the caller cannot measure it (in which case the existing
     *   travelling column is left alone rather than being reset, so a single
     *   unmeasurable item does not destroy the column the user built up).
     */
    fun record(zoneKey: String, index: Int, itemCenterPx: Float? = null) {
        indices[zoneKey] = index
        lastZoneKey = zoneKey
        if (itemCenterPx != null && itemCenterPx >= 0f) {
            columnCenterPx = itemCenterPx
        }
    }

    /**
     * Overrides the travelling column without claiming a zone. Used when focus
     * enters something that is not a rail item (a hero action, a nav tab) but
     * still establishes a horizontal intent.
     */
    fun recordColumn(itemCenterPx: Float) {
        if (itemCenterPx >= 0f) columnCenterPx = itemCenterPx
    }

    /** Remembered index for [zoneKey], or null if the user has never been there. */
    fun indexFor(zoneKey: String): Int? = indices[zoneKey]

    fun isLastZone(zoneKey: String): Boolean = lastZoneKey == zoneKey

    /** The travelling column, or null when none has been established. */
    fun columnOrNull(): Float? = columnCenterPx.takeIf { it >= 0f }

    /**
     * Drops every remembered index but keeps the travelling column. Called when
     * the underlying content is genuinely replaced (a different profile, a
     * cleared search) so stale indices cannot point at unrelated titles.
     */
    fun resetZones() {
        indices.clear()
        lastZoneKey = null
    }

    internal companion object {
        /**
         * Flattened as [columnCenterPx, lastZoneKey, k0, v0, k1, v1, ...] so
         * the whole thing survives on a `SavedStateHandle`-backed bundle.
         */
        val Saver = listSaver<FocusZoneMemory, Any?>(
            save = { memory ->
                buildList {
                    add(memory.columnCenterPx)
                    add(memory.lastZoneKey)
                    memory.indices.forEach { (key, index) ->
                        add(key)
                        add(index)
                    }
                }
            },
            restore = { saved ->
                val column = saved.getOrNull(0) as? Float ?: -1f
                val lastZone = saved.getOrNull(1) as? String
                val map = mutableMapOf<String, Int>()
                var cursor = 2
                while (cursor + 1 < saved.size) {
                    val key = saved[cursor] as? String
                    val index = saved[cursor + 1] as? Int
                    if (key != null && index != null) map[key] = index
                    cursor += 2
                }
                FocusZoneMemory(map, lastZone, column)
            }
        )
    }
}

/**
 * Creates a [FocusZoneMemory] that survives navigating away and back.
 *
 * One per screen, held at screen scope — *outside* any `AnimatedContent` or
 * state-driven subtree, since state remembered inside one of those is discarded
 * the moment the state object changes identity.
 */
@Composable
fun rememberFocusZoneMemory(): FocusZoneMemory =
    rememberSaveable(saver = FocusZoneMemory.Saver) {
        FocusZoneMemory(emptyMap(), null, -1f)
    }

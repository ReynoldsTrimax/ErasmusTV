package com.erasmustv.app.ui.focus

import kotlin.math.abs

/**
 * ERASMUS SPATIAL FOCUS — the geometry layer.
 *
 * Everything in this file is pure: no Compose, no Android, no state. It takes
 * the measured bounds of the candidates that are currently on screen and
 * answers one question — *given where the user is and which way they pressed,
 * which item did they mean?*
 *
 * It is separated out precisely so it can be reasoned about and unit-tested
 * without a device, because "pressing DOWN from card 3 lands on the card below
 * card 3" is a claim about arithmetic, not about rendering.
 *
 * ## Why not Euclidean distance
 *
 * The obvious implementation — nearest candidate by straight-line distance —
 * produces the diagonal jumps that make TV interfaces feel broken. From the
 * 4th card of a landscape rail, the *closest* poster in the row below is often
 * the 2nd, because the rows have different card widths and the 2nd poster's
 * corner happens to be nearer than the 5th poster's centre.
 *
 * So the ordering is deliberately lexicographic, per the Erasmus focus spec:
 *
 *   1. **Direction** — a candidate must genuinely lie in the pressed
 *      direction. Anything else is not a candidate at all.
 *   2. **Alignment** — of what remains, prefer the candidate whose centre
 *      lines up on the cross axis. This is what makes a column feel like a
 *      column.
 *   3. **Distance** — only used to break ties between similarly aligned
 *      candidates.
 */

/** A candidate's bounds on one axis, in window pixels. */
data class FocusSpan(
    val index: Int,
    val start: Float,
    val size: Float
) {
    val center: Float get() = start + size / 2f
    val end: Float get() = start + size
}

/** A candidate's full 2D bounds in window pixels. Used for grids. */
data class FocusBounds(
    val index: Int,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

enum class SpatialDirection { Left, Right, Up, Down }

object SpatialFocus {

    /**
     * How much two candidates' alignment scores may differ before the better
     * aligned one wins outright rather than being compared on distance.
     *
     * In window pixels. Sized at roughly a third of a poster card so that two
     * posters in adjacent columns are never treated as "equally aligned",
     * while a landscape card and the poster beneath it — which can never line
     * up exactly — still are.
     */
    private const val ALIGNMENT_TOLERANCE_PX = 48f

    /**
     * A candidate must clear the current item's edge by at least this much to
     * count as being "in" the pressed direction. Guards against a candidate
     * that merely overlaps by a pixel registering as the item below.
     */
    private const val DIRECTION_THRESHOLD_PX = 4f

    /**
     * Index of the candidate whose centre is nearest [desiredCenter].
     *
     * This is the row-to-row workhorse. When the user leaves a rail we carry
     * the horizontal centre of the card they were on; the destination rail
     * asks this function which of its on-screen cards sits under that same
     * horizontal position.
     *
     * Because it compares centres — not indices — it is correct across rails
     * with different card widths, which index arithmetic can never be.
     *
     * @param desiredCenter null when there is no travelling column (first
     *   entry to the screen), in which case the first candidate wins.
     * @return the chosen index, or null when there are no candidates.
     */
    fun nearestIndex(candidates: List<FocusSpan>, desiredCenter: Float?): Int? =
        rankByNearest(candidates, desiredCenter).firstOrNull()

    /**
     * Every candidate ranked best-first by proximity to [desiredCenter].
     *
     * Callers walk this list rather than taking only the winner, because not
     * every measured candidate can actually accept focus: a grid's group
     * headers occupy real indices and real bounds but are deliberately not
     * focusable, and a card can be momentarily detached mid-scroll. Walking the
     * ranking means the move lands on the next best *real* target instead of
     * failing outright — which is the difference between DOWN from the last
     * movie row reaching the first series card and DOWN doing nothing at all.
     */
    fun rankByNearest(candidates: List<FocusSpan>, desiredCenter: Float?): List<Int> {
        if (candidates.isEmpty()) return emptyList()
        if (desiredCenter == null) return candidates.map { it.index }.sorted()

        return candidates.sortedWith(
            compareBy(
                // A candidate that actually contains the desired position ranks
                // above one that is merely closest by centre — it is literally
                // what the user is pointing at. Matters when a wide card sits
                // beside a narrow one.
                { candidate ->
                    if (desiredCenter >= candidate.start && desiredCenter < candidate.end) 0 else 1
                },
                { candidate -> abs(candidate.center - desiredCenter) },
                // Deterministic final tie-break, so two equidistant cards do not
                // resolve differently depending on list order.
                { candidate -> candidate.index }
            )
        ).map { it.index }
    }

    /**
     * Directional resolution inside a 2D field (a grid).
     *
     * Applies the direction → alignment → distance ordering described at the
     * top of this file. Returns null when nothing lies in that direction,
     * which the caller should treat as "consume the key and stay put" rather
     * than "let focus escape somewhere unrelated".
     */
    fun resolveDirectional(
        current: FocusBounds,
        candidates: List<FocusBounds>,
        direction: SpatialDirection
    ): Int? = rankDirectional(current, candidates, direction).firstOrNull()

    /** Every candidate in [direction], ranked best-first. See [rankByNearest]. */
    fun rankDirectional(
        current: FocusBounds,
        candidates: List<FocusBounds>,
        direction: SpatialDirection
    ): List<Int> {
        val inDirection = candidates.filter {
            it.index != current.index && isInDirection(current, it, direction)
        }
        if (inDirection.isEmpty()) return emptyList()

        val isVertical = direction == SpatialDirection.Up || direction == SpatialDirection.Down

        return inDirection.sortedWith(
            compareBy(
                // 1. Alignment on the cross axis, quantised into tolerance
                //    bands so "close enough to be the same column" really is
                //    treated as the same column.
                { candidate ->
                    val misalignment = if (isVertical) {
                        crossAxisMisalignment(
                            current.left, current.right, current.centerX,
                            candidate.left, candidate.right, candidate.centerX
                        )
                    } else {
                        crossAxisMisalignment(
                            current.top, current.bottom, current.centerY,
                            candidate.top, candidate.bottom, candidate.centerY
                        )
                    }
                    (misalignment / ALIGNMENT_TOLERANCE_PX).toInt()
                },
                // 2. Travel along the pressed axis — the nearest row/column in
                //    that direction, never two rows away.
                { candidate -> primaryAxisDistance(current, candidate, direction) },
                // 3. Deterministic final tie-break.
                { candidate -> candidate.index }
            )
        ).map { it.index }
    }

    /**
     * True when [candidate] lies far enough past [current]'s trailing edge in
     * [direction] to be a legitimate destination.
     *
     * Uses the *centre* of the candidate against the *edge* of the current
     * item. Edge-to-edge would reject the item directly below in a grid whose
     * rows have a negative gap from shadow headroom; centre-to-edge is stable
     * against that.
     */
    private fun isInDirection(
        current: FocusBounds,
        candidate: FocusBounds,
        direction: SpatialDirection
    ): Boolean = when (direction) {
        SpatialDirection.Left -> candidate.centerX < current.left + DIRECTION_THRESHOLD_PX
        SpatialDirection.Right -> candidate.centerX > current.right - DIRECTION_THRESHOLD_PX
        SpatialDirection.Up -> candidate.centerY < current.top + DIRECTION_THRESHOLD_PX
        SpatialDirection.Down -> candidate.centerY > current.bottom - DIRECTION_THRESHOLD_PX
    }

    /**
     * Distance travelled along the pressed axis, measured edge to edge so a
     * tall neighbour is not penalised for its own height.
     */
    private fun primaryAxisDistance(
        current: FocusBounds,
        candidate: FocusBounds,
        direction: SpatialDirection
    ): Float = when (direction) {
        SpatialDirection.Left -> current.left - candidate.right
        SpatialDirection.Right -> candidate.left - current.right
        SpatialDirection.Up -> current.top - candidate.bottom
        SpatialDirection.Down -> candidate.top - current.bottom
    }.coerceAtLeast(0f)

    /**
     * Cross-axis misalignment between two spans.
     *
     * Zero while the spans overlap at all — two cards in the same column are
     * equally "in the column" whether or not their centres match, which is
     * what keeps a landscape → portrait transition from feeling arbitrary.
     * Beyond overlap it grows with the gap, so column 5 always beats column 6.
     */
    private fun crossAxisMisalignment(
        currentStart: Float,
        currentEnd: Float,
        currentCenter: Float,
        candidateStart: Float,
        candidateEnd: Float,
        candidateCenter: Float
    ): Float {
        val overlap = minOf(currentEnd, candidateEnd) - maxOf(currentStart, candidateStart)
        if (overlap > 0f) {
            // Overlapping: rank by how far the centres are apart, but scaled
            // down so any overlap still beats any non-overlap.
            return abs(candidateCenter - currentCenter) * 0.25f
        }
        return -overlap + abs(candidateCenter - currentCenter)
    }
}

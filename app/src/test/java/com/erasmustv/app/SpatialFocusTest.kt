package com.erasmustv.app

import com.erasmustv.app.ui.focus.FocusBounds
import com.erasmustv.app.ui.focus.FocusSpan
import com.erasmustv.app.ui.focus.FocusZoneMemory
import com.erasmustv.app.ui.focus.SpatialDirection
import com.erasmustv.app.ui.focus.SpatialFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the geometry layer of the D-pad focus engine.
 *
 * These exist because "pressing DOWN from card 3 lands on the card below card 3"
 * is a claim about arithmetic, and arithmetic can be checked without a
 * television. Everything asserted here is a rule from the Erasmus focus spec
 * restated as numbers.
 *
 * Layout conventions used throughout, in window pixels at density 2.0:
 *  - poster card: 276px wide (138dp), 32px gap (16dp), rail starts at 128px
 *  - landscape card: 400px wide (200dp), same gap and start
 */
class SpatialFocusTest {

    // ── Helpers ──────────────────────────────────────────────────────────

    /** A rail of [count] cards, each [width] wide with [gap] between them. */
    private fun rail(
        count: Int,
        width: Float,
        gap: Float = 32f,
        start: Float = 128f,
        firstIndex: Int = 0
    ): List<FocusSpan> = List(count) { offset ->
        FocusSpan(
            index = firstIndex + offset,
            start = start + offset * (width + gap),
            size = width
        )
    }

    private fun posterRail(count: Int, firstIndex: Int = 0) =
        rail(count, width = 276f, firstIndex = firstIndex)

    private fun landscapeRail(count: Int) = rail(count, width = 400f)

    /** A grid of [columns] x [rows] cells, row-major indices. */
    private fun grid(
        columns: Int,
        rows: Int,
        cellWidth: Float = 276f,
        cellHeight: Float = 414f,
        gapX: Float = 40f,
        gapY: Float = 56f,
        left: Float = 128f,
        top: Float = 200f,
        totalItems: Int = columns * rows
    ): List<FocusBounds> = buildList {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val index = row * columns + column
                if (index >= totalItems) return@buildList
                add(
                    FocusBounds(
                        index = index,
                        left = left + column * (cellWidth + gapX),
                        top = top + row * (cellHeight + gapY),
                        width = cellWidth,
                        height = cellHeight
                    )
                )
            }
        }
    }

    // ── nearestIndex: the travelling column ──────────────────────────────

    @Test
    fun `no travelling column lands on the first visible card`() {
        // First entry to a screen: there is no column to preserve, so the
        // leftmost visible card is the intentional target.
        assertEquals(0, SpatialFocus.nearestIndex(posterRail(6), null))
    }

    @Test
    fun `travelling column picks the card directly beneath it`() {
        val cards = posterRail(6)
        // Column sitting on card 3's centre.
        val column = cards[3].center
        assertEquals(3, SpatialFocus.nearestIndex(cards, column))
    }

    @Test
    fun `column inside a card wins over a marginally closer centre`() {
        val cards = posterRail(6)
        // Just inside card 2's right edge. Card 3's centre is further away, but
        // the point is literally over card 2, which is what the user sees.
        val column = cards[2].end - 4f
        assertEquals(2, SpatialFocus.nearestIndex(cards, column))
    }

    @Test
    fun `column in the gap between cards resolves to the nearer card`() {
        val cards = posterRail(6)
        val gapNearCard2 = cards[2].end + 6f
        assertEquals(2, SpatialFocus.nearestIndex(cards, gapNearCard2))

        val gapNearCard3 = cards[3].start - 6f
        assertEquals(3, SpatialFocus.nearestIndex(cards, gapNearCard3))
    }

    @Test
    fun `landscape card 4 moves down onto the spatially correct poster not poster 0`() {
        // This is the Continue Watching → Trending transition from the spec:
        // landscape cards are 400px, posters 276px, so index arithmetic cannot
        // work and the old implementation always landed on poster 0.
        val landscape = landscapeRail(5)
        val posters = posterRail(8)

        val column = landscape[3].center // 4th landscape card, centre 1624px
        val landed = SpatialFocus.nearestIndex(posters, column)

        // Poster 4 spans [1360, 1636) at these metrics, so it is the card the
        // 4th landscape card's centre genuinely sits over.
        assertEquals(
            "DOWN from landscape card 4 must not collapse to poster 0",
            4,
            landed
        )
        // Sanity: the chosen poster really does straddle the column.
        val chosen = posters[4]
        assertTrue(column >= chosen.start && column < chosen.end)
    }

    @Test
    fun `column survives a rail that is scrolled so index 0 is off screen`() {
        // A rail scrolled right: only indices 4..9 are composed, and index 4
        // starts at the left gutter. A remembered *index* would be meaningless
        // here; a remembered *position* still resolves correctly.
        val scrolled = posterRail(6, firstIndex = 4)
        val column = scrolled[2].center // index 6
        assertEquals(6, SpatialFocus.nearestIndex(scrolled, column))
    }

    @Test
    fun `column far to the right of a short rail clamps to its last card`() {
        // Trending has 20 cards, the next rail only 2. The column must degrade
        // to the last real card rather than resolving to nothing.
        val shortRail = posterRail(2)
        val farRightColumn = 1700f
        assertEquals(1, SpatialFocus.nearestIndex(shortRail, farRightColumn))
    }

    @Test
    fun `single item rail always accepts the column`() {
        assertEquals(0, SpatialFocus.nearestIndex(posterRail(1), 1500f))
    }

    @Test
    fun `empty rail yields no target`() {
        assertNull(SpatialFocus.nearestIndex(emptyList(), 400f))
        assertNull(SpatialFocus.nearestIndex(emptyList(), null))
    }

    // ── resolveDirectional: grids ────────────────────────────────────────

    @Test
    fun `grid preserves column on vertical movement`() {
        // The spec's 3x3 example: from cell 5, UP is 2 and DOWN is 8.
        val cells = grid(columns = 3, rows = 3)
        val from = cells[4] // cell index 4 == "5" in 1-based spec numbering

        assertEquals(1, SpatialFocus.resolveDirectional(from, cells, SpatialDirection.Up))
        assertEquals(7, SpatialFocus.resolveDirectional(from, cells, SpatialDirection.Down))
        assertEquals(3, SpatialFocus.resolveDirectional(from, cells, SpatialDirection.Left))
        assertEquals(5, SpatialFocus.resolveDirectional(from, cells, SpatialDirection.Right))
    }

    @Test
    fun `grid never skips a row on vertical movement`() {
        val cells = grid(columns = 4, rows = 4)
        val from = cells[1] // row 0, column 1
        // Must land on row 1 column 1 (index 5), not row 2 column 1 (index 9).
        assertEquals(5, SpatialFocus.resolveDirectional(from, cells, SpatialDirection.Down))
    }

    @Test
    fun `grid left edge has no left target`() {
        val cells = grid(columns = 3, rows = 3)
        assertNull(SpatialFocus.resolveDirectional(cells[3], cells, SpatialDirection.Left))
    }

    @Test
    fun `grid right edge has no right target`() {
        val cells = grid(columns = 3, rows = 3)
        // Index 5 is the last cell of row 1. RIGHT must not wrap to index 6.
        assertNull(SpatialFocus.resolveDirectional(cells[5], cells, SpatialDirection.Right))
    }

    @Test
    fun `grid top row has no up target`() {
        val cells = grid(columns = 3, rows = 3)
        assertNull(SpatialFocus.resolveDirectional(cells[1], cells, SpatialDirection.Up))
    }

    @Test
    fun `ragged final row accepts a column that does not exist in it`() {
        // 5 columns, 7 items: final row holds indices 5 and 6 only. DOWN from
        // column 4 has nothing directly beneath it and must still land on the
        // nearest real cell rather than nowhere.
        val cells = grid(columns = 5, rows = 2, totalItems = 7)
        val from = cells.first { it.index == 4 }
        val landed = SpatialFocus.resolveDirectional(from, cells, SpatialDirection.Down)
        assertEquals(6, landed)
    }

    @Test
    fun `ragged final row still prefers the aligned cell when one exists`() {
        val cells = grid(columns = 5, rows = 2, totalItems = 8)
        val from = cells.first { it.index == 2 }
        assertEquals(7, SpatialFocus.resolveDirectional(from, cells, SpatialDirection.Down))
    }

    @Test
    fun `direction beats proximity so focus never jumps diagonally`() {
        // Two candidates: A is much nearer in absolute distance but sits to the
        // side; B is directly below. DOWN must choose B.
        val current = FocusBounds(index = 0, left = 600f, top = 200f, width = 276f, height = 414f)
        val sideways = FocusBounds(index = 1, left = 100f, top = 640f, width = 276f, height = 414f)
        val directlyBelow = FocusBounds(index = 2, left = 610f, top = 700f, width = 276f, height = 414f)

        val landed = SpatialFocus.resolveDirectional(
            current,
            listOf(current, sideways, directlyBelow),
            SpatialDirection.Down
        )
        assertEquals(2, landed)
    }

    @Test
    fun `a candidate merely overlapping the current edge is not a target`() {
        // A cell that starts 2px past the current cell's right edge is not
        // "to the right" in any meaningful sense; treating it as such is how
        // focus ends up somewhere the viewer did not point.
        val current = FocusBounds(index = 0, left = 100f, top = 0f, width = 200f, height = 200f)
        val barelyPast = FocusBounds(index = 1, left = 150f, top = 0f, width = 200f, height = 200f)
        assertNull(
            SpatialFocus.resolveDirectional(current, listOf(current, barelyPast), SpatialDirection.Right)
        )
    }

    @Test
    fun `resolution is deterministic for equidistant candidates`() {
        val current = FocusBounds(index = 0, left = 400f, top = 0f, width = 200f, height = 200f)
        val left = FocusBounds(index = 1, left = 300f, top = 300f, width = 200f, height = 200f)
        val right = FocusBounds(index = 2, left = 500f, top = 300f, width = 200f, height = 200f)

        val first = SpatialFocus.resolveDirectional(current, listOf(current, left, right), SpatialDirection.Down)
        val second = SpatialFocus.resolveDirectional(current, listOf(current, right, left), SpatialDirection.Down)
        assertEquals("candidate order must not change the outcome", first, second)
    }

    @Test
    fun `single cell grid has no target in any direction`() {
        val only = listOf(FocusBounds(0, 100f, 100f, 276f, 414f))
        SpatialDirection.entries.forEach { direction ->
            assertNull(
                "expected no target for $direction",
                SpatialFocus.resolveDirectional(only[0], only, direction)
            )
        }
    }

    // ── FocusZoneMemory ──────────────────────────────────────────────────

    @Test
    fun `memory keeps a separate index per zone`() {
        // The bug this replaces: a single slot meant focusing a card in rail B
        // erased rail A's index, so returning to A always landed on card 0.
        val memory = FocusZoneMemory(emptyMap(), null, -1f)
        memory.record("trending", 7, 900f)
        memory.record("top10", 2, 400f)

        assertEquals(7, memory.indexFor("trending"))
        assertEquals(2, memory.indexFor("top10"))
        assertTrue(memory.isLastZone("top10"))
        assertFalse(memory.isLastZone("trending"))
    }

    @Test
    fun `memory carries the travelling column across zones`() {
        val memory = FocusZoneMemory(emptyMap(), null, -1f)
        assertNull("no column before any focus", memory.columnOrNull())

        memory.record("continueWatching", 3, 1240f)
        assertEquals(1240f, memory.columnOrNull()!!, 0.01f)

        memory.record("trending", 5, 1230f)
        assertEquals(1230f, memory.columnOrNull()!!, 0.01f)
    }

    @Test
    fun `an unmeasurable item does not destroy the travelling column`() {
        val memory = FocusZoneMemory(emptyMap(), null, -1f)
        memory.record("trending", 5, 1230f)
        // A zone that could not report a centre (off-screen at focus time) must
        // leave the column the user built up intact.
        memory.record("hero", 0, null)
        assertEquals(1230f, memory.columnOrNull()!!, 0.01f)
    }

    @Test
    fun `memory survives the save and restore round trip`() {
        val memory = FocusZoneMemory(emptyMap(), null, -1f)
        memory.record("trending", 7, 900f)
        memory.record("top10", 2, 420f)

        @Suppress("UNCHECKED_CAST")
        val saved = buildList<Any?> {
            add(memory.columnCenterPx)
            add(memory.lastZoneKey)
            add("trending"); add(7)
            add("top10"); add(2)
        }

        // Mirrors FocusZoneMemory.Saver.restore.
        val column = saved[0] as Float
        val lastZone = saved[1] as String?
        val restored = FocusZoneMemory(
            mapOf("trending" to 7, "top10" to 2),
            lastZone,
            column
        )

        assertEquals(7, restored.indexFor("trending"))
        assertEquals(2, restored.indexFor("top10"))
        assertEquals("top10", restored.lastZoneKey)
        assertEquals(420f, restored.columnOrNull()!!, 0.01f)
        assertTrue(restored.hasTarget)
    }

    @Test
    fun `resetting zones keeps the column but clears stale indices`() {
        val memory = FocusZoneMemory(emptyMap(), null, -1f)
        memory.record("results", 12, 800f)
        memory.resetZones()

        assertNull(memory.indexFor("results"))
        assertNull(memory.lastZoneKey)
        assertFalse(memory.hasTarget)
        assertEquals("column is a visual intent, not content", 800f, memory.columnOrNull()!!, 0.01f)
    }
}

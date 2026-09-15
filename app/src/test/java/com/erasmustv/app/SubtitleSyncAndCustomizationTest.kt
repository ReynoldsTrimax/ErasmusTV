package com.erasmustv.app

import com.erasmustv.app.data.local.SubtitleFont
import com.erasmustv.app.data.local.SubtitleSize
import com.erasmustv.app.data.subtitle.SubtitleCue
import com.erasmustv.app.data.subtitle.SubtitleParser
import com.erasmustv.app.data.subtitle.SubtitleSyncEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleSyncAndCustomizationTest {

    @Test
    fun testSrtParsingAndTagSanitization() {
        val srtContent = """
            1
            00:01:20,000 --> 00:01:23,500
            Hello, <i>world</i>! <b>Welcome</b> to ErasmusTV.

            2
            00:01:25,120 --> 00:01:28,950
            <font color="#ffff00">Enjoy the show.</font>
        """.trimIndent()

        val cues = SubtitleParser.parse(srtContent)
        assertEquals(2, cues.size)

        assertEquals(80_000L, cues[0].startMs)
        assertEquals(83_500L, cues[0].endMs)
        assertEquals("Hello, world! Welcome to ErasmusTV.", cues[0].text)

        assertEquals(85_120L, cues[1].startMs)
        assertEquals(88_950L, cues[1].endMs)
        assertEquals("Enjoy the show.", cues[1].text)
    }

    @Test
    fun testVttParsingWithMillis() {
        val vttContent = """
            WEBVTT

            00:00:10.500 --> 00:00:14.200
            This is a WebVTT caption.

            00:00:15.000 --> 00:00:18.000
            Line 1
            Line 2
        """.trimIndent()

        val cues = SubtitleParser.parse(vttContent)
        assertEquals(2, cues.size)

        assertEquals(10_500L, cues[0].startMs)
        assertEquals(14_200L, cues[0].endMs)
        assertEquals("This is a WebVTT caption.", cues[0].text)

        assertEquals("Line 1\nLine 2", cues[1].text)
    }

    @Test
    fun testCueActiveWithOffset() {
        val cue = SubtitleCue(
            index = 1,
            startMs = 10_000L,
            endMs = 15_000L,
            text = "Synchronized text"
        )

        // Without offset
        assertTrue(cue.isActiveAt(12_000L, 0L))
        assertFalse(cue.isActiveAt(9_999L, 0L))
        assertFalse(cue.isActiveAt(15_001L, 0L))

        // With -2000ms offset (subtitles were ahead by 2s, effectivePos = position - offset)
        // position 8000ms - (-2000ms) = 10000ms -> active!
        assertTrue(cue.isActiveAt(8_000L, -2_000L))
        assertFalse(cue.isActiveAt(7_500L, -2_000L))
    }

    @Test
    fun testAutoSyncCrossCorrelationEngine() {
        // Build primary cues delayed by 1500ms
        val shiftedCues = listOf(
            SubtitleCue(1, 11_500L, 14_500L, "First dialogue"),
            SubtitleCue(2, 21_500L, 24_500L, "Second dialogue"),
            SubtitleCue(3, 31_500L, 35_500L, "Third dialogue")
        )

        // Build reference cues at nominal ground-truth timing
        val referenceCues = listOf(
            SubtitleCue(1, 10_000L, 13_000L, "First dialogue"),
            SubtitleCue(2, 20_000L, 23_000L, "Second dialogue"),
            SubtitleCue(3, 30_000L, 34_000L, "Third dialogue")
        )

        val engine = SubtitleSyncEngine(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined))
        val detectedOffset = engine.computeOffsetAgainstReference(
            targetCues = shiftedCues,
            referenceCues = referenceCues,
            searchWindowMs = 5000L
        )

        // Because target was shifted by +1500ms, best delta to align target with reference is -1500ms
        assertEquals(-1500L, detectedOffset)
    }

    @Test
    fun testSubtitleCustomizationEnums() {
        // 5 font options requirement
        val fonts = SubtitleFont.values()
        assertEquals(5, fonts.size)
        assertTrue(fonts.any { it == SubtitleFont.SANS_SERIF })
        assertTrue(fonts.any { it == SubtitleFont.SERIF })
        assertTrue(fonts.any { it == SubtitleFont.MONOSPACE })
        assertTrue(fonts.any { it == SubtitleFont.CASUAL })
        assertTrue(fonts.any { it == SubtitleFont.BOSTONE })

        // 3 size options requirement
        val sizes = SubtitleSize.values()
        assertEquals(3, sizes.size)
        assertEquals(20f, SubtitleSize.SMALL.spSize)
        assertEquals(26f, SubtitleSize.MEDIUM.spSize)
        assertEquals(32f, SubtitleSize.LARGE.spSize)
    }
}

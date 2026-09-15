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

    @Test
    fun testAssParsingWithJujutsuKaisenFormat() {
        val assContent = """
            [Script Info]
            Title: [Erai-raws] Jujutsu Kaisen - 01
            ScriptType: v4.00+

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, Strikeout, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Main,Trebuchet MS,24,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,1,2,0010,0010,0018,1

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:02.07,0:00:03.54,Main,Gojou,0000,0000,0000,,Morning.
            Dialogue: 0,0:00:21.61,0:00:24.12,Main,Gojou,0000,0000,0000,,So which one are you right now?
            Dialogue: 0,0:00:29.60,0:00:32.09,Main,Gojou,0000,0000,0000,,I'm in charge of the \Nfirst-years at Jujutsu Tech.
            Dialogue: 0,0:01:05.00,0:01:11.96,Show_Title,,0000,0000,0000,,{\blur1\fad(1191,431)\t(1192,1316,1 \c&H0700D7&\3c&H6659E4&)}Jujutsu Kaisen
        """.trimIndent()

        val cues = SubtitleParser.parse(assContent)
        assertEquals(4, cues.size)

        // Cue 1: 0:00:02.07 = 2070ms -> 3540ms
        assertEquals(2070L, cues[0].startMs)
        assertEquals(3540L, cues[0].endMs)
        assertEquals("Morning.", cues[0].text)

        // Cue 2: 21610ms -> 24120ms
        assertEquals(21610L, cues[1].startMs)
        assertEquals(24120L, cues[1].endMs)
        assertEquals("So which one are you right now?", cues[1].text)

        // Cue 3: Line breaks \N normalized to \n
        assertEquals(29600L, cues[2].startMs)
        assertEquals(32090L, cues[2].endMs)
        assertEquals("I'm in charge of the \nfirst-years at Jujutsu Tech.", cues[2].text)

        // Cue 4: Complex style override tags stripped
        assertEquals(65000L, cues[3].startMs)
        assertEquals(71960L, cues[3].endMs)
        assertEquals("Jujutsu Kaisen", cues[3].text)
    }

    @Test
    fun testWebVttWithoutHoursAndWithCueSettings() {
        val vttContent = """
            WEBVTT

            STYLE
            ::cue {
              color: yellow;
            }

            00:10.500 --> 00:14.200 position:10% align:start
            <v Satoru>Short timestamp with cue settings</v>

            01:15.000 --> 01:18.000
            Second line without hours
        """.trimIndent()

        val cues = SubtitleParser.parse(vttContent)
        assertEquals(2, cues.size)

        assertEquals(10_500L, cues[0].startMs)
        assertEquals(14_200L, cues[0].endMs)
        assertEquals("Short timestamp with cue settings", cues[0].text)

        assertEquals(75_000L, cues[1].startMs)
        assertEquals(78_000L, cues[1].endMs)
        assertEquals("Second line without hours", cues[1].text)
    }

    @Test
    fun testErrorResponseReturnsEmptyCuesWithoutCrashing() {
        val htmlError = """
            <!DOCTYPE html>
            <html><body><h1>502 Bad Gateway</h1></body></html>
        """.trimIndent()

        val dbError = "Sorry. We have problem with network connection to database server, try reload page.<!-- Not connected"

        assertTrue(SubtitleParser.parse(htmlError).isEmpty())
        assertTrue(SubtitleParser.parse(dbError).isEmpty())
        assertTrue(SubtitleParser.parse("").isEmpty())
    }
}

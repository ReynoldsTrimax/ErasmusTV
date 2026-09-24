package com.erasmustv.app

import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.TvSpring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the motion numbers to the spring model they were derived from.
 *
 * Compose takes (dampingRatio, stiffness); the design system is specified in
 * (damping ratio, response). These assertions pin the conversion and the
 * house rule that only momentum-carrying motion is allowed to overshoot.
 */
class MotionSpringTest {

    /** stiffness = ω², ω = 2π / response (unit mass). */
    @Test
    fun responseConvertsToStiffness() {
        assertEquals(631.65f, TvMotion.stiffnessFor(TvMotion.RESPONSE_SNAPPY), 0.5f)
        assertEquals(438.65f, TvMotion.stiffnessFor(TvMotion.RESPONSE_STANDARD), 0.5f)
        assertEquals(246.74f, TvMotion.stiffnessFor(TvMotion.RESPONSE_RELAXED), 0.5f)
    }

    /** A shorter response must always be a stiffer spring, never the reverse. */
    @Test
    fun shorterResponseIsStiffer() {
        assertTrue(
            TvSpring.FocusFast.stiffness > TvSpring.Focus.stiffness &&
                TvSpring.Focus.stiffness > TvSpring.Reposition.stiffness
        )
    }

    /** Focus feedback is the fastest thing in the app. */
    @Test
    fun focusRespondsWithinAQuarterSecond() {
        assertEquals(0.25f, TvSpring.FocusFast.response, 0.001f)
        assertEquals(0.30f, TvSpring.Focus.response, 0.001f)
    }

    /** Overshoot is reserved for motion the user gave momentum to. */
    @Test
    fun onlyMomentumProfilesOvershoot() {
        val overshoots = TvSpring.entries.filter { it.damping < 1f }
        assertEquals(listOf(TvSpring.Sheet, TvSpring.Momentum), overshoots)
        overshoots.forEach { assertEquals(0.8f, it.damping, 0.001f) }
        TvSpring.entries.forEach { assertTrue(it.damping in 0.8f..1.0f) }
    }

    /** Every profile sits inside the 0.25–0.40s response band. */
    @Test
    fun responsesStayInsideTheBand() {
        TvSpring.entries.forEach {
            assertTrue("${it.name} response ${it.response}", it.response in 0.25f..0.40f)
        }
    }
}

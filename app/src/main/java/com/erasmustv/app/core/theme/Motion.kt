package com.erasmustv.app.core.theme

import android.provider.Settings
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * Android TV Motion System & Visual Depth Specifications.
 *
 * Principles:
 *  - Purposeful & Restrained: Motion serves affordance, focus feedback, and spatial depth.
 *  - 10-Foot Legibility: Focus scale (1.04f) and depth elevation (6-8dp) are calibrated for TV distances.
 *  - High Performance: All focus & shimmer transitions run on GPU compositor layers (graphicsLayer).
 *  - Accessibility: Full honor of system reduced-motion and animator duration scale settings.
 */
object TvMotion {
    // ── Durations ─────────────────────────────────────────────────────────
    // Durations are for *non-spatial* transitions only: opacity crossfades,
    // colour shifts, and the resting shimmer. Anything that moves, lifts, or
    // resizes uses a spring (see [TvSpring]) so it can be interrupted and
    // re-targeted from wherever it currently is on screen.
    const val DURATION_FAST = 160
    const val DURATION_MEDIUM = 220
    const val DURATION_ENTER = 280
    const val DURATION_CAROUSEL = 350
    const val DURATION_SHIMMER = 1300

    /** Focus-driven opacity/colour response on a card or tile. */
    const val DURATION_FOCUS = 200

    /** Button / chip focus response — a touch quicker than a full card lift. */
    const val DURATION_FOCUS_FAST = 180

    /** Hero artwork crossfade when the featured title changes. */
    const val DURATION_HERO_CROSSFADE = 520

    /** Hero atmospheric colour transition (artwork-derived ambience). */
    const val DURATION_HERO_AMBIENT = 600

    val EasingSilk = FastOutSlowInEasing
    val EasingSnap = FastOutLinearInEasing

    // Focus scale tiers — restrained, never a "huge zoom." Prefer
    // FocusScaleSubtle for new/redesigned components; the remaining tokens
    // are kept for components already built against them.
    val FocusScaleSubtle = 1.025f
    val FocusScaleCard = 1.04f
    val FocusScaleButton = 1.05f
    val FocusScaleStudio = 1.045f
    val FocusScaleNav = 1.03f

    // ── Spring parameters (damping ratio + response) ───────────────────────
    // Two numbers describe every spatial motion in the app, per Apple's
    // designer-facing spring model:
    //
    //   damping ratio — 1.0 settles with no overshoot; below 1.0 overshoots.
    //   response      — seconds to reach the target. Not a duration: a spring
    //                   has no fixed end, settle time emerges from the pair.
    //
    // House rule: damping 1.0 everywhere by default. Bounce is reserved for
    // motion the *user* gave momentum to — a panel thrown in from an edge —
    // never for something that merely appeared.

    /** Critically damped: graceful settle, zero overshoot. */
    const val DAMPING_CRITICAL = 1.0f

    /** Slight overshoot, for momentum-carrying motion only. */
    const val DAMPING_MOMENTUM = 0.8f

    /** 0.25s — remote-input focus feedback, the most latency-sensitive path. */
    const val RESPONSE_SNAPPY = 0.25f

    /** 0.30s — standard UI response; card lifts, sheets. */
    const val RESPONSE_STANDARD = 0.3f

    /** 0.40s — larger repositions, where a slower arc reads as weight. */
    const val RESPONSE_RELAXED = 0.4f

    /**
     * Compose expresses a spring as (dampingRatio, stiffness) at unit mass,
     * so stiffness is the square of the natural frequency ω = 2π / response.
     *
     *   response 0.25s → ω 25.13 rad/s → stiffness ≈ 632
     *   response 0.30s → ω 20.94 rad/s → stiffness ≈ 439
     *   response 0.40s → ω 15.71 rad/s → stiffness ≈ 247
     */
    fun stiffnessFor(response: Float): Float {
        val omega = (2.0 * Math.PI / response).toFloat()
        return omega * omega
    }
}

/**
 * The app's spring vocabulary. Each entry is one (damping, response) pair,
 * chosen for a specific class of motion rather than tuned per call site.
 *
 * Springs — not tweens — because on TV the remote outruns the animation: a
 * viewer holding D-pad right retargets the focus animation every ~80ms. A
 * duration-based tween restarts from its own interpolation each time and
 * visibly stutters; a spring continues from the current value and velocity, so
 * fast traversal stays continuous.
 */
enum class TvSpring(val damping: Float, val response: Float) {
    /** Card and tile focus lift. */
    Focus(TvMotion.DAMPING_CRITICAL, TvMotion.RESPONSE_STANDARD),

    /** Button, chip, and nav-item focus — quickest thing in the app. */
    FocusFast(TvMotion.DAMPING_CRITICAL, TvMotion.RESPONSE_SNAPPY),

    /** Something moving or resizing in place: nav pill, timeline thumb. */
    Reposition(TvMotion.DAMPING_CRITICAL, TvMotion.RESPONSE_RELAXED),

    /** Panels and sheets arriving from an edge — carries a little momentum. */
    Sheet(TvMotion.DAMPING_MOMENTUM, TvMotion.RESPONSE_STANDARD),

    /** Motion the user threw: overshoots, then settles. */
    Momentum(TvMotion.DAMPING_MOMENTUM, TvMotion.RESPONSE_RELAXED);

    val stiffness: Float get() = TvMotion.stiffnessFor(response)
}

/** Generic spring spec for this profile. */
fun <T> TvSpring.spec(visibilityThreshold: T? = null): SpringSpec<T> =
    spring(dampingRatio = damping, stiffness = stiffness, visibilityThreshold = visibilityThreshold)

/**
 * Float spring (scale, elevation in px, progress).
 *
 * Under reduced motion the value snaps: the state change still lands, it just
 * doesn't travel.
 */
fun TvSpring.floatSpec(isReducedMotion: Boolean = false): FiniteAnimationSpec<Float> =
    if (isReducedMotion) snap() else spec(0.001f)

/** Dp spring, with a sub-pixel settle threshold so it doesn't creep. */
fun TvSpring.dpSpec(isReducedMotion: Boolean = false): FiniteAnimationSpec<Dp> =
    if (isReducedMotion) snap() else spec(Dp.VisibilityThreshold)

/** IntOffset spring, for slide enter/exit transitions. */
fun TvSpring.offsetSpec(isReducedMotion: Boolean = false): FiniteAnimationSpec<IntOffset> =
    if (isReducedMotion) snap() else spec(IntOffset.VisibilityThreshold)

/**
 * Detects whether the user or system has reduced motion / animator duration scale disabled.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            val resolver = context.contentResolver
            val durationScale = Settings.Global.getFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                resolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Placeholder fill for skeleton loading states.
 *
 * Deliberately *not* a travelling luminous shimmer: a bright sweep crossing
 * the screen is the most eye-catching thing in the room while the app is doing
 * nothing, which inverts the intended hierarchy. Instead this is a very
 * low-amplitude breathing tone — enough to read as "content is arriving",
 * quiet enough to ignore. Falls back to a static fill under reduced motion.
 */
@Composable
fun rememberSkeletonBrush(isReducedMotion: Boolean = rememberReducedMotion()): Brush {
    if (isReducedMotion) {
        return remember {
            Brush.verticalGradient(colors = listOf(SurfaceCard, SurfaceCard))
        }
    }

    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonPulseProgress"
    )

    // Interpolates between two adjacent near-black surface tones only, so the
    // pulse never brightens beyond a resting card.
    val tone = lerp(SurfaceCard, SurfaceElevated, progress)
    return Brush.verticalGradient(colors = listOf(tone, tone))
}

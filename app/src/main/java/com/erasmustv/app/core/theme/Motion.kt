package com.erasmustv.app.core.theme

import android.provider.Settings
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    // Standard durations (Silk & Snap profiles)
    const val DURATION_FAST = 160
    const val DURATION_MEDIUM = 220
    const val DURATION_ENTER = 280
    const val DURATION_CAROUSEL = 350
    const val DURATION_SHIMMER = 1300

    val EasingSilk = FastOutSlowInEasing
    val EasingSnap = FastOutLinearInEasing

    val FocusScaleCard = 1.04f
    val FocusScaleButton = 1.05f
    val FocusScaleStudio = 1.045f
    val FocusScaleNav = 1.03f
}

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
 * Creates a luminous diagonal shimmer brush that travels across dark surfaces.
 * Respects reduced-motion settings by falling back to a static layered gradient.
 */
@Composable
fun rememberShimmerBrush(isReducedMotion: Boolean = rememberReducedMotion()): Brush {
    if (isReducedMotion) {
        return remember {
            Brush.linearGradient(
                colors = listOf(
                    SurfaceCard,
                    SurfaceElevated,
                    SurfaceCard
                )
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TvMotion.DURATION_SHIMMER, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(
            SurfaceCard,
            SurfaceElevated,
            Color(0xFF282834),
            Color(0xFF323242),
            SurfaceElevated,
            SurfaceCard
        ),
        start = Offset(translateAnim - 700f, translateAnim - 700f),
        end = Offset(translateAnim, translateAnim)
    )
}

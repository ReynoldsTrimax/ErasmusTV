package com.erasmustv.app.ui.screens.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.rememberReducedMotion

/**
 * Android TV Pre-Playback Loading Joke Text with luminous left-to-right text shimmer sweep.
 *
 * Characteristics:
 * - Centered, moderate typography (~16sp, 24sp line height).
 * - Letters are completely stationary.
 * - A glint wave sweeps smoothly from left to right every 2500ms.
 * - Base text is muted white (~48% alpha against PitchBlack).
 * - Peak shimmer glint lights up to luminous white (~96% alpha).
 * - Respects system reduced-motion settings by falling back to static muted white text.
 */
@Composable
fun TvShimmerLoadingJokeText(
    jokeText: String,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = rememberReducedMotion()
    var textWidthPx by remember { mutableFloatStateOf(0f) }

    val transition = rememberInfiniteTransition(label = "TvJokeShimmerTransition")
    val sweepProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TvJokeShimmerProgress"
    )

    // Compute animated linear gradient for stationary text
    val currentWidth = if (textWidthPx > 0f) textWidthPx else 1000f
    val bandWidth = maxOf(300f, currentWidth * 0.35f)
    val center = -bandWidth + sweepProgress * (currentWidth + 2f * bandWidth)
    val startX = center - bandWidth
    val endX = center + bandWidth

    val baseColor = Color(0x7AFFFFFF)   // ~48% muted white
    val peakColor = Color(0xF5FFFFFF)   // ~96% bright luminous glint

    val shimmerBrush = remember(isReducedMotion, startX, endX) {
        if (isReducedMotion) {
            Brush.linearGradient(listOf(baseColor, baseColor))
        } else {
            Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to baseColor,
                    0.35f to baseColor,
                    0.50f to peakColor,
                    0.65f to baseColor,
                    1.0f to baseColor
                ),
                start = Offset(startX, 0f),
                end = Offset(endX, 0f),
                tileMode = TileMode.Clamp
            )
        }
    }

    Text(
        text = jokeText,
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            brush = shimmerBrush
        ),
        textAlign = TextAlign.Center,
        modifier = modifier
            .onSizeChanged { size ->
                if (size.width > 0) {
                    textWidthPx = size.width.toFloat()
                }
            }
            .semantics {
                contentDescription = jokeText
            }
    )
}

/**
 * Fullscreen container variant for the shimmering loading joke.
 */
@Composable
fun TvShimmerLoadingJoke(
    jokeText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack),
        contentAlignment = Alignment.Center
    ) {
        TvShimmerLoadingJokeText(
            jokeText = jokeText,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .widthIn(max = 840.dp)
                .padding(horizontal = 32.dp, vertical = 24.dp)
        )
    }
}

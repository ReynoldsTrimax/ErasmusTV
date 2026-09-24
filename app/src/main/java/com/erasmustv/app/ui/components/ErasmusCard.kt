package com.erasmustv.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.RatingGold
import com.erasmustv.app.core.theme.SurfaceCardFocused
import com.erasmustv.app.core.theme.SurfaceCardRest
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.rememberReducedMotion
import com.erasmustv.app.data.model.MediaItem

/**
 * ERASMUS CARD SYSTEM — shared artwork surface.
 *
 * Every card in the application (poster, Continue Watching, Top 10, grid cell,
 * episode still) renders its artwork through this one composable, so corner
 * radius, resting fill, hairline edge, and focus clarity behave identically
 * everywhere. Card *shape* differs only in aspect ratio and what metadata sits
 * around it — never in visual language.
 *
 * Design intent: the artwork should read as the card itself, not as an image
 * dropped inside a container. That means the fill and hairline sit behind and
 * flush with the image, and there is no visible frame, inset, or padding ring.
 *
 * Focus is expressed here as a *clarity* change: unfocused artwork carries a
 * faint darkening veil which lifts on focus. Combined with the scale and
 * elevation from [TvFocusableCard], the focused card appears to step forward
 * into the light rather than switching on a border effect.
 */
@Composable
fun ErasmusCardArtwork(
    model: Any?,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = ErasmusShapes.Card,
    contentScale: ContentScale = ContentScale.Crop,
    /** Faint veil over resting artwork; 0f disables the clarity shift. */
    restingVeilAlpha: Float = 0.14f,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val isReducedMotion = rememberReducedMotion()

    val focusSpec = remember(isReducedMotion) {
        tween<Float>(
            durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS,
            easing = TvMotion.EasingSilk
        )
    }

    // Kept as State objects and read inside the draw lambda below, so the focus
    // transition invalidates only drawing. Reading them in composition would
    // recompose every visible card on every animation frame.
    val veilAlpha = animateFloatAsState(
        targetValue = if (isFocused) 0f else restingVeilAlpha,
        animationSpec = focusSpec,
        label = "cardVeilAlpha"
    )
    val edgeAlpha = animateFloatAsState(
        targetValue = if (isFocused) 0f else 1f,
        animationSpec = focusSpec,
        label = "cardEdgeAlpha"
    )

    val density = LocalDensity.current
    val hairlinePx = remember(density) { with(density) { 1.dp.toPx() } }
    val outline = remember(shape) { shape }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isFocused) SurfaceCardFocused else SurfaceCardRest)
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()

                    // 1. Clarity veil — lifts as the card takes focus.
                    val veil = veilAlpha.value
                    if (veil > 0.001f) {
                        drawRect(color = Color.Black.copy(alpha = veil))
                    }

                    // 2. Hairline edge so dark artwork does not dissolve into the
                    // canvas. It fades out on focus, leaving the focus outline as
                    // the only edge treatment — never two stacked borders.
                    val edge = edgeAlpha.value
                    if (edge > 0.001f) {
                        val stroke = Stroke(width = hairlinePx)
                        val path = outline.createOutline(size, layoutDirection, this)
                        drawOutline(
                            outline = path,
                            color = BorderHairline.copy(alpha = BorderHairline.alpha * edge),
                            style = stroke
                        )
                    }
                }
        )

        overlay()
    }
}

/**
 * Bottom-anchored readability gradient for artwork that must carry text or a
 * progress indicator (Continue Watching, episode stills). Deliberately short
 * and weighted to the very bottom so it darkens only what it needs to.
 */
@Composable
fun BoxScope.ErasmusCardBottomScrim(
    modifier: Modifier = Modifier,
    strength: Float = 0.78f
) {
    Box(
        modifier = modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.52f to Color.Transparent,
                        0.74f to Color.Black.copy(alpha = strength * 0.34f),
                        0.88f to Color.Black.copy(alpha = strength * 0.68f),
                        1.00f to Color.Black.copy(alpha = strength)
                    )
                )
            )
    )
}

/**
 * The standard card title line. One line, ellipsised — a card title that wraps
 * makes a rail's baselines ragged and is unreadable at viewing distance anyway.
 */
@Composable
fun ErasmusCardTitle(
    title: String,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false
) {
    Text(
        text = title,
        style = ErasmusTvTypography.CardTitle,
        color = if (isFocused) TextPrimary else TextPrimary.copy(alpha = 0.92f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Muted single-line metadata beneath a card title: `★ 7.4 · 2024 · Series`.
 *
 * Kept deliberately quiet — metadata exists to disambiguate two similar
 * posters, not to form a second content block competing with the artwork.
 */
@Composable
fun ErasmusCardMetadata(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val metaStyle = ErasmusTvTypography.CardMeta

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        val hasRating = item.voteAverage != null && item.voteAverage > 0
        if (hasRating) {
            Text(
                text = "★ ${item.ratingFormatted}",
                style = metaStyle,
                color = RatingGold.copy(alpha = 0.82f),
                maxLines = 1
            )
        }

        item.year?.let { year ->
            if (hasRating) {
                Text(text = " · ", style = metaStyle, color = TextMuted)
            }
            Text(
                text = year,
                style = metaStyle,
                color = TextSecondary,
                maxLines = 1
            )
        }

        if (hasRating || item.year != null) {
            Text(text = " · ", style = metaStyle, color = TextMuted)
        }

        Text(
            text = if (item.isTv) "Series" else "Movie",
            style = metaStyle,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Builds a Coil request for card artwork. Crossfade is short and only applied
 * on first decode; cached artwork appears immediately, which matters when a
 * rail is scrolled quickly with a D-pad.
 */
@Composable
fun rememberCardImageRequest(url: String?): ImageRequest {
    val context = LocalContext.current
    return remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(180)
            .build()
    }
}

package com.erasmustv.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.AccentNeutral
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.rememberReducedMotion
import com.erasmustv.app.data.model.ContinueWatchingItem
import com.erasmustv.app.ui.focus.FeedFocusCoordinator
import com.erasmustv.app.ui.focus.RailFocusHandle
import com.erasmustv.app.ui.focus.railFocusItem

/**
 * ERASMUS CARD TYPE 1 — the Continue Watching rail.
 *
 * This is the **only** landscape rail in the application. Its 16:9 shape is
 * load-bearing information design, not decoration: a wider card signals "you
 * are already inside this title" and visually separates resumption from
 * discovery, which every other (portrait) shelf represents.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingRow(
    items: List<ContinueWatchingItem>,
    onItemClick: (ContinueWatchingItem) -> Unit,
    handle: RailFocusHandle,
    coordinator: FeedFocusCoordinator,
    modifier: Modifier = Modifier,
    title: String = "Continue Watching",
    onLeftEdge: (() -> Boolean)? = null
) {
    if (items.isEmpty()) return

    ErasmusContentRail(
        title = title,
        handle = handle,
        itemCount = items.size,
        modifier = modifier
    ) {
        itemsIndexed(
            items = items,
            // Season and episode are part of the identity: resuming a different
            // episode of the same show is a different card, and focus must not
            // silently follow the old one.
            key = { _, it -> "${it.mediaType}:${it.tmdbId}:${it.season}:${it.episode}" }
        ) { index, item ->
            ContinueWatchingCard(
                item = item,
                onClick = { onItemClick(item) },
                cardModifier = Modifier.railFocusItem(
                    handle = handle,
                    coordinator = coordinator,
                    index = index,
                    isFirstItem = index == 0,
                    onLeftEdge = onLeftEdge
                )
            )
        }
    }
}

/**
 * The landscape resume card.
 *
 * Artwork fills the card edge to edge. A short bottom scrim exists purely so
 * the progress indicator stays legible over bright backdrops. Progress is a
 * thin warm-neutral fill on a dark track — visible at viewing distance,
 * without becoming a coloured bar that dominates the artwork.
 *
 * Public because Continue Watching content is also surfaced outside the Home
 * rail; reusing this composable is what guarantees a resume card never gets
 * re-implemented (and never drifts into portrait) elsewhere.
 */
@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    cardWidth: Dp = ErasmusDimens.LandscapeCardWidth
) {
    val artworkUrl = remember(item.backdropPath, item.posterPath) {
        if (!item.backdropPath.isNullOrBlank()) AppConfig.backdropUrl(item.backdropPath)
        else AppConfig.posterUrl(item.posterPath)
    }
    val imageRequest = rememberCardImageRequest(artworkUrl)
    val isReducedMotion = rememberReducedMotion()

    val episodeLabel = remember(item) {
        if (item.mediaType.equals("tv", ignoreCase = true) &&
            item.season != null && item.episode != null
        ) {
            "S${item.season} · E${item.episode}"
        } else null
    }

    val a11yDescription = remember(item, episodeLabel) {
        buildString {
            append("Resume ")
            append(item.title)
            episodeLabel?.let { append(", Season ${item.season} Episode ${item.episode}") }
            append(", ${item.resumeLabel}")
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = item.progressRatio,
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 0 else 600,
            easing = TvMotion.EasingSilk
        ),
        label = "cwProgressFill"
    )

    Column(modifier = modifier.width(cardWidth)) {
        TvFocusableCard(
            onClick = onClick,
            modifier = cardModifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = ErasmusShapes.CardLarge,
            focusedScale = TvMotion.FocusScaleCard,
            focusedBorderColor = FocusWhite,
            focusedBorderWidth = 1.5.dp,
            contentDescription = a11yDescription,
            role = Role.Button
        ) { isFocused ->
            val playAlpha by animateFloatAsState(
                targetValue = if (isFocused) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS,
                    easing = TvMotion.EasingSilk
                ),
                label = "cwPlayAlpha"
            )
            val playScale by animateFloatAsState(
                targetValue = if (isFocused) 1f else 0.86f,
                animationSpec = tween(
                    durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS,
                    easing = TvMotion.EasingSilk
                ),
                label = "cwPlayScale"
            )

            ErasmusCardArtwork(
                model = imageRequest,
                isFocused = isFocused,
                shape = ErasmusShapes.CardLarge,
                modifier = Modifier.fillMaxSize()
            ) {
                // Short scrim: only enough to keep the progress rail readable.
                ErasmusCardBottomScrim(strength = 0.62f)

                if (playAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(38.dp)
                            .graphicsLayer {
                                scaleX = playScale
                                scaleY = playScale
                                alpha = playAlpha
                            }
                            .background(PitchBlack.copy(alpha = 0.58f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = FocusWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                ResumeProgressBar(
                    progress = animatedProgress,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 9.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(ErasmusDimens.CardMetadataGap))

        ErasmusCardTitle(title = item.title)

        Spacer(modifier = Modifier.height(3.dp))

        // Episode context first (what you're on), then time remaining (how much
        // is left) — the two facts that decide whether to resume right now.
        val subtitle = remember(item, episodeLabel) {
            listOfNotNull(episodeLabel, item.resumeLabel.takeIf { it.isNotBlank() })
                .joinToString(" · ")
        }

        Text(
            text = subtitle,
            style = ErasmusTvTypography.CardMeta,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Thin inset resume indicator. Inset rather than flush to the card edge so it
 * reads as a deliberate element of the card rather than a rendering artifact,
 * and so it survives the rounded corner radius cleanly.
 */
@Composable
private fun ResumeProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val height = ErasmusDimens.ProgressBarHeight
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .height(height)
            .background(Color.White.copy(alpha = 0.26f), shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .height(height)
                .background(AccentNeutral, shape)
        )
    }
}

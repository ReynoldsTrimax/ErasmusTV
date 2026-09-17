package com.erasmustv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.focusRequester
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.RatingGold
import com.erasmustv.app.core.theme.SurfaceCard
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import androidx.compose.ui.text.font.FontWeight
import com.erasmustv.app.data.model.ContinueWatchingItem
import androidx.compose.ui.semantics.Role

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingRow(
    items: List<ContinueWatchingItem>,
    onItemClick: (ContinueWatchingItem) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onNavigateLeftToRail: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Continue Watching",
            style = ErasmusTvTypography.SectionTitle,
            modifier = Modifier.padding(start = 64.dp, end = 36.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        val pivotBringIntoViewSpec = remember {
            TvPivotBringIntoViewSpec(0.45f)
        }

        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides pivotBringIntoViewSpec
        ) {
            LazyRow(
                contentPadding = PaddingValues(end = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 64.dp)
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, it -> "${it.mediaType}:${it.tmdbId}:${it.season}:${it.episode}" }
                ) { index, item ->
                    var cardModifier: Modifier = Modifier
                    if (index == 0 && firstItemFocusRequester != null) {
                        cardModifier = cardModifier.focusRequester(firstItemFocusRequester)
                    }
                    cardModifier = cardModifier.onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionLeft -> {
                                    if (index == 0 && onNavigateLeftToRail != null) {
                                        try {
                                            onNavigateLeftToRail()
                                            true
                                        } catch (_: Exception) { false }
                                    } else false
                                }
                                Key.DirectionDown -> {
                                    if (onNavigateDown != null) {
                                        try {
                                            onNavigateDown()
                                        } catch (_: Exception) {}
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (onNavigateUp != null) {
                                        try {
                                            onNavigateUp()
                                        } catch (_: Exception) {}
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }

                    ContinueWatchingCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        cardWidth = 200,
                        cardModifier = cardModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    cardWidth: Int = 200
) {
    val context = LocalContext.current
    val imagePath = item.backdropPath ?: item.posterPath
    val imageRequest = remember(imagePath) {
        ImageRequest.Builder(context)
            .data(
                if (item.backdropPath != null) AppConfig.backdropUrl(item.backdropPath)
                else AppConfig.posterUrl(item.posterPath)
            )
            .crossfade(false)
            .build()
    }

    val a11yDescription = remember(item) {
        buildString {
            append("Resume ")
            append(item.title)
            if (item.mediaType == "tv" && item.season != null && item.episode != null) {
                append(", Season ${item.season} Episode ${item.episode}")
            }
            append(", ${item.resumeLabel}")
        }
    }

    val isReducedMotion = com.erasmustv.app.core.theme.rememberReducedMotion()
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = item.progressRatio,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (isReducedMotion) 0 else 600,
            easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
        ),
        label = "cwProgressFill"
    )

    Column(modifier = modifier.width(cardWidth.dp)) {
        TvFocusableCard(
            onClick = onClick,
            modifier = cardModifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RectangleShape,
            focusedScale = com.erasmustv.app.core.theme.TvMotion.FocusScaleCard,
            focusedBorderColor = FocusWhite,
            focusedBorderWidth = 1.5.dp,
            contentDescription = a11yDescription,
            role = Role.Button
        ) { isFocused ->
            val playIconAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isFocused) 1.0f else 0.0f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = if (isReducedMotion) 0 else com.erasmustv.app.core.theme.TvMotion.DURATION_FAST,
                    easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
                ),
                label = "cwPlayAlpha"
            )
            val playIconScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isFocused) 1.0f else 0.82f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = if (isReducedMotion) 0 else com.erasmustv.app.core.theme.TvMotion.DURATION_FAST,
                    easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
                ),
                label = "cwPlayScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                if (isFocused) Color(0xFF1E1E28) else Color(0xFF141418),
                                if (isFocused) Color(0xFF14141A) else Color(0xFF0C0C0F)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isFocused) FocusWhite else com.erasmustv.app.core.theme.SurfaceCardBorder,
                        shape = RectangleShape
                    )
            ) {
                // Landscape Backdrop Art
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Frosted Play Icon on Focus with smooth scale and alpha
                if (playIconAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = playIconScale
                                scaleY = playIconScale
                                alpha = playIconAlpha
                            }
                            .background(PitchBlack.copy(alpha = 0.76f), RectangleShape)
                            .border(1.dp, Color(0x66FFFFFF), RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = FocusWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Ultra-thin 2.5dp Animated Progress Bar at Bottom of Card (Recommendation #9)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .align(Alignment.BottomCenter)
                        .background(PitchBlack.copy(alpha = 0.85f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgress)
                            .height(2.5.dp)
                            .background(RatingGold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            style = ErasmusTvTypography.CardTitle,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Unified Resume & Progress Metadata (Recommendation #9: e.g. "S1:E4 · 32 min remaining" or "68% watched · 42 min remaining")
        val progressSubtitle = remember(item) {
            val remaining = if (item.resumeLabel.isNotBlank()) item.resumeLabel else "Resume"
            if (item.mediaType == "tv" && item.season != null && item.episode != null) {
                "S${item.season}:E${item.episode} · $remaining"
            } else {
                val pct = (item.progressRatio * 100).toInt().coerceIn(1, 99)
                "$pct% watched · $remaining"
            }
        }

        Text(
            text = progressSubtitle,
            style = ErasmusTvTypography.Badge.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

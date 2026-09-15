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
import androidx.compose.runtime.remember
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
import com.erasmustv.app.core.theme.SurfaceCard
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.data.model.ContinueWatchingItem

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
                                            true
                                        } catch (_: Exception) { false }
                                    } else false
                                }
                                Key.DirectionUp -> {
                                    if (onNavigateUp != null) {
                                        try {
                                            onNavigateUp()
                                            true
                                        } catch (_: Exception) { false }
                                    } else false
                                }
                                else -> false
                            }
                        } else false
                    }

                    ContinueWatchingCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        cardWidth = 138,
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
    cardWidth: Int = 138
) {
    val context = LocalContext.current
    val imageRequest = remember(item.posterPath, item.backdropPath) {
        ImageRequest.Builder(context)
            .data(AppConfig.posterUrl(item.posterPath ?: item.backdropPath))
            .crossfade(false)
            .build()
    }

    Column(modifier = modifier.width(cardWidth.dp)) {
        TvFocusableCard(
            onClick = onClick,
            modifier = cardModifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = RectangleShape,
            focusedScale = 1.0f,
            focusedBorderColor = FocusWhite,
            focusedBorderWidth = 1.5.dp
        ) { isFocused ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .background(SurfaceCard)
                    .border(
                        width = 1.dp,
                        color = if (isFocused) FocusWhite else com.erasmustv.app.core.theme.SurfaceCardBorder,
                        shape = RectangleShape
                    )
            ) {
                // Vertical Poster art
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Progress Bar line at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .align(Alignment.BottomCenter)
                        .background(PitchBlack.copy(alpha = 0.8f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = item.progressRatio)
                            .height(2.5.dp)
                            .background(FocusWhite)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.mediaType == "tv" && item.season != null && item.episode != null) {
                Text(
                    text = "S${item.season} · E${item.episode}",
                    style = ErasmusTvTypography.Badge.copy(fontSize = 9.5.sp),
                    color = TextMuted
                )
            }
            Text(
                text = item.resumeLabel,
                style = ErasmusTvTypography.Badge.copy(fontSize = 9.5.sp),
                color = TextMuted
            )
        }
    }
}

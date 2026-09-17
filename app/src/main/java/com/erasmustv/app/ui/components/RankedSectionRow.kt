package com.erasmustv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.MediaItem
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Ranked Content Rail matching Reference screenshots 3 & 5.
 * Features oversized graphic ranking typography ("1", "2", "3"...)
 * positioned behind and alongside the movie/TV posters.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RankedSectionRow(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onNavigateLeftToRail: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section Header Row: Title on Left, "View All →" on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 64.dp, end = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = ErasmusTvTypography.SectionTitle
            )

            if (onViewAllClick != null) {
                ViewAllAction(onClick = onViewAllClick)
            }
        }

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
                    items = items.take(10),
                    key = { _, item -> "ranked:${item.mediaType}:${item.id}" }
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

                    RankedItemCard(
                        rank = index + 1,
                        item = item,
                        onClick = { onItemClick(item) },
                        cardModifier = cardModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun RankedItemCard(
    rank: Int,
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier
) {
    // Width accommodates the oversized number offset on the left plus the 138dp poster width
    Box(
        modifier = modifier
            .width(182.dp)
    ) {
        // Giant graphic rank number sitting on the left / behind the poster
        Text(
            text = rank.toString(),
            style = ErasmusTvTypography.HeroRankNumber.copy(
                fontSize = if (rank == 10) 72.sp else 88.sp,
                fontWeight = FontWeight.Black,
                color = FocusWhite
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .graphicsLayer {
                    translationX = if (rank == 1) (-2).dp.toPx() else (-8).dp.toPx()
                    translationY = (-20).dp.toPx()
                }
                .zIndex(0f)
        )

        // Poster Card overlapping the right part of the number
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(1f)
        ) {
            MediaPosterCard(
                item = item,
                onClick = onClick,
                cardWidth = 138,
                cardModifier = cardModifier,
                rank = rank,
                badge = "TOP 10"
            )
        }
    }
}

@Composable
fun ViewAllAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "View all titles in section"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .background(if (isFocused) Color(0x2EFFFFFF) else Color.Transparent, androidx.compose.ui.graphics.RectangleShape)
            .border(
                width = 1.dp,
                color = if (isFocused) FocusWhite else Color.Transparent,
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "View All",
                style = ErasmusTvTypography.SectionAction.copy(
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isFocused) FocusWhite else TextMuted
            )
            Text(
                text = "›",
                style = ErasmusTvTypography.SectionAction.copy(fontSize = 16.sp),
                color = if (isFocused) FocusWhite else TextMuted
            )
        }
    }
}

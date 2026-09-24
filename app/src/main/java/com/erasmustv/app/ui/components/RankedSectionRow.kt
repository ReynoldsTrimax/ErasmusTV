package com.erasmustv.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.focus.FeedFocusCoordinator
import com.erasmustv.app.ui.focus.RailFocusHandle
import com.erasmustv.app.ui.focus.railFocusItem

/**
 * ERASMUS TOP 10 RAIL — vertical posters with a ranked numeral.
 *
 * Top 10 rails are *not* a separate card type: they render the same 2:3 poster
 * as every other shelf. The only addition is a numeral set behind the poster's
 * lower-left corner.
 *
 * The numeral is deliberately large but very low contrast. It establishes
 * hierarchy peripherally — you read the order without reading the digits — and
 * because it never covers the artwork or carries a bright fill, the poster
 * remains the primary visual element. No badge is stamped onto the artwork:
 * the numeral already communicates the ranking, and doubling up made the card
 * read as a promotional graphic rather than a piece of cinema.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RankedSectionRow(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    handle: RailFocusHandle,
    coordinator: FeedFocusCoordinator,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    cardWidth: Dp = ErasmusDimens.PosterCardWidth,
    onLeftEdge: (() -> Boolean)? = null
) {
    if (items.isEmpty()) return

    val ranked = remember(items) { items.take(10) }

    ErasmusContentRail(
        title = title,
        handle = handle,
        itemCount = ranked.size,
        modifier = modifier,
        onViewAllClick = onViewAllClick
    ) {
        itemsIndexed(
            items = ranked,
            key = { _, item -> "ranked:${item.mediaType}:${item.id}" }
        ) { index, item ->
            RankedPosterCard(
                rank = index + 1,
                item = item,
                onClick = { onItemClick(item) },
                cardWidth = cardWidth,
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

@Composable
private fun RankedPosterCard(
    rank: Int,
    item: MediaItem,
    onClick: () -> Unit,
    cardWidth: Dp,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier
) {
    val gutter = ErasmusDimens.RankNumeralGutter
    val posterHeight = cardWidth * 1.5f

    Box(modifier = modifier.width(gutter + cardWidth)) {
        // Numeral layer, bottom-aligned to the artwork (not to the metadata
        // block beneath it) so the digit sits against the poster's base edge.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .height(posterHeight)
                .width(gutter + cardWidth)
        ) {
            Text(
                text = rank.toString(),
                style = ErasmusTvTypography.HeroRankNumber.copy(
                    fontSize = if (rank >= 10) 58.sp else 66.sp,
                    lineHeight = 58.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-3).sp,
                    // Low contrast by design: present, never dominant.
                    color = TextPrimary.copy(alpha = 0.17f)
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 2.dp)
            )
        }

        MediaPosterCard(
            item = item,
            onClick = onClick,
            cardWidth = cardWidth,
            cardModifier = cardModifier,
            rank = rank,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

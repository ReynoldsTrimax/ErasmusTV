package com.erasmustv.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.focus.FeedFocusCoordinator
import com.erasmustv.app.ui.focus.RailFocusHandle
import com.erasmustv.app.ui.focus.railFocusItem

/**
 * Standard Erasmus content shelf — vertical poster cards only.
 *
 * Every content rail in the application uses this, with the single exception
 * of Continue Watching (see [ContinueWatchingRow], the only landscape rail).
 *
 * ## Focus
 *
 * The shelf no longer takes a `firstItemFocusRequester` and a `restoreItemIndex`.
 * It takes a [RailFocusHandle], which owns a requester for *every* card, so the
 * focus engine can hand focus to whichever card the user's travelling column
 * points at rather than to one index chosen at composition time. That is the
 * mechanism behind column-preserving vertical movement, and it is why the old
 * "scroll, wait 50ms, then focus" sequence is gone: any composed card is
 * focusable in the same frame as the key press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaSectionRow(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    handle: RailFocusHandle,
    coordinator: FeedFocusCoordinator,
    modifier: Modifier = Modifier,
    onViewAllClick: (() -> Unit)? = null,
    cardWidth: Dp = ErasmusDimens.PosterCardWidth,
    showNewBadge: Boolean = false,
    /** LEFT on the first card. Return true if handled; null consumes the key. */
    onLeftEdge: (() -> Boolean)? = null
) {
    if (items.isEmpty()) return

    ErasmusContentRail(
        title = title,
        handle = handle,
        itemCount = items.size,
        modifier = modifier,
        onViewAllClick = onViewAllClick
    ) {
        itemsIndexed(
            items = items,
            // Stable, content-derived identity. Focus identity must never be a
            // render position: inserting or removing a title would otherwise
            // silently move focus to a different film.
            key = { _, item -> "${item.mediaType}:${item.id}" }
        ) { index, item ->
            MediaPosterCard(
                item = item,
                onClick = { onItemClick(item) },
                cardWidth = cardWidth,
                cardModifier = Modifier.railFocusItem(
                    handle = handle,
                    coordinator = coordinator,
                    index = index,
                    isFirstItem = index == 0,
                    onLeftEdge = onLeftEdge
                ),
                badge = if (showNewBadge) "NEW" else null
            )
        }
    }
}

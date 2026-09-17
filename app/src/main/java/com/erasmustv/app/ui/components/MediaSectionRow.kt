package com.erasmustv.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.data.model.MediaItem

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Standard horizontal streaming rail matching Reference screenshots.
 * Features clean section header with "View All ›" action, generous padding,
 * and artwork-dominated poster cards.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaSectionRow(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    onViewAllClick: (() -> Unit)? = null,
    cardWidth: Int = 140,
    firstItemFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    showNewBadge: Boolean = false,
    onNavigateLeftToRail: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section Header Row: Title on Left, "View All ›" on Right
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
                    key = { _, item -> "${item.mediaType}:${item.id}" }
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

                    MediaPosterCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        cardWidth = cardWidth,
                        cardModifier = cardModifier,
                        badge = if (showNewBadge) "NEW" else null
                    )
                }
            }
        }
    }
}

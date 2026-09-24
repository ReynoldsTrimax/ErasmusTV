package com.erasmustv.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.ui.focus.RailFocusHandle
import com.erasmustv.app.ui.focus.railFocusContainer

/**
 * ERASMUS CONTENT RAIL — the shared scaffold for every horizontal shelf.
 *
 * Gutters, heading treatment, card gap, and pivot scroll behaviour live here
 * once. A rail's identity comes from the cards it renders, never from its own
 * chrome: there is no container, no panel, no background, and no divider, so
 * stacked rails read as one continuous cinematic surface.
 *
 * ## Focus responsibilities
 *
 * The rail owns its `LazyListState` and hands it to [handle], which is what
 * lets the focus engine read this shelf's on-screen card positions when
 * *another* shelf asks "which of your cards is under this column". Without that
 * link, vertical movement can only ever target a fixed index.
 *
 * The state is created with [rememberLazyListState], which is saveable. Because
 * a `LazyColumn` wraps each keyed item in its own saved-state holder, a shelf's
 * horizontal scroll offset survives being scrolled out of the feed and back —
 * so returning to a shelf does not snap it to card 1.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ErasmusContentRail(
    title: String,
    handle: RailFocusHandle,
    itemCount: Int,
    modifier: Modifier = Modifier,
    onViewAllClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ErasmusRailHeader(
            title = title,
            onViewAllClick = onViewAllClick,
            trailing = trailing
        )

        Spacer(modifier = Modifier.height(ErasmusDimens.RailTitleGap))

        // Keeps the focused card at a stable pivot point rather than letting it
        // ride to the screen edge, so D-pad traversal never clips a lifted card
        // and the focused card always keeps comfortable breathing room from the
        // bezel.
        val pivotBringIntoViewSpec = remember { TvPivotBringIntoViewSpec(0.42f) }

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides pivotBringIntoViewSpec
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(
                    start = ErasmusDimens.RailStartGutter,
                    // Extra end room so the last card's focus lift and shadow
                    // have space to render instead of being cut by the bezel.
                    end = ErasmusDimens.RailEndGutter,
                    // A LazyRow clips to its own bounds, so a focused card's
                    // scale lift and soft shadow need headroom *inside* those
                    // bounds or the top/bottom of the artwork gets sliced.
                    top = ErasmusDimens.RailFocusHeadroom,
                    bottom = ErasmusDimens.RailFocusHeadroom
                ),
                horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.CardSpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .railFocusContainer(handle, listState, itemCount),
                content = content
            )
        }
    }
}

/**
 * Rail heading: a strong-but-restrained section title with an optional,
 * deliberately understated "All" affordance on the right.
 */
@Composable
fun ErasmusRailHeader(
    title: String,
    modifier: Modifier = Modifier,
    onViewAllClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ErasmusDimens.RailStartGutter,
                end = ErasmusDimens.RailEndGutter
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = title,
            style = ErasmusTvTypography.SectionTitle
        )

        when {
            trailing != null -> trailing()
            onViewAllClick != null -> ViewAllAction(onClick = onViewAllClick)
        }
    }
}

/**
 * The "All" shelf affordance. Text-only at rest, gaining a rounded tinted
 * capsule on focus — enough to be unmistakably focused, quiet enough that it
 * never competes with the shelf title beside it.
 */
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
                contentDescription = "View all titles in this section"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .background(
                color = if (isFocused) Color.White.copy(alpha = 0.10f) else Color.Transparent,
                shape = ErasmusShapes.Button
            )
            .border(
                width = 1.dp,
                color = if (isFocused) FocusWhite.copy(alpha = 0.85f) else Color.Transparent,
                shape = ErasmusShapes.Button
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            androidx.compose.material3.Text(
                text = "All",
                style = ErasmusTvTypography.SectionAction.copy(
                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (isFocused) FocusWhite else TextMuted
            )
            androidx.compose.material3.Text(
                text = "›",
                style = ErasmusTvTypography.SectionAction.copy(fontSize = 15.sp),
                color = if (isFocused) FocusWhite else TextMuted
            )
        }
    }
}

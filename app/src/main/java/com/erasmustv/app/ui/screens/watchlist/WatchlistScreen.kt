package com.erasmustv.app.ui.screens.watchlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusSpacing
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.rememberPosterGridColumns
import com.erasmustv.app.core.theme.rememberScreenHorizontalMargin
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.ErasmusActionButton
import com.erasmustv.app.ui.components.ErasmusButtonStyle
import com.erasmustv.app.ui.components.ErasmusEmptyState
import com.erasmustv.app.ui.components.ErasmusPageHeader
import com.erasmustv.app.ui.components.MediaPosterCard
import com.erasmustv.app.ui.components.TvFloatingNavBar
import com.erasmustv.app.ui.components.TvGridSkeleton
import com.erasmustv.app.ui.components.TvPivotBringIntoViewSpec
import com.erasmustv.app.ui.focus.SpatialDirection
import com.erasmustv.app.ui.focus.gridFocusContainer
import com.erasmustv.app.ui.focus.gridFocusItem
import com.erasmustv.app.ui.focus.rememberFocusZoneMemory
import com.erasmustv.app.ui.focus.rememberGridFocusHandle
import com.erasmustv.app.ui.navigation.NavRoutes
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/** Focus-engine zone key for the single Watch List grid. */
private const val WATCHLIST_GRID_KEY = "watchlist_grid"

/**
 * ERASMUS WATCH LIST.
 *
 * A single responsive grid of vertical posters — the same poster card used by
 * every rail elsewhere, so a saved title looks identical here and on Home.
 *
 * The list is read straight from [WatchlistViewModel] / `WatchlistRepository`;
 * there is no second store or local copy, which is what keeps this page, the
 * detail pages' saved state, and the underlying persistence in agreement.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }
    var isNavFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val horizontalMargin = rememberScreenHorizontalMargin()
    val columns = rememberPosterGridColumns()

    // ------------------------------------------------------------------
    // Focus engine.
    //
    // The grid state is remembered here so the scroll offset survives a detail
    // round trip. Without it the remembered *index* pointed at a cell the grid
    // had not composed — it had reset to offset 0 — so restoration silently
    // failed even though the memory was correct.
    // ------------------------------------------------------------------
    val gridState = rememberLazyGridState()
    val gridHandle = rememberGridFocusHandle(WATCHLIST_GRID_KEY)
    val focusMemory = rememberFocusZoneMemory()

    // Re-read on every entry so a title removed from a detail page is gone the
    // moment the user arrives back here.
    LaunchedEffect(Unit) {
        viewModel.loadWatchlist()
    }

    // ------------------------------------------------------------------
    // Focus restoration, driven by *item identity* rather than a one-shot flag.
    //
    // The previous implementation guarded its single `requestFocus()` behind a
    // `rememberSaveable` boolean. That flag survives the trip to a detail page,
    // so on the way back it was already true and the restore never ran — and
    // because `loadWatchlist()` flushes the grid through Loading (which swaps in
    // a non-focusable skeleton and destroys the focused node), the page ended up
    // with no focus at all after every return and after every removal.
    //
    // Keying on the loaded item identities instead means focus is re-established
    // exactly when the grid is rebuilt, whatever the reason.
    // ------------------------------------------------------------------

    /**
     * Whether this screen instance has claimed its entry focus yet.
     *
     * Deliberately *not* saveable, and deliberately checked before `isNavFocused`
     * rather than after. On arrival the framework parks default focus on the
     * first focusable it finds, which is the floating nav — so a plain
     * "don't steal focus from the nav" guard permanently lost the race and left
     * the page's intentional entry target (the first poster) unfocused. Claiming
     * once per screen instance fixes entry, while still refusing to pull focus
     * back out of the nav on later refreshes once the user has gone there on
     * purpose.
     */
    var hasClaimedEntryFocus by remember { mutableStateOf(false) }

    val loadedKeys = (uiState as? WatchlistUiState.Success)?.items
        ?.joinToString(",") { "${it.mediaType}:${it.id}" }
    LaunchedEffect(loadedKeys) {
        if (loadedKeys.isNullOrEmpty()) return@LaunchedEffect
        if (hasClaimedEntryFocus && isNavFocused) return@LaunchedEffect
        val remembered = focusMemory.indexFor(WATCHLIST_GRID_KEY) ?: 0
        // Clamped, because the remembered cell may have been the one the user
        // just removed — in which case focus lands on its neighbour rather than
        // being lost.
        val itemCount = (uiState as? WatchlistUiState.Success)?.items?.size ?: 0
        if (itemCount == 0) return@LaunchedEffect
        val target = remembered.coerceIn(0, itemCount - 1)
        if (gridHandle.focusIndexOrNearest(target) ||
            runCatching { contentFocusRequester.requestFocus() }.isSuccess
        ) {
            hasClaimedEntryFocus = true
        }
    }

    BackHandler(enabled = !isNavFocused) {
        try {
            navFocusRequester.requestFocus()
        } catch (_: Exception) {
            onNavigate(NavRoutes.HOME)
        }
    }
    BackHandler(enabled = isNavFocused) {
        onNavigate(NavRoutes.HOME)
    }

    val focusNav: () -> Boolean = {
        runCatching { navFocusRequester.requestFocus() }.isSuccess
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        when (val state = uiState) {
            is WatchlistUiState.Loading -> TvGridSkeleton(columns = columns)

            is WatchlistUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalMargin),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ErasmusEmptyState(
                        title = "Can't load your Watch List",
                        message = state.message
                    )
                    Spacer(modifier = Modifier.height(ErasmusSpacing.Large))
                    ErasmusActionButton(
                        text = "Try Again",
                        onClick = { viewModel.loadWatchlist() },
                        style = ErasmusButtonStyle.Primary,
                        modifier = Modifier.focusRequester(contentFocusRequester),
                        onNavigateLeft = { focusNav() },
                        onNavigateUp = { focusNav() },
                        // Nothing lies below or beside this button. Consuming
                        // those directions is what the empty-state branch already
                        // did; omitting it here let focus vanish off the retry
                        // button on the error branch.
                        onNavigateDown = {},
                        onNavigateRight = {}
                    )
                }
            }

            is WatchlistUiState.Success -> {
                if (state.items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = horizontalMargin,
                                end = horizontalMargin,
                                top = ErasmusDimens.NavPillContentClearance
                            ),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErasmusEmptyState(
                            icon = Icons.Default.BookmarkBorder,
                            title = "Your Watch List is empty",
                            message = "Open any film or series and choose Add to Watch List to keep it here."
                        )
                        Spacer(modifier = Modifier.height(ErasmusSpacing.Large))
                        ErasmusActionButton(
                            text = "Browse Titles",
                            onClick = { onNavigate(NavRoutes.HOME) },
                            icon = Icons.Default.Home,
                            style = ErasmusButtonStyle.Primary,
                            modifier = Modifier.focusRequester(contentFocusRequester),
                            onNavigateLeft = { focusNav() },
                            onNavigateUp = { focusNav() },
                            // Nothing lies below or beside this button; consume
                            // those directions so focus cannot vanish.
                            onNavigateDown = {},
                            onNavigateRight = {}
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = horizontalMargin,
                                end = horizontalMargin,
                                top = ErasmusDimens.NavPillContentClearance
                            )
                    ) {
                        ErasmusPageHeader(
                            title = "Watch List",
                            subtitle = if (state.items.size == 1) {
                                "1 saved title"
                            } else {
                                "${state.items.size} saved titles"
                            }
                        )

                        Spacer(modifier = Modifier.height(ErasmusSpacing.Large))

                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.5f) }
                        ) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(columns),
                                contentPadding = PaddingValues(
                                    top = ErasmusDimens.RailFocusHeadroom,
                                    bottom = ErasmusSpacing.XLarge
                                ),
                                horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.GridItemSpacing),
                                verticalArrangement = Arrangement.spacedBy(ErasmusDimens.GridRowSpacing),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .gridFocusContainer(gridHandle, gridState, state.items.size)
                            ) {
                                itemsIndexed(
                                    items = state.items,
                                    // Content-derived identity, so removing a
                                    // title cannot slide focus onto a different
                                    // one by way of a shifted index.
                                    key = { _, item -> "${item.mediaType}:${item.id}" }
                                ) { index, item ->
                                    MediaPosterCard(
                                        item = item,
                                        onClick = { onMediaClick(item) },
                                        cardWidth = Dp.Unspecified,
                                        cardModifier = Modifier.gridFocusItem(
                                            handle = gridHandle,
                                            index = index,
                                            onFocused = { focusedIndex ->
                                                focusMemory.record(
                                                    WATCHLIST_GRID_KEY,
                                                    focusedIndex,
                                                    gridHandle.centerXOf(focusedIndex)
                                                )
                                            },
                                            onEscapeUp = focusNav,
                                            onEscapeLeft = focusNav,
                                            // More rows exist below the fold:
                                            // scroll rather than treating the last
                                            // visible row as the grid's edge.
                                            onScrollRequest = { direction ->
                                                coroutineScope.launch {
                                                    runCatching {
                                                        val delta = if (direction == SpatialDirection.Down) {
                                                            columns
                                                        } else {
                                                            -columns
                                                        }
                                                        gridState.animateScrollToItem(
                                                            (index + delta)
                                                                .coerceIn(0, state.items.size - 1)
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        TvFloatingNavBar(
            currentRoute = NavRoutes.WATCHLIST,
            activeProfile = (uiState as? WatchlistUiState.Success)?.activeProfile,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            navFocusRequester = navFocusRequester,
            onFocusChanged = { isNavFocused = it },
            onNavigateIntoContent = {
                // Back to the poster the user was on, not the top-left cell.
                val remembered = focusMemory.indexFor(WATCHLIST_GRID_KEY) ?: 0
                gridHandle.focusIndexOrNearest(remembered) ||
                    runCatching { contentFocusRequester.requestFocus() }.isSuccess
            },
            onReselectCurrent = {
                coroutineScope.launch {
                    runCatching { gridState.animateScrollToItem(0) }
                    gridHandle.focusIndexOrNearest(0)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

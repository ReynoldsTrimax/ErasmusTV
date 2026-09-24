package com.erasmustv.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusSpacing
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.rememberHeroAmbientColor
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.ui.focus.FocusDebugOverlay
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.erasmustv.app.ui.focus.FeedFocusCoordinator
import com.erasmustv.app.ui.focus.FocusZoneMemory
import com.erasmustv.app.ui.focus.RailFocusHandle
import com.erasmustv.app.ui.focus.RailZone
import com.erasmustv.app.ui.focus.SingleTargetZone
import com.erasmustv.app.ui.focus.rememberFeedBringIntoViewSpec
import com.erasmustv.app.ui.focus.rememberFeedFocusCoordinator
import com.erasmustv.app.ui.focus.rememberFocusZoneMemory
import com.erasmustv.app.ui.focus.rememberRailFocusHandleStore
import kotlinx.coroutines.launch

/** Focus-engine zone key for a feed hero's action row. */
const val FEED_ZONE_HERO = "__hero__"

/**
 * One poster shelf in a feed.
 *
 * [key] is the shelf's focus identity — the thing the engine remembers a column
 * against — so it must be stable across content refreshes and must not be a
 * position in a list.
 */
data class FeedShelf(
    val key: String,
    val title: String,
    val items: List<MediaItem>,
    val isRanked: Boolean = false,
    val showNewBadge: Boolean = false,
    val viewAllRoute: String? = null,
    val cardWidth: Dp = ErasmusDimens.PosterCardWidth
)

/**
 * A shelf the feed cannot render itself — currently only Home's landscape
 * Continue Watching rail, whose cards are a different shape and whose item type
 * is not [MediaItem].
 *
 * It still participates fully in the focus engine: the scaffold hands it a
 * [RailFocusHandle] and the [FeedFocusCoordinator], so vertical movement into
 * and out of it preserves the travelling column exactly like any other shelf.
 * That is what makes the landscape → portrait transition behave.
 */
class FeedLeadingShelf(
    val key: String,
    val trailingSpacing: Dp = ErasmusDimens.RailSpacingLarge,
    val content: @Composable (
        handle: RailFocusHandle,
        coordinator: FeedFocusCoordinator,
        onLeftEdge: () -> Boolean
    ) -> Unit
)

/**
 * ERASMUS FEED SCAFFOLD — the single implementation of a browse page.
 *
 * Home, Movies, TV Shows, and Anime are all "a hero, then a stack of shelves,
 * with a floating nav over the top". They used to be four hand-rolled copies of
 * that idea, each with its own focus requesters, its own `navigateToRow`
 * helper, its own `targetUpIndex`/`targetDownRequester` arithmetic, and its own
 * back handling. Predictably they drifted: one screen forgot to wire a shelf's
 * vertical callbacks at all, another left nothing focused when the user returned
 * from the player, and each had a slightly different idea of what UP from the
 * top row should do.
 *
 * Consolidating them here is the structural half of the D-pad work. There is now
 * one answer to each of the following, and it is the same answer on every page:
 *
 *  - **Page entry focus** is the hero's primary action, or the first shelf with
 *    content if there is no hero.
 *  - **Return from a detail page** restores the exact card the user opened.
 *  - **UP from the top row** enters the floating nav; **DOWN from the nav**
 *    returns to the zone the user actually left.
 *  - **Vertical movement** preserves the travelling column, resolved spatially.
 *  - **BACK** steps from content up to the nav, then to [onBackFromNav].
 *
 * @param onBackFromNav what BACK does once focus is already in the navigation.
 *   Null means "let the system handle it", which is correct on the root screen
 *   so the app stays exitable; secondary pages pass a hop back to Home.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ErasmusFeedScaffold(
    currentRoute: String,
    activeProfile: WatchProfile?,
    heroItem: MediaItem?,
    heroFeaturedItems: List<MediaItem>,
    shelves: List<FeedShelf>,
    onNavigate: (String) -> Unit,
    onProfileClick: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    leadingShelf: FeedLeadingShelf? = null,
    onBackFromNav: (() -> Unit)? = null,
    state: FeedScaffoldState = rememberFeedScaffoldState()
) {
    val listState = state.listState
    val coordinator = state.coordinator
    val handleStore = state.handleStore
    val coroutineScope = rememberCoroutineScope()

    val navFocusRequester = remember { FocusRequester() }
    val heroFocusRequester = remember { FocusRequester() }
    var isNavFocused by remember { mutableStateOf(false) }

    // Shelves with no content are dropped before the zone map is built, so an
    // empty shelf can never become a vertical dead end the user has to press
    // through.
    val visibleShelves = shelves.filter { it.items.isNotEmpty() }

    var selectedHeroItem by remember(heroItem?.id) { mutableStateOf(heroItem) }
    val displayHero = selectedHeroItem ?: heroItem
    val hasHero = displayHero != null

    // ------------------------------------------------------------------
    // Zone map, top to bottom. This list *is* the page's vertical structure as
    // far as navigation is concerned; there is no other copy of it to fall out
    // of sync with the layout below.
    // ------------------------------------------------------------------
    val leadingRow = if (hasHero) 1 else 0
    val shelvesStartRow = leadingRow + if (leadingShelf != null) 1 else 0

    val zoneOrder = buildList {
        if (hasHero) add(FEED_ZONE_HERO)
        leadingShelf?.let { add(it.key) }
        visibleShelves.forEach { add(it.key) }
    }
    coordinator.setZoneOrder(zoneOrder)

    if (hasHero) {
        coordinator.register(SingleTargetZone(FEED_ZONE_HERO, 0, heroFocusRequester))
    }
    leadingShelf?.let { shelf ->
        coordinator.register(RailZone(leadingRow, handleStore.handleFor(shelf.key)))
    }
    visibleShelves.forEachIndexed { index, shelf ->
        coordinator.register(RailZone(shelvesStartRow + index, handleStore.handleFor(shelf.key)))
    }

    // ------------------------------------------------------------------
    // LEFT at the left edge of a shelf is a wall, not a shortcut into the nav.
    //
    // It used to hand focus to the floating navigation, which meant walking left
    // through a shelf and pressing once more teleported focus out of the content
    // and up into the chrome — a jump the viewer did not ask for and cannot undo
    // with the opposite key. The nav is reached deliberately, with UP from the
    // top row or with BACK. Returning true consumes the press so focus simply
    // stays on the first card.
    // ------------------------------------------------------------------
    val consumeLeftEdge: () -> Boolean = { true }

    val focusNav: () -> Boolean = {
        try {
            navFocusRequester.requestFocus()
            true
        } catch (_: Exception) {
            false
        }
    }

    // ------------------------------------------------------------------
    // Entry focus, requested exactly once per visit.
    //
    // Keyed on Unit, so a later API response cannot re-run it and pull focus
    // away from wherever the user has already navigated to. The saveable flag
    // distinguishes a first visit from a return trip, and — unlike the previous
    // implementation — a return trip with nothing remembered falls through to
    // the intentional entry target instead of leaving the page with no focus at
    // all.
    // ------------------------------------------------------------------
    LaunchedEffect(Unit) {
        if (state.hasRequestedInitialFocus && coordinator.restoreRememberedFocus()) {
            return@LaunchedEffect
        }
        val landed = zoneOrder.any { coordinator.focusZone(it) }
        if (!landed) focusNav()
        state.hasRequestedInitialFocus = true
    }

    // BACK from content steps up to the navigation. BACK from the navigation
    // defers to [onBackFromNav] — null on the root screen, so the system handles
    // it and the app remains exitable.
    androidx.activity.compose.BackHandler(enabled = !isNavFocused) { focusNav() }
    if (onBackFromNav != null) {
        androidx.activity.compose.BackHandler(enabled = isNavFocused) { onBackFromNav() }
    }

    val density = LocalDensity.current
    val navClearancePx = with(density) { ErasmusDimens.NavPillContentClearance.toPx() }
    val focusClearancePx = with(density) { ErasmusDimens.FocusScrollClearance.toPx() }
    val verticalBringIntoViewSpec = rememberFeedBringIntoViewSpec(navClearancePx, focusClearancePx)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        // ------------------------------------------------------------------
        // Frosted hero backdrop: the hero artwork, heavily frosted, filling the
        // whole page behind the feed so its colour continues all the way down
        // instead of fading out a few hundred pixels below the hero.
        // ------------------------------------------------------------------
        val heroArtworkKey = displayHero?.let { it.backdropPath ?: it.posterPath }
        val heroImageUrl = AppConfig.backdropUrl(heroArtworkKey)
            ?: AppConfig.posterUrl(displayHero?.posterPath)
        val ambientColor = rememberHeroAmbientColor(
            artworkPath = heroArtworkKey,
            imageUrl = AppConfig.backdropUrl(heroArtworkKey),
            // Some catalog entries carry a stale backdrop that 404s; the poster
            // still yields the correct ambience.
            fallbackImageUrl = AppConfig.posterUrl(displayHero?.posterPath)
        )
        HeroFrostedBackdrop(
            artworkUrl = heroImageUrl,
            ambientColor = ambientColor,
            modifier = Modifier.fillMaxSize()
        )

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides verticalBringIntoViewSpec
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = ErasmusSpacing.SectionLarge)
            ) {
                if (hasHero) {
                    item(key = FEED_ZONE_HERO) {
                        HeroBillboard(
                            item = displayHero,
                            onPlayClick = onPlayClick,
                            onDetailsClick = onMediaClick,
                            heroFocusRequester = heroFocusRequester,
                            featuredItems = heroFeaturedItems.take(5),
                            onFeaturedSelect = { selectedHeroItem = it },
                            onFocused = { coordinator.onZoneFocused(FEED_ZONE_HERO) },
                            onNavigateLeft = { focusNav() },
                            // UP from the hero first scrolls the feed to its
                            // absolute top, so the full hero (title and artwork)
                            // is revealed from under the floating nav. Only once
                            // already at the very top does UP hand off to the
                            // navigation — so the hero is always reachable and the
                            // remote never feels like it skipped straight past it.
                            onNavigateUp = {
                                val atTop = listState.firstVisibleItemIndex == 0 &&
                                    listState.firstVisibleItemScrollOffset == 0
                                if (atTop) {
                                    focusNav()
                                } else {
                                    coroutineScope.launch {
                                        runCatching { listState.animateScrollToItem(0) }
                                    }
                                }
                            },
                            onNavigateDown = { coordinator.moveVertical(FEED_ZONE_HERO, +1) },
                            // Only auto-advance while the hero is the thing on
                            // screen; a carousel rotating out of view is motion
                            // spent on nobody.
                            isAutoAdvanceEnabled = listState.firstVisibleItemIndex == 0
                        )
                        // Deliberately tight: the hero's bottom fade and the
                        // ambient wash carry the transition, so the first shelf
                        // begins inside that atmosphere rather than after a gap.
                        Spacer(modifier = Modifier.height(ErasmusSpacing.Large))
                    }
                }

                leadingShelf?.let { shelf ->
                    item(key = shelf.key) {
                        shelf.content(
                            handleStore.handleFor(shelf.key),
                            coordinator,
                            consumeLeftEdge
                        )
                        Spacer(modifier = Modifier.height(shelf.trailingSpacing))
                    }
                }

                items(
                    items = visibleShelves,
                    key = { shelf -> shelf.key }
                ) { shelf ->
                    val handle = handleStore.handleFor(shelf.key)
                    val onViewAll = shelf.viewAllRoute?.let { route -> { onNavigate(route) } }

                    if (shelf.isRanked) {
                        RankedSectionRow(
                            title = shelf.title,
                            items = shelf.items,
                            onItemClick = onMediaClick,
                            handle = handle,
                            coordinator = coordinator,
                            onViewAllClick = onViewAll,
                            cardWidth = shelf.cardWidth,
                            onLeftEdge = consumeLeftEdge
                        )
                    } else {
                        MediaSectionRow(
                            title = shelf.title,
                            items = shelf.items,
                            onItemClick = onMediaClick,
                            handle = handle,
                            coordinator = coordinator,
                            onViewAllClick = onViewAll,
                            cardWidth = shelf.cardWidth,
                            showNewBadge = shelf.showNewBadge,
                            onLeftEdge = consumeLeftEdge
                        )
                    }
                    Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
                }
            }
        }

        TvFloatingNavBar(
            currentRoute = currentRoute,
            activeProfile = activeProfile,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            navFocusRequester = navFocusRequester,
            isCompact = listState.firstVisibleItemIndex > 0,
            onFocusChanged = { focused -> isNavFocused = focused },
            // DOWN out of the nav returns to the zone the user actually left,
            // not a fixed row.
            onNavigateIntoContent = { coordinator.enterContent() },
            onReselectCurrent = {
                coroutineScope.launch {
                    runCatching {
                        listState.animateScrollToItem(0)
                        zoneOrder.firstOrNull()?.let { coordinator.focusZone(it) }
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Development-only: shows the active zone, the travelling column, and the
        // zone map against the real layout. Compiled out of release builds.
        FocusDebugOverlay(
            coordinator = coordinator,
            zoneOrder = zoneOrder,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

/**
 * The scaffold's focus and scroll state.
 *
 * Hoisted out of [ErasmusFeedScaffold] so a screen can create it *above* its
 * loading/error/content branch. That placement is the whole point: state
 * remembered inside such a branch is discarded when the branch changes and is
 * not restored when Navigation Compose recreates the screen after a detail page
 * pops, which is exactly how focus restoration was being silently defeated.
 */
class FeedScaffoldState internal constructor(
    val listState: LazyListState,
    val memory: FocusZoneMemory,
    val coordinator: FeedFocusCoordinator,
    val handleStore: com.erasmustv.app.ui.focus.RailFocusHandleStore,
    hasRequestedInitialFocus: androidx.compose.runtime.MutableState<Boolean>
) {
    var hasRequestedInitialFocus: Boolean by hasRequestedInitialFocus
}

@Composable
fun rememberFeedScaffoldState(): FeedScaffoldState {
    val listState = rememberLazyListState()
    val memory = rememberFocusZoneMemory()
    val scope = rememberCoroutineScope()
    val coordinator = rememberFeedFocusCoordinator(listState, scope, memory)
    val handleStore = rememberRailFocusHandleStore()
    val hasRequestedInitialFocus = rememberSaveable { mutableStateOf(false) }
    return remember(listState, memory, coordinator, handleStore) {
        FeedScaffoldState(listState, memory, coordinator, handleStore, hasRequestedInitialFocus)
    }
}

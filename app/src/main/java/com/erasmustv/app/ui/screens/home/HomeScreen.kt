package com.erasmustv.app.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.erasmustv.app.core.theme.ErasmusSpacing
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.rememberReducedMotion
import com.erasmustv.app.data.model.ContinueWatchingItem
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.ContinueWatchingRow
import com.erasmustv.app.ui.components.ErasmusActionButton
import com.erasmustv.app.ui.components.ErasmusButtonStyle
import com.erasmustv.app.ui.components.ErasmusEmptyState
import com.erasmustv.app.ui.components.ErasmusFeedScaffold
import com.erasmustv.app.ui.components.FeedLeadingShelf
import com.erasmustv.app.ui.components.FeedShelf
import com.erasmustv.app.ui.components.TvFeedSkeleton
import com.erasmustv.app.ui.components.rememberFeedScaffoldState
import com.erasmustv.app.ui.navigation.NavRoutes

/**
 * Focus-engine zone key for the Continue Watching rail. Every other shelf uses
 * its own `HomeSection.id`; this rail has no section record of its own.
 */
private const val ZONE_CONTINUE_WATCHING = "__continue_watching__"

/** Which branch of the feed is on screen. */
private enum class HomePhase { Loading, Error, Feed }

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onResumeClick: (ContinueWatchingItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isReducedMotion = rememberReducedMotion()

    // Held above the phase transition so scroll position, focus memory, and the
    // "focus already claimed" flag all survive both a loading refresh and a round
    // trip through a detail page.
    val scaffoldState = rememberFeedScaffoldState()

    // ------------------------------------------------------------------
    // Transition on phase, not on payload.
    //
    // Driving AnimatedContent from `uiState` itself meant every new Success
    // instance — including the background Continue Watching refresh that fires
    // on every return from the player — disposed the entire feed subtree and
    // with it every FocusRequester the engine holds, so focus simply vanished.
    // Keying on the phase lets data updates recompose the feed in place, per the
    // rule that an API response must never move the user's focus.
    // ------------------------------------------------------------------
    val phase = when (uiState) {
        is HomeUiState.Loading -> HomePhase.Loading
        is HomeUiState.Error -> HomePhase.Error
        is HomeUiState.Success -> HomePhase.Feed
    }

    // Retains the last real payload so the outgoing branch still has something
    // to draw during the crossfade.
    var latestData by remember { mutableStateOf<HomeData?>(null) }
    (uiState as? HomeUiState.Success)?.let { latestData = it.data }
    val errorMessage = (uiState as? HomeUiState.Error)?.message

    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            if (isReducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                fadeIn(
                    animationSpec = tween(TvMotion.DURATION_ENTER, easing = TvMotion.EasingSilk)
                ) togetherWith fadeOut(
                    animationSpec = tween(TvMotion.DURATION_FAST, easing = TvMotion.EasingSilk)
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack),
        label = "homeScreenPhaseTransition"
    ) { currentPhase ->
        when (currentPhase) {
            HomePhase.Loading -> TvFeedSkeleton()

            HomePhase.Error -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(ErasmusSpacing.SectionLarge * 2))
                ErasmusEmptyState(
                    title = "Something went wrong",
                    message = errorMessage ?: "Unable to load your feed."
                )
                Spacer(modifier = Modifier.height(ErasmusSpacing.Large))
                ErasmusActionButton(
                    text = "Try Again",
                    onClick = { viewModel.loadHomeFeed(forceRefresh = true) },
                    style = ErasmusButtonStyle.Primary
                )
            }

            HomePhase.Feed -> {
                val data = latestData ?: return@AnimatedContent

                val heroItem = data.heroItem
                    ?: data.sections.firstOrNull()?.items?.firstOrNull()
                    ?: data.trending.firstOrNull()
                    ?: data.popularMovies.firstOrNull()
                    ?: data.popularTv.firstOrNull()
                    ?: data.topRated.firstOrNull()

                val shelves = data.sections.map { section ->
                    FeedShelf(
                        key = section.id,
                        title = section.title,
                        items = section.items,
                        isRanked = section.isRanked,
                        showNewBadge = section.showNewBadge,
                        viewAllRoute = section.viewAllRoute
                    )
                }

                // Continue Watching is the one shelf the scaffold cannot render
                // itself — landscape cards, and a different item type — so it is
                // injected as a leading shelf. It still receives a handle and the
                // coordinator, so moving down out of its 16:9 cards into the
                // portrait shelf beneath resolves against measured geometry
                // rather than collapsing to card one.
                val leadingShelf = if (data.continueWatching.isNotEmpty()) {
                    FeedLeadingShelf(key = ZONE_CONTINUE_WATCHING) { handle, coordinator, onLeftEdge ->
                        ContinueWatchingRow(
                            items = data.continueWatching,
                            onItemClick = onResumeClick,
                            handle = handle,
                            coordinator = coordinator,
                            onLeftEdge = onLeftEdge
                        )
                    }
                } else null

                ErasmusFeedScaffold(
                    currentRoute = NavRoutes.HOME,
                    activeProfile = data.activeProfile,
                    heroItem = heroItem,
                    heroFeaturedItems = data.trending,
                    shelves = shelves,
                    onNavigate = onNavigate,
                    onProfileClick = onProfileClick,
                    onMediaClick = onMediaClick,
                    onPlayClick = onPlayClick,
                    leadingShelf = leadingShelf,
                    // Home is the root browse screen: BACK from the navigation is
                    // left to the system so the app stays exitable from here.
                    onBackFromNav = null,
                    state = scaffoldState
                )
            }
        }
    }
}

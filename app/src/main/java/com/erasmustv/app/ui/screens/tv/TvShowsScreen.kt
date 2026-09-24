package com.erasmustv.app.ui.screens.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.ErasmusActionButton
import com.erasmustv.app.ui.components.ErasmusButtonStyle
import com.erasmustv.app.ui.components.ErasmusEmptyState
import com.erasmustv.app.ui.components.ErasmusFeedScaffold
import com.erasmustv.app.ui.components.FeedShelf
import com.erasmustv.app.ui.components.TvFeedSkeleton
import com.erasmustv.app.ui.components.rememberFeedScaffoldState
import com.erasmustv.app.ui.navigation.NavRoutes

private enum class TvShowsPhase { Loading, Error, Feed }

@Composable
fun TvShowsScreen(
    viewModel: TvShowsViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isReducedMotion = rememberReducedMotion()

    val scaffoldState = rememberFeedScaffoldState()

    val phase = when (uiState) {
        is TvShowsUiState.Loading -> TvShowsPhase.Loading
        is TvShowsUiState.Error -> TvShowsPhase.Error
        is TvShowsUiState.Success -> TvShowsPhase.Feed
    }

    var latestData by remember { mutableStateOf<TvShowsData?>(null) }
    (uiState as? TvShowsUiState.Success)?.let { latestData = it.data }
    val errorMessage = (uiState as? TvShowsUiState.Error)?.message

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
        label = "tvShowsScreenPhaseTransition"
    ) { currentPhase ->
        when (currentPhase) {
            TvShowsPhase.Loading -> TvFeedSkeleton()

            TvShowsPhase.Error -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ErasmusEmptyState(
                    title = "Something went wrong",
                    message = errorMessage ?: "Unable to load series."
                )
                Spacer(modifier = Modifier.height(ErasmusSpacing.Large))
                ErasmusActionButton(
                    text = "Try Again",
                    onClick = { viewModel.loadTvFeed() },
                    style = ErasmusButtonStyle.Primary
                )
            }

            TvShowsPhase.Feed -> {
                val data = latestData ?: return@AnimatedContent

                ErasmusFeedScaffold(
                    currentRoute = NavRoutes.TV,
                    activeProfile = data.activeProfile,
                    heroItem = data.heroShow
                        ?: data.popular.firstOrNull()
                        ?: data.topRated.firstOrNull(),
                    heroFeaturedItems = data.popular,
                    shelves = listOf(
                        FeedShelf("top_10_tv", "Top 10 TV Shows Today", data.popular, isRanked = true),
                        FeedShelf("top_rated_tv", "Top Rated Series", data.topRated)
                    ),
                    onNavigate = onNavigate,
                    onProfileClick = onProfileClick,
                    onMediaClick = onMediaClick,
                    onPlayClick = onPlayClick,
                    onBackFromNav = { onNavigate(NavRoutes.HOME) },
                    state = scaffoldState
                )
            }
        }
    }
}

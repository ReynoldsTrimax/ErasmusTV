package com.erasmustv.app.ui.screens.movies

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

private enum class MoviesPhase { Loading, Error, Feed }

@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isReducedMotion = rememberReducedMotion()

    // Above the phase transition, so scroll position and focus memory survive a
    // refresh and a round trip through a detail page.
    val scaffoldState = rememberFeedScaffoldState()

    val phase = when (uiState) {
        is MoviesUiState.Loading -> MoviesPhase.Loading
        is MoviesUiState.Error -> MoviesPhase.Error
        is MoviesUiState.Success -> MoviesPhase.Feed
    }

    var latestData by remember { mutableStateOf<MoviesData?>(null) }
    (uiState as? MoviesUiState.Success)?.let { latestData = it.data }
    val errorMessage = (uiState as? MoviesUiState.Error)?.message

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
        label = "moviesScreenPhaseTransition"
    ) { currentPhase ->
        when (currentPhase) {
            MoviesPhase.Loading -> TvFeedSkeleton()

            MoviesPhase.Error -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ErasmusEmptyState(
                    title = "Something went wrong",
                    message = errorMessage ?: "Unable to load movies."
                )
                Spacer(modifier = Modifier.height(ErasmusSpacing.Large))
                ErasmusActionButton(
                    text = "Try Again",
                    onClick = { viewModel.loadMoviesFeed() },
                    style = ErasmusButtonStyle.Primary
                )
            }

            MoviesPhase.Feed -> {
                val data = latestData ?: return@AnimatedContent

                ErasmusFeedScaffold(
                    currentRoute = NavRoutes.MOVIES,
                    activeProfile = data.activeProfile,
                    heroItem = data.heroMovie
                        ?: data.popular.firstOrNull()
                        ?: data.topRated.firstOrNull()
                        ?: data.nowPlaying.firstOrNull(),
                    heroFeaturedItems = data.popular,
                    shelves = listOf(
                        FeedShelf("top_10_movies", "Top 10 Movies Today", data.popular, isRanked = true),
                        FeedShelf("now_playing", "Now Playing in Theaters", data.nowPlaying),
                        FeedShelf("top_rated_movies", "Top Rated Movies", data.topRated),
                        FeedShelf("upcoming", "Upcoming Releases", data.upcoming)
                    ),
                    onNavigate = onNavigate,
                    onProfileClick = onProfileClick,
                    onMediaClick = onMediaClick,
                    onPlayClick = onPlayClick,
                    // A secondary browse page: BACK from the navigation returns to
                    // Home rather than leaving the app.
                    onBackFromNav = { onNavigate(NavRoutes.HOME) },
                    state = scaffoldState
                )
            }
        }
    }
}

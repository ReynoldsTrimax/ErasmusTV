package com.erasmustv.app.ui.screens.anime

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

private enum class AnimePhase { Loading, Error, Feed }

@Composable
fun AnimeScreen(
    viewModel: AnimeViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isReducedMotion = rememberReducedMotion()

    val scaffoldState = rememberFeedScaffoldState()

    val phase = when (uiState) {
        is AnimeUiState.Loading -> AnimePhase.Loading
        is AnimeUiState.Error -> AnimePhase.Error
        is AnimeUiState.Success -> AnimePhase.Feed
    }

    var latestData by remember { mutableStateOf<AnimeFeedData?>(null) }
    (uiState as? AnimeUiState.Success)?.let { latestData = it.data }
    val errorMessage = (uiState as? AnimeUiState.Error)?.message

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
        label = "animeScreenPhaseTransition"
    ) { currentPhase ->
        when (currentPhase) {
            AnimePhase.Loading -> TvFeedSkeleton()

            AnimePhase.Error -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ErasmusEmptyState(
                    title = "Something went wrong",
                    message = errorMessage ?: "Unable to load anime."
                )
                Spacer(modifier = Modifier.height(ErasmusSpacing.Large))
                ErasmusActionButton(
                    text = "Try Again",
                    onClick = { viewModel.loadAnimeFeed() },
                    style = ErasmusButtonStyle.Primary
                )
            }

            AnimePhase.Feed -> {
                val data = latestData ?: return@AnimatedContent

                ErasmusFeedScaffold(
                    currentRoute = NavRoutes.ANIME,
                    activeProfile = data.activeProfile,
                    heroItem = data.heroAnime,
                    heroFeaturedItems = data.featuredList,
                    shelves = listOf(
                        FeedShelf("trending_anime", "Trending Anime", data.trendingAnime, isRanked = true),
                        FeedShelf("top_rated_anime", "Top Rated Anime", data.topRatedAnime),
                        FeedShelf("shonen_anime", "Popular Shonen & Action", data.shonenAnime),
                        // Previously wired with no focus requester, no vertical
                        // callbacks, and no focus memory at all — the one shelf on
                        // the page you could fall into but never navigate out of
                        // predictably. Going through the scaffold makes it
                        // identical to every other shelf by construction.
                        FeedShelf("anime_movies", "Anime Movies", data.animeMovies)
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

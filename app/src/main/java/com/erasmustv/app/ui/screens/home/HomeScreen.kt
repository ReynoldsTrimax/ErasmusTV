package com.erasmustv.app.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.data.model.ContinueWatchingItem
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.ContinueWatchingRow
import com.erasmustv.app.ui.components.HeroBillboard
import com.erasmustv.app.ui.components.MediaSectionRow
import com.erasmustv.app.ui.components.RankedSectionRow
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvLeftNavRail
import com.erasmustv.app.ui.navigation.NavRoutes

@OptIn(ExperimentalFoundationApi::class)
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
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FocusWhite)
                }
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(120.dp))
                    Text(
                        text = state.message,
                        style = ErasmusTvTypography.Body,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TvFocusableCard(onClick = { viewModel.loadHomeFeed() }) {
                        Text(
                            text = "Retry",
                            style = ErasmusTvTypography.ButtonText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is HomeUiState.Success -> {
                val data = state.data
                val heroFocusRequester = remember { FocusRequester() }
                val continueWatchingFocusRequester = remember { FocusRequester() }
                val trendingFocusRequester = remember { FocusRequester() }
                val newMoviesFocusRequester = remember { FocusRequester() }
                val popularTvFocusRequester = remember { FocusRequester() }
                val topRatedFocusRequester = remember { FocusRequester() }
                val railFocusRequester = remember { FocusRequester() }

                val allRowRequesters = remember {
                    listOf(
                        heroFocusRequester,
                        continueWatchingFocusRequester,
                        trendingFocusRequester,
                        newMoviesFocusRequester,
                        popularTvFocusRequester,
                        topRatedFocusRequester
                    )
                }

                var activeContentFocusRequester by remember { mutableStateOf(heroFocusRequester) }
                var isRailFocused by remember { mutableStateOf(false) }

                val listState = rememberLazyListState()

                // Active Hero item support for carousel selection
                var selectedHeroItem by remember(data.heroItem?.id) { mutableStateOf(data.heroItem) }

                // Request initial focus exactly once; never steal focus on recomposition or return from details
                var hasRequestedInitialFocus by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!hasRequestedInitialFocus) {
                        try {
                            heroFocusRequester.requestFocus()
                            hasRequestedInitialFocus = true
                        } catch (_: Exception) {}
                    }
                }

                val coroutineScope = rememberCoroutineScope()

                // Safe focus requester helper: ensures LazyColumn scrolls to target row index
                // and requests focus on the target row's focus requester.
                fun navigateToRow(targetIndex: Int, requester: FocusRequester) {
                    coroutineScope.launch {
                        try {
                            listState.animateScrollToItem(targetIndex)
                            kotlinx.coroutines.delay(50)
                            requester.requestFocus()
                        } catch (_: Exception) {
                            try {
                                requester.requestFocus()
                            } catch (_: Exception) {}
                        }
                    }
                }

                // Deterministic Back Button Handling:
                // When in content, pressing BACK moves focus cleanly to the sidebar rail
                androidx.activity.compose.BackHandler(enabled = !isRailFocused) {
                    try {
                        railFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }

                // Vertical spec: exactly matches MediaDetailScreen & provides smooth scrolling.
                // Keeps hero pinned at top, scrolls down smoothly when moving to lower rows,
                // and scrolls back up cleanly when returning.
                val verticalBringIntoViewSpec = remember {
                    object : BringIntoViewSpec {
                        override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                            // Item is ABOVE the viewport (scrolled past top) — must scroll up to reveal it.
                            if (offset < 0f) {
                                return offset
                            }
                            // Item is already fully visible on screen — don't shift scroll
                            if (offset + size <= containerSize) {
                                return 0f
                            }
                            // Item is partially or fully below the viewport — centre it smoothly.
                            val childCenter = offset + (size / 2f)
                            val targetCenter = containerSize * 0.5f
                            return childCenter - targetCenter
                        }
                    }
                }

                val contentShift by animateDpAsState(
                    targetValue = if (isRailFocused) 76.dp else 0.dp,
                    animationSpec = tween(160, easing = FastOutSlowInEasing),
                    label = "homeContentShift"
                )

                CompositionLocalProvider(
                    LocalBringIntoViewSpec provides verticalBringIntoViewSpec
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = contentShift.toPx()
                            },
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        // Full Cinematic Hero Billboard (Reference 4 & 5)
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = heroFocusRequester
                                    }
                                }
                            ) {
                                HeroBillboard(
                                    item = selectedHeroItem ?: data.heroItem,
                                    onPlayClick = onPlayClick,
                                    onDetailsClick = onMediaClick,
                                    heroFocusRequester = heroFocusRequester,
                                    featuredItems = data.trending.take(5),
                                    onFeaturedSelect = { selectedHeroItem = it },
                                    onNavigateLeft = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = {
                                        if (data.continueWatching.isNotEmpty()) {
                                            navigateToRow(1, continueWatchingFocusRequester)
                                        } else {
                                            navigateToRow(1, trendingFocusRequester)
                                        }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Continue Watching row if active items exist
                        if (data.continueWatching.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.onFocusChanged {
                                        if (it.hasFocus) {
                                            activeContentFocusRequester = continueWatchingFocusRequester
                                        }
                                    }
                                ) {
                                    ContinueWatchingRow(
                                        items = data.continueWatching,
                                        onItemClick = onResumeClick,
                                        firstItemFocusRequester = continueWatchingFocusRequester,
                                        onNavigateLeftToRail = {
                                            try {
                                                railFocusRequester.requestFocus()
                                            } catch (_: Exception) {}
                                        },
                                        onNavigateDown = {
                                            navigateToRow(2, trendingFocusRequester)
                                        },
                                        onNavigateUp = {
                                            navigateToRow(0, heroFocusRequester)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }

                        val trendingRowIndex = if (data.continueWatching.isNotEmpty()) 2 else 1
                        val newMoviesRowIndex = trendingRowIndex + 1
                        val popularTvRowIndex = newMoviesRowIndex + 1
                        val topRatedRowIndex = popularTvRowIndex + 1

                        // Ranked Content Rail: "Trending Right Now" with giant graphic numbers 1-10
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = trendingFocusRequester
                                    }
                                }
                            ) {
                                RankedSectionRow(
                                    title = "Trending Right Now",
                                    items = data.trending,
                                    onItemClick = onMediaClick,
                                    onViewAllClick = { onNavigate(NavRoutes.MOVIES) },
                                    firstItemFocusRequester = trendingFocusRequester,
                                    onNavigateLeftToRail = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = {
                                        navigateToRow(newMoviesRowIndex, newMoviesFocusRequester)
                                    },
                                    onNavigateUp = {
                                        if (data.continueWatching.isNotEmpty()) {
                                            navigateToRow(1, continueWatchingFocusRequester)
                                        } else {
                                            navigateToRow(0, heroFocusRequester)
                                        }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(36.dp))
                        }

                        // Content Rail: "New Movies"
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = newMoviesFocusRequester
                                    }
                                }
                            ) {
                                MediaSectionRow(
                                    title = "New Movies",
                                    items = data.popularMovies,
                                    onItemClick = onMediaClick,
                                    onViewAllClick = { onNavigate(NavRoutes.MOVIES) },
                                    firstItemFocusRequester = newMoviesFocusRequester,
                                    onNavigateLeftToRail = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = {
                                        navigateToRow(popularTvRowIndex, popularTvFocusRequester)
                                    },
                                    onNavigateUp = {
                                        navigateToRow(trendingRowIndex, trendingFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(36.dp))
                        }

                        // Content Rail: "Popular TV Shows"
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = popularTvFocusRequester
                                    }
                                }
                            ) {
                                MediaSectionRow(
                                    title = "Popular TV Shows",
                                    items = data.popularTv,
                                    onItemClick = onMediaClick,
                                    onViewAllClick = { onNavigate(NavRoutes.TV) },
                                    firstItemFocusRequester = popularTvFocusRequester,
                                    onNavigateLeftToRail = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = {
                                        navigateToRow(topRatedRowIndex, topRatedFocusRequester)
                                    },
                                    onNavigateUp = {
                                        navigateToRow(newMoviesRowIndex, newMoviesFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(36.dp))
                        }

                        // Top Rated Rail
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = topRatedFocusRequester
                                    }
                                }
                            ) {
                                MediaSectionRow(
                                    title = "Top Rated",
                                    items = data.topRated,
                                    onItemClick = onMediaClick,
                                    firstItemFocusRequester = topRatedFocusRequester,
                                    onNavigateLeftToRail = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = null,
                                    onNavigateUp = {
                                        navigateToRow(popularTvRowIndex, popularTvFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                }

                // Persistent Minimal Left Navigation Rail
                TvLeftNavRail(
                    currentRoute = NavRoutes.HOME,
                    activeProfile = data.activeProfile,
                    onNavigate = onNavigate,
                    onProfileClick = onProfileClick,
                    railFocusRequester = railFocusRequester,
                    onFocusChanged = { focused -> isRailFocused = focused },
                    onNavigateRight = {
                        var success = false
                        try {
                            activeContentFocusRequester.requestFocus()
                            success = true
                        } catch (_: Exception) {
                            success = false
                        }
                        if (!success) {
                            for (req in allRowRequesters) {
                                try {
                                    req.requestFocus()
                                    success = true
                                    break
                                } catch (_: Exception) {}
                            }
                        }
                        if (!success) {
                            coroutineScope.launch {
                                try {
                                    listState.scrollToItem(0)
                                    heroFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                            success = true
                        }
                        success
                    },
                    onReselectCurrent = {
                        coroutineScope.launch {
                            try {
                                listState.animateScrollToItem(0)
                                heroFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }
    }
}

package com.erasmustv.app.ui.screens.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
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
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.HeroBillboard
import com.erasmustv.app.ui.components.MediaSectionRow
import com.erasmustv.app.ui.components.RankedSectionRow
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvLeftNavRail
import com.erasmustv.app.ui.navigation.NavRoutes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvShowsScreen(
    viewModel: TvShowsViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        when (val state = uiState) {
            is TvShowsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FocusWhite)
                }
            }
            is TvShowsUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(120.dp))
                    Text(text = state.message, style = ErasmusTvTypography.Body, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    TvFocusableCard(onClick = { viewModel.loadTvFeed() }) {
                        Text(text = "Retry", style = ErasmusTvTypography.ButtonText, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            is TvShowsUiState.Success -> {
                val data = state.data
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                val heroFocusRequester = remember { FocusRequester() }
                val rankedFocusRequester = remember { FocusRequester() }
                val topRatedFocusRequester = remember { FocusRequester() }
                val railFocusRequester = remember { FocusRequester() }

                val allRowRequesters = remember {
                    listOf(
                        heroFocusRequester,
                        rankedFocusRequester,
                        topRatedFocusRequester
                    )
                }

                var activeContentFocusRequester by remember { mutableStateOf(heroFocusRequester) }
                var isRailFocused by remember { mutableStateOf(false) }

                val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                var selectedHeroItem by remember(data.heroShow?.id) { mutableStateOf(data.heroShow) }

                // Request initial focus once; never steal focus on recomposition or return
                var hasRequestedInitialFocus by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!hasRequestedInitialFocus) {
                        try {
                            heroFocusRequester.requestFocus()
                            hasRequestedInitialFocus = true
                        } catch (_: Exception) {}
                    }
                }

                // Safe focus requester helper: if target focus requester is not yet attached/composed,
                // scroll LazyColumn to that item index first, then request focus.
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
                // When in content, pressing BACK hops focus cleanly to the sidebar rail
                androidx.activity.compose.BackHandler(enabled = !isRailFocused) {
                    try {
                        railFocusRequester.requestFocus()
                    } catch (_: Exception) {
                        onNavigate(NavRoutes.HOME)
                    }
                }
                // When in rail, pressing BACK returns to Home screen
                androidx.activity.compose.BackHandler(enabled = isRailFocused) {
                    onNavigate(NavRoutes.HOME)
                }

                // Vertical spec: exactly matches MediaDetailScreen & provides smooth scrolling.
                val verticalBringIntoViewSpec = remember {
                    object : BringIntoViewSpec {
                        override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                            if (offset < 0f) {
                                return offset
                            }
                            if (offset + size <= containerSize) {
                                return 0f
                            }
                            val childCenter = offset + (size / 2f)
                            val targetCenter = containerSize * 0.5f
                            return childCenter - targetCenter
                        }
                    }
                }

                val contentShift by animateDpAsState(
                    targetValue = if (isRailFocused) 76.dp else 0.dp,
                    animationSpec = tween(160, easing = FastOutSlowInEasing),
                    label = "tvShowsContentShift"
                )

                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides verticalBringIntoViewSpec
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
                        // Full Cinematic Hero Billboard
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = heroFocusRequester
                                    }
                                }
                            ) {
                                HeroBillboard(
                                    item = selectedHeroItem ?: data.heroShow,
                                    onPlayClick = onPlayClick,
                                    onDetailsClick = onMediaClick,
                                    heroFocusRequester = heroFocusRequester,
                                    featuredItems = data.popular.take(5),
                                    onFeaturedSelect = { selectedHeroItem = it },
                                    onNavigateLeft = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = {
                                        navigateToRow(1, rankedFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Top 10 Ranked TV Shows
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = rankedFocusRequester
                                    }
                                }
                            ) {
                                RankedSectionRow(
                                    title = "Top 10 TV Shows Today",
                                    items = data.popular,
                                    onItemClick = onMediaClick,
                                    firstItemFocusRequester = rankedFocusRequester,
                                    onNavigateLeftToRail = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = {
                                        navigateToRow(2, topRatedFocusRequester)
                                    },
                                    onNavigateUp = {
                                        navigateToRow(0, heroFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(36.dp))
                        }

                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = topRatedFocusRequester
                                    }
                                }
                            ) {
                                MediaSectionRow(
                                    title = "Top Rated Series",
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
                                        navigateToRow(1, rankedFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                }

                // Left Nav Rail
                TvLeftNavRail(
                    currentRoute = NavRoutes.TV,
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

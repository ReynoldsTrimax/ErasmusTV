package com.erasmustv.app.ui.screens.watchlist

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.MediaPosterCard
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvLeftNavRail
import com.erasmustv.app.ui.components.TvPivotBringIntoViewSpec
import com.erasmustv.app.ui.navigation.NavRoutes

import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

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
    val railFocusRequester = remember { FocusRequester() }
    var isRailFocused by remember { mutableStateOf(false) }

    val contentShift by animateDpAsState(
        targetValue = if (isRailFocused) 76.dp else 0.dp,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "watchlistContentShift"
    )

    // Deterministic Back Handler: pressing BACK navigates back to Home screen
    BackHandler {
        onNavigate(NavRoutes.HOME)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        when (val state = uiState) {
            is WatchlistUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = TextPrimary,
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            }
            is WatchlistUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 140.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, style = ErasmusTvTypography.Body, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    TvFocusableCard(
                        onClick = { viewModel.loadWatchlist() },
                        modifier = Modifier.focusRequester(contentFocusRequester)
                    ) {
                        Text(text = "Retry", style = ErasmusTvTypography.ButtonText, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            is WatchlistUiState.Success -> {
                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Your Watchlist is empty",
                                style = ErasmusTvTypography.SectionTitle.copy(fontSize = 24.sp),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Add movies and TV shows from their detail pages to watch later.",
                                style = ErasmusTvTypography.Body,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = contentShift.toPx()
                            }
                            .padding(start = 64.dp, top = 36.dp, end = 48.dp)
                    ) {
                        Text(
                            text = "My List (${state.items.size})",
                            style = ErasmusTvTypography.SectionTitle.copy(fontSize = 24.sp),
                            modifier = Modifier.padding(bottom = 18.dp)
                        )

                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.5f) }
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(state.items, key = { _, it -> "${it.mediaType}:${it.id}" }) { index, item ->
                                    val isFirstCol = index % 5 == 0
                                    Box(
                                        modifier = Modifier
                                            .then(if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier)
                                            .onKeyEvent { event ->
                                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && isFirstCol) {
                                                    try {
                                                        railFocusRequester.requestFocus()
                                                        true
                                                    } catch (_: Exception) {
                                                        false
                                                    }
                                                } else false
                                            }
                                    ) {
                                        MediaPosterCard(
                                            item = item,
                                            onClick = { onMediaClick(item) },
                                            cardWidth = 140
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Persistent Left Nav Rail
        val activeProfile = (uiState as? WatchlistUiState.Success)?.activeProfile
        TvLeftNavRail(
            currentRoute = NavRoutes.WATCHLIST,
            activeProfile = activeProfile,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            railFocusRequester = railFocusRequester,
            onFocusChanged = { isRailFocused = it },
            onNavigateRight = {
                try {
                    contentFocusRequester.requestFocus()
                    true
                } catch (_: Exception) {
                    false
                }
            },
            onReselectCurrent = {
                try {
                    contentFocusRequester.requestFocus()
                } catch (_: Exception) {}
            },
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}


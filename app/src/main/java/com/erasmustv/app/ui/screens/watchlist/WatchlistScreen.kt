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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.RatingGold
import com.erasmustv.app.core.theme.SurfaceElevated
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.MediaPosterCard
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvGridSkeleton
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

    // Request initial focus on first load; never steal focus if rail is already active
    var hasRequestedInitialFocus by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (!hasRequestedInitialFocus && !isRailFocused && uiState is WatchlistUiState.Success) {
            try {
                contentFocusRequester.requestFocus()
                hasRequestedInitialFocus = true
            } catch (_: Exception) {}
        }
    }

    // Deterministic Back Handler:
    // When in content, pressing BACK hops focus cleanly to the sidebar rail
    BackHandler(enabled = !isRailFocused) {
        try {
            railFocusRequester.requestFocus()
        } catch (_: Exception) {
            onNavigate(NavRoutes.HOME)
        }
    }
    // When in rail, pressing BACK returns to Home screen
    BackHandler(enabled = isRailFocused) {
        onNavigate(NavRoutes.HOME)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        when (val state = uiState) {
            is WatchlistUiState.Loading -> {
                TvGridSkeleton(contentShift = contentShift)
            }
            is WatchlistUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 64.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, style = ErasmusTvTypography.Body, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    TvFocusableCard(
                        onClick = { viewModel.loadWatchlist() },
                        shape = RectangleShape,
                        focusedScale = 1.025f,
                        focusedBorderColor = FocusWhite,
                        focusedBorderWidth = 1.5.dp,
                        modifier = Modifier.focusRequester(contentFocusRequester)
                    ) { isFocused ->
                        Text(
                            text = "Retry",
                            style = ErasmusTvTypography.ButtonText,
                            color = if (isFocused) PitchBlack else TextPrimary,
                            modifier = Modifier
                                .background(if (isFocused) Color.White else SurfaceElevated, RectangleShape)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }
            is WatchlistUiState.Success -> {
                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = contentShift.toPx()
                            }
                            .padding(start = 64.dp, end = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(SurfaceElevated, RectangleShape)
                                    .border(1.dp, BorderHairline, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = RatingGold,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "MY LIST",
                                style = ErasmusTvTypography.Badge.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 1.6.sp,
                                    color = RatingGold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your Watchlist is empty",
                                style = ErasmusTvTypography.SectionTitle.copy(fontSize = 22.sp),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Explore movies and TV shows to add them to your watchlist.",
                                style = ErasmusTvTypography.Body.copy(fontSize = 13.sp),
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            TvFocusableCard(
                                onClick = { onNavigate(NavRoutes.HOME) },
                                shape = RectangleShape,
                                focusedScale = 1.025f,
                                focusedBorderColor = FocusWhite,
                                focusedBorderWidth = 1.5.dp,
                                contentDescription = "Explore Movies & Shows",
                                role = androidx.compose.ui.semantics.Role.Button,
                                modifier = Modifier
                                    .focusRequester(contentFocusRequester)
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionLeft -> {
                                                    try {
                                                        railFocusRequester.requestFocus()
                                                        true
                                                    } catch (_: Exception) {
                                                        false
                                                    }
                                                }
                                                Key.DirectionUp, Key.DirectionRight, Key.DirectionDown -> true
                                                else -> false
                                            }
                                        } else false
                                    }
                            ) { isFocused ->
                                Row(
                                    modifier = Modifier
                                        .background(
                                            color = if (isFocused) Color.White else SurfaceElevated,
                                            shape = RectangleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isFocused) Color.Transparent else BorderHairline,
                                            shape = RectangleShape
                                        )
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = if (isFocused) PitchBlack else TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Explore Movies & Shows",
                                        style = ErasmusTvTypography.ButtonText,
                                        color = if (isFocused) PitchBlack else TextPrimary
                                    )
                                }
                            }
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
                                    val isTopRow = index < 5
                                    val isBottomRow = index >= (state.items.size - ((state.items.size - 1) % 5 + 1))
                                    Box(
                                        modifier = Modifier
                                            .then(if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier)
                                            .onKeyEvent { event ->
                                                if (event.type == KeyEventType.KeyDown) {
                                                    when (event.key) {
                                                        Key.DirectionLeft -> {
                                                            if (isFirstCol) {
                                                                try {
                                                                    railFocusRequester.requestFocus()
                                                                    true
                                                                } catch (_: Exception) {
                                                                    false
                                                                }
                                                            } else false
                                                        }
                                                        Key.DirectionUp -> {
                                                            if (isTopRow) true else false
                                                        }
                                                        Key.DirectionDown -> {
                                                            if (isBottomRow) true else false
                                                        }
                                                        else -> false
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


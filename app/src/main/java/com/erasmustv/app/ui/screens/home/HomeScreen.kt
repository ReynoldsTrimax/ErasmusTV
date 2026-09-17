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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.erasmustv.app.ui.components.TvFeedSkeleton
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvLeftNavRail
import com.erasmustv.app.ui.navigation.NavRoutes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.rememberReducedMotion

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
    val isReducedMotion = rememberReducedMotion()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                if (isReducedMotion) {
                    androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                } else {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = TvMotion.DURATION_ENTER,
                            easing = TvMotion.EasingSilk
                        )
                    ) togetherWith fadeOut(
                        animationSpec = tween(
                            durationMillis = TvMotion.DURATION_FAST,
                            easing = TvMotion.EasingSilk
                        )
                    )
                }
            },
            label = "homeScreenFeedTransition"
        ) { state ->
            when (state) {
                is HomeUiState.Loading -> {
                    TvFeedSkeleton()
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        val data = state.data
                        val heroFocusRequester = remember { FocusRequester() }
                        val continueWatchingFocusRequester = remember { FocusRequester() }
                        val railFocusRequester = remember { FocusRequester() }

                        val sectionFocusRequesters = remember(data.sections.size) {
                            List(data.sections.size) { FocusRequester() }
                        }
                        val sectionsStartIndex = if (data.continueWatching.isNotEmpty()) 2 else 1

                        val allRowRequesters = remember(sectionFocusRequesters.size) {
                            listOf(heroFocusRequester, continueWatchingFocusRequester) + sectionFocusRequesters
                        }

                        var activeContentFocusRequester by remember { mutableStateOf(heroFocusRequester) }
                        var isRailFocused by remember { mutableStateOf(false) }

                        val listState = rememberLazyListState()

                        // Active Hero item support for carousel selection with fallback cascading
                        val initialHero = data.heroItem
                            ?: data.sections.firstOrNull()?.items?.firstOrNull()
                            ?: data.trending.firstOrNull()
                            ?: data.popularMovies.firstOrNull()
                            ?: data.popularTv.firstOrNull()
                            ?: data.topRated.firstOrNull()
                        var selectedHeroItem by remember(initialHero?.id) {
                            mutableStateOf(initialHero)
                        }

                        // Request initial focus exactly once; never steal focus on recomposition or return from details
                        var hasRequestedInitialFocus by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            if (!hasRequestedInitialFocus) {
                                kotlinx.coroutines.delay(60)
                                try {
                                    if (initialHero != null) {
                                        heroFocusRequester.requestFocus()
                                    } else if (data.continueWatching.isNotEmpty()) {
                                        continueWatchingFocusRequester.requestFocus()
                                    } else if (sectionFocusRequesters.isNotEmpty()) {
                                        sectionFocusRequesters[0].requestFocus()
                                    } else {
                                        railFocusRequester.requestFocus()
                                    }
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
                                    item = selectedHeroItem ?: initialHero,
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
                                        } else if (sectionFocusRequesters.isNotEmpty()) {
                                            navigateToRow(sectionsStartIndex, sectionFocusRequesters[0])
                                        }
                                    },
                                    isAutoAdvanceEnabled = listState.firstVisibleItemIndex == 0
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
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
                                            if (sectionFocusRequesters.isNotEmpty()) {
                                                navigateToRow(sectionsStartIndex, sectionFocusRequesters[0])
                                            }
                                        },
                                        onNavigateUp = {
                                            navigateToRow(0, heroFocusRequester)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(22.dp))
                            }
                        }

                        // Dynamic 20+ Curated Catalog Sections
                        itemsIndexed(
                            items = data.sections,
                            key = { _, section -> section.id }
                        ) { sIndex, section ->
                            val currentRequester = sectionFocusRequesters.getOrElse(sIndex) { heroFocusRequester }
                            val targetDownRequester = if (sIndex < sectionFocusRequesters.size - 1) sectionFocusRequesters[sIndex + 1] else null
                            val targetUpRequester = if (sIndex > 0) {
                                sectionFocusRequesters[sIndex - 1]
                            } else if (data.continueWatching.isNotEmpty()) {
                                continueWatchingFocusRequester
                            } else {
                                heroFocusRequester
                            }
                            val targetUpIndex = if (sIndex > 0) sectionsStartIndex + sIndex - 1 else if (data.continueWatching.isNotEmpty()) 1 else 0
                            val targetDownIndex = sectionsStartIndex + sIndex + 1

                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = currentRequester
                                    }
                                }
                            ) {
                                if (section.isRanked) {
                                    RankedSectionRow(
                                        title = section.title,
                                        items = section.items,
                                        onItemClick = onMediaClick,
                                        onViewAllClick = section.viewAllRoute?.let { route -> { onNavigate(route) } },
                                        firstItemFocusRequester = currentRequester,
                                        onNavigateLeftToRail = {
                                            try {
                                                railFocusRequester.requestFocus()
                                            } catch (_: Exception) {}
                                        },
                                        onNavigateDown = if (targetDownRequester != null) {
                                            { navigateToRow(targetDownIndex, targetDownRequester) }
                                        } else null,
                                        onNavigateUp = {
                                            navigateToRow(targetUpIndex, targetUpRequester)
                                        }
                                    )
                                } else {
                                    MediaSectionRow(
                                        title = section.title,
                                        items = section.items,
                                        onItemClick = onMediaClick,
                                        onViewAllClick = section.viewAllRoute?.let { route -> { onNavigate(route) } },
                                        firstItemFocusRequester = currentRequester,
                                        showNewBadge = section.showNewBadge,
                                        onNavigateLeftToRail = {
                                            try {
                                                railFocusRequester.requestFocus()
                                            } catch (_: Exception) {}
                                        },
                                        onNavigateDown = if (targetDownRequester != null) {
                                            { navigateToRow(targetDownIndex, targetDownRequester) }
                                        } else null,
                                        onNavigateUp = {
                                            navigateToRow(targetUpIndex, targetUpRequester)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
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
}
}



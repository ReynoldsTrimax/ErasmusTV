package com.erasmustv.app.ui.screens.anime

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.RatingGold
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.HeroBillboard
import com.erasmustv.app.ui.components.MediaSectionRow
import com.erasmustv.app.ui.components.RankedSectionRow
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvLeftNavRail
import com.erasmustv.app.ui.components.TvPivotBringIntoViewSpec
import com.erasmustv.app.ui.navigation.NavRoutes
import kotlinx.coroutines.launch

private val AnimeHeroHorizontalGradient = Brush.horizontalGradient(
    colors = listOf(
        PitchBlack,
        PitchBlack.copy(alpha = 0.98f),
        PitchBlack.copy(alpha = 0.88f),
        PitchBlack.copy(alpha = 0.65f),
        PitchBlack.copy(alpha = 0.35f),
        Color.Transparent
    )
)

private val AnimeHeroVerticalGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Transparent,
        PitchBlack.copy(alpha = 0.35f),
        PitchBlack.copy(alpha = 0.75f),
        PitchBlack.copy(alpha = 0.96f),
        PitchBlack
    )
)

/**
 * Anime Page matching reference screenshot bingr.one/anime.
 * Features:
 * - Cinematic hero billboard with Attack on Titan / featured anime
 * - Japanese title subtext & badge metadata
 * - Content rows below: Trending Anime, Top Rated Anime, Popular Shonen, Anime Movies
 * - Full center-pivot scrolling and D-pad TV navigation
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeScreen(
    viewModel: AnimeViewModel,
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
            is AnimeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FocusWhite)
                }
            }
            is AnimeUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = state.message, style = ErasmusTvTypography.Body, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    TvFocusableCard(onClick = { viewModel.loadAnimeFeed() }) {
                        Text(text = "Retry", style = ErasmusTvTypography.ButtonText, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            is AnimeUiState.Success -> {
                val data = state.data
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                val heroPlayFocusRequester = remember { FocusRequester() }
                val heroSeeMoreFocusRequester = remember { FocusRequester() }
                val trendingFocusRequester = remember { FocusRequester() }
                val topRatedFocusRequester = remember { FocusRequester() }
                val shonenFocusRequester = remember { FocusRequester() }
                val railFocusRequester = remember { FocusRequester() }

                val allRowRequesters = remember {
                    listOf(
                        heroPlayFocusRequester,
                        trendingFocusRequester,
                        topRatedFocusRequester,
                        shonenFocusRequester
                    )
                }

                var activeContentFocusRequester by remember { mutableStateOf(heroPlayFocusRequester) }
                var isRailFocused by remember { mutableStateOf(false) }

                val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                var selectedHeroItem by remember(data.heroAnime?.id) { mutableStateOf(data.heroAnime) }

                // Request initial focus once; never steal focus on recomposition or return
                var hasRequestedInitialFocus by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!hasRequestedInitialFocus) {
                        try {
                            heroPlayFocusRequester.requestFocus()
                            hasRequestedInitialFocus = true
                        } catch (_: Exception) {}
                    }
                }

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
                // When in content or rail, pressing BACK returns to Home screen
                androidx.activity.compose.BackHandler {
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
                    label = "animeContentShift"
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
                        // Top Hero Billboard with Anime Branding & Carousel
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = heroPlayFocusRequester
                                    }
                                }
                            ) {
                                HeroBillboard(
                                    item = selectedHeroItem ?: data.heroAnime,
                                    onPlayClick = onPlayClick,
                                    onDetailsClick = onMediaClick,
                                    heroFocusRequester = heroPlayFocusRequester,
                                    featuredItems = data.trendingAnime.take(5),
                                    onFeaturedSelect = { selectedHeroItem = it },
                                    onNavigateLeft = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = {
                                        navigateToRow(1, trendingFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Ranked Content Rail: "Trending Anime"
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = trendingFocusRequester
                                    }
                                }
                            ) {
                                RankedSectionRow(
                                    title = "Trending Anime",
                                    items = data.trendingAnime,
                                    onItemClick = onMediaClick,
                                    firstItemFocusRequester = trendingFocusRequester,
                                    onNavigateLeftToRail = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = {
                                        navigateToRow(2, topRatedFocusRequester)
                                    },
                                    onNavigateUp = {
                                        navigateToRow(0, heroPlayFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(36.dp))
                        }

                        // Top Rated Anime
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = topRatedFocusRequester
                                    }
                                }
                            ) {
                                MediaSectionRow(
                                    title = "Top Rated Anime",
                                    items = data.topRatedAnime,
                                    onItemClick = onMediaClick,
                                    firstItemFocusRequester = topRatedFocusRequester,
                                    onNavigateLeftToRail = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = {
                                        navigateToRow(3, shonenFocusRequester)
                                    },
                                    onNavigateUp = {
                                        navigateToRow(1, trendingFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(36.dp))
                        }

                        // Popular Shonen & Action Anime
                        item {
                            Box(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        activeContentFocusRequester = shonenFocusRequester
                                    }
                                }
                            ) {
                                MediaSectionRow(
                                    title = "Popular Shonen & Action",
                                    items = data.shonenAnime,
                                    onItemClick = onMediaClick,
                                    firstItemFocusRequester = shonenFocusRequester,
                                    onNavigateLeftToRail = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    },
                                    onNavigateDown = null,
                                    onNavigateUp = {
                                        navigateToRow(2, topRatedFocusRequester)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(36.dp))
                        }

                        // Anime Movies (if any)
                        if (data.animeMovies.isNotEmpty()) {
                            item {
                                MediaSectionRow(
                                    title = "Anime Movies",
                                    items = data.animeMovies,
                                    onItemClick = onMediaClick,
                                    onNavigateLeftToRail = {
                                        try {
                                            railFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    }
                                )
                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }
                    }
                }

                // Persistent Left Navigation Rail
                TvLeftNavRail(
                    currentRoute = NavRoutes.ANIME,
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
                                    heroPlayFocusRequester.requestFocus()
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
                                heroPlayFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }
    }
}

package com.erasmustv.app.ui.screens.studios

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.erasmustv.app.R
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.StudioAppleWhite
import com.erasmustv.app.core.theme.StudioDisneyBlue
import com.erasmustv.app.core.theme.StudioHboPurple
import com.erasmustv.app.core.theme.StudioHotstarCyan
import com.erasmustv.app.core.theme.StudioHuluGreen
import com.erasmustv.app.core.theme.StudioNetflixRed
import com.erasmustv.app.core.theme.StudioParamountBlue
import com.erasmustv.app.core.theme.StudioPeacockYellow
import com.erasmustv.app.core.theme.StudioPrimeBlue
import com.erasmustv.app.core.theme.SurfaceElevated
import com.erasmustv.app.core.theme.SurfaceStudioCard
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.MediaPosterCard
import com.erasmustv.app.ui.components.TvFeedSkeleton
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvLeftNavRail
import com.erasmustv.app.ui.components.TvPivotBringIntoViewSpec
import com.erasmustv.app.ui.navigation.NavRoutes

val STUDIOS_DATA = listOf(
    StudioInfo("netflix", "Netflix", "NETFLIX", "Netflix", providerId = 8, logoDrawableRes = R.drawable.ic_studio_netflix),
    StudioInfo("hulu", "Hulu", "hulu", "Hulu", providerId = 15, logoDrawableRes = R.drawable.ic_studio_hulu),
    StudioInfo("prime", "Prime Video", "prime video", "Amazon", providerId = 9, logoDrawableRes = R.drawable.ic_studio_prime),
    StudioInfo("appletv", "Apple TV+", "Apple tv+", "Apple", providerId = 350, logoDrawableRes = R.drawable.ic_studio_appletv),
    StudioInfo("disney", "Disney+", "Disney+", "Disney", providerId = 337, logoDrawableRes = R.drawable.ic_studio_disney),
    StudioInfo("hbo", "HBO Max", "HBO\nmax", "HBO", providerId = 1899, logoDrawableRes = R.drawable.ic_studio_hbo),
    StudioInfo("peacock", "Peacock", "peacock", "Universal", providerId = 386, logoDrawableRes = R.drawable.ic_studio_peacock),
    StudioInfo("paramount", "Paramount+", "Paramount+", "Paramount", providerId = 531, logoDrawableRes = R.drawable.ic_studio_paramount),
    StudioInfo("hotstar", "Hotstar Specials", "hotstar\nspecials", "Marvel", providerId = 122, logoDrawableRes = R.drawable.ic_studio_hotstar)
)

/**
 * Studios & Channels Screen matching Reference screenshots 1 & 2.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudiosScreen(
    viewModel: StudiosViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val isReducedMotion = com.erasmustv.app.core.theme.rememberReducedMotion()
    val catalogFirstItemRequester = remember { FocusRequester() }

    val studioFocusRequesters = remember {
        STUDIOS_DATA.associate { it.id to FocusRequester() }
    }
    val railFocusRequester = remember { FocusRequester() }
    var isRailFocused by remember { mutableStateOf(false) }

    val contentShift by animateDpAsState(
        targetValue = if (isRailFocused) 76.dp else 0.dp,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "studiosContentShift"
    )

    // Hardware/Remote BACK Button Handler: Return from StudioCatalog to Hub
    androidx.activity.compose.BackHandler(enabled = uiState is StudiosUiState.StudioCatalog) {
        viewModel.backToHub()
    }

    // Return from Hub to Sidebar (or Home)
    androidx.activity.compose.BackHandler(enabled = uiState is StudiosUiState.Hub && !isRailFocused) {
        try {
            railFocusRequester.requestFocus()
        } catch (_: Exception) {
            onNavigate(NavRoutes.HOME)
        }
    }

    androidx.activity.compose.BackHandler(enabled = uiState is StudiosUiState.Hub && isRailFocused) {
        onNavigate(NavRoutes.HOME)
    }

    // Deterministic Focus Restoration: When returning to Hub, restore focus to the previously selected studio
    LaunchedEffect(uiState) {
        if (uiState is StudiosUiState.Hub) {
            val target = studioFocusRequesters[viewModel.lastSelectedStudioId]
                ?: studioFocusRequesters[STUDIOS_DATA.first().id]
            try {
                target?.requestFocus()
            } catch (_: Exception) {}
        } else if (uiState is StudiosUiState.StudioCatalog) {
            try {
                catalogFirstItemRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                if (isReducedMotion) {
                    androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                } else {
                    androidx.compose.animation.fadeIn(
                        androidx.compose.animation.core.tween(
                            durationMillis = com.erasmustv.app.core.theme.TvMotion.DURATION_ENTER,
                            easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
                        )
                    ) togetherWith androidx.compose.animation.fadeOut(
                        androidx.compose.animation.core.tween(
                            durationMillis = com.erasmustv.app.core.theme.TvMotion.DURATION_FAST,
                            easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
                        )
                    )
                }
            },
            label = "studiosStateTransition"
        ) { state ->
            when (state) {
                is StudiosUiState.Hub -> {
                    // Studios Grid View (Reference Screenshot 1)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = contentShift.toPx()
                            }
                            .padding(start = 64.dp, top = 36.dp, end = 44.dp)
                    ) {
                        Text(
                            text = "Studios",
                            style = ErasmusTvTypography.HeroTitleLarge.copy(fontSize = 32.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        )

                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.5f) }
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 36.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(STUDIOS_DATA, key = { _, it -> it.id }) { index, studio ->
                                    val requester = studioFocusRequesters[studio.id]
                                    val isFirstColumn = (index % 3 == 0)
                                    val isTopRow = index < 3
                                    val isBottomRow = index >= (STUDIOS_DATA.size - ((STUDIOS_DATA.size - 1) % 3 + 1))
                                    StudioTile(
                                        studio = studio,
                                        onClick = { viewModel.selectStudio(studio) },
                                        onNavigateLeftToRail = {
                                            try {
                                                railFocusRequester.requestFocus()
                                            } catch (_: Exception) {}
                                        },
                                        isFirstColumn = isFirstColumn,
                                        isTopRow = isTopRow,
                                        isBottomRow = isBottomRow,
                                        modifier = if (requester != null) Modifier.focusRequester(requester) else Modifier
                                    )
                                }
                            }
                        }
                    }
                }
                is StudiosUiState.Loading -> {
                    TvFeedSkeleton(contentShift = contentShift)
                }
                is StudiosUiState.StudioCatalog -> {
                    // Studio Specific Catalog (Reference Screenshot 2)
                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.5f) }
                    ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = contentShift.toPx()
                            }
                            .padding(start = 64.dp, top = 28.dp, end = 44.dp)
                    ) {
                        item {
                            // Header with Back arrow & Studio Title (Sharp styling)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                TvFocusableCard(
                                    onClick = { viewModel.backToHub() },
                                    shape = RectangleShape,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .focusRequester(catalogFirstItemRequester),
                                    focusedBorderColor = FocusWhite
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (isFocused) Color(0x33FFFFFF) else SurfaceElevated, RectangleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = FocusWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = state.studio.name,
                                    style = ErasmusTvTypography.HeroTitleLarge.copy(fontSize = 32.sp)
                                )
                            }
                        }

                        // "Trending on [Studio]" Row with Movies / Series toggle (Reference 2)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Trending on ${state.studio.name}",
                                    style = ErasmusTvTypography.SectionTitle
                                )

                                // Movies / Series Tab Toggle
                                Row(
                                    modifier = Modifier
                                        .background(Color(0x18FFFFFF), RectangleShape)
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    StudioFilterTab(
                                        label = "Movies",
                                        isSelected = state.isShowingMovies,
                                        onClick = { viewModel.toggleFilter(true) }
                                    )
                                    StudioFilterTab(
                                        label = "Series",
                                        isSelected = !state.isShowingMovies,
                                        onClick = { viewModel.toggleFilter(false) }
                                    )
                                }
                            }

                            // Horizontal Poster Row
                            val currentList = if (state.isShowingMovies) state.movieItems else state.tvItems
                            CompositionLocalProvider(
                                LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.42f) }
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(bottom = 28.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(currentList, key = { "${it.mediaType}:${it.id}" }) { item ->
                                        MediaPosterCard(
                                            item = item,
                                            onClick = { onMediaClick(item) },
                                            cardWidth = 140
                                        )
                                    }
                                }
                            }
                        }

                        // Popular Releases Grid
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Popular on ${state.studio.name}",
                                style = ErasmusTvTypography.SectionTitle,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val allItems = state.trendingItems
                            CompositionLocalProvider(
                                LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.42f) }
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(bottom = 44.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(allItems.reversed(), key = { "all:${it.mediaType}:${it.id}" }) { item ->
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
        }

        // Persistent Left Navigation Rail
        TvLeftNavRail(
            currentRoute = NavRoutes.STUDIOS,
            activeProfile = activeProfile,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            railFocusRequester = railFocusRequester,
            onFocusChanged = { isRailFocused = it },
            onNavigateRight = {
                try {
                    if (uiState is StudiosUiState.Hub) {
                        val target = studioFocusRequesters[viewModel.lastSelectedStudioId]
                            ?: studioFocusRequesters[STUDIOS_DATA.first().id]
                        target?.requestFocus()
                        true
                    } else {
                        catalogFirstItemRequester.requestFocus()
                        true
                    }
                } catch (_: Exception) {
                    false
                }
            },
            onReselectCurrent = {
                if (uiState is StudiosUiState.StudioCatalog) {
                    viewModel.backToHub()
                } else if (uiState is StudiosUiState.Hub) {
                    val target = studioFocusRequesters[viewModel.lastSelectedStudioId]
                        ?: studioFocusRequesters[STUDIOS_DATA.first().id]
                    try {
                        target?.requestFocus()
                    } catch (_: Exception) {}
                }
            },
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
private fun StudioTile(
    studio: StudioInfo,
    onClick: () -> Unit,
    onNavigateLeftToRail: (() -> Unit)? = null,
    isFirstColumn: Boolean = false,
    isTopRow: Boolean = false,
    isBottomRow: Boolean = false,
    modifier: Modifier = Modifier
) {
    TvFocusableCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp) // Cinematic tile height with generous breathing room (Recommendation #12)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft -> {
                            if (isFirstColumn && onNavigateLeftToRail != null) {
                                onNavigateLeftToRail()
                                true
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
            },
        shape = RectangleShape,
        focusedScale = com.erasmustv.app.core.theme.TvMotion.FocusScaleStudio,
        focusedBorderColor = FocusWhite,
        focusedBorderWidth = 2.dp
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .background(
                    if (isFocused) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF282834), Color(0xFF1A1A22))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFF141418), Color(0xFF0C0C0F))
                        )
                    }
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) FocusWhite else Color(0x1AFFFFFF),
                    shape = RectangleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            StudioTileContent(studio = studio, isFocused = isFocused)
        }
    }
}

@Composable
private fun StudioTileContent(studio: StudioInfo, isFocused: Boolean) {
    val isReducedMotion = com.erasmustv.app.core.theme.rememberReducedMotion()
    val logoScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) 1.05f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = com.erasmustv.app.core.theme.TvMotion.DURATION_FAST,
            easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
        ),
        label = "studioLogoScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .graphicsLayer {
                scaleX = logoScale
                scaleY = logoScale
            }
    ) {
        if (studio.logoDrawableRes != null) {
            // Normalized logo container: max-width 55%, max-height 46%, contain (Recommendation #13)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = studio.logoDrawableRes),
                    contentDescription = studio.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .fillMaxHeight(0.46f)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = studio.name.uppercase(),
                    style = ErasmusTvTypography.HeroTitleLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = if (isFocused) FocusWhite else TextPrimary
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ORIGINALS",
                    style = ErasmusTvTypography.Badge.copy(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isFocused) FocusWhite.copy(alpha = 0.85f) else TextMuted
                    )
                )
            }
        }
    }
}

@Composable
private fun StudioFilterTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .clip(RectangleShape)
            .background(
                when {
                    isSelected -> FocusWhite
                    isFocused -> Color(0x33FFFFFF)
                    else -> Color.Transparent
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = ErasmusTvTypography.Badge.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) PitchBlack else if (isFocused) FocusWhite else TextSecondary
        )
    }
}

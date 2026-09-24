package com.erasmustv.app.ui.screens.studios

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.R
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.ErasmusSpacing
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceCardFocused
import com.erasmustv.app.core.theme.SurfaceCardRest
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.rememberReducedMotion
import com.erasmustv.app.core.theme.rememberScreenHorizontalMargin
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.ErasmusChip
import com.erasmustv.app.ui.components.ErasmusContentRail
import com.erasmustv.app.ui.components.ErasmusPageHeader
import com.erasmustv.app.ui.components.MediaPosterCard
import com.erasmustv.app.ui.components.TvFloatingNavBar
import com.erasmustv.app.core.theme.TvSpring
import com.erasmustv.app.core.theme.floatSpec
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvPivotBringIntoViewSpec
import com.erasmustv.app.ui.components.TvRailsSkeleton
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.erasmustv.app.ui.focus.GridFocusHandle
import com.erasmustv.app.ui.focus.RailZone
import com.erasmustv.app.ui.focus.SpatialDirection
import com.erasmustv.app.ui.focus.gridFocusContainer
import com.erasmustv.app.ui.focus.gridFocusItem
import com.erasmustv.app.ui.focus.railFocusItem
import com.erasmustv.app.ui.focus.rememberFeedFocusCoordinator
import com.erasmustv.app.ui.focus.rememberFocusZoneMemory
import com.erasmustv.app.ui.focus.rememberGridFocusHandle
import com.erasmustv.app.ui.focus.rememberRailFocusHandleStore
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

private const val STUDIO_GRID_COLUMNS = 3

/**
 * ERASMUS STUDIOS.
 *
 * Two states in one screen: a tile grid of channels, and a selected channel's
 * catalogue. The grid is a fixed three columns because the channel set is a
 * fixed nine — a 3x3 arrangement fills the safe area exactly, with no ragged
 * final row and no dead band at the bottom.
 *
 * Studio marks come from the application's own bundled drawables; no external
 * or placeholder brand artwork is introduced here.
 */
@OptIn(ExperimentalFoundationApi::class)
/** Focus-engine zone keys for the Studios hub and a studio catalogue. */
private const val ZONE_STUDIO_GRID = "__studio_grid__"
private const val ZONE_CATALOG_TRENDING = "__studio_catalog_trending__"
private const val ZONE_CATALOG_POPULAR = "__studio_catalog_popular__"

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
    val isReducedMotion = rememberReducedMotion()
    val horizontalMargin = rememberScreenHorizontalMargin()

    val catalogFirstItemRequester = remember { FocusRequester() }
    // ------------------------------------------------------------------
    // Focus engine.
    //
    // The hub is a grid, so all four directions are resolved against measured
    // cell bounds. That is what makes UP/DOWN column-preserving and makes the
    // ragged final row behave — DOWN from column 3 of a nine-tile 3x3 grid lands
    // on the tile directly beneath it, and RIGHT on the last tile of a row stops
    // rather than wrapping to the start of the next one.
    //
    // The catalogue is a feed of rails, so it uses the same coordinator the
    // browse screens do.
    // ------------------------------------------------------------------
    val coroutineScope = rememberCoroutineScope()
    val hubGridState = rememberLazyGridState()
    val hubGridHandle = rememberGridFocusHandle(ZONE_STUDIO_GRID)

    val catalogListState = rememberLazyListState()
    val catalogMemory = rememberFocusZoneMemory()
    val catalogCoordinator = rememberFeedFocusCoordinator(
        catalogListState,
        coroutineScope,
        catalogMemory
    )
    val catalogHandles = rememberRailFocusHandleStore()

    val navFocusRequester = remember { FocusRequester() }
    var isNavFocused by remember { mutableStateOf(false) }

    // BACK inside a catalogue returns to the hub before it ever leaves Studios.
    androidx.activity.compose.BackHandler(enabled = uiState is StudiosUiState.StudioCatalog) {
        viewModel.backToHub()
    }
    androidx.activity.compose.BackHandler(
        enabled = uiState is StudiosUiState.Hub && !isNavFocused
    ) {
        try {
            navFocusRequester.requestFocus()
        } catch (_: Exception) {
            onNavigate(NavRoutes.HOME)
        }
    }
    androidx.activity.compose.BackHandler(
        enabled = uiState is StudiosUiState.Hub && isNavFocused
    ) {
        onNavigate(NavRoutes.HOME)
    }

    /** Index of the studio the user last opened, so the hub restores to it. */
    val lastStudioIndex = STUDIOS_DATA.indexOfFirst { it.id == viewModel.lastSelectedStudioId }
        .coerceAtLeast(0)

    // Focus follows the hub/catalogue *transition*, not every state emission.
    //
    // The previous `LaunchedEffect(uiState)` re-fired on every StudioCatalog copy
    // — including the one produced by pressing the Movies/Series filter chip —
    // which yanked focus off the chip and onto the Back arrow mid-interaction.
    // Keying on the branch means a filter change leaves focus exactly where the
    // user put it.
    val isHub = uiState is StudiosUiState.Hub
    LaunchedEffect(isHub) {
        if (isHub) {
            hubGridHandle.focusIndexOrNearest(lastStudioIndex)
        } else {
            catalogCoordinator.enterContent()
        }
    }

    val focusNav: () -> Boolean = {
        runCatching { navFocusRequester.requestFocus() }.isSuccess
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                if (isReducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(
                        tween(TvMotion.DURATION_ENTER, easing = TvMotion.EasingSilk)
                    ) togetherWith fadeOut(
                        tween(TvMotion.DURATION_FAST, easing = TvMotion.EasingSilk)
                    )
                }
            },
            label = "studiosStateTransition"
        ) { state ->
            when (state) {
                is StudiosUiState.Hub -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = horizontalMargin,
                                top = ErasmusDimens.NavPillContentClearance,
                                end = horizontalMargin
                            )
                    ) {
                        ErasmusPageHeader(
                            title = "Studios",
                            subtitle = "Browse by channel"
                        )

                        Spacer(modifier = Modifier.height(ErasmusSpacing.Large))

                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.5f) }
                        ) {
                            LazyVerticalGrid(
                                state = hubGridState,
                                columns = GridCells.Fixed(STUDIO_GRID_COLUMNS),
                                horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.GridItemSpacing),
                                verticalArrangement = Arrangement.spacedBy(ErasmusDimens.GridRowSpacing),
                                contentPadding = PaddingValues(
                                    top = ErasmusDimens.RailFocusHeadroom,
                                    bottom = ErasmusSpacing.XLarge
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .gridFocusContainer(
                                        hubGridHandle,
                                        hubGridState,
                                        STUDIOS_DATA.size
                                    )
                            ) {
                                gridItemsIndexed(STUDIOS_DATA, key = { _, it -> it.id }) { index, studio ->
                                    StudioTile(
                                        studio = studio,
                                        onClick = { viewModel.selectStudio(studio) },
                                        dpadModifier = Modifier.gridFocusItem(
                                            handle = hubGridHandle,
                                            index = index,
                                            // The top row and the first column
                                            // are the hub's two deliberate exits;
                                            // every other edge is a wall.
                                            onEscapeUp = focusNav,
                                            onEscapeLeft = focusNav,
                                            onScrollRequest = { direction ->
                                                coroutineScope.launch {
                                                    runCatching {
                                                        val delta =
                                                            if (direction == SpatialDirection.Down) {
                                                                STUDIO_GRID_COLUMNS
                                                            } else {
                                                                -STUDIO_GRID_COLUMNS
                                                            }
                                                        hubGridState.animateScrollToItem(
                                                            (index + delta)
                                                                .coerceIn(0, STUDIOS_DATA.size - 1)
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                is StudiosUiState.Loading -> TvRailsSkeleton(railCount = 2)

                is StudiosUiState.StudioCatalog -> {
                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.5f) }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = ErasmusDimens.NavPillContentClearance,
                                bottom = ErasmusSpacing.SectionLarge
                            )
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Medium),
                                    modifier = Modifier.padding(start = ErasmusDimens.RailStartGutter)
                                ) {
                                    CatalogBackButton(
                                        onClick = { viewModel.backToHub() },
                                        onNavigateLeftEdge = { focusNav() },
                                        // UP from the catalogue's Back arrow is
                                        // the page's route into the navigation.
                                        // It used to be consumed with no target,
                                        // which made it a dead key on the very
                                        // node the screen force-focused on entry.
                                        onNavigateUp = { focusNav() },
                                        modifier = Modifier.focusRequester(catalogFirstItemRequester)
                                    )
                                    ErasmusPageHeader(title = state.studio.name)
                                }

                                Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
                            }

                            item(key = ZONE_CATALOG_TRENDING) {
                                val currentList = if (state.isShowingMovies) state.movieItems else state.tvItems
                                val trendingHandle = catalogHandles.handleFor(ZONE_CATALOG_TRENDING)
                                ErasmusContentRail(
                                    title = "Trending",
                                    handle = trendingHandle,
                                    itemCount = currentList.size,
                                    trailing = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Small)
                                        ) {
                                            ErasmusChip(
                                                label = "Movies",
                                                isSelected = state.isShowingMovies,
                                                onClick = { viewModel.toggleFilter(true) },
                                                height = 34.dp
                                            )
                                            ErasmusChip(
                                                label = "Series",
                                                isSelected = !state.isShowingMovies,
                                                onClick = { viewModel.toggleFilter(false) },
                                                height = 34.dp
                                            )
                                        }
                                    }
                                ) {
                                    itemsIndexed(
                                        items = currentList,
                                        key = { _, item -> "${item.mediaType}:${item.id}" }
                                    ) { index, item ->
                                        MediaPosterCard(
                                            item = item,
                                            onClick = { onMediaClick(item) },
                                            cardModifier = Modifier.railFocusItem(
                                                handle = trendingHandle,
                                                coordinator = catalogCoordinator,
                                                index = index,
                                                isFirstItem = index == 0,
                                                onLeftEdge = focusNav
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
                            }

                            item(key = ZONE_CATALOG_POPULAR) {
                                val popularHandle = catalogHandles.handleFor(ZONE_CATALOG_POPULAR)
                                ErasmusContentRail(
                                    title = "Popular on ${state.studio.name}",
                                    handle = popularHandle,
                                    itemCount = state.trendingItems.size
                                ) {
                                    itemsIndexed(
                                        items = state.trendingItems,
                                        key = { _, item -> "all:${item.mediaType}:${item.id}" }
                                    ) { index, item ->
                                        MediaPosterCard(
                                            item = item,
                                            onClick = { onMediaClick(item) },
                                            cardModifier = Modifier.railFocusItem(
                                                handle = popularHandle,
                                                coordinator = catalogCoordinator,
                                                index = index,
                                                isFirstItem = index == 0,
                                                onLeftEdge = focusNav
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        TvFloatingNavBar(
            currentRoute = NavRoutes.STUDIOS,
            activeProfile = activeProfile,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            navFocusRequester = navFocusRequester,
            onFocusChanged = { isNavFocused = it },
            onNavigateIntoContent = {
                if (uiState is StudiosUiState.Hub) {
                    // Back to the tile the user was on, not tile one.
                    hubGridHandle.focusIndexOrNearest(lastStudioIndex)
                } else {
                    catalogCoordinator.enterContent()
                }
            },
            onReselectCurrent = {
                if (uiState is StudiosUiState.StudioCatalog) {
                    viewModel.backToHub()
                } else {
                    hubGridHandle.focusIndexOrNearest(lastStudioIndex)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * A channel tile.
 *
 * Dark charcoal with a barely-there vertical tonal shift so the tile reads as a
 * physical surface rather than a flat swatch, a hairline edge, and the standard
 * Erasmus focus response. The mark is centred and size-capped so nine logos of
 * wildly different aspect ratios all sit at the same optical weight.
 *
 * Grid edges are handled explicitly: LEFT from column one and UP from row one
 * leave for the navigation, and DOWN on the final row is consumed so focus
 * cannot fall out of the grid into nothing.
 */
@Composable
private fun StudioTile(
    studio: StudioInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dpadModifier: Modifier = Modifier
) {
    TvFocusableCard(
        onClick = onClick,
        contentDescription = "${studio.name} channel",
        role = Role.Button,
        shape = ErasmusShapes.Tile,
        focusedScale = TvMotion.FocusScaleStudio,
        focusedBorderColor = FocusWhite,
        focusedBorderWidth = 1.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(ErasmusDimens.StudioTileHeight)
            // All four directions, including RIGHT — which was previously
            // unguarded, so RIGHT on the last tile of a row wrapped to the first
            // tile of the next one.
            .then(dpadModifier)
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ErasmusShapes.Tile)
                .background(
                    Brush.verticalGradient(
                        if (isFocused) {
                            listOf(SurfaceCardFocused, SurfaceCardRest)
                        } else {
                            listOf(SurfaceCardRest, PitchBlack)
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isFocused) Color.Transparent else Color.White.copy(alpha = 0.08f),
                    shape = ErasmusShapes.Tile
                ),
            contentAlignment = Alignment.Center
        ) {
            StudioTileContent(studio = studio, isFocused = isFocused)
        }
    }
}

@Composable
private fun StudioTileContent(studio: StudioInfo, isFocused: Boolean) {
    val isReducedMotion = rememberReducedMotion()
    val logoScale by animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) 1.04f else 1f,
        animationSpec = TvSpring.Focus.floatSpec(isReducedMotion),
        label = "studioLogoScale"
    )

    // Marks are dimmed slightly at rest and come to full strength on focus, so
    // the focused channel is the only one reading at full brightness.
    val logoAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.82f,
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS,
            easing = TvMotion.EasingSilk
        ),
        label = "studioLogoAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ErasmusSpacing.Large, vertical = ErasmusSpacing.Medium)
            .graphicsLayer {
                scaleX = logoScale
                scaleY = logoScale
                alpha = logoAlpha
            }
    ) {
        if (studio.logoDrawableRes != null) {
            Image(
                painter = painterResource(id = studio.logoDrawableRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .fillMaxHeight(0.44f)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = studio.name.uppercase(),
                    style = ErasmusTvTypography.ButtonText.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ORIGINALS",
                    style = ErasmusTvTypography.Badge.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    ),
                    color = TextMuted
                )
            }
        }
    }
}

/** Circular Back affordance, matching the detail page's equivalent control. */
@Composable
private fun CatalogBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateLeftEdge: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Back to all studios"
            }
            .size(40.dp)
            .clip(CircleShape)
            .background(
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.10f),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isFocused) FocusWhite else Color.White.copy(alpha = 0.16f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft -> {
                            onNavigateLeftEdge?.invoke()
                            true
                        }
                        Key.DirectionUp -> {
                            onNavigateUp?.invoke()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = if (isFocused) PitchBlack else TextPrimary,
            modifier = Modifier.size(17.dp)
        )
    }
}

package com.erasmustv.app.ui.screens.categories

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.BorderSubtle
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
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
import com.erasmustv.app.ui.components.TvNavIcons
import com.erasmustv.app.ui.components.TvPivotBringIntoViewSpec
import com.erasmustv.app.ui.navigation.NavRoutes
import com.erasmustv.app.ui.screens.studios.StudioInfo

/**
 * Categories Screen matching bingr.one/categories.
 * Replaces the standalone Studios page, with studios integrated as an interactive row.
 * Clicking a studio tile opens that studio's full catalog view with back navigation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val firstItemFocusRequester = remember { FocusRequester() }
    var isRailFocused by remember { mutableStateOf(false) }

    val contentShift by animateDpAsState(
        targetValue = if (isRailFocused) 76.dp else 0.dp,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "categoriesContentShift"
    )

    BackHandler(enabled = uiState is CategoriesUiState.StudioCatalog) {
        viewModel.backToOverview()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        when (val state = uiState) {
            is CategoriesUiState.Loading -> {
                TvFeedSkeleton(contentShift = contentShift)
            }
            is CategoriesUiState.Overview -> {
                CompositionLocalProvider(
                    LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.48f) }
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = contentShift.toPx()
                            }
                            .padding(start = 64.dp, top = 24.dp, end = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        contentPadding = PaddingValues(bottom = 60.dp)
                    ) {
                        // Editorial Screen Header
                        item {
                            Text(
                                text = "Categories",
                                style = ErasmusTvTypography.HeroTitleLarge.copy(fontSize = 28.sp),
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Section 1: Browse
                        item {
                            CategorySectionHeader(title = "Browse")

                            CompositionLocalProvider(
                                LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.42f) }
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(state.browseItems, key = { _, it -> it.id }) { index, item ->
                                        BrowseTile(
                                            item = item,
                                            onClick = {
                                                if (item.route != null) {
                                                    onNavigate(item.route)
                                                }
                                            },
                                            modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                                        )
                                    }
                                }
                            }
                        }

                        // Section 2: Studios (Embedded inside Categories)
                        item {
                            CategorySectionHeader(title = "Studios")

                            CompositionLocalProvider(
                                LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.42f) }
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(state.studios, key = { it.id }) { studio ->
                                        StudioTile(
                                            studio = studio,
                                            onClick = { viewModel.selectStudio(studio) }
                                        )
                                    }
                                }
                            }
                        }

                        // Section 3: Popular Languages
                        item {
                            CategorySectionHeader(title = "Popular Languages")

                            CompositionLocalProvider(
                                LocalBringIntoViewSpec provides remember { TvPivotBringIntoViewSpec(0.42f) }
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(state.languages, key = { it.id }) { lang ->
                                        LanguageTile(language = lang)
                                    }
                                }
                            }
                        }


                    }
                }
            }
            is CategoriesUiState.StudioCatalog -> {
                // Studio Specific Catalog (replaces standalone studios page)
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
                            // Header with Back arrow & Studio Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                TvFocusableCard(
                                    onClick = { viewModel.backToOverview() },
                                    shape = RectangleShape,
                                    focusedScale = 1.0f,
                                    focusedBorderColor = FocusWhite,
                                    focusedBorderWidth = 1.5.dp,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .focusRequester(firstItemFocusRequester)
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (isFocused) Color(0x33FFFFFF) else SurfaceElevated,
                                                RectangleShape
                                            )
                                            .border(
                                                1.dp,
                                                if (isFocused) FocusWhite else BorderHairline,
                                                RectangleShape
                                            ),
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

                        // Trending on [Studio] Row with Movies / Series toggle
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
                                    CategoryFilterTab(
                                        label = "Movies",
                                        isSelected = state.isShowingMovies,
                                        onClick = { viewModel.toggleFilter(true) }
                                    )
                                    CategoryFilterTab(
                                        label = "Series",
                                        isSelected = !state.isShowingMovies,
                                        onClick = { viewModel.toggleFilter(false) }
                                    )
                                }
                            }

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

                        // Popular on Studio Row
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

        // Persistent Left Navigation Rail
        TvLeftNavRail(
            currentRoute = NavRoutes.CATEGORIES,
            activeProfile = activeProfile,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            onFocusChanged = { isRailFocused = it },
            onNavigateRight = {
                try {
                    firstItemFocusRequester.requestFocus()
                    true
                } catch (_: Exception) {
                    false
                }
            },
            onReselectCurrent = {
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (_: Exception) {}
            },
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
private fun CategorySectionHeader(title: String) {
    Text(
        text = title,
        style = ErasmusTvTypography.SectionTitle,
        color = TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

@Composable
private fun BrowseTile(
    item: BrowseCategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF18181B), Color(0xFF27272A))
    )

    TvFocusableCard(
        onClick = onClick,
        modifier = modifier
            .width(185.dp)
            .height(86.dp),
        shape = RectangleShape,
        focusedScale = 1.025f,
        focusedBorderColor = FocusWhite,
        focusedBorderWidth = 1.5.dp
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .background(darkGradient)
                .border(
                    width = 1.dp,
                    color = if (isFocused) Color.Transparent else BorderSubtle,
                    shape = RectangleShape
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = item.title,
                style = ErasmusTvTypography.HeroTitleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = FocusWhite
            )
        }
    }
}

@Composable
private fun StudioTile(
    studio: StudioInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusableCard(
        onClick = onClick,
        modifier = modifier
            .width(185.dp)
            .height(86.dp),
        shape = RectangleShape,
        focusedScale = 1.025f,
        focusedBorderColor = FocusWhite,
        focusedBorderWidth = 1.5.dp
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .background(
                    if (isFocused) Color(0xFF1E1E24) else SurfaceElevated
                )
                .border(
                    width = 1.dp,
                    color = if (isFocused) Color.Transparent else BorderSubtle,
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (studio.logoDrawableRes != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = studio.logoDrawableRes),
                    contentDescription = studio.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .sizeIn(maxWidth = 135.dp, maxHeight = 34.dp)
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = if (isFocused) FocusWhite else TextPrimary
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
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
private fun LanguageTile(
    language: LanguageCategoryItem,
    modifier: Modifier = Modifier
) {
    val darkGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF141414), Color(0xFF1F1F23))
    )

    TvFocusableCard(
        onClick = {},
        modifier = modifier
            .width(185.dp)
            .height(86.dp),
        shape = RectangleShape,
        focusedScale = 1.025f,
        focusedBorderColor = FocusWhite,
        focusedBorderWidth = 1.5.dp
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .background(darkGradient)
                .border(
                    width = 1.dp,
                    color = if (isFocused) Color.Transparent else BorderSubtle,
                    shape = RectangleShape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = language.localTitle,
                    style = ErasmusTvTypography.HeroTitleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = FocusWhite
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = language.englishTitle,
                    style = ErasmusTvTypography.Badge.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterTab(
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

package com.erasmustv.app.ui.screens.search

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.SurfaceElevated
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.MediaPosterCard
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvLeftNavRail
import com.erasmustv.app.ui.navigation.NavRoutes

private val KEYBOARD_ROWS = listOf(
    listOf("a", "b", "c", "d", "e", "f"),
    listOf("g", "h", "i", "j", "k", "l"),
    listOf("m", "n", "o", "p", "q", "r"),
    listOf("s", "t", "u", "v", "w", "x"),
    listOf("y", "z", "1", "2", "3", "4"),
    listOf("5", "6", "7", "8", "9", "0")
)

private val SEARCH_CATEGORIES = listOf(
    SearchCategory("Action", movieGenreId = 28, tvGenreId = 10759),
    SearchCategory("Comedies", movieGenreId = 35, tvGenreId = 35),
    SearchCategory("Horror", movieGenreId = 27, tvGenreId = null),
    SearchCategory("Crime", movieGenreId = 80, tvGenreId = 80),
    SearchCategory("Anime", movieGenreId = 16, tvGenreId = 16),
    SearchCategory("Documentaries", movieGenreId = 99, tvGenreId = 99),
    SearchCategory("Sci-Fi", movieGenreId = 878, tvGenreId = 10765),
    SearchCategory("Drama", movieGenreId = 18, tvGenreId = 18),
    SearchCategory("Thriller", movieGenreId = 53, tvGenreId = null),
    SearchCategory("Romance", movieGenreId = 10749, tvGenreId = 10766)
)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val query by viewModel.query.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()

    val initialKeyFocusRequester = remember { FocusRequester() }
    val resultsFirstCardRequester = remember { FocusRequester() }
    val railFocusRequester = remember { FocusRequester() }
    var isRailFocused by remember { mutableStateOf(false) }

    val contentShift by animateDpAsState(
        targetValue = if (isRailFocused) 76.dp else 0.dp,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "searchContentShift"
    )

    LaunchedEffect(Unit) {
        try {
            initialKeyFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Deterministic Back Handler:
    // 1. If query or category exists, back clears and returns focus to keyboard
    // 2. If query is empty and keyboard/results focused, back focuses the sidebar rail
    // 3. If rail is focused, back navigates to Home
    androidx.activity.compose.BackHandler(enabled = query.isNotEmpty() || selectedCategory != null || !isRailFocused) {
        if (query.isNotEmpty() || selectedCategory != null) {
            viewModel.onQueryChange("")
            try {
                initialKeyFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            try {
                railFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    androidx.activity.compose.BackHandler(enabled = isRailFocused) {
        onNavigate(NavRoutes.HOME)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = contentShift.toPx()
                }
                .padding(start = 64.dp, top = 28.dp, end = 36.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Left Panel: Search Input + Grid Keyboard + Genre Categories
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                // Search Input Header Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RectangleShape)
                        .border(1.dp, BorderHairline, RectangleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = if (query.isNotEmpty()) FocusWhite else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when {
                                query.isNotEmpty() -> query
                                selectedCategory != null -> selectedCategory!!.name
                                else -> "Search titles, genres, studios..."
                            },
                            style = ErasmusTvTypography.Body.copy(
                                fontSize = 13.sp,
                                fontWeight = if (query.isNotEmpty() || selectedCategory != null) FontWeight.Medium else FontWeight.Normal
                            ),
                            color = if (query.isNotEmpty() || selectedCategory != null) TextPrimary else TextMuted.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (query.isNotEmpty() || selectedCategory != null) {
                            TvFocusableCard(
                                onClick = { viewModel.onQueryChange("") },
                                shape = RectangleShape,
                                modifier = Modifier.size(26.dp),
                                focusedScale = 1.0f,
                                focusedBorderColor = FocusWhite,
                                focusedBorderWidth = 1.5.dp
                            ) { isFocused ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(if (isFocused) Color(0x33FFFFFF) else Color.Transparent, RectangleShape)
                                        .border(
                                            width = 1.dp,
                                            color = if (isFocused) FocusWhite else BorderHairline,
                                            shape = RectangleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = FocusWhite,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // On-Screen TV Keyboard
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KEYBOARD_ROWS.forEachIndexed { rowIndex, rowKeys ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowKeys.forEachIndexed { colIndex, char ->
                                val isFirstKey = rowIndex == 0 && colIndex == 0
                                val isFirstCol = colIndex == 0
                                val isLastCol = colIndex == rowKeys.size - 1
                                TvKeyboardKey(
                                    label = char,
                                    onClick = { viewModel.onQueryChange(query + char) },
                                    onNavigateLeft = if (isFirstCol) { { railFocusRequester.requestFocus() } } else null,
                                    onNavigateRight = if (isLastCol) { { try { resultsFirstCardRequester.requestFocus() } catch (_: Exception) {} } } else null,
                                    isTopRow = (rowIndex == 0),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .then(if (isFirstKey) Modifier.focusRequester(initialKeyFocusRequester) else Modifier)
                                )
                            }
                        }
                    }

                    // Special Action Keys Row: Space, Del, Clear
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        TvKeyboardActionKey(
                            label = "Space",
                            icon = Icons.Default.SpaceBar,
                            onClick = { viewModel.onQueryChange("$query ") },
                            onNavigateLeft = { railFocusRequester.requestFocus() },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(34.dp)
                        )

                        TvKeyboardActionKey(
                            label = "Del",
                            icon = Icons.AutoMirrored.Filled.Backspace,
                            onClick = {
                                if (query.isNotEmpty()) {
                                    viewModel.onQueryChange(query.dropLast(1))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        )

                        TvKeyboardActionKey(
                            label = "Clear",
                            icon = Icons.Default.Clear,
                            onClick = { viewModel.onQueryChange("") },
                            onNavigateRight = { try { resultsFirstCardRequester.requestFocus() } catch (_: Exception) {} },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Genres List
                Text(
                    text = "Browse by Genre",
                    style = ErasmusTvTypography.Badge.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(SEARCH_CATEGORIES) { category ->
                        CategoryTextFilterItem(
                            title = category.name,
                            isSelected = selectedCategory?.name == category.name,
                            onClick = { viewModel.selectCategory(category) },
                            onNavigateLeft = { railFocusRequester.requestFocus() },
                            onNavigateRight = { try { resultsFirstCardRequester.requestFocus() } catch (_: Exception) {} }
                        )
                    }
                }
            }

            // Right Panel: Results Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (val state = uiState) {
                    is SearchUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = FocusWhite)
                        }
                    }
                    is SearchUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = state.message, style = ErasmusTvTypography.Body, color = TextPrimary)
                        }
                    }
                    is SearchUiState.Success -> {
                        val displayItems = if (state.results.isNotEmpty()) state.results else state.trendingSuggestions
                        val headerTitle = when {
                            state.isGenreCategory -> "${state.query} Movies & Shows"
                            state.results.isNotEmpty() -> "Results for \"${state.query}\""
                            else -> "Top Searches"
                        }

                        Text(
                            text = headerTitle,
                            style = ErasmusTvTypography.SectionTitle,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = displayItems,
                                key = { _, item -> "${item.mediaType}:${item.id}" }
                            ) { index, item ->
                                val isFirstColInResults = index % 3 == 0
                                val isTopRow = index < 3
                                val isBottomRow = index >= (displayItems.size - ((displayItems.size - 1) % 3 + 1))
                                MediaPosterCard(
                                    item = item,
                                    onClick = { onMediaClick(item) },
                                    cardWidth = 140,
                                    cardModifier = Modifier
                                        .then(if (index == 0) Modifier.focusRequester(resultsFirstCardRequester) else Modifier)
                                        .onKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown) {
                                                when (event.key) {
                                                    Key.DirectionLeft -> {
                                                        if (isFirstColInResults) {
                                                            try {
                                                                initialKeyFocusRequester.requestFocus()
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
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        // Persistent Left Nav Rail
        TvLeftNavRail(
            currentRoute = NavRoutes.SEARCH,
            activeProfile = activeProfile,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            railFocusRequester = railFocusRequester,
            onFocusChanged = { focused -> isRailFocused = focused },
            onNavigateRight = {
                try {
                    initialKeyFocusRequester.requestFocus()
                    true
                } catch (_: Exception) {
                    false
                }
            },
            onReselectCurrent = {
                try {
                    initialKeyFocusRequester.requestFocus()
                } catch (_: Exception) {}
            },
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
private fun TvKeyboardKey(
    label: String,
    onClick: () -> Unit,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
    isTopRow: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(
                if (isFocused) FocusWhite else SurfaceDark
            )
            .border(
                width = 1.dp,
                color = if (isFocused) FocusWhite else BorderHairline,
                shape = RectangleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (onNavigateLeft != null) {
                                onNavigateLeft()
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (onNavigateRight != null) {
                                onNavigateRight()
                                true
                            } else false
                        }
                        Key.DirectionUp -> {
                            if (isTopRow) true else false
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = ErasmusTvTypography.Badge.copy(
                fontSize = 13.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isFocused) PitchBlack else TextPrimary
        )
    }
}

@Composable
private fun TvKeyboardActionKey(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = modifier
            .clip(RectangleShape)
            .background(
                if (isFocused) FocusWhite else SurfaceDark
            )
            .border(
                width = 1.dp,
                color = if (isFocused) FocusWhite else BorderHairline,
                shape = RectangleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (onNavigateLeft != null) {
                                onNavigateLeft()
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (onNavigateRight != null) {
                                onNavigateRight()
                                true
                            } else false
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) PitchBlack else TextSecondary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = ErasmusTvTypography.Badge.copy(
                fontSize = 11.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isFocused) PitchBlack else TextPrimary
        )
    }
}

@Composable
private fun CategoryTextFilterItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .background(
                when {
                    isFocused -> FocusWhite.copy(alpha = 0.20f)
                    isSelected -> FocusWhite.copy(alpha = 0.10f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = if (isFocused) FocusWhite else Color.Transparent,
                shape = RectangleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (onNavigateLeft != null) {
                                onNavigateLeft()
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (onNavigateRight != null) {
                                onNavigateRight()
                                true
                            } else false
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            style = ErasmusTvTypography.Body.copy(
                fontSize = 13.sp,
                fontWeight = if (isFocused || isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isFocused || isSelected) FocusWhite else TextSecondary
        )
    }
}

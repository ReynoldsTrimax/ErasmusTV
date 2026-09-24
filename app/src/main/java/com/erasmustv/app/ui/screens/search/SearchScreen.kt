package com.erasmustv.app.ui.screens.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.ErasmusSpacing
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceCardRest
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.rememberReducedMotion
import com.erasmustv.app.core.theme.rememberScreenHorizontalMargin
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.ui.components.ErasmusActionButton
import com.erasmustv.app.ui.components.ErasmusButtonStyle
import com.erasmustv.app.ui.components.ErasmusEmptyState
import com.erasmustv.app.core.theme.TvSpring
import com.erasmustv.app.core.theme.floatSpec
import com.erasmustv.app.ui.components.MediaPosterCard
import com.erasmustv.app.ui.components.TvFloatingNavBar
import com.erasmustv.app.ui.components.TvSearchSkeleton
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.erasmustv.app.ui.focus.FocusZoneMemory
import com.erasmustv.app.ui.focus.GridFocusHandle
import com.erasmustv.app.ui.focus.SpatialDirection
import com.erasmustv.app.ui.focus.gridFocusContainer
import com.erasmustv.app.ui.focus.gridFocusItem
import com.erasmustv.app.ui.focus.rememberFocusZoneMemory
import com.erasmustv.app.ui.focus.rememberGridFocusHandle
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

/** Width of the input column. Leaves the results grid the dominant share. */
private val SEARCH_PANEL_WIDTH = 312.dp
private val KEY_HEIGHT = 34.dp
private val KEY_GAP = 6.dp

/** Compact rounded geometry for keys — rounded, but not pill-shaped blobs. */
private val KeyShape = RoundedCornerShape(10.dp)

/**
 * Focus-engine zone key for the results grid.
 *
 * One key, not three. The previous split into movies/series/all groupings meant
 * the *same* `resultsFirstCardRequester` instance was attached to a movie card
 * and a series card simultaneously whenever both groups were present, so
 * restoring focus to a series result silently landed on the first movie
 * instead. The grid is one focus region; which card inside it is remembered is
 * the engine's job, by index.
 */
private const val RESULTS_GRID_KEY = "search_results"

/**
 * ERASMUS SEARCH.
 *
 * Two regions: an input column on the left (field, on-screen key grid, genre
 * chips) and a results grid on the right. Search behaviour, debouncing, genre
 * discovery, and the trending-suggestion fallback are all unchanged — this is
 * a presentation and interaction redesign only.
 */
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
    val navFocusRequester = remember { FocusRequester() }
    var isNavFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val horizontalMargin = rememberScreenHorizontalMargin()

    // ------------------------------------------------------------------
    // Focus engine. The results grid is a genuine 2D region, so all four
    // directions inside it resolve against measured cell bounds — including
    // across the span-width group headers, which are not focusable and must be
    // stepped over rather than absorbing the key.
    // ------------------------------------------------------------------
    val resultsGridState = rememberLazyGridState()
    val resultsHandle = rememberGridFocusHandle(RESULTS_GRID_KEY)
    val focusMemory = rememberFocusZoneMemory()

    LaunchedEffect(Unit) {
        // A fresh visit starts on the keys, which is the page's primary control.
        // A return trip from a result's detail page goes back to that result.
        val remembered = focusMemory.indexFor(RESULTS_GRID_KEY)
        if (remembered == null || !resultsHandle.focusIndexOrNearest(remembered)) {
            runCatching { initialKeyFocusRequester.requestFocus() }
        }
    }

    // Back behaviour, in order of specificity:
    //  1. An active query or genre is cleared first (an overlay-like state).
    //  2. Otherwise focus returns to the floating navigation.
    //  3. From the navigation, Back leaves for Home.
    androidx.activity.compose.BackHandler(
        enabled = query.isNotEmpty() || selectedCategory != null || !isNavFocused
    ) {
        if (query.isNotEmpty() || selectedCategory != null) {
            viewModel.onQueryChange("")
            try {
                initialKeyFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            try {
                navFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    androidx.activity.compose.BackHandler(enabled = isNavFocused) {
        onNavigate(NavRoutes.HOME)
    }

    // ------------------------------------------------------------------
    // Hold the last real result set while a debounced query is in flight.
    //
    // The view model emits Loading on every keystroke, which swapped the grid for
    // a non-focusable skeleton and destroyed the focused node — so a viewer who
    // had walked into the results and then pressed a key lost their place
    // entirely. Retaining the previous results means the grid only disappears
    // when there is genuinely nothing to show yet.
    // ------------------------------------------------------------------
    var lastLoadedState by remember { mutableStateOf<SearchUiState.Success?>(null) }
    (uiState as? SearchUiState.Success)?.let { lastLoadedState = it }
    val displayState: SearchUiState = when (uiState) {
        is SearchUiState.Loading -> lastLoadedState ?: uiState
        else -> uiState
    }

    // RIGHT out of the input column enters the results grid at the row the user
    // last stood on, falling back to its first cell.
    val focusResults: () -> Unit = {
        val remembered = focusMemory.indexFor(RESULTS_GRID_KEY) ?: 0
        if (!resultsHandle.focusIndexOrNearest(remembered)) {
            resultsHandle.focusNearestInFirstVisibleRow(null)
        }
        Unit
    }
    val focusNav: () -> Unit = {
        runCatching { navFocusRequester.requestFocus() }
        Unit
    }
    val focusKeyboard: () -> Unit = {
        runCatching { initialKeyFocusRequester.requestFocus() }
        Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalMargin,
                    top = ErasmusDimens.NavPillContentClearance,
                    end = horizontalMargin,
                    bottom = ErasmusSpacing.Large
                ),
            horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.XLarge)
        ) {
            // ---------------- Input column ----------------
            Column(
                modifier = Modifier
                    .width(SEARCH_PANEL_WIDTH)
                    .fillMaxHeight()
            ) {
                SearchField(
                    query = query,
                    categoryName = selectedCategory?.name,
                    onClear = {
                        viewModel.onQueryChange("")
                        focusKeyboard()
                    },
                    onNavigateToNav = focusNav,
                    onNavigateToKeys = focusKeyboard,
                    onNavigateToResults = focusResults
                )

                Spacer(modifier = Modifier.height(ErasmusSpacing.Medium))

                // On-screen key grid. The app has no IME dependency, so this
                // grid *is* the text input; it stays a grid because letter
                // position predictability matters more than novelty on a remote.
                Column(
                    verticalArrangement = Arrangement.spacedBy(KEY_GAP),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KEYBOARD_ROWS.forEachIndexed { rowIndex, rowKeys ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowKeys.forEachIndexed { colIndex, char ->
                                val isFirstKey = rowIndex == 0 && colIndex == 0
                                SearchKey(
                                    label = char,
                                    onClick = { viewModel.onQueryChange(query + char) },
                                    onNavigateLeft = if (colIndex == 0) focusNav else null,
                                    onNavigateRight = if (colIndex == rowKeys.lastIndex) focusResults else null,
                                    onNavigateUp = if (rowIndex == 0) focusNav else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(KEY_HEIGHT)
                                        .then(
                                            if (isFirstKey) {
                                                Modifier.focusRequester(initialKeyFocusRequester)
                                            } else Modifier
                                        )
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SearchActionKey(
                            label = "Space",
                            icon = Icons.Default.SpaceBar,
                            onClick = { viewModel.onQueryChange("$query ") },
                            onNavigateLeft = focusNav,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(KEY_HEIGHT)
                        )
                        SearchActionKey(
                            label = "Del",
                            icon = Icons.AutoMirrored.Filled.Backspace,
                            onClick = {
                                if (query.isNotEmpty()) viewModel.onQueryChange(query.dropLast(1))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(KEY_HEIGHT)
                        )
                        SearchActionKey(
                            label = "Clear",
                            icon = Icons.Default.Clear,
                            onClick = { viewModel.onQueryChange("") },
                            onNavigateRight = focusResults,
                            modifier = Modifier
                                .weight(1f)
                                .height(KEY_HEIGHT)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ErasmusSpacing.Large))

                Text(
                    text = "Browse by Genre",
                    style = ErasmusTvTypography.Badge.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextMuted,
                    modifier = Modifier.padding(start = 2.dp, bottom = ErasmusSpacing.Small)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(ErasmusSpacing.Small),
                    contentPadding = PaddingValues(bottom = ErasmusSpacing.Medium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(SEARCH_CATEGORIES, key = { it.name }) { category ->
                        GenreChipRow(
                            title = category.name,
                            isSelected = selectedCategory?.name == category.name,
                            onClick = { viewModel.selectCategory(category) },
                            onNavigateLeft = focusNav,
                            onNavigateRight = focusResults
                        )
                    }
                }
            }

            // ---------------- Results column ----------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (val state = displayState) {
                    is SearchUiState.Loading -> TvSearchSkeleton(columns = 3)

                    is SearchUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ErasmusEmptyState(
                                icon = Icons.Default.Search,
                                title = "Search unavailable",
                                message = "Check the connection, or try a different title."
                            )
                            Spacer(modifier = Modifier.height(ErasmusSpacing.Large))
                            ErasmusActionButton(
                                text = "Try Again",
                                onClick = {
                                    if (query.isNotBlank()) {
                                        viewModel.onQueryChange(query)
                                    } else {
                                        selectedCategory?.let { viewModel.selectCategory(it) }
                                    }
                                },
                                style = ErasmusButtonStyle.Primary
                            )
                        }
                    }

                    is SearchUiState.Success -> {
                        val isSearchActive = state.query.isNotBlank()
                        val displayItems = if (isSearchActive) state.results else state.trendingSuggestions

                        val movies = remember(displayItems) { displayItems.filter { it.mediaType == "movie" } }
                        val series = remember(displayItems) { displayItems.filter { it.mediaType != "movie" } }
                        val hasBoth = movies.isNotEmpty() && series.isNotEmpty()

                        val headerTitle = when {
                            state.isGenreCategory -> state.query
                            isSearchActive -> "Results for \u201C${state.query}\u201D"
                            else -> "Top Searches"
                        }

                        if (displayItems.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ErasmusEmptyState(
                                    icon = Icons.Default.Search,
                                    title = if (isSearchActive) {
                                        "No matches for \u201C${state.query}\u201D"
                                    } else {
                                        "Nothing to suggest yet"
                                    },
                                    message = if (isSearchActive) {
                                        "Try a shorter title, a different spelling, or pick a genre on the left."
                                    } else {
                                        "Spell a title with the keys on the left, or choose a genre."
                                    }
                                )
                            }
                        } else {
                            val columns = 3
                            // Grid indices include the span-width group headers,
                            // so the engine's item count is the total, not the
                            // card count.
                            val gridItemCount = if (hasBoth) {
                                movies.size + series.size + 2
                            } else {
                                displayItems.size + 1
                            }
                            LazyVerticalGrid(
                                state = resultsGridState,
                                columns = GridCells.Fixed(columns),
                                contentPadding = PaddingValues(
                                    top = ErasmusDimens.RailFocusHeadroom,
                                    bottom = ErasmusSpacing.XLarge
                                ),
                                horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.GridItemSpacing),
                                verticalArrangement = Arrangement.spacedBy(ErasmusDimens.GridRowSpacing),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .gridFocusContainer(resultsHandle, resultsGridState, gridItemCount)
                            ) {
                                if (hasBoth) {
                                    item(span = { GridItemSpan(columns) }) {
                                        ResultsGroupHeader(
                                            title = if (isSearchActive) "Movies" else "Top Movies",
                                            count = movies.size
                                        )
                                    }
                                    itemsIndexed(
                                        items = movies,
                                        key = { _, item -> "m:${item.id}" }
                                    ) { index, item ->
                                        // +1 for the "Movies" header above.
                                        ResultCard(
                                            item = item,
                                            gridIndex = index + 1,
                                            handle = resultsHandle,
                                            memory = focusMemory,
                                            onClick = { onMediaClick(item) },
                                            onEscapeLeft = focusKeyboard,
                                            onEscapeUp = focusNav,
                                            onScroll = { target ->
                                                coroutineScope.launch {
                                                    runCatching {
                                                        resultsGridState.animateScrollToItem(target)
                                                    }
                                                }
                                            },
                                            columns = columns,
                                            gridItemCount = gridItemCount
                                        )
                                    }

                                    item(span = { GridItemSpan(columns) }) {
                                        ResultsGroupHeader(
                                            title = if (isSearchActive) "Series & Shows" else "Top Series",
                                            count = series.size,
                                            modifier = Modifier.padding(top = ErasmusSpacing.Small)
                                        )
                                    }
                                    itemsIndexed(
                                        items = series,
                                        key = { _, item -> "s:${item.id}" }
                                    ) { index, item ->
                                        // +2 for both group headers above.
                                        ResultCard(
                                            item = item,
                                            gridIndex = movies.size + index + 2,
                                            handle = resultsHandle,
                                            memory = focusMemory,
                                            onClick = { onMediaClick(item) },
                                            onEscapeLeft = focusKeyboard,
                                            onEscapeUp = focusNav,
                                            onScroll = { target ->
                                                coroutineScope.launch {
                                                    runCatching {
                                                        resultsGridState.animateScrollToItem(target)
                                                    }
                                                }
                                            },
                                            columns = columns,
                                            gridItemCount = gridItemCount
                                        )
                                    }
                                } else {
                                    item(span = { GridItemSpan(columns) }) {
                                        ResultsGroupHeader(
                                            title = headerTitle,
                                            count = displayItems.size
                                        )
                                    }
                                    itemsIndexed(
                                        items = displayItems,
                                        key = { _, item -> "${item.mediaType}:${item.id}" }
                                    ) { index, item ->
                                        ResultCard(
                                            item = item,
                                            gridIndex = index + 1,
                                            handle = resultsHandle,
                                            memory = focusMemory,
                                            onClick = { onMediaClick(item) },
                                            onEscapeLeft = focusKeyboard,
                                            onEscapeUp = focusNav,
                                            onScroll = { target ->
                                                coroutineScope.launch {
                                                    runCatching {
                                                        resultsGridState.animateScrollToItem(target)
                                                    }
                                                }
                                            },
                                            columns = columns,
                                            gridItemCount = gridItemCount
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }

        TvFloatingNavBar(
            currentRoute = NavRoutes.SEARCH,
            activeProfile = activeProfile,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            navFocusRequester = navFocusRequester,
            onFocusChanged = { focused -> isNavFocused = focused },
            // DOWN out of the nav returns to whichever region the user came
            // from: the results grid if they were in it, otherwise the keys.
            onNavigateIntoContent = {
                val remembered = focusMemory.indexFor(RESULTS_GRID_KEY)
                val landed = remembered != null && resultsHandle.focusIndexOrNearest(remembered)
                landed || runCatching { initialKeyFocusRequester.requestFocus() }.isSuccess
            },
            onReselectCurrent = focusKeyboard,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * The search field. Display-only by design: text is composed with the on-screen
 * key grid, so this never needs to host a system IME. It still behaves like a
 * field visually — icon, live value, placeholder, and a clear affordance that
 * only exists when there is something to clear.
 */
@Composable
private fun SearchField(
    query: String,
    categoryName: String?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToNav: (() -> Unit)? = null,
    onNavigateToKeys: (() -> Unit)? = null,
    onNavigateToResults: (() -> Unit)? = null
) {
    val isActive = query.isNotEmpty() || categoryName != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(ErasmusShapes.InputLarge)
            .background(SurfaceCardRest, ErasmusShapes.InputLarge)
            .border(
                width = 1.dp,
                color = if (isActive) {
                    Color.White.copy(alpha = 0.26f)
                } else {
                    Color.White.copy(alpha = 0.10f)
                },
                shape = ErasmusShapes.InputLarge
            )
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isActive) TextPrimary.copy(alpha = 0.85f) else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when {
                    query.isNotEmpty() -> query
                    categoryName != null -> categoryName
                    else -> "Search titles and genres"
                },
                style = ErasmusTvTypography.Body.copy(
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isActive) TextPrimary else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isActive) {
                ClearAffordance(
                    onClick = onClear,
                    onNavigateUp = onNavigateToNav,
                    onNavigateDown = onNavigateToKeys,
                    onNavigateLeft = onNavigateToKeys,
                    onNavigateRight = onNavigateToResults
                )
            }
        }
    }
}

@Composable
private fun ClearAffordance(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Clear search"
            }
            .size(28.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                color = if (isFocused) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isFocused) FocusWhite else Color.Transparent,
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            // Every direction is accounted for.
            //
            // This control sits at the top of the input column with the search
            // field's text to its left and nothing above it, and it previously had
            // no key handling at all: the only focusable on the page with no
            // defined exits, so landing on it was a dead end in three of four
            // directions.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onNavigateLeft?.invoke(); true }
                    Key.DirectionUp -> { onNavigateUp?.invoke(); true }
                    Key.DirectionDown -> { onNavigateDown?.invoke(); true }
                    Key.DirectionRight -> { onNavigateRight?.invoke(); true }
                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = null,
            tint = if (isFocused) FocusWhite else TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * A single character key. Compact, rounded, and quiet at rest; on focus it
 * inverts to a light fill so the cursor position is unmistakable across a
 * dense 6x6 grid where a mere outline would be easy to lose.
 */
@Composable
private fun SearchKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isReducedMotion = rememberReducedMotion()

    val scale by animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) 1.06f else 1f,
        animationSpec = TvSpring.FocusFast.floatSpec(isReducedMotion),
        label = "keyScale"
    )

    val background by animateColorAsState(
        targetValue = if (isFocused) FocusWhite else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS_FAST),
        label = "keyBackground"
    )

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Letter $label"
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(KeyShape)
            .background(background, KeyShape)
            .border(
                width = 1.dp,
                color = if (isFocused) FocusWhite else Color.White.copy(alpha = 0.09f),
                shape = KeyShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { event -> handleKeyNavigation(event, onNavigateLeft, onNavigateRight, onNavigateUp) }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            style = ErasmusTvTypography.ButtonText.copy(
                fontSize = 14.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isFocused) PitchBlack else TextPrimary.copy(alpha = 0.88f)
        )
    }
}

@Composable
private fun SearchActionKey(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isReducedMotion = rememberReducedMotion()

    val background by animateColorAsState(
        targetValue = if (isFocused) FocusWhite else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS_FAST),
        label = "actionKeyBackground"
    )

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$label key"
            }
            .clip(KeyShape)
            .background(background, KeyShape)
            .border(
                width = 1.dp,
                color = if (isFocused) FocusWhite else Color.White.copy(alpha = 0.09f),
                shape = KeyShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { event -> handleKeyNavigation(event, onNavigateLeft, onNavigateRight, null) }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) PitchBlack else TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = ErasmusTvTypography.ButtonText.copy(
                fontSize = 12.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isFocused) PitchBlack else TextPrimary.copy(alpha = 0.88f),
            maxLines = 1
        )
    }
}

/**
 * Full-width genre chip. Rounded and consistent with [ErasmusChip] geometry,
 * but laid out as a full-width row so the genre column keeps a single,
 * completely predictable vertical D-pad path.
 */
@Composable
private fun GenreChipRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isReducedMotion = rememberReducedMotion()

    val background by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White.copy(alpha = 0.20f)
            isSelected -> Color.White.copy(alpha = 0.11f)
            else -> Color.White.copy(alpha = 0.04f)
        },
        animationSpec = tween(if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS_FAST),
        label = "genreBackground"
    )

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
                contentDescription = "$title genre${if (isSelected) ", selected" else ""}"
            }
            .fillMaxWidth()
            .height(38.dp)
            .clip(ErasmusShapes.Button)
            .background(background, ErasmusShapes.Button)
            .border(
                width = 1.dp,
                color = when {
                    isFocused -> FocusWhite
                    isSelected -> Color.White.copy(alpha = 0.26f)
                    else -> Color.Transparent
                },
                shape = ErasmusShapes.Button
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { event -> handleKeyNavigation(event, onNavigateLeft, onNavigateRight, null) }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = ErasmusTvTypography.ButtonText.copy(
                fontSize = 13.5.sp,
                fontWeight = if (isFocused || isSelected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = when {
                isFocused -> FocusWhite
                isSelected -> TextPrimary
                else -> TextSecondary
            }
        )
    }
}

/**
 * Results group heading. The count is plain muted text rather than a badge —
 * it is reference information, not a status to be highlighted.
 */
@Composable
private fun ResultsGroupHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = ErasmusTvTypography.SectionTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = "$count ${if (count == 1) "title" else "titles"}",
            style = ErasmusTvTypography.CardMeta,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 3.dp)
        )
    }
}

/**
 * A results cell. Cards fill their grid cell, and LEFT from the first column
 * returns to the key grid rather than dead-ending at the panel boundary.
 */
@Composable
private fun ResultCard(
    item: MediaItem,
    gridIndex: Int,
    handle: GridFocusHandle,
    memory: FocusZoneMemory,
    onClick: () -> Unit,
    onEscapeLeft: () -> Unit,
    onEscapeUp: () -> Unit,
    onScroll: (Int) -> Unit,
    columns: Int,
    gridItemCount: Int
) {
    MediaPosterCard(
        item = item,
        onClick = onClick,
        cardWidth = Dp.Unspecified,
        cardModifier = Modifier.gridFocusItem(
            handle = handle,
            index = gridIndex,
            onFocused = { index ->
                memory.record(RESULTS_GRID_KEY, index, handle.centerXOf(index))
            },
            // The input column is to the LEFT of the results, not above, so LEFT
            // is the route back to the keys and UP from the top row is the route
            // to the navigation. That follows the page's actual geometry rather
            // than a generic "UP means go back" convention.
            onEscapeLeft = { onEscapeLeft(); true },
            onEscapeUp = { onEscapeUp(); true },
            onScrollRequest = { direction ->
                val delta = if (direction == SpatialDirection.Down) columns else -columns
                onScroll((gridIndex + delta).coerceIn(0, (gridItemCount - 1).coerceAtLeast(0)))
            }
        )
    )
}

/** Shared directional hand-off for the input column's focusable controls. */
private fun handleKeyNavigation(
    event: androidx.compose.ui.input.key.KeyEvent,
    onNavigateLeft: (() -> Unit)?,
    onNavigateRight: (() -> Unit)?,
    onNavigateUp: (() -> Unit)?
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        Key.DirectionLeft -> onNavigateLeft?.let { it(); true } ?: false
        Key.DirectionRight -> onNavigateRight?.let { it(); true } ?: false
        Key.DirectionUp -> onNavigateUp?.let { it(); true } ?: false
        else -> false
    }
}

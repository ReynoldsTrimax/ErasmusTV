package com.erasmustv.app.ui.screens.search

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.erasmustv.app.ui.components.MediaPosterCard
import com.erasmustv.app.ui.components.TvFloatingNavBar
import com.erasmustv.app.ui.components.TvSearchSkeleton
import com.erasmustv.app.ui.focus.FocusZoneMemory
import com.erasmustv.app.ui.focus.GridFocusHandle
import com.erasmustv.app.ui.focus.SpatialDirection
import com.erasmustv.app.ui.focus.gridFocusContainer
import com.erasmustv.app.ui.focus.gridFocusItem
import com.erasmustv.app.ui.focus.rememberFocusZoneMemory
import com.erasmustv.app.ui.focus.rememberGridFocusHandle
import com.erasmustv.app.ui.navigation.NavRoutes
import kotlinx.coroutines.launch

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

/**
 * Focus-engine zone key for the results grid.
 *
 * One key for one region. Results are a single undifferentiated set — movies and
 * series share the grid — so there is exactly one focus zone below the query
 * controls and no headers inside it to step over.
 */
private const val RESULTS_GRID_KEY = "search_results"

/** Results columns. The grid owns the full page width now that the on-screen key grid is gone. */
private const val RESULTS_COLUMNS = 5

/** The query bar never grows wider than this, however wide the panel is. */
private val SEARCH_BAR_MAX_WIDTH = 620.dp
private val SEARCH_BAR_HEIGHT = 50.dp

/**
 * ERASMUS SEARCH.
 *
 * A query bar, a genre strip, and one results grid — stacked, full width.
 *
 * Two deliberate departures from the previous design:
 *
 *  - **No in-app key grid.** Text is entered through the platform IME, opened by
 *    pressing OK on the bar. The custom 6x6 grid was six rows of focusables
 *    standing between the viewer and their results, and every keystroke through
 *    it re-ran a network search.
 *  - **No movie/series split.** Results arrive from TMDB's multi-search in
 *    relevance order and are shown in that order. Splitting them into two
 *    groups pushed the best match below a header and made the grid's D-pad
 *    behaviour depend on which media types happened to match.
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

    val searchBarFocusRequester = remember { FocusRequester() }
    val firstGenreFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }
    var isNavFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val horizontalMargin = rememberScreenHorizontalMargin()

    val resultsGridState = rememberLazyGridState()
    val genreListState = rememberLazyListState()
    val resultsHandle = rememberGridFocusHandle(RESULTS_GRID_KEY)
    val focusMemory = rememberFocusZoneMemory()

    val focusSearchBar: () -> Unit = {
        runCatching { searchBarFocusRequester.requestFocus() }
        Unit
    }
    val focusGenres: () -> Unit = {
        runCatching { firstGenreFocusRequester.requestFocus() }
        Unit
    }
    val focusNav: () -> Unit = {
        runCatching { navFocusRequester.requestFocus() }
        Unit
    }
    // DOWN out of the query controls enters the grid at the row last stood on,
    // falling back to its first cell.
    val focusResults: () -> Unit = {
        val remembered = focusMemory.indexFor(RESULTS_GRID_KEY) ?: 0
        if (!resultsHandle.focusIndexOrNearest(remembered)) {
            resultsHandle.focusNearestInFirstVisibleRow(null)
        }
        Unit
    }

    LaunchedEffect(Unit) {
        // A fresh visit starts on the bar, which is the page's primary control.
        // A return trip from a result's detail page goes back to that result.
        val remembered = focusMemory.indexFor(RESULTS_GRID_KEY)
        if (remembered == null || !resultsHandle.focusIndexOrNearest(remembered)) {
            focusSearchBar()
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
            focusSearchBar()
        } else {
            focusNav()
        }
    }

    androidx.activity.compose.BackHandler(enabled = isNavFocused) {
        onNavigate(NavRoutes.HOME)
    }

    // ------------------------------------------------------------------
    // Hold the last real result set while a debounced query is in flight.
    //
    // The view model emits Loading on every committed query, which would swap the
    // grid for a non-focusable skeleton and destroy the focused node. Retaining
    // the previous results means the grid only disappears when there is
    // genuinely nothing to show yet.
    // ------------------------------------------------------------------
    var lastLoadedState by remember { mutableStateOf<SearchUiState.Success?>(null) }
    (uiState as? SearchUiState.Success)?.let { lastLoadedState = it }
    val displayState: SearchUiState = when (uiState) {
        is SearchUiState.Loading -> lastLoadedState ?: uiState
        else -> uiState
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalMargin,
                    top = ErasmusDimens.NavPillContentClearance,
                    end = horizontalMargin,
                    bottom = ErasmusSpacing.Large
                )
        ) {
            SearchQueryBar(
                query = query,
                categoryName = selectedCategory?.name,
                onQueryChange = viewModel::onQueryChange,
                onNavigateUp = focusNav,
                onNavigateDown = focusGenres,
                focusRequester = searchBarFocusRequester,
                modifier = Modifier.widthIn(max = SEARCH_BAR_MAX_WIDTH)
            )

            Spacer(modifier = Modifier.height(ErasmusSpacing.Medium))

            LazyRow(
                state = genreListState,
                horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Small),
                contentPadding = PaddingValues(end = ErasmusSpacing.Medium),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(SEARCH_CATEGORIES, key = { _, it -> it.name }) { index, category ->
                    GenreChip(
                        title = category.name,
                        isSelected = selectedCategory?.name == category.name,
                        onClick = { viewModel.selectCategory(category) },
                        onNavigateUp = focusSearchBar,
                        onNavigateDown = focusResults,
                        // Edges are walls: the strip is the only thing on its row,
                        // so sideways focus must not leak out of it.
                        consumeLeft = index == 0,
                        consumeRight = index == SEARCH_CATEGORIES.lastIndex,
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstGenreFocusRequester)
                        } else Modifier
                    )
                }
            }

            Spacer(modifier = Modifier.height(ErasmusSpacing.Medium))

            when (val state = displayState) {
                is SearchUiState.Loading -> TvSearchSkeleton(columns = RESULTS_COLUMNS)

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
                                    "Try a shorter title, a different spelling, or pick a genre above."
                                } else {
                                    "Press OK on the search bar to type a title, or choose a genre."
                                }
                            )
                        }
                    } else {
                        // One header at index 0, then one cell per result. No
                        // per-media-type grouping, so grid index arithmetic is a
                        // single constant offset.
                        val gridItemCount = displayItems.size + 1
                        LazyVerticalGrid(
                            state = resultsGridState,
                            columns = GridCells.Fixed(RESULTS_COLUMNS),
                            contentPadding = PaddingValues(
                                bottom = ErasmusSpacing.XLarge
                            ),
                            horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.GridItemSpacing),
                            verticalArrangement = Arrangement.spacedBy(ErasmusDimens.GridRowSpacing),
                            modifier = Modifier
                                .fillMaxSize()
                                .gridFocusContainer(resultsHandle, resultsGridState, gridItemCount)
                        ) {
                            item(span = { GridItemSpan(RESULTS_COLUMNS) }) {
                                ResultsHeader(title = headerTitle, count = displayItems.size)
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
                                    onEscapeUp = focusGenres,
                                    onScroll = { target ->
                                        coroutineScope.launch {
                                            runCatching {
                                                resultsGridState.animateScrollToItem(target)
                                            }
                                        }
                                    },
                                    gridItemCount = gridItemCount
                                )
                            }
                        }
                    }
                }

                else -> Unit
            }
        }

        TvFloatingNavBar(
            currentRoute = NavRoutes.SEARCH,
            activeProfile = activeProfile,
            onNavigate = onNavigate,
            onProfileClick = onProfileClick,
            navFocusRequester = navFocusRequester,
            onFocusChanged = { focused -> isNavFocused = focused },
            // DOWN out of the nav lands on the bar, which is directly beneath it.
            onNavigateIntoContent = {
                runCatching { searchBarFocusRequester.requestFocus() }.isSuccess
            },
            onReselectCurrent = focusSearchBar,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * The query bar.
 *
 * Two states in one control. At rest it is a focusable button showing the
 * current query; pressing OK swaps in a [BasicTextField] and raises the platform
 * IME, which is the only text-entry surface on the page.
 *
 * The IME is raised on *activation*, never on focus. A field that opens the
 * keyboard merely because the D-pad passed over it makes the page impossible to
 * traverse.
 */
@Composable
private fun SearchQueryBar(
    query: String,
    categoryName: String?,
    onQueryChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var isEditing by remember { mutableStateOf(false) }
    val editorFocusRequester = remember { FocusRequester() }
    // Guards the editor's own focus-loss handler: it fires once with
    // `isFocused == false` before focus has been requested, and acting on that
    // would close the editor the same frame it opened.
    var editorHasTakenFocus by remember { mutableStateOf(false) }

    val stopEditing: () -> Unit = {
        keyboardController?.hide()
        isEditing = false
        editorHasTakenFocus = false
        runCatching { focusRequester.requestFocus() }
        Unit
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            runCatching { editorFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    androidx.activity.compose.BackHandler(enabled = isEditing) { stopEditing() }

    val isActive = query.isNotEmpty() || categoryName != null
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused || isEditing -> FocusWhite
            isActive -> Color.White.copy(alpha = 0.26f)
            else -> Color.White.copy(alpha = 0.10f)
        },
        animationSpec = tween(TvMotion.DURATION_FOCUS_FAST),
        label = "searchBarBorder"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SEARCH_BAR_HEIGHT)
            .clip(ErasmusShapes.InputLarge)
            .background(SurfaceCardRest, ErasmusShapes.InputLarge)
            .border(width = 1.5.dp, color = borderColor, shape = ErasmusShapes.InputLarge)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isActive || isFocused) TextPrimary.copy(alpha = 0.9f) else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            if (isEditing) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = ErasmusTvTypography.Body.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(FocusWhite),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { stopEditing() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(editorFocusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                editorHasTakenFocus = true
                            } else if (editorHasTakenFocus) {
                                // The IME was dismissed, or focus moved away.
                                keyboardController?.hide()
                                isEditing = false
                                editorHasTakenFocus = false
                            }
                        }
                )
            } else {
                Text(
                    text = when {
                        query.isNotEmpty() -> query
                        categoryName != null -> categoryName
                        else -> "Press OK to search titles"
                    },
                    style = ErasmusTvTypography.Body.copy(
                        fontSize = 15.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isActive) TextPrimary else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { isEditing = true }
                        )
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (event.key) {
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    isEditing = true
                                    true
                                }
                                Key.DirectionUp -> { onNavigateUp(); true }
                                Key.DirectionDown -> { onNavigateDown(); true }
                                // The bar spans the row on its own; sideways
                                // presses have nowhere legitimate to go.
                                Key.DirectionLeft, Key.DirectionRight -> true
                                else -> false
                            }
                        }
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = if (query.isEmpty()) {
                                "Search. Press OK to open the keyboard."
                            } else {
                                "Search, current query $query. Press OK to edit."
                            }
                        }
                )
            }
        }
    }
}

/**
 * Genre chip. Sized for a single horizontal strip beneath the query bar, so the
 * genre list costs one row of vertical D-pad travel instead of ten.
 */
@Composable
private fun GenreChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    modifier: Modifier = Modifier,
    consumeLeft: Boolean = false,
    consumeRight: Boolean = false
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
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { onNavigateUp(); true }
                    Key.DirectionDown -> { onNavigateDown(); true }
                    Key.DirectionLeft -> consumeLeft
                    Key.DirectionRight -> consumeRight
                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp),
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
            },
            maxLines = 1
        )
    }
}

/** Grid heading. The count is plain muted text — reference information, not a status. */
@Composable
private fun ResultsHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ErasmusSpacing.Small),
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
 * A results cell. UP from the top row reaches the genre strip; the left, right
 * and bottom edges are walls, which is what stops a press at the edge of the
 * grid from teleporting focus into unrelated chrome.
 */
@Composable
private fun ResultCard(
    item: MediaItem,
    gridIndex: Int,
    handle: GridFocusHandle,
    memory: FocusZoneMemory,
    onClick: () -> Unit,
    onEscapeUp: () -> Unit,
    onScroll: (Int) -> Unit,
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
            onEscapeUp = { onEscapeUp(); true },
            onEscapeLeft = { true },
            onScrollRequest = { direction ->
                val delta = if (direction == SpatialDirection.Down) RESULTS_COLUMNS else -RESULTS_COLUMNS
                onScroll((gridIndex + delta).coerceIn(0, (gridItemCount - 1).coerceAtLeast(0)))
            }
        )
    )
}

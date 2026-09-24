package com.erasmustv.app.ui.screens.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.config.AppConfig
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
import com.erasmustv.app.core.theme.rememberHeroAmbientColor
import com.erasmustv.app.data.model.CastMember
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.MediaRating
import com.erasmustv.app.data.model.TvEpisode
import com.erasmustv.app.data.model.TvSeason
import com.erasmustv.app.ui.components.ErasmusActionButton
import com.erasmustv.app.ui.components.ErasmusButtonStyle
import com.erasmustv.app.ui.components.ErasmusCardArtwork
import com.erasmustv.app.ui.components.ErasmusCardBottomScrim
import com.erasmustv.app.ui.components.ErasmusEmptyState
import com.erasmustv.app.ui.components.ErasmusRailHeader
import com.erasmustv.app.ui.components.HeroFrostedBackdrop
import com.erasmustv.app.ui.components.MediaSectionRow
import com.erasmustv.app.ui.components.TvDetailSkeleton
import com.erasmustv.app.ui.components.TvFloatingNavBar
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.focus.FeedFocusCoordinator
import com.erasmustv.app.ui.focus.RailFocusHandle
import com.erasmustv.app.ui.focus.RailZone
import com.erasmustv.app.ui.focus.SingleTargetZone
import com.erasmustv.app.ui.focus.railFocusContainer
import com.erasmustv.app.ui.focus.railFocusItem
import com.erasmustv.app.ui.focus.rememberFeedBringIntoViewSpec
import com.erasmustv.app.ui.focus.rememberFeedFocusCoordinator
import com.erasmustv.app.ui.focus.rememberFocusZoneMemory
import com.erasmustv.app.ui.focus.rememberRailFocusHandleStore
import com.erasmustv.app.ui.navigation.NavRoutes
import kotlinx.coroutines.launch

/**
 * Left inset for detail copy. Slightly deeper than a rail gutter so the hero
 * composition reads as a title card rather than as another shelf.
 */
private val DETAIL_CONTENT_INSET = 82.dp

/**
 * Readability wash over the backdrop, weighted to the left where the copy sits.
 * Stops are close together so the falloff has no visible banding on an OLED.
 */
private val DetailHeroHorizontalGradient = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.00f to PitchBlack.copy(alpha = 0.95f),
        0.18f to PitchBlack.copy(alpha = 0.86f),
        0.38f to PitchBlack.copy(alpha = 0.62f),
        0.58f to PitchBlack.copy(alpha = 0.30f),
        0.78f to PitchBlack.copy(alpha = 0.08f),
        1.00f to Color.Transparent
    )
)

/**
 * Vertical shading that grounds the lower third for copy legibility. It no
 * longer terminates in opaque black: the hard floor is what created a visible
 * seam against the screen-level [HeroAmbientWash]. Instead the artwork
 * composite is feathered to transparent by [DetailHeroBottomFadeMask] so the
 * ambient wash shows *through* the hero's lower edge — one continuous surface.
 */
private val DetailHeroVerticalGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to PitchBlack.copy(alpha = 0.55f),
        0.16f to Color.Transparent,
        0.52f to Color.Transparent,
        0.70f to PitchBlack.copy(alpha = 0.22f),
        0.84f to PitchBlack.copy(alpha = 0.42f),
        0.94f to PitchBlack.copy(alpha = 0.58f),
        1.00f to PitchBlack.copy(alpha = 0.66f)
    )
)

/**
 * Alpha mask applied to the artwork composite via [BlendMode.DstIn]: keeps the
 * hero opaque through its upper two-thirds, then feathers the entire stack
 * (image + shading) to fully transparent at the bottom so the ambient wash
 * beneath becomes the visible surface. This is what dissolves the detail hero
 * into the page instead of ending on a hard black edge.
 */
private val DetailHeroBottomFadeMask = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color.Black,
        0.58f to Color.Black,
        0.70f to Color.Black.copy(alpha = 0.88f),
        0.80f to Color.Black.copy(alpha = 0.60f),
        0.88f to Color.Black.copy(alpha = 0.34f),
        0.95f to Color.Black.copy(alpha = 0.12f),
        1.00f to Color.Transparent
    )
)

@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
/**
 * Focus-engine zone keys for a detail page. The page is structurally a feed, so
 * it declares its vertical bands the same way a browse screen does.
 */
private const val ZONE_ACTIONS = "__detail_actions__"
private const val ZONE_SEASONS = "__detail_seasons__"
private const val ZONE_EPISODES = "__detail_episodes__"
private const val ZONE_SIMILAR = "__detail_similar__"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaDetailScreen(
    viewModel: MediaDetailViewModel,
    onBackClick: () -> Unit,
    onPlayClick: (mediaType: String, id: String, title: String, season: Int?, episode: Int?, posterPath: String?, backdropPath: String?, logoPath: String?, tagline: String?) -> Unit,
    onSimilarClick: (MediaItem) -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // ------------------------------------------------------------------
    // Focus engine.
    //
    // A detail page is structurally a feed — an actions row, then episodes, then
    // recommendations — so it uses the same coordinator, the same travelling
    // column, and the same zone map as the browse screens rather than its own
    // hand-rolled chain of `animateScrollToItem` + `delay(40)` + `requestFocus`.
    // ------------------------------------------------------------------
    val focusMemory = rememberFocusZoneMemory()
    val coordinator = rememberFeedFocusCoordinator(listState, coroutineScope, focusMemory)
    val handleStore = rememberRailFocusHandleStore()

    val primaryActionFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }
    var isNavFocused by remember { mutableStateOf(false) }

    val focusNav: () -> Boolean = {
        try {
            navFocusRequester.requestFocus()
            true
        } catch (_: Exception) {
            false
        }
    }

    // BACK on a detail page leaves the page, in one press.
    //
    // The browsing screens deliberately do the two-step dance (first BACK moves
    // focus to the navigation, because they are roots with nowhere to pop to).
    // A detail page is not a root: the user pressed BACK to go back, and making
    // them press it twice reads as the remote having missed the first press.
    BackHandler {
        onBackClick()
    }

    val mediaId = (uiState as? DetailUiState.MovieSuccess)?.movie?.id
        ?: (uiState as? DetailUiState.TvSuccess)?.tv?.id

    // Entry focus is the primary action, claimed once per title.
    //
    // Keyed on the media id rather than on the whole state, so the watchlist
    // toggle, the resume position arriving, or a season being selected cannot
    // re-run it and drag focus back to Watch Now from wherever the user is.
    var focusedTitleId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mediaId) {
        if (mediaId == null || focusedTitleId == mediaId) return@LaunchedEffect
        // Returning from the player: restore the exact control the user left.
        if (!coordinator.restoreRememberedFocus()) {
            runCatching { primaryActionFocusRequester.requestFocus() }
        }
        focusedTitleId = mediaId
    }

    // The floating navigation overlays the top of the page, so anything scrolled
    // to the top must stop clear of it; the bottom inset keeps a focused card's
    // lift from being clipped by the screen edge.
    val navClearancePx = with(LocalDensity.current) {
        ErasmusDimens.NavPillContentClearance.toPx()
    }
    val focusClearancePx = with(LocalDensity.current) {
        ErasmusDimens.FocusScrollClearance.toPx()
    }
    val detailBringIntoViewSpec = rememberFeedBringIntoViewSpec(navClearancePx, focusClearancePx)

    // Artwork-derived ambience, identical to the Home/Movies/TV heroes so a
    // detail page feels like the same world rather than a separate screen.
    val ambientArtworkPath = (uiState as? DetailUiState.MovieSuccess)?.movie
        ?.let { it.backdropPath ?: it.posterPath }
        ?: (uiState as? DetailUiState.TvSuccess)?.tv?.let { it.backdropPath ?: it.posterPath }
    val ambientPosterPath = (uiState as? DetailUiState.MovieSuccess)?.movie?.posterPath
        ?: (uiState as? DetailUiState.TvSuccess)?.tv?.posterPath

    val ambientColor = rememberHeroAmbientColor(
        artworkPath = ambientArtworkPath,
        imageUrl = AppConfig.backdropUrl(ambientArtworkPath),
        fallbackImageUrl = AppConfig.posterUrl(ambientPosterPath)
    )
    val heroImageUrl = AppConfig.backdropUrl(ambientArtworkPath)
        ?: AppConfig.posterUrl(ambientPosterPath)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        if (uiState is DetailUiState.MovieSuccess || uiState is DetailUiState.TvSuccess) {
            HeroFrostedBackdrop(
                artworkUrl = heroImageUrl,
                ambientColor = ambientColor,
                heroHeight = ErasmusDimens.DetailHeroHeight,
                modifier = Modifier.fillMaxSize()
            )
        }

        when (val state = uiState) {
            is DetailUiState.Loading -> TvDetailSkeleton()

            is DetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ErasmusSpacing.XLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ErasmusEmptyState(
                        title = "Can't load this title",
                        message = state.message
                    )
                    Spacer(modifier = Modifier.height(ErasmusSpacing.Large))
                    Row(horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.MediumSmall)) {
                        ErasmusActionButton(
                            text = "Try Again",
                            onClick = { viewModel.loadDetails() },
                            style = ErasmusButtonStyle.Primary
                        )
                        ErasmusActionButton(
                            text = "Go Back",
                            onClick = onBackClick,
                            style = ErasmusButtonStyle.Secondary
                        )
                    }
                }
            }

            is DetailUiState.MovieSuccess -> {
                val movie = state.movie

                // Zone map for a film: the actions row, then recommendations.
                // The cast rail sits between them and is deliberately absent —
                // it holds no interactive elements, so making it a focus stop
                // would mean pressing OK on a headshot and having nothing happen.
                // It scrolls through view on the way past instead.
                val hasSimilar = movie.similar.isNotEmpty()
                val similarRow = 1 + (if (movie.cast.isNotEmpty()) 1 else 0)
                val similarHandle = handleStore.handleFor(ZONE_SIMILAR)

                coordinator.setZoneOrder(
                    buildList {
                        add(ZONE_ACTIONS)
                        if (hasSimilar) add(ZONE_SIMILAR)
                    }
                )
                coordinator.register(
                    SingleTargetZone(ZONE_ACTIONS, 0, primaryActionFocusRequester)
                )
                if (hasSimilar) {
                    coordinator.register(RailZone(similarRow, similarHandle))
                }

                CompositionLocalProvider(LocalBringIntoViewSpec provides detailBringIntoViewSpec) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = ErasmusSpacing.SectionLarge)
                    ) {
                        item(key = ZONE_ACTIONS) {
                            DetailHero(
                                isTv = false,
                                title = movie.title,
                                backdropPath = movie.backdropPath ?: movie.posterPath,
                                logoPath = movie.logoPath,
                                tagline = movie.tagline,
                                voteAverage = movie.voteAverage,
                                voteCount = movie.voteCount,
                                year = movie.year,
                                runtimeOrSeasons = movie.durationFormatted,
                                certification = movie.certification,
                                status = movie.status,
                                genres = movie.genres.map { it.name },
                                overview = movie.overview,
                                cast = movie.cast,
                                ratings = movie.ratings,
                                inWatchlist = state.inWatchlist,
                                primaryActionLabel = if (state.resumePosition > 0) "Resume" else "Watch Now",
                                playFocusRequester = primaryActionFocusRequester,
                                onFocused = { coordinator.onZoneFocused(ZONE_ACTIONS) },
                                onPlayClick = {
                                    onPlayClick(
                                        "movie", movie.id, movie.title, null, null,
                                        movie.posterPath, movie.backdropPath, movie.logoPath, movie.tagline
                                    )
                                },
                                onToggleWatchlist = { viewModel.toggleWatchlist() },
                                onNavigateLeftToRail = { focusNav() },
                                onNavigateUpToNav = { focusNav() },
                                onNavigateDown = { coordinator.moveVertical(ZONE_ACTIONS, +1) },
                                onBackClick = onBackClick
                            )
                        }

                        if (movie.cast.isNotEmpty()) {
                            item(key = "__cast__") {
                                Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
                                CastSection(movie.cast)
                            }
                        }

                        if (hasSimilar) {
                            item(key = ZONE_SIMILAR) {
                                Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
                                MediaSectionRow(
                                    title = "More Like This",
                                    items = movie.similar,
                                    onItemClick = onSimilarClick,
                                    handle = similarHandle,
                                    coordinator = coordinator,
                                    onLeftEdge = focusNav
                                )
                            }
                        }
                    }
                }
            }

            is DetailUiState.TvSuccess -> {
                val tv = state.tv
                val selectedSeasonNum = state.selectedSeason?.seasonNumber ?: 1
                val primaryLabel = if (state.resumePosition > 0) {
                    "Resume S$selectedSeasonNum E1"
                } else {
                    "Watch Now"
                }

                // Zone map for a series. The season chips are their own zone
                // rather than an appendage of the episode rail, which is what
                // gives the chips → episodes → recommendations chain a defined
                // step in each direction instead of the dead stop that existed
                // whenever a series had more than one season.
                val hasEpisodes = tv.seasons.isNotEmpty()
                val hasMultipleSeasons = tv.seasons.count { it.seasonNumber > 0 } > 1
                val hasSimilar = tv.similar.isNotEmpty()

                val episodesRow = 1
                val similarRow = episodesRow +
                    (if (hasEpisodes) 1 else 0) +
                    (if (tv.cast.isNotEmpty()) 1 else 0)

                val seasonHandle = handleStore.handleFor(ZONE_SEASONS)
                val episodeHandle = handleStore.handleFor(ZONE_EPISODES)
                val similarHandle = handleStore.handleFor(ZONE_SIMILAR)

                coordinator.setZoneOrder(
                    buildList {
                        add(ZONE_ACTIONS)
                        if (hasEpisodes && hasMultipleSeasons) add(ZONE_SEASONS)
                        if (hasEpisodes) add(ZONE_EPISODES)
                        if (hasSimilar) add(ZONE_SIMILAR)
                    }
                )
                coordinator.register(
                    SingleTargetZone(ZONE_ACTIONS, 0, primaryActionFocusRequester)
                )
                if (hasEpisodes) {
                    if (hasMultipleSeasons) {
                        coordinator.register(RailZone(episodesRow, seasonHandle))
                    }
                    coordinator.register(RailZone(episodesRow, episodeHandle))
                }
                if (hasSimilar) {
                    coordinator.register(RailZone(similarRow, similarHandle))
                }

                CompositionLocalProvider(LocalBringIntoViewSpec provides detailBringIntoViewSpec) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = ErasmusSpacing.SectionLarge)
                    ) {
                        item(key = ZONE_ACTIONS) {
                            DetailHero(
                                isTv = true,
                                title = tv.title,
                                backdropPath = tv.backdropPath ?: tv.posterPath,
                                logoPath = tv.logoPath,
                                tagline = tv.tagline,
                                voteAverage = tv.voteAverage,
                                voteCount = tv.voteCount,
                                year = tv.year,
                                runtimeOrSeasons = tv.numberOfSeasons?.let {
                                    if (it == 1) "1 Season" else "$it Seasons"
                                } ?: "",
                                certification = tv.certification,
                                status = tv.status,
                                genres = tv.genres.map { it.name },
                                overview = tv.overview,
                                cast = tv.cast,
                                ratings = tv.ratings,
                                inWatchlist = state.inWatchlist,
                                primaryActionLabel = primaryLabel,
                                playFocusRequester = primaryActionFocusRequester,
                                onFocused = { coordinator.onZoneFocused(ZONE_ACTIONS) },
                                onPlayClick = {
                                    val s = state.selectedSeason?.seasonNumber ?: 1
                                    onPlayClick(
                                        "tv", tv.id, tv.title, s, 1,
                                        tv.posterPath, tv.backdropPath, tv.logoPath, tv.tagline
                                    )
                                },
                                onToggleWatchlist = { viewModel.toggleWatchlist() },
                                onNavigateLeftToRail = { focusNav() },
                                onNavigateUpToNav = { focusNav() },
                                onNavigateDown = { coordinator.moveVertical(ZONE_ACTIONS, +1) },
                                onBackClick = onBackClick
                            )
                        }

                        if (hasEpisodes) {
                            item(key = ZONE_EPISODES) {
                                Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
                                TvEpisodesSection(
                                    seasons = tv.seasons,
                                    selectedSeason = state.selectedSeason,
                                    onSeasonSelect = { viewModel.selectSeason(it) },
                                    onEpisodeClick = { ep ->
                                        onPlayClick(
                                            "tv", tv.id, tv.title, ep.seasonNumber, ep.episodeNumber,
                                            tv.posterPath, ep.stillPath ?: tv.backdropPath,
                                            tv.logoPath, tv.tagline
                                        )
                                    },
                                    seasonHandle = seasonHandle,
                                    episodeHandle = episodeHandle,
                                    coordinator = coordinator,
                                    onLeftEdge = focusNav
                                )
                            }
                        }

                        if (tv.cast.isNotEmpty()) {
                            item(key = "__cast__") {
                                Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
                                CastSection(tv.cast)
                            }
                        }

                        if (hasSimilar) {
                            item(key = ZONE_SIMILAR) {
                                Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
                                MediaSectionRow(
                                    title = "More Like This",
                                    items = tv.similar,
                                    onItemClick = onSimilarClick,
                                    handle = similarHandle,
                                    coordinator = coordinator,
                                    onLeftEdge = focusNav
                                )
                            }
                        }
                    }
                }
            }
        }

        if (onNavigate != null) {
            TvFloatingNavBar(
                currentRoute = NavRoutes.DETAILS,
                activeProfile = activeProfile,
                onNavigate = onNavigate,
                onProfileClick = { onProfileClick?.invoke() },
                navFocusRequester = navFocusRequester,
                onFocusChanged = { isNavFocused = it },
                // Returns false when no zone could take focus, so the nav's own
                // BACK fallback still works. The previous version always returned
                // true, which silently disabled it.
                onNavigateIntoContent = { coordinator.enterContent() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * DETAIL HERO.
 *
 * Full-bleed artwork with left-weighted copy, matching the Home/Movies/TV hero
 * composition so the app has exactly one hero language. Only information that
 * actually exists in the loaded data is rendered — no "—" placeholders, no
 * empty rating tiles, no invented actions.
 */
@Composable
private fun DetailHero(
    isTv: Boolean,
    title: String,
    backdropPath: String?,
    logoPath: String?,
    tagline: String?,
    voteAverage: Double?,
    voteCount: Int?,
    year: String?,
    runtimeOrSeasons: String,
    certification: String?,
    status: String?,
    genres: List<String>,
    overview: String?,
    cast: List<CastMember>,
    ratings: List<MediaRating>,
    inWatchlist: Boolean,
    primaryActionLabel: String,
    playFocusRequester: FocusRequester?,
    onPlayClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onNavigateLeftToRail: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    /** UP from the hero's Back affordance — the top of the page reaches the nav. */
    onNavigateUpToNav: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val backFocusRequester = remember { FocusRequester() }
    val imageRequest = remember(backdropPath) {
        ImageRequest.Builder(context)
            .data(AppConfig.backdropUrl(backdropPath))
            .crossfade(TvMotion.DURATION_HERO_CROSSFADE)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ErasmusDimens.DetailHeroHeight)
    ) {
        // Artwork composite (image + shading) rendered into one offscreen layer,
        // then alpha-masked at the bottom so the whole stack — not just the
        // photo — dissolves into the screen-level ambient wash behind the feed.
        // Masking the composite rather than the image alone is what removes the
        // hard horizontal seam at the hero's lower edge.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = DetailHeroBottomFadeMask, blendMode = BlendMode.DstIn)
                }
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DetailHeroHorizontalGradient)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DetailHeroVerticalGradient)
            )
        }

        if (onBackClick != null) {
            HeroBackButton(
                onClick = onBackClick,
                onNavigateLeftToRail = onNavigateLeftToRail,
                onNavigateUp = onNavigateUpToNav,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = DETAIL_CONTENT_INSET, top = 24.dp)
                    .focusRequester(backFocusRequester)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.58f)
                .padding(
                    start = DETAIL_CONTENT_INSET,
                    bottom = ErasmusSpacing.Large
                )
        ) {
            // Prefer the title's own logotype when the catalogue has one — it is
            // the title's real typography and instantly more cinematic than a
            // system font rendering of the same words.
            var isLogoError by remember(logoPath) { mutableStateOf(false) }

            if (!logoPath.isNullOrBlank() && !isLogoError) {
                Box(
                    modifier = Modifier
                        .heightIn(
                            min = ErasmusDimens.HeroLogoMinHeight,
                            max = ErasmusDimens.HeroLogoMaxHeight
                        )
                        .fillMaxWidth(0.92f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(AppConfig.logoUrl(logoPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth(),
                        onError = { isLogoError = true }
                    )
                }
            } else {
                Text(
                    text = title,
                    style = ErasmusTvTypography.HeroTitleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!tagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(ErasmusSpacing.Small))
                Text(
                    text = tagline,
                    style = ErasmusTvTypography.Body.copy(
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(ErasmusSpacing.MediumSmall))

            // One quiet metadata line instead of a row of bordered pills. Pills
            // turn factual data into visual noise; a single separated line reads
            // faster and keeps the artwork as the loudest element.
            val metadataLine = remember(isTv, certification, year, runtimeOrSeasons, status) {
                buildList {
                    add(if (isTv) "Series" else "Film")
                    certification?.takeIf { it.isNotBlank() }?.let(::add)
                    year?.takeIf { it.isNotBlank() }?.let(::add)
                    runtimeOrSeasons.takeIf { it.isNotBlank() }?.let(::add)
                    if (isTv) status?.takeIf { it.isNotBlank() }?.let(::add)
                }.joinToString("  ·  ")
            }
            Text(
                text = metadataLine,
                style = ErasmusTvTypography.CardMeta,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val ratingLine = remember(ratings, voteAverage, voteCount) {
                buildRatingLine(ratings, voteAverage, voteCount)
            }
            if (ratingLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ratingLine,
                    style = ErasmusTvTypography.CardMeta,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = genres.take(3).joinToString("  ·  "),
                    style = ErasmusTvTypography.CardMeta,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            overview?.takeIf { it.isNotBlank() }?.let { text ->
                Spacer(modifier = Modifier.height(ErasmusSpacing.MediumSmall))
                Text(
                    text = text,
                    style = ErasmusTvTypography.BodyLarge,
                    color = TextPrimary.copy(alpha = 0.78f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 560.dp)
                )
            }

            Spacer(modifier = Modifier.height(ErasmusSpacing.Medium))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.MediumSmall),
                modifier = Modifier
                    // UP from the actions goes to the visible Back affordance
                    // directly above them, and UP again reaches the navigation.
                    // Two presses, both landing on a real control in its true
                    // spatial position — rather than teleporting past a control
                    // the user can plainly see.
                    .focusProperties { up = backFocusRequester }
                    .onFocusChanged { if (it.hasFocus) onFocused?.invoke() }
            ) {
                ErasmusActionButton(
                    text = primaryActionLabel,
                    onClick = onPlayClick,
                    icon = Icons.Default.PlayArrow,
                    style = ErasmusButtonStyle.Primary,
                    shape = CircleShape,
                    modifier = playFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                    onNavigateLeft = onNavigateLeftToRail,
                    onNavigateDown = onNavigateDown
                )

                ErasmusActionButton(
                    text = if (inWatchlist) "In Watch List" else "Add to Watch List",
                    onClick = onToggleWatchlist,
                    icon = if (inWatchlist) Icons.Default.Check else Icons.Default.BookmarkBorder,
                    style = ErasmusButtonStyle.Secondary,
                    shape = CircleShape,
                    contentDescriptionOverride = if (inWatchlist) {
                        "In your Watch List. Select to remove."
                    } else {
                        "Add to your Watch List"
                    },
                    onNavigateDown = onNavigateDown
                )

                cast.firstOrNull()?.name?.let { lead ->
                    Text(
                        text = "Starring $lead",
                        style = ErasmusTvTypography.CardMeta,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Builds a single line from whichever external ratings actually resolved, e.g.
 * `TMDB 7.8 · IMDb 8.1 · Rotten Tomatoes 94%`. Providers that returned nothing
 * are omitted entirely rather than rendered as an empty tile.
 *
 * The raw vote count is deliberately excluded: with four providers resolved it
 * pushed the line past the copy column and got ellipsised, and "how many people
 * voted" is the least useful number on the page.
 */
private fun buildRatingLine(
    ratings: List<MediaRating>,
    voteAverage: Double?,
    voteCount: Int?
): String {
    fun scoreFor(vararg keys: String): String? = ratings.firstOrNull { rating ->
        keys.any { key ->
            rating.provider.equals(key, ignoreCase = true) ||
                rating.label.contains(key, ignoreCase = true)
        }
    }?.score?.takeIf { it.isNotBlank() && it != "—" }

    val parts = buildList {
        val tmdb = scoreFor("tmdb")
            ?: voteAverage?.takeIf { it > 0 }?.let { String.format(java.util.Locale.US, "%.1f", it) }
        tmdb?.let { add("TMDB $it") }
        scoreFor("imdb")?.let { add("IMDb $it") }
        scoreFor("rotten_tomatoes", "rotten")?.let { add("Rotten Tomatoes $it") }
        scoreFor("metacritic")?.let { add("Metacritic $it") }
    }

    return parts.joinToString("  ·  ")
}

/**
 * Quiet circular Back affordance. The remote's BACK button is the primary way
 * out; this exists as a visible equivalent, so it is sized and toned to be
 * findable without competing with the title treatment beside it.
 */
@Composable
private fun HeroBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateLeftToRail: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Back"
            }
            .size(40.dp)
            .clip(CircleShape)
            .background(
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isFocused) FocusWhite else Color.White.copy(alpha = 0.18f),
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
                            onNavigateLeftToRail?.invoke()
                            true
                        }
                        // The Back affordance is the topmost control in the page
                        // body, so UP from here is the page's route into the
                        // floating navigation. Consumed either way, so focus can
                        // never escape off the top of the page.
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

/**
 * EPISODES.
 *
 * Season chips above a horizontal rail of 16:9 episode stills. Episode stills
 * are landscape because they are frames of video, not catalogue posters — this
 * is the one other place where wide artwork is correct, and it is never a
 * content *shelf*.
 *
 * ## Focus
 *
 * The chips and the episode rail are two distinct zones in the page's focus map,
 * both driven by the shared engine. Previously neither was: the page's single
 * `episodesFocusRequester` was attached to the first episode card only when
 * `realSeasons.size <= 1`, so on *every multi-season series* it was attached to
 * nothing at all. DOWN from Watch Now silently did nothing, and the whole
 * episodes region was unreachable by remote. Registering both as zones makes
 * the season → episode → recommendations chain fall out of the same mechanism
 * every shelf on every other page uses.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvEpisodesSection(
    seasons: List<TvSeason>,
    selectedSeason: TvSeason?,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeClick: (TvEpisode) -> Unit,
    seasonHandle: RailFocusHandle,
    episodeHandle: RailFocusHandle,
    coordinator: FeedFocusCoordinator,
    onLeftEdge: () -> Boolean
) {
    val realSeasons = remember(seasons) { seasons.filter { it.seasonNumber > 0 } }
    val episodes = selectedSeason?.episodes ?: emptyList()

    Column(modifier = Modifier.fillMaxWidth()) {
        ErasmusRailHeader(title = "Episodes")

        Spacer(modifier = Modifier.height(ErasmusSpacing.MediumSmall))

        if (realSeasons.size > 1) {
            val seasonListState = rememberLazyListState()
            LazyRow(
                state = seasonListState,
                contentPadding = PaddingValues(
                    start = ErasmusDimens.RailStartGutter,
                    end = ErasmusDimens.RailEndGutter
                ),
                horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Small),
                modifier = Modifier.railFocusContainer(
                    seasonHandle,
                    seasonListState,
                    realSeasons.size
                )
            ) {
                itemsIndexed(realSeasons, key = { _, season -> season.seasonNumber }) { index, season ->
                    SeasonChip(
                        label = season.name,
                        isSelected = season.seasonNumber == selectedSeason?.seasonNumber,
                        onClick = { onSeasonSelect(season.seasonNumber) },
                        dpadModifier = Modifier.railFocusItem(
                            handle = seasonHandle,
                            coordinator = coordinator,
                            index = index,
                            isFirstItem = index == 0,
                            onLeftEdge = onLeftEdge
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(ErasmusSpacing.Medium))
        }

        val episodeListState = rememberLazyListState()
        LazyRow(
            state = episodeListState,
            contentPadding = PaddingValues(
                start = ErasmusDimens.RailStartGutter,
                end = ErasmusDimens.RailEndGutter,
                top = ErasmusDimens.RailFocusHeadroom,
                bottom = ErasmusDimens.RailFocusHeadroom
            ),
            horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.CardSpacing),
            modifier = Modifier.railFocusContainer(
                episodeHandle,
                episodeListState,
                episodes.size
            )
        ) {
            itemsIndexed(episodes, key = { _, it -> it.id }) { index, ep ->
                EpisodeCard(
                    episode = ep,
                    onClick = { onEpisodeClick(ep) },
                    dpadModifier = Modifier.railFocusItem(
                        handle = episodeHandle,
                        coordinator = coordinator,
                        index = index,
                        isFirstItem = index == 0,
                        onLeftEdge = onLeftEdge
                    )
                )
            }
        }
    }
}

@Composable
private fun SeasonChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dpadModifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
                contentDescription = "$label${if (isSelected) ", selected" else ""}"
            }
            .height(34.dp)
            .clip(ErasmusShapes.Button)
            .background(
                color = when {
                    isFocused -> Color.White
                    isSelected -> Color.White.copy(alpha = 0.16f)
                    else -> Color.White.copy(alpha = 0.05f)
                },
                shape = ErasmusShapes.Button
            )
            .border(
                width = 1.dp,
                color = when {
                    isFocused -> FocusWhite
                    isSelected -> Color.White.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
                shape = ErasmusShapes.Button
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .then(dpadModifier)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = ErasmusTvTypography.ButtonText.copy(
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = when {
                isFocused -> PitchBlack
                isSelected -> TextPrimary
                else -> TextSecondary
            },
            maxLines = 1
        )
    }
}

@Composable
private fun EpisodeCard(
    episode: TvEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dpadModifier: Modifier = Modifier
) {
    val imageRequest = remember(episode.stillPath) {
        AppConfig.stillUrl(episode.stillPath)
    }

    val a11yDescription = remember(episode) {
        buildString {
            append("Episode ${episode.episodeNumber}, ")
            append(episode.name)
            episode.runtime?.let { append(", $it minutes") }
        }
    }

    Column(modifier = modifier.width(226.dp)) {
        TvFocusableCard(
            onClick = onClick,
            contentDescription = a11yDescription,
            role = Role.Button,
            shape = ErasmusShapes.CardLarge,
            focusedScale = TvMotion.FocusScaleCard,
            focusedBorderColor = FocusWhite,
            focusedBorderWidth = 1.5.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .then(dpadModifier)
        ) { isFocused ->
            ErasmusCardArtwork(
                model = imageRequest,
                isFocused = isFocused,
                shape = ErasmusShapes.CardLarge,
                modifier = Modifier.fillMaxSize()
            ) {
                ErasmusCardBottomScrim(strength = 0.5f)

                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(38.dp)
                            .background(PitchBlack.copy(alpha = 0.58f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = FocusWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(ErasmusDimens.CardMetadataGap))

        Text(
            text = "${episode.episodeNumber}.  ${episode.name}",
            style = ErasmusTvTypography.CardTitle,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        episode.durationFormatted.takeIf { it.isNotBlank() }?.let { duration ->
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = duration,
                style = ErasmusTvTypography.CardMeta,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

/**
 * Cast rail. Circular portraits, because a circle crops a headshot far more
 * gracefully than a rectangle and visually separates people from titles.
 */
@Composable
private fun CastSection(cast: List<CastMember>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ErasmusRailHeader(title = "Cast")

        Spacer(modifier = Modifier.height(ErasmusDimens.RailTitleGap))

        LazyRow(
            contentPadding = PaddingValues(
                start = ErasmusDimens.RailStartGutter,
                end = ErasmusDimens.RailEndGutter
            ),
            horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Large)
        ) {
            items(cast.take(12), key = { it.name }) { member ->
                CastMemberItem(member = member)
            }
        }
    }
}

@Composable
private fun CastMemberItem(member: CastMember) {
    val context = LocalContext.current
    val imageRequest = remember(member.profilePath) {
        ImageRequest.Builder(context)
            .data(AppConfig.posterUrl(member.profilePath))
            .crossfade(180)
            .build()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(92.dp)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(SurfaceCardRest, CircleShape)
        )
        Spacer(modifier = Modifier.height(ErasmusSpacing.Small))
        Text(
            text = member.name,
            style = ErasmusTvTypography.CardMeta.copy(color = TextPrimary),
            color = TextPrimary.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        member.character?.takeIf { it.isNotBlank() }?.let { character ->
            Text(
                text = character,
                style = ErasmusTvTypography.CardMeta.copy(fontSize = 11.sp),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

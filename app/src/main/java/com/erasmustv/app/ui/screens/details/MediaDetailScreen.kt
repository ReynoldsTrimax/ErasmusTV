package com.erasmustv.app.ui.screens.details

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.BorderSubtle
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.RatingGold
import com.erasmustv.app.core.theme.SurfaceCard
import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.SurfacePill
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.CastMember
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.TvEpisode
import com.erasmustv.app.data.model.TvSeason
import com.erasmustv.app.data.model.WatchProvider
import com.erasmustv.app.ui.components.MediaSectionRow
import com.erasmustv.app.ui.components.TvFocusableCard
import com.erasmustv.app.ui.components.TvLeftNavRail
import com.erasmustv.app.ui.navigation.NavRoutes
import kotlinx.coroutines.launch

private val ElectricBlue = Color(0xFF1D90F5)

// Pre-computed gradient brushes dissolving into page background
private val DetailHeroHorizontalGradient = Brush.horizontalGradient(
    colors = listOf(
        PitchBlack.copy(alpha = 0.94f),
        PitchBlack.copy(alpha = 0.82f),
        PitchBlack.copy(alpha = 0.52f),
        PitchBlack.copy(alpha = 0.15f),
        Color.Transparent
    ),
    startX = 0f,
    endX = 1350f
)

private val DetailHeroVerticalGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Transparent,
        PitchBlack.copy(alpha = 0.20f),
        PitchBlack.copy(alpha = 0.60f),
        PitchBlack.copy(alpha = 0.90f),
        PitchBlack
    )
)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaDetailScreen(
    viewModel: MediaDetailViewModel,
    onBackClick: () -> Unit,
    onPlayClick: (mediaType: String, id: String, title: String, season: Int?, episode: Int?, posterPath: String?, backdropPath: String?) -> Unit,
    onSimilarClick: (MediaItem) -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val trailerUrl by viewModel.trailerStreamUrl.collectAsState()
    val trailerHeaders by viewModel.trailerHeaders.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()

    val primaryActionFocusRequester = remember { FocusRequester() }
    val railFocusRequester = remember { FocusRequester() }
    var isRailFocused by remember { mutableStateOf(false) }

    // Deterministic Back Navigation:
    // Single Back press on the TV remote always exits directly back to previous screen
    BackHandler {
        onBackClick()
    }

    val mediaId = (uiState as? DetailUiState.MovieSuccess)?.movie?.id ?: (uiState as? DetailUiState.TvSuccess)?.tv?.id
    LaunchedEffect(mediaId) {
        if (mediaId != null) {
            kotlinx.coroutines.delay(180)
            try {
                primaryActionFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val contentShift by animateDpAsState(
        targetValue = if (isRailFocused) 76.dp else 0.dp,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "detailContentShift"
    )

    val detailBringIntoViewSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                // Item is ABOVE the viewport (scrolled past top) — must scroll up to reveal it.
                // offset is negative when the item's top edge is above the visible area.
                if (offset < 0f) {
                    return offset // negative → scrolls LazyColumn upward
                }
                // Item is already on-screen and in the upper visible band — don't re-centre it.
                // This prevents the hero from being dragged away from the top edge when its
                // buttons receive focus.
                if (offset + size <= containerSize) {
                    return 0f
                }
                // Item is partially or fully below the viewport — centre it.
                val childCenter = offset + (size / 2f)
                val targetCenter = containerSize * 0.5f
                return childCenter - targetCenter
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = FocusWhite,
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            }
            is DetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = state.message, style = ErasmusTvTypography.Body, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    TvFocusableCard(onClick = onBackClick) {
                        Text(text = "Back", style = ErasmusTvTypography.ButtonText, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            is DetailUiState.MovieSuccess -> {
                val movie = state.movie
                val listState = rememberLazyListState()

                CompositionLocalProvider(LocalBringIntoViewSpec provides detailBringIntoViewSpec) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = contentShift.toPx()
                            }
                    ) {
                    item {
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
                            certification = movie.certification ?: "PG-13",
                            status = movie.status,
                            genres = movie.genres.map { it.name },
                            overview = movie.overview,
                            cast = movie.cast,
                            whereToWatch = movie.whereToWatch,
                            inWatchlist = state.inWatchlist,
                            resumePosition = state.resumePosition,
                            primaryActionLabel = if (state.resumePosition > 0) "Resume" else "Play Movie",
                            trailerUrl = trailerUrl,
                            trailerHeaders = trailerHeaders,
                            playFocusRequester = primaryActionFocusRequester,
                            onPlayClick = {
                                onPlayClick("movie", movie.id, movie.title, null, null, movie.posterPath, movie.backdropPath)
                            },
                            onToggleWatchlist = { viewModel.toggleWatchlist() },
                            onNavigateLeftToRail = {
                                try {
                                    railFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            },
                            onBackClick = onBackClick
                        )
                    }

                    // Tracking Widget (Reference Image 1: HAVE YOU WATCHED IT?)
                    item {
                        HaveYouWatchedSection()
                    }

                    // Where to watch providers section
                    if (movie.whereToWatch.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            WhereToWatchSection(movie.whereToWatch)
                        }
                    }

                    // Cast row
                    if (movie.cast.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            CastSection(movie.cast)
                        }
                    }

                    // Similar movies
                    if (movie.similar.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            MediaSectionRow(
                                title = "More Like This",
                                items = movie.similar,
                                onItemClick = onSimilarClick,
                                onNavigateLeftToRail = {
                                    try {
                                        railFocusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
                }
            }
            is DetailUiState.TvSuccess -> {
                val tv = state.tv
                val listState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()

                val selectedSeasonNum = state.selectedSeason?.seasonNumber ?: 1
                val primaryLabel = if (state.resumePosition > 0) {
                    "Resume · S${selectedSeasonNum}:E1"
                } else {
                    "Play · S${selectedSeasonNum}:E1"
                }

                CompositionLocalProvider(LocalBringIntoViewSpec provides detailBringIntoViewSpec) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = contentShift.toPx()
                            }
                    ) {
                    item {
                        DetailHero(
                            isTv = true,
                            title = tv.title,
                            backdropPath = tv.backdropPath ?: tv.posterPath,
                            logoPath = tv.logoPath,
                            tagline = tv.tagline,
                            voteAverage = tv.voteAverage,
                            voteCount = tv.voteCount,
                            year = tv.year,
                            runtimeOrSeasons = "${tv.numberOfSeasons ?: 1} Seasons",
                            certification = tv.certification ?: "TV-MA",
                            status = tv.status ?: "Returning Series",
                            genres = tv.genres.map { it.name },
                            overview = tv.overview,
                            cast = tv.cast,
                            whereToWatch = tv.whereToWatch,
                            inWatchlist = state.inWatchlist,
                            resumePosition = state.resumePosition,
                            primaryActionLabel = primaryLabel,
                            trailerUrl = trailerUrl,
                            trailerHeaders = trailerHeaders,
                            playFocusRequester = primaryActionFocusRequester,
                            onPlayClick = {
                                val s = state.selectedSeason?.seasonNumber ?: 1
                                onPlayClick("tv", tv.id, tv.title, s, 1, tv.posterPath, tv.backdropPath)
                            },
                            onToggleWatchlist = { viewModel.toggleWatchlist() },
                            onNavigateLeftToRail = {
                                try {
                                    railFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            },
                            onMoreEpisodesClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(1)
                                }
                            },
                            onBackClick = onBackClick
                        )
                    }

                    // TV Season Selector & Episode List
                    if (tv.seasons.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            TvEpisodesSection(
                                seasons = tv.seasons,
                                selectedSeason = state.selectedSeason,
                                onSeasonSelect = { viewModel.selectSeason(it) },
                                onEpisodeClick = { ep ->
                                    onPlayClick("tv", tv.id, tv.title, ep.seasonNumber, ep.episodeNumber, tv.posterPath, ep.stillPath ?: tv.backdropPath)
                                }
                            )
                        }
                    }

                    // Tracking Widget (Reference Image 1: HAVE YOU WATCHED IT?)
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        HaveYouWatchedSection()
                    }

                    // Where to watch providers
                    if (tv.whereToWatch.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            WhereToWatchSection(tv.whereToWatch)
                        }
                    }

                    // Cast row
                    if (tv.cast.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            CastSection(tv.cast)
                        }
                    }

                    // Similar series
                    if (tv.similar.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            MediaSectionRow(
                                title = "More Like This",
                                items = tv.similar,
                                onItemClick = onSimilarClick,
                                onNavigateLeftToRail = {
                                    try {
                                        railFocusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
                }
            }
        }

        // Frosted Glass Left Navigation Rail Overlay
        if (onNavigate != null) {
            TvLeftNavRail(
                currentRoute = NavRoutes.DETAILS,
                activeProfile = activeProfile,
                onNavigate = onNavigate,
                onProfileClick = { onProfileClick?.invoke() },
                railFocusRequester = railFocusRequester,
                onFocusChanged = { isRailFocused = it },
                onNavigateRight = {
                    var success = false
                    try {
                        primaryActionFocusRequester.requestFocus()
                        success = true
                    } catch (_: Exception) {
                        success = false
                    }
                    success
                },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

/**
 * Immersive Movie Details Hero matching Reference Image 1.
 * Features:
 *  - Background trailer playing muted on loop via Media3 ExoPlayer (Netflix-style)
 *  - Frosted glass sidebar bleed underneath (x = 0)
 *  - Middle-left positioned large title logo with italic tagline
 *  - Subtle classification, year, status badges
 *  - Genre pills
 *  - Multi-provider rating score cards (TMDB, IMDb, Rotten Tomatoes, Metacritic)
 *  - Available On streaming provider chips
 *  - Play · S1:E1 Electric Blue pill CTA with creator / cast credit
 *  - Bottom-right trailer volume and pause/play toggle controls
 */
@OptIn(UnstableApi::class)
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
    whereToWatch: List<WatchProvider>,
    inWatchlist: Boolean,
    resumePosition: Long,
    primaryActionLabel: String,
    trailerUrl: String?,
    trailerHeaders: Map<String, String>,
    playFocusRequester: FocusRequester?,
    onPlayClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onNavigateLeftToRail: (() -> Unit)? = null,
    onMoreEpisodesClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val backFocusRequester = remember { FocusRequester() }
    val imageRequest = remember(backdropPath) {
        ImageRequest.Builder(context)
            .data(AppConfig.backdropUrl(backdropPath))
            .crossfade(true)
            .build()
    }

    var isTrailerMuted by remember { mutableStateOf(true) }
    var isTrailerPlaying by remember { mutableStateOf(true) }
    var isVideoReady by remember { mutableStateOf(false) }

    // Background looping video trailer player
    val exoPlayer = remember(context) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(AppConfig.STREAM_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory {
                if (trailerHeaders.isNotEmpty()) {
                    httpDataSourceFactory.setDefaultRequestProperties(trailerHeaders)
                }
                httpDataSourceFactory.createDataSource()
            }

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isVideoReady = true
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(trailerUrl) {
        if (!trailerUrl.isNullOrBlank()) {
            exoPlayer.setMediaItem(Media3Item.fromUri(trailerUrl))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    val videoAlpha by animateFloatAsState(
        targetValue = if (isVideoReady && isTrailerPlaying) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "hero_video_fade"
    )

    val creatorOrStar = remember(cast) {
        cast.firstOrNull()?.name?.let { "Starring $it" } ?: ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        // 1. Full-bleed Backdrop Image
        AsyncImage(
            model = imageRequest,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Looping Video Trailer layer fading in over backdrop
        if (!trailerUrl.isNullOrBlank()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = videoAlpha }
            )
        }

        // 3. Dark Horizontal Vignette from left edge (x = 0)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DetailHeroHorizontalGradient)
        )

        // 4. Dark Vertical Dissolve to PitchBlack at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DetailHeroVerticalGradient)
        )

        // 0. Top-Left Back Button (Always visible on movie detail screen)
        if (onBackClick != null) {
            TvFocusableCard(
                onClick = onBackClick,
                shape = RectangleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 82.dp, top = 26.dp)
                    .focusRequester(backFocusRequester)
                    .focusProperties {
                        up = FocusRequester.Cancel
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionLeft) {
                            onNavigateLeftToRail?.invoke()
                            true
                        } else false
                    }
            ) { isFocused ->
                Row(
                    modifier = Modifier
                        .background(
                            if (isFocused) Color.White else Color(0x2E1E1E28),
                            RectangleShape
                        )
                        .border(
                            1.dp,
                            if (isFocused) Color.White else Color(0x26FFFFFF),
                            RectangleShape
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = if (isFocused) PitchBlack else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Back",
                        style = ErasmusTvTypography.Badge.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isFocused) PitchBlack else Color.White
                    )
                }
            }
        }

        // 5. Middle-Left Hero Content Column with generous headroom
        Column(
            modifier = Modifier
                .fillMaxWidth(0.60f)
                .padding(start = 82.dp, top = 74.dp, bottom = 12.dp)
                .align(Alignment.TopStart)
        ) {
            // Authentic Title Logo (enlarged height ~55-95dp)
            var isLogoError by remember(logoPath) { mutableStateOf(false) }

            if (!logoPath.isNullOrBlank() && !isLogoError) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 55.dp, max = 95.dp)
                        .fillMaxWidth(0.95f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(AppConfig.logoUrl(logoPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .heightIn(min = 60.dp, max = 110.dp)
                            .fillMaxWidth(),
                        onError = { isLogoError = true }
                    )
                }
            } else {
                Text(
                    text = title,
                    style = ErasmusTvTypography.HeroTitleLarge.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Black
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Italic Tagline (Reference Image 1: "Love was never part of the deal.")
            if (!tagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tagline,
                    style = ErasmusTvTypography.Body.copy(
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextSecondary.copy(alpha = 0.95f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Badges Row: [TV SERIES] [TV-MA] [2026] [Returning Series]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Type badge
                DetailMetadataBadge(text = if (isTv) "TV SERIES" else "MOVIE")

                // Age rating / certification
                certification?.let { cert ->
                    DetailMetadataBadge(text = cert)
                }

                // Year
                year?.let { yr ->
                    DetailMetadataBadge(text = yr)
                }

                // Status or runtime
                val statusText = if (isTv) (status ?: runtimeOrSeasons) else runtimeOrSeasons
                if (statusText.isNotBlank()) {
                    DetailMetadataBadge(text = statusText)
                }
            }

            // Genre pills row
            if (genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(7.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    genres.take(3).forEach { genreName ->
                        Box(
                            modifier = Modifier
                                .background(Color(0x2E1E1E28), RectangleShape)
                                .border(1.dp, Color(0x1FFFFFFF), RectangleShape)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = genreName.uppercase(),
                                style = ErasmusTvTypography.Badge.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-Provider Rating Cards (TMDB, IMDb, Rotten Tomatoes, Metacritic)
            RatingCardsRow(voteAverage = voteAverage, voteCount = voteCount)

            // Available On Streaming Providers Row
            if (whereToWatch.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AVAILABLE ON",
                        style = ErasmusTvTypography.Badge.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextMuted
                    )

                    whereToWatch.take(2).forEach { provider ->
                        Row(
                            modifier = Modifier
                                .background(Color(0x2B181822), RectangleShape)
                                .border(1.dp, Color(0x26FFFFFF), RectangleShape)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (!provider.logoPath.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(AppConfig.posterUrl(provider.logoPath))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = provider.name,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = provider.name,
                                style = ErasmusTvTypography.Badge.copy(fontSize = 10.sp),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Synopsis / Overview
            overview?.let { ov ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ov,
                    style = ErasmusTvTypography.Body.copy(
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = TextSecondary.copy(alpha = 0.9f)
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Play Button in Solid White + Add to List)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.focusProperties {
                    up = backFocusRequester
                }
            ) {
                // Primary Play Rectangle Button
                TvFocusableCard(
                    onClick = onPlayClick,
                    shape = RectangleShape,
                    focusedBorderColor = Color.White,
                    modifier = (playFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionLeft) {
                                onNavigateLeftToRail?.invoke()
                                true
                            } else false
                        }
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (isFocused) Color.White else Color(0xEBFFFFFF),
                                RectangleShape
                            )
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color.White else Color.Transparent,
                                shape = RectangleShape
                            )
                            .padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = PitchBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = primaryActionLabel,
                            style = ErasmusTvTypography.ButtonText.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = PitchBlack
                        )
                    }
                }

                // Add to List frosted rectangular button
                TvFocusableCard(
                    onClick = onToggleWatchlist,
                    shape = RectangleShape,
                    focusedBorderColor = Color.White
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (isFocused) Color(0x55FFFFFF) else Color(0x22FFFFFF),
                                RectangleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isFocused) Color.White else Color(0x26FFFFFF),
                                shape = RectangleShape
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (inWatchlist) Icons.Default.Check else Icons.Default.BookmarkBorder,
                            contentDescription = "My List",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (inWatchlist) "In List" else "Add to List",
                            style = ErasmusTvTypography.ButtonText.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White
                        )
                    }
                }

                // Creator / Star attribution text
                if (creatorOrStar.isNotBlank()) {
                    Text(
                        text = creatorOrStar,
                        style = ErasmusTvTypography.Badge.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 6. Bottom-Right Controls: Mute/Unmute & Pause/Play Video Trailer (Reference Image 1)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 24.dp)
                .focusProperties {
                    up = FocusRequester.Cancel
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mute / Unmute Button
            HeroControlButton(
                icon = if (isTrailerMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isTrailerMuted) "Unmute Trailer" else "Mute Trailer",
                onClick = {
                    isTrailerMuted = !isTrailerMuted
                    exoPlayer.volume = if (isTrailerMuted) 0f else 1f
                }
            )

            // Pause / Play Button
            HeroControlButton(
                icon = if (isTrailerPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isTrailerPlaying) "Pause Trailer" else "Play Trailer",
                onClick = {
                    isTrailerPlaying = !isTrailerPlaying
                    exoPlayer.playWhenReady = isTrailerPlaying
                }
            )
        }
    }
}

@Composable
private fun DetailMetadataBadge(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0x2E1E1E28), RectangleShape)
            .border(1.dp, Color(0x26FFFFFF), RectangleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ErasmusTvTypography.Badge.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )
    }
}

/**
 * 4 Multi-Provider Rating score cards matching Reference Image 1.
 */
@Composable
private fun RatingCardsRow(voteAverage: Double?, voteCount: Int?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TMDB Card
        RatingScoreCard(
            label = "TMDB",
            score = if (voteAverage != null && voteAverage > 0) String.format("%.1f/10", voteAverage) else "—",
            subText = if (voteCount != null && voteCount > 0) "$voteCount votes" else "—"
        )

        // IMDb Card
        RatingScoreCard(
            label = "IMDb",
            score = "—",
            subText = "—"
        )

        // Rotten Tomatoes Card
        RatingScoreCard(
            label = "ROTTEN TOMATOES",
            score = "—",
            subText = "—"
        )

        // Metacritic Card
        RatingScoreCard(
            label = "METACRITIC",
            score = "—",
            subText = "—"
        )
    }
}

@Composable
private fun RatingScoreCard(
    label: String,
    score: String,
    subText: String
) {
    Box(
        modifier = Modifier
            .width(82.dp)
            .background(Color(0x3D14141E), RectangleShape)
            .border(1.dp, Color(0x1FFFFFFF), RectangleShape)
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = label,
                style = ErasmusTvTypography.Badge.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = score,
                style = ErasmusTvTypography.Badge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subText,
                style = ErasmusTvTypography.Badge.copy(fontSize = 8.sp),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .clip(RectangleShape)
            .background(if (isFocused) Color(0x66FFFFFF) else Color(0x2E1E1E28), RectangleShape)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color(0x26FFFFFF),
                shape = RectangleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * "HAVE YOU WATCHED IT? Not tracked yet" Tracking Widget matching Reference Image 1.
 */
@Composable
private fun HaveYouWatchedSection() {
    var userStatus by remember { mutableStateOf<String?>("none") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 82.dp, vertical = 6.dp)
            .background(Color(0x2614141E), RectangleShape)
            .border(1.dp, Color(0x1FFFFFFF), RectangleShape)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HAVE YOU WATCHED IT?",
                    style = ErasmusTvTypography.Badge.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (userStatus) {
                        "watching" -> "Currently watching"
                        "completed" -> "Completed"
                        else -> "Not tracked yet"
                    },
                    style = ErasmusTvTypography.Badge.copy(fontSize = 10.sp),
                    color = TextMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvFocusableCard(
                    onClick = { userStatus = if (userStatus == "watching") "none" else "watching" },
                    shape = RectangleShape,
                    focusedBorderColor = Color.White
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .background(
                                when {
                                    userStatus == "watching" -> Color.White
                                    isFocused -> Color(0x40FFFFFF)
                                    else -> Color(0x20FFFFFF)
                                },
                                RectangleShape
                            )
                            .border(1.dp, if (isFocused) Color.White else Color(0x26FFFFFF), RectangleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Watching",
                            tint = if (userStatus == "watching") PitchBlack else Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Watching",
                            style = ErasmusTvTypography.ButtonText.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (userStatus == "watching") PitchBlack else Color.White
                        )
                    }
                }

                TvFocusableCard(
                    onClick = { userStatus = if (userStatus == "completed") "none" else "completed" },
                    shape = RectangleShape,
                    focusedBorderColor = Color.White
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .background(
                                when {
                                    userStatus == "completed" -> Color.White
                                    isFocused -> Color(0x40FFFFFF)
                                    else -> Color(0x20FFFFFF)
                                },
                                RectangleShape
                            )
                            .border(1.dp, if (isFocused) Color.White else Color(0x26FFFFFF), RectangleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = if (userStatus == "completed") PitchBlack else Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Completed",
                            style = ErasmusTvTypography.ButtonText.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (userStatus == "completed") PitchBlack else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvEpisodesSection(
    seasons: List<TvSeason>,
    selectedSeason: TvSeason?,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeClick: (TvEpisode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 82.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EPISODES",
                style = ErasmusTvTypography.SectionTitle.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                ),
                color = Color.White
            )

            Text(
                text = "ALL EPISODES",
                style = ErasmusTvTypography.Badge.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                ),
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Season Selection Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 82.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(seasons.filter { it.seasonNumber > 0 }) { season ->
                val isSelected = season.seasonNumber == selectedSeason?.seasonNumber
                TvFocusableCard(
                    onClick = { onSeasonSelect(season.seasonNumber) },
                    shape = RectangleShape,
                    focusedBorderColor = Color.White
                ) { isFocused ->
                    Text(
                        text = season.name,
                        style = ErasmusTvTypography.Badge.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = when {
                            isSelected -> PitchBlack
                            isFocused -> Color.White
                            else -> TextSecondary
                        },
                        modifier = Modifier
                            .background(
                                when {
                                    isSelected -> Color.White
                                    isFocused -> Color(0x33FFFFFF)
                                    else -> Color(0x1F1E1E28)
                                },
                                RectangleShape
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isSelected -> Color.White
                                    isFocused -> Color.White
                                    else -> Color(0x1FFFFFFF)
                                },
                                RectangleShape
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Episodes Horizontal Carousel (16:9 widescreen thumbnails)
        val episodes = selectedSeason?.episodes ?: emptyList()
        LazyRow(
            contentPadding = PaddingValues(horizontal = 82.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(episodes, key = { it.id }) { ep ->
                EpisodeCard(episode = ep, onClick = { onEpisodeClick(ep) })
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: TvEpisode,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(episode.stillPath) {
        ImageRequest.Builder(context)
            .data(AppConfig.stillUrl(episode.stillPath))
            .crossfade(true)
            .build()
    }

    Column(modifier = Modifier.width(230.dp)) {
        TvFocusableCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RectangleShape,
            focusedScale = 1.0f,
            focusedBorderColor = Color.White,
            focusedBorderWidth = 1.5.dp
        ) { isFocused ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .background(SurfaceCard)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark vignette over thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    PitchBlack.copy(alpha = 0.65f)
                                )
                            )
                        )
                )

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .clip(RectangleShape)
                        .background(
                            if (isFocused) Color.White else PitchBlack.copy(alpha = 0.6f),
                            RectangleShape
                        )
                        .border(
                            1.dp,
                            if (isFocused) Color.White else Color(0x33FFFFFF),
                            RectangleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isFocused) PitchBlack else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${episode.episodeNumber}. ${episode.name}",
            style = ErasmusTvTypography.CardTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = episode.durationFormatted,
            style = ErasmusTvTypography.Badge.copy(fontSize = 11.sp),
            color = TextMuted
        )
    }
}

@Composable
private fun CastSection(cast: List<CastMember>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Top Cast",
            style = ErasmusTvTypography.SectionTitle,
            modifier = Modifier.padding(horizontal = 82.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 82.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(cast.take(12)) { member ->
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
            .crossfade(true)
            .build()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = member.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RectangleShape)
                .background(SurfaceDark)
                .border(1.dp, Color(0x26FFFFFF), RectangleShape)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = member.name,
            style = ErasmusTvTypography.Badge.copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        member.character?.let { char ->
            Text(
                text = char,
                style = ErasmusTvTypography.Badge.copy(fontSize = 9.sp),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WhereToWatchSection(providers: List<WatchProvider>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Where to Watch",
            style = ErasmusTvTypography.SectionTitle,
            modifier = Modifier.padding(horizontal = 82.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 82.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(providers) { provider ->
                ProviderItem(provider = provider)
            }
        }
    }
}

@Composable
private fun ProviderItem(provider: WatchProvider) {
    val context = LocalContext.current
    val imageRequest = remember(provider.logoPath) {
        ImageRequest.Builder(context)
            .data(AppConfig.posterUrl(provider.logoPath))
            .crossfade(true)
            .build()
    }

    Row(
        modifier = Modifier
            .background(Color(0x2614141E), RectangleShape)
            .border(1.dp, Color(0x1FFFFFFF), RectangleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = provider.name,
            modifier = Modifier
                .size(24.dp)
                .clip(RectangleShape)
        )
        Text(text = provider.name, style = ErasmusTvTypography.Badge, color = TextPrimary)
    }
}


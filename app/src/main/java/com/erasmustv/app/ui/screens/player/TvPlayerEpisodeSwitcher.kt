@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.erasmustv.app.ui.screens.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.TvEpisode
import com.erasmustv.app.data.model.TvSeason
import kotlinx.coroutines.delay

/**
 * Right-docked TV in-player Episode Switcher widget inspired by reference TV streaming layouts.
 * Features an interactive Episode List view (with expandable cards, 16:9 thumbnails, and Now Playing soundwave badge)
 * and an in-place Season Selector view.
 */
@Composable
fun TvPlayerEpisodeSwitcher(
    visible: Boolean,
    showTitle: String,
    currentSeason: Int,
    currentEpisode: Int,
    seasons: List<TvSeason>,
    episodes: List<TvEpisode>,
    isLoading: Boolean,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    getEpisodeProgress: (season: Int, episode: Int) -> Float = { _, _ -> 0f },
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (Int, Int, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeasonPickerOpen by remember { mutableStateOf(false) }

    // Reset view to episode list whenever opened fresh
    LaunchedEffect(visible) {
        if (visible) {
            isSeasonPickerOpen = false
        }
    }

    if (visible) {
        BackHandler {
            if (isSeasonPickerOpen) {
                isSeasonPickerOpen = false
            } else {
                onClose()
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            initialOffsetX = { it }
        ),
        exit = fadeOut() + slideOutHorizontally(
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            targetOffsetX = { it }
        ),
        modifier = modifier
    ) {
        // Fullscreen container: left scrim area dims the video player and handles clicks to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack.copy(alpha = 0.55f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Right-docked episodes panel with locked focus trap
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(490.dp)
                    .background(Color(0xF5131316))
                    .clickable(enabled = false) {}
                    .focusProperties {
                        onExit = { FocusRequester.Cancel }
                    }
                    .padding(top = 32.dp, bottom = 32.dp, start = 28.dp, end = 28.dp)
            ) {
                if (isSeasonPickerOpen) {
                    SeasonPickerView(
                        showTitle = showTitle,
                        currentSeason = currentSeason,
                        seasons = seasons,
                        onSelectSeason = { s ->
                            onSelectSeason(s)
                            isSeasonPickerOpen = false
                        },
                        onBackToEpisodes = {
                            isSeasonPickerOpen = false
                        }
                    )
                } else {
                    EpisodeListView(
                        currentSeason = currentSeason,
                        currentEpisode = currentEpisode,
                        episodes = episodes,
                        isLoading = isLoading,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        getEpisodeProgress = getEpisodeProgress,
                        onOpenSeasonPicker = {
                            if (seasons.size > 1) {
                                isSeasonPickerOpen = true
                            }
                        },
                        onSelectEpisode = onSelectEpisode,
                        onClose = onClose
                    )
                }
            }
        }
    }
}

/**
 * View 1: Episodes list with expandable cards, 16:9 thumbnails, and Now Playing indicators.
 */
@Composable
private fun EpisodeListView(
    currentSeason: Int,
    currentEpisode: Int,
    episodes: List<TvEpisode>,
    isLoading: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    getEpisodeProgress: (season: Int, episode: Int) -> Float,
    onOpenSeasonPicker: () -> Unit,
    onSelectEpisode: (Int, Int, String) -> Unit,
    onClose: () -> Unit
) {
    val headerFocusRequester = remember { FocusRequester() }
    val displayedSeason = episodes.firstOrNull()?.seasonNumber ?: currentSeason
    val isCurrentPlayingSeason = (displayedSeason == currentSeason)
    val initialIndex = if (isCurrentPlayingSeason) {
        episodes.indexOfFirst {
            it.seasonNumber == currentSeason && it.episodeNumber == currentEpisode
        }.coerceAtLeast(0)
    } else {
        0
    }

    val episodeFocusRequesters = remember(episodes.size) {
        List(episodes.size) { FocusRequester() }
    }

    val listState = rememberLazyListState()

    // Auto-scroll and auto-focus currently playing episode (or first episode of new season) on display
    LaunchedEffect(displayedSeason, currentSeason, currentEpisode, episodes.size) {
        if (episodes.isNotEmpty()) {
            val scrollTarget = (initialIndex - 1).coerceAtLeast(0)
            listState.scrollToItem(scrollTarget)
            delay(80)
            try {
                episodeFocusRequesters.getOrNull(initialIndex)?.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val closeFocusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: Season Button + Close X
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SeasonHeaderButton(
                seasonName = "Season $displayedSeason",
                focusRequester = headerFocusRequester,
                onClick = onOpenSeasonPicker,
                onBack = onClose,
                onDpadRight = { closeFocusRequester.requestFocus() },
                onDpadDown = {
                    episodeFocusRequesters.getOrNull(initialIndex)?.requestFocus()
                        ?: episodeFocusRequesters.getOrNull(0)?.requestFocus()
                }
            )

            EpisodeCloseButton(
                focusRequester = closeFocusRequester,
                onClose = onClose,
                onDpadLeft = { headerFocusRequester.requestFocus() },
                onDpadDown = {
                    episodeFocusRequesters.getOrNull(initialIndex)?.requestFocus()
                        ?: episodeFocusRequesters.getOrNull(0)?.requestFocus()
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = FocusWhite,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (episodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No episodes available for this season.",
                    style = ErasmusTvTypography.Body,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(
                    episodes,
                    key = { _, ep -> "${ep.seasonNumber}_${ep.episodeNumber}_${ep.id}" }
                ) { index, ep ->
                    val isPlayingThis = (ep.seasonNumber == currentSeason && ep.episodeNumber == currentEpisode)
                    val progressRatio = if (isPlayingThis && durationMs > 0L) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        getEpisodeProgress(ep.seasonNumber, ep.episodeNumber)
                    }
                    EpisodeRowItem(
                        episode = ep,
                        isPlaying = isPlayingThis,
                        progressRatio = progressRatio,
                        focusRequester = episodeFocusRequesters.getOrNull(index),
                        onClick = {
                            onSelectEpisode(ep.seasonNumber, ep.episodeNumber, ep.name)
                            onClose()
                        },
                        onDpadUpAtTop = if (index == 0) {
                            { headerFocusRequester.requestFocus() }
                        } else null,
                        isLastItem = (index == episodes.size - 1),
                        onDpadLeft = { headerFocusRequester.requestFocus() }
                    )
                }
            }
        }
    }
}

/**
 * Header button displaying "← Season X". Pressing it opens the Season Picker view.
 */
@Composable
private fun SeasonHeaderButton(
    seasonName: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onBack: () -> Unit = {},
    onDpadRight: () -> Unit = {},
    onDpadDown: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            onDpadDown()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            // Lock focus inside: consume UP so it cannot escape out top of drawer
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onDpadRight()
                            true
                        }
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE,
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .then(
                if (isFocused) {
                    Modifier
                        .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                        .background(Color(0xFF1B1B1E), RoundedCornerShape(4.dp))
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = TvPlayerIcons.Back,
            contentDescription = "Change Season",
            tint = if (isFocused) FocusWhite else TextPrimary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = seasonName,
            style = ErasmusTvTypography.SectionTitle.copy(
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isFocused) FocusWhite else TextPrimary
        )
    }
}

/**
 * Close (X) button shown in the episode switcher header alongside the season selector.
 */
@Composable
private fun EpisodeCloseButton(
    focusRequester: FocusRequester,
    onClose: () -> Unit,
    onDpadLeft: () -> Unit = {},
    onDpadDown: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(38.dp)
            .focusRequester(focusRequester)
            .then(
                if (isFocused) {
                    Modifier
                        .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                        .background(Color(0xFF1B1B1E), RoundedCornerShape(4.dp))
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClose
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClose()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onDpadLeft()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            onDpadDown()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            // Lock: consume UP so focus stays inside drawer
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // Lock: consume RIGHT
                            true
                        }
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE -> {
                            onClose()
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
            imageVector = TvPlayerIcons.Close,
            contentDescription = "Close Episodes",
            tint = if (isFocused) FocusWhite else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * An episode row in the drawer.
 * When focused: expands to show 16:9 thumbnail, "Now Playing" soundwave badge, and overview description.
 * When unfocused: displays a clean compact row with episode number, name, and watch progress bar.
 */
@Composable
private fun EpisodeRowItem(
    episode: TvEpisode,
    isPlaying: Boolean,
    progressRatio: Float,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onDpadUpAtTop: (() -> Unit)? = null,
    isLastItem: Boolean = false,
    onDpadLeft: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val reqModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Box(
        modifier = reqModifier
            .fillMaxWidth()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (onDpadUpAtTop != null) {
                                onDpadUpAtTop()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (isLastItem) {
                                // Lock focus inside: consume DOWN on the last item so focus stays trapped
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (onDpadLeft != null) {
                                onDpadLeft()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // Lock focus inside: consume RIGHT
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .then(
                if (isFocused) {
                    Modifier
                        .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                        .background(Color(0xFF1B1B1E), RoundedCornerShape(4.dp))
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .padding(horizontal = 14.dp, vertical = if (isFocused) 12.dp else 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Number, Title, and Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "${episode.episodeNumber}",
                        style = ErasmusTvTypography.BodyLarge.copy(
                            fontSize = if (isPlaying) 17.sp else 15.sp,
                            fontWeight = if (isFocused || isPlaying) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isFocused || isPlaying) FocusWhite else TextSecondary
                    )

                    Text(
                        text = episode.name,
                        style = ErasmusTvTypography.BodyLarge.copy(
                            fontSize = if (isPlaying) 17.sp else 15.sp,
                            fontWeight = if (isFocused || isPlaying) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isFocused || isPlaying) FocusWhite else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Watch progress line on the right (pure white fill, mathematically accurate)
                WatchProgressBar(
                    progressRatio = progressRatio
                )
            }

            // Expanded content when focused: 16:9 thumbnail + synopsis overview
            if (isFocused) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 16:9 Thumbnail
                    Box(
                        modifier = Modifier
                            .width(148.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SurfaceDark)
                    ) {
                        val thumbUrl = AppConfig.stillUrl(episode.stillPath)
                        if (!thumbUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = episode.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF222228)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = TvPlayerIcons.Play,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Now Playing Badge with Soundwave (clean over thumbnail, no black background box)
                        if (isPlaying) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = TvPlayerIcons.Soundwave,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Now Playing",
                                        style = ErasmusTvTypography.Badge.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Synopsis / Description
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp)
                    ) {
                        val overview = episode.overview?.trim().orEmpty()
                        if (overview.isNotBlank()) {
                            Text(
                                text = overview,
                                style = ErasmusTvTypography.Body.copy(
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                ),
                                color = TextSecondary,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "No description available.",
                                style = ErasmusTvTypography.Body.copy(fontSize = 11.5.sp),
                                color = TextMuted
                            )
                        }

                        val duration = episode.durationFormatted
                        if (duration.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = duration,
                                style = ErasmusTvTypography.Badge.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sleek watch progress bar (pure white line for watched portion, subtle track for remaining)
 */
@Composable
private fun WatchProgressBar(
    progressRatio: Float
) {
    Box(
        modifier = Modifier
            .width(68.dp)
            .height(2.5.dp)
            .background(Color(0x2EFFFFFF), RoundedCornerShape(1.dp))
    ) {
        if (progressRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressRatio.coerceIn(0f, 1f))
                    .background(Color.White, RoundedCornerShape(1.dp))
            )
        }
    }
}

/**
 * View 2: Season Selector view (Image 2) displaying the show title header
 * and a vertical list of seasons with checkmark and selection indicator.
 */
@Composable
private fun SeasonPickerView(
    showTitle: String,
    currentSeason: Int,
    seasons: List<TvSeason>,
    onSelectSeason: (Int) -> Unit,
    onBackToEpisodes: () -> Unit
) {
    val seasonFocusRequesters = remember(seasons.size) {
        List(seasons.size) { FocusRequester() }
    }

    LaunchedEffect(Unit) {
        val activeIdx = seasons.indexOfFirst { it.seasonNumber == currentSeason }.coerceAtLeast(0)
        delay(60)
        try {
            seasonFocusRequesters.getOrNull(activeIdx)?.requestFocus()
        } catch (_: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: Series Title
        Text(
            text = showTitle,
            style = ErasmusTvTypography.SectionTitle.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Seasons List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(
                seasons,
                key = { _, s -> s.id.ifEmpty { "season_${s.seasonNumber}" } }
            ) { index, season ->
                val isSelected = season.seasonNumber == currentSeason
                SeasonRowItem(
                    season = season,
                    isSelected = isSelected,
                    isFirstItem = (index == 0),
                    isLastItem = (index == seasons.size - 1),
                    focusRequester = seasonFocusRequesters.getOrNull(index),
                    onClick = { onSelectSeason(season.seasonNumber) },
                    onBack = onBackToEpisodes
                )
            }
        }
    }
}

/**
 * Season item in the season list.
 * Highlights with a clean white outline on focus and displays an arrow indicator on the right.
 */
@Composable
private fun SeasonRowItem(
    season: TvSeason,
    isSelected: Boolean,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onBack: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val reqModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    val seasonLabel = if (season.name.isNotBlank()) season.name else "Season ${season.seasonNumber}"

    Row(
        modifier = reqModifier
            .fillMaxWidth()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (isFirstItem) {
                                // Lock focus inside: consume UP on first item
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (isLastItem) {
                                // Lock focus inside: consume DOWN on last item
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // Lock focus inside: consume RIGHT
                            true
                        }
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE,
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .then(
                if (isFocused) {
                    Modifier
                        .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                        .background(Color(0xFF1B1B1E), RoundedCornerShape(4.dp))
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = TvPlayerIcons.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            Text(
                text = seasonLabel,
                style = ErasmusTvTypography.BodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isFocused || isSelected) FocusWhite else TextSecondary
            )
        }

        if (isFocused) {
            Icon(
                imageVector = TvPlayerIcons.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

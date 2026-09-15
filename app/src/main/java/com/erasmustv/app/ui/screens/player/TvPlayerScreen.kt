package com.erasmustv.app.ui.screens.player

import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.BorderFocused
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.data.local.SubtitleFont
import com.erasmustv.app.data.local.SubtitlePreferencesManager
import com.erasmustv.app.data.local.SubtitleSize
import com.erasmustv.app.data.subtitle.SubtitleSyncEngine
import java.nio.ByteBuffer

import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.ui.components.TvFocusableCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    viewModel: TvPlayerViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val streamState by viewModel.streamState.collectAsState()
    val currentServerId by viewModel.currentServerId.collectAsState()

    // Player UI States
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var activeMenu by remember { mutableStateOf(PlayerActiveMenu.None) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // Scrubbing State
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var anchorPositionMs by remember { mutableLongStateOf(0L) }
    var isTimelineFocused by remember { mutableStateOf(false) }

    // Row Memory for Deterministic TV Navigation
    var lastFocusedTopBarButton by remember { mutableStateOf("back") }
    var lastFocusedBottomControlButton by remember { mutableStateOf("playPause") }

    // Real Media3 Tracks
    val audioTracks = remember { mutableStateListOf<TvAudioTrack>() }
    val subtitleTracks = remember { mutableStateListOf<TvSubtitleTrack>() }
    val qualityTracks = remember { mutableStateListOf<TvQualityTrack>() }

    // Focus Requesters
    val rootFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }

    // Top Bar Focus Requesters
    val backFocusRequester = remember { FocusRequester() }
    val restartFocusRequester = remember { FocusRequester() }
    val serverFocusRequester = remember { FocusRequester() }

    // Bottom Controls Focus Requesters
    val playPauseFocusRequester = remember { FocusRequester() }
    val audioFocusRequester = remember { FocusRequester() }
    val subtitleFocusRequester = remember { FocusRequester() }
    val qualityFocusRequester = remember { FocusRequester() }

    // Error Retry Focus Requester
    val retryFocusRequester = remember { FocusRequester() }

    // Subtitle Customization & Background Auto-Sync Engine
    val subtitlePrefs = remember { SubtitlePreferencesManager(context) }
    val subtitleFont by subtitlePrefs.fontFlow.collectAsState(initial = SubtitleFont.SANS_SERIF)
    val subtitleSize by subtitlePrefs.sizeFlow.collectAsState(initial = SubtitleSize.MEDIUM)

    val subtitleSyncEngine = remember { SubtitleSyncEngine(coroutineScope) }
    val currentOffsetMs by subtitleSyncEngine.currentOffsetMs.collectAsState()
    val autoSyncStatus by subtitleSyncEngine.statusText.collectAsState()

    val activeSubtitleCues by viewModel.activeSubtitleCues.collectAsState()
    val referenceSubtitleCues by viewModel.referenceSubtitleCues.collectAsState()

    LaunchedEffect(activeSubtitleCues, referenceSubtitleCues) {
        subtitleSyncEngine.setActiveCues(activeSubtitleCues, referenceSubtitleCues)
    }

    // Active Cue at current position with auto-sync offset applied
    val activeCueText = remember(currentPositionMs, currentOffsetMs, activeSubtitleCues) {
        val pos = currentPositionMs
        activeSubtitleCues.firstOrNull { it.isActiveAt(pos, currentOffsetMs) }?.text
    }

    // ExoPlayer Instance with TeeAudioProcessor for real-time speech activity detection
    val exoPlayer = remember {
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory {
                val state = viewModel.streamState.value
                val referer = if (state is PlayerStreamState.Ready) state.referer else "https://cinejoy.to/"
                val headers = mapOf(
                    "Referer" to referer,
                    "Origin" to "https://cinejoy.to",
                    "Accept" to "*/*"
                )
                DefaultHttpDataSource.Factory()
                    .setUserAgent(AppConfig.STREAM_USER_AGENT)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(20000)
                    .setReadTimeoutMs(20000)
                    .setDefaultRequestProperties(headers)
                    .createDataSource()
            }

        val audioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessors(
                arrayOf(
                    TeeAudioProcessor(
                        object : TeeAudioProcessor.AudioBufferSink {
                            override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
                                subtitleSyncEngine.onAudioFormatChanged(sampleRateHz, channelCount)
                            }

                            override fun handleBuffer(buffer: ByteBuffer) {
                                subtitleSyncEngine.onAudioBuffer(buffer)
                            }
                        }
                    )
                )
            )
            .build()

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                ctx: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return audioSink
            }
        }

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }


    // Parse real Media3 tracks
    fun updateTracksFromPlayer(tracks: Tracks) {
        audioTracks.clear()
        subtitleTracks.clear()
        qualityTracks.clear()

        var textDisabled = true
        var videoHasOverride = false

        for (group in tracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language ?: ""
                        val displayLang = if (lang.isNotBlank()) {
                            Locale(lang).displayLanguage.takeIf { it.isNotBlank() } ?: lang
                        } else "Audio ${audioTracks.size + 1}"
                        val label = format.label ?: displayLang
                        val isSelected = group.isTrackSelected(i)

                        audioTracks.add(
                            TvAudioTrack(
                                id = "audio_${group.mediaTrackGroup.id}_$i",
                                mediaTrackGroup = group.mediaTrackGroup,
                                trackIndex = i,
                                label = label,
                                language = lang,
                                isSelected = isSelected
                            )
                        )
                    }
                }

                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language ?: ""
                        val displayLang = if (lang.isNotBlank()) {
                            Locale(lang).displayLanguage.takeIf { it.isNotBlank() } ?: lang
                        } else "Subtitle ${subtitleTracks.size + 1}"
                        val label = format.label ?: displayLang
                        val isSelected = group.isTrackSelected(i)
                        if (isSelected) textDisabled = false

                        subtitleTracks.add(
                            TvSubtitleTrack(
                                id = "sub_${group.mediaTrackGroup.id}_$i",
                                mediaTrackGroup = group.mediaTrackGroup,
                                trackIndex = i,
                                label = label,
                                language = lang,
                                isSelected = isSelected,
                                isOff = false
                            )
                        )
                    }
                }

                C.TRACK_TYPE_VIDEO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val h = format.height
                        val w = format.width
                        val bitrate = format.bitrate
                        val isSelected = group.isTrackSelected(i)
                        if (isSelected) videoHasOverride = true

                        val label = when {
                            h >= 2160 -> "4K Ultra HD"
                            h >= 1080 -> "1080p Full HD"
                            h >= 720 -> "720p HD"
                            h >= 480 -> "480p SD"
                            h > 0 -> "${h}p"
                            else -> "Standard Quality"
                        }

                        qualityTracks.add(
                            TvQualityTrack(
                                id = "video_${group.mediaTrackGroup.id}_$i",
                                mediaTrackGroup = group.mediaTrackGroup,
                                trackIndex = i,
                                label = label,
                                width = w,
                                height = h,
                                bitrate = bitrate,
                                isAuto = false,
                                isSelected = isSelected
                            )
                        )
                    }
                }
            }
        }

        // Explicit "Off" option for subtitles
        subtitleTracks.add(
            0,
            TvSubtitleTrack(
                id = "sub_off",
                label = "Off",
                language = "",
                isSelected = textDisabled,
                isOff = true
            )
        )

        // Explicit "Auto" option for video quality
        qualityTracks.add(
            0,
            TvQualityTrack(
                id = "video_auto",
                label = "Auto (Adaptive)",
                isAuto = true,
                isSelected = !videoHasOverride
            )
        )
    }

    // Attach ExoPlayer Listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracksFromPlayer(tracks)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                viewModel.onPlaybackError(error.message ?: "Playback error encountered. Please retry or choose another server.")
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            val seconds = exoPlayer.currentPosition / 1000L
            val durationSeconds = if (exoPlayer.duration > 0) exoPlayer.duration / 1000L else null
            viewModel.persistProgress(seconds, durationSeconds)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Position Ticker (100ms interval while playing for accurate subtitle sync)
    LaunchedEffect(isPlaying, isScrubbing) {
        while (isPlaying && !isScrubbing) {
            currentPositionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(100)
        }
    }

    // Auto-hide controls timer (8s of inactivity when playing, suspended when scrubbing, menu open, or timeline focused)
    LaunchedEffect(showControls, isPlaying, isScrubbing, isTimelineFocused, activeMenu, lastInteractionTime) {
        if (showControls && isPlaying && !isScrubbing && !isTimelineFocused && activeMenu == PlayerActiveMenu.None) {
            delay(8000)
            showControls = false
        }
    }

    // Focus root container when controls are hidden so D-pad events are captured
    LaunchedEffect(showControls) {
        if (!showControls) {
            delay(50)
            rootFocusRequester.requestFocus()
        }
    }

    // Stream ready handler: load stream into ExoPlayer
    LaunchedEffect(streamState) {
        if (streamState is PlayerStreamState.Ready) {
            val ready = streamState as PlayerStreamState.Ready

            val mimeType = if (ready.streamUrl.contains(".mp4", ignoreCase = true)) {
                MimeTypes.VIDEO_MP4
            } else {
                MimeTypes.APPLICATION_M3U8
            }

            val subtitleConfigs = ready.captions.filter { it.url.isNotBlank() }.map { sub ->
                val subMime = when {
                    sub.mimeType.isNotBlank() -> sub.mimeType
                    sub.url.contains(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                    sub.url.contains(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                    sub.url.contains("strem.io", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                    sub.url.contains("wyzie", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                    else -> MimeTypes.APPLICATION_SUBRIP
                }
                Media3Item.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(subMime)
                    .setLanguage(sub.language)
                    .setLabel(sub.label)
                    .setSelectionFlags(if (sub.language.equals("en", ignoreCase = true) || sub.language.equals("eng", ignoreCase = true)) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }

            val mediaItem = Media3Item.Builder()
                .setUri(Uri.parse(ready.streamUrl))
                .setMimeType(mimeType)
                .setSubtitleConfigurations(subtitleConfigs)
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (ready.startPositionMs > 0) {
                exoPlayer.seekTo(ready.startPositionMs)
            }
            exoPlayer.playWhenReady = true
        }
    }

    // Hierarchical Back Handler: Submenu -> Scrubbing -> Controls -> Exit
    BackHandler {
        when {
            streamState is PlayerStreamState.Error -> {
                onExit()
            }

            activeMenu != PlayerActiveMenu.None -> {
                val closingMenu = activeMenu
                activeMenu = PlayerActiveMenu.None
                coroutineScope.launch {
                    delay(50)
                    when (closingMenu) {
                        PlayerActiveMenu.Audio -> audioFocusRequester.requestFocus()
                        PlayerActiveMenu.Subtitles -> subtitleFocusRequester.requestFocus()
                        PlayerActiveMenu.Quality -> qualityFocusRequester.requestFocus()
                        PlayerActiveMenu.Servers -> serverFocusRequester.requestFocus()
                        else -> playPauseFocusRequester.requestFocus()
                    }
                }
            }

            isScrubbing -> {
                isScrubbing = false
                scrubPositionMs = anchorPositionMs
                coroutineScope.launch {
                    delay(50)
                    timelineFocusRequester.requestFocus()
                }
            }

            showControls -> {
                showControls = false
                rootFocusRequester.requestFocus()
            }

            else -> {
                val seconds = exoPlayer.currentPosition / 1000L
                val durationSeconds = if (exoPlayer.duration > 0) exoPlayer.duration / 1000L else null
                viewModel.persistProgress(seconds, durationSeconds)
                onExit()
            }
        }
    }

    // Initial focus on play/pause button
    LaunchedEffect(Unit) {
        playPauseFocusRequester.requestFocus()
    }

    // Active labels for bottom controls
    val currentAudioLabel = audioTracks.find { it.isSelected }?.let { "Audio: ${it.label}" } ?: "Audio"
    val currentSubtitleLabel = subtitleTracks.find { it.isSelected }?.let {
        if (it.isOff) "Subtitles: Off" else "Subtitles: ${it.label}"
    } ?: "Subtitles"
    val currentQualityLabel = qualityTracks.find { it.isSelected }?.let {
        if (it.isAuto) "Quality: Auto" else "Quality: ${it.label}"
    } ?: "Quality"
    val currentServerName = viewModel.availableServers.find { it.id == currentServerId }?.name ?: currentServerId

    // Root Player Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .focusRequester(rootFocusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    lastInteractionTime = System.currentTimeMillis()
                    if (!showControls) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER,
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                showControls = true
                                coroutineScope.launch {
                                    delay(50)
                                    playPauseFocusRequester.requestFocus()
                                }
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                showControls = true
                                isScrubbing = true
                                anchorPositionMs = exoPlayer.currentPosition
                                scrubPositionMs = (exoPlayer.currentPosition - 5_000L).coerceAtLeast(0L)
                                coroutineScope.launch {
                                    delay(50)
                                    timelineFocusRequester.requestFocus()
                                }
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                showControls = true
                                isScrubbing = true
                                anchorPositionMs = exoPlayer.currentPosition
                                scrubPositionMs = (exoPlayer.currentPosition + 5_000L).coerceAtMost(durationMs)
                                coroutineScope.launch {
                                    delay(50)
                                    timelineFocusRequester.requestFocus()
                                }
                                true
                            }

                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                    showControls = true
                                } else {
                                    exoPlayer.play()
                                }
                                true
                            }

                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                exoPlayer.play()
                                true
                            }

                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                exoPlayer.pause()
                                showControls = true
                                true
                            }

                            else -> false
                        }
                    } else false
                } else false
            }
            .focusable()
    ) {
        // Layer 1: Native ExoPlayer Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    isFocusable = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    subtitleView?.visibility = android.view.View.GONE
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Layer 1.5: Floating Custom Subtitles (Zero background box, customizable fonts & sizes, auto-synced)
        TvSubtitleOverlay(
            activeText = activeCueText,
            font = subtitleFont,
            size = subtitleSize,
            isControlsVisible = showControls
        )


        // Hidden controls capture overlay (when controls are hidden, capture all D-pad interactions)
        if (!showControls && streamState !is PlayerStreamState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(rootFocusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            lastInteractionTime = System.currentTimeMillis()
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER,
                                KeyEvent.KEYCODE_NUMPAD_ENTER,
                                KeyEvent.KEYCODE_DPAD_UP,
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    showControls = true
                                    coroutineScope.launch {
                                        delay(50)
                                        playPauseFocusRequester.requestFocus()
                                    }
                                    true
                                }

                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    showControls = true
                                    isScrubbing = true
                                    anchorPositionMs = exoPlayer.currentPosition
                                    scrubPositionMs = (exoPlayer.currentPosition - 5_000L).coerceAtLeast(0L)
                                    coroutineScope.launch {
                                        delay(50)
                                        timelineFocusRequester.requestFocus()
                                    }
                                    true
                                }

                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    showControls = true
                                    isScrubbing = true
                                    anchorPositionMs = exoPlayer.currentPosition
                                    scrubPositionMs = (exoPlayer.currentPosition + 5_000L).coerceAtMost(durationMs)
                                    coroutineScope.launch {
                                        delay(50)
                                        timelineFocusRequester.requestFocus()
                                    }
                                    true
                                }

                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                        showControls = true
                                    } else {
                                        exoPlayer.play()
                                    }
                                    true
                                }

                                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                    exoPlayer.play()
                                    true
                                }

                                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                    exoPlayer.pause()
                                    showControls = true
                                    true
                                }

                                else -> false
                            }
                        } else false
                    }
            )
            LaunchedEffect(Unit) {
                rootFocusRequester.requestFocus()
            }
        }

        // Layer 2: Buffering Overlay
        if (isBuffering || streamState is PlayerStreamState.Resolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PitchBlack.copy(alpha = 0.50f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = FocusWhite,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (streamState is PlayerStreamState.Resolving) "Connecting to stream cluster..." else "Buffering...",
                        style = ErasmusTvTypography.BodyLarge,
                        color = TextPrimary
                    )
                }
            }
        }

        // Layer 3: Playback Error Screen
        if (streamState is PlayerStreamState.Error) {
            val err = streamState as PlayerStreamState.Error
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PitchBlack.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.padding(48.dp)
                ) {
                    Text(
                        text = "Unable to Play Video",
                        style = ErasmusTvTypography.BillboardTitle,
                        color = TextPrimary
                    )
                    Text(
                        text = err.message,
                        style = ErasmusTvTypography.Body,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TvFocusableCard(
                            onClick = { viewModel.retry() },
                            shape = RectangleShape,
                            focusedBorderColor = BorderFocused,
                            modifier = Modifier.focusRequester(retryFocusRequester)
                        ) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .background(if (isFocused) SurfaceDark else PitchBlack, RectangleShape)
                                    .padding(horizontal = 24.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = "Try Again",
                                    style = ErasmusTvTypography.ButtonText,
                                    color = if (isFocused) FocusWhite else TextPrimary
                                )
                            }
                        }

                        TvFocusableCard(
                            onClick = { activeMenu = PlayerActiveMenu.Servers },
                            shape = RectangleShape,
                            focusedBorderColor = BorderFocused
                        ) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .background(if (isFocused) SurfaceDark else PitchBlack, RectangleShape)
                                    .padding(horizontal = 24.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = "Change Server",
                                    style = ErasmusTvTypography.ButtonText,
                                    color = if (isFocused) FocusWhite else TextPrimary
                                )
                            }
                        }

                        TvFocusableCard(
                            onClick = onExit,
                            shape = RectangleShape,
                            focusedBorderColor = BorderFocused
                        ) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .background(if (isFocused) SurfaceDark else PitchBlack, RectangleShape)
                                    .padding(horizontal = 24.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = "Exit",
                                    style = ErasmusTvTypography.ButtonText,
                                    color = if (isFocused) FocusWhite else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                retryFocusRequester.requestFocus()
            }
        }

        // Layer 4: Cinematic HUD Controls Overlay
        AnimatedVisibility(
            visible = showControls && streamState !is PlayerStreamState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to PitchBlack.copy(alpha = 0.85f),
                                0.22f to Color.Transparent,
                                0.72f to Color.Transparent,
                                1.0f to PitchBlack.copy(alpha = 0.92f)
                            )
                        )
                    )
            ) {
                // Top Bar
                TvPlayerTopBar(
                    title = viewModel.title,
                    mediaType = viewModel.mediaType,
                    season = viewModel.season,
                    episode = viewModel.episode,
                    currentServerName = currentServerName,
                    onExit = {
                        val seconds = exoPlayer.currentPosition / 1000L
                        val durationSeconds = if (exoPlayer.duration > 0) exoPlayer.duration / 1000L else null
                        viewModel.persistProgress(seconds, durationSeconds)
                        onExit()
                    },
                    onRestart = {
                        exoPlayer.seekTo(0L)
                        currentPositionMs = 0L
                    },
                    onOpenServers = {
                        activeMenu = PlayerActiveMenu.Servers
                    },
                    backFocusRequester = backFocusRequester,
                    restartFocusRequester = restartFocusRequester,
                    serverFocusRequester = serverFocusRequester,
                    onButtonFocused = { btnId -> lastFocusedTopBarButton = btnId },
                    timelineFocusRequester = timelineFocusRequester,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Bottom HUD Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 24.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Timeline Scrubber
                    TvPlayerTimeline(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        isScrubbing = isScrubbing,
                        scrubPositionMs = scrubPositionMs,
                        anchorPositionMs = anchorPositionMs,
                        onStartScrub = { initialPos ->
                            isScrubbing = true
                            anchorPositionMs = initialPos
                            scrubPositionMs = initialPos
                        },
                        onScrub = { targetPos ->
                            scrubPositionMs = targetPos
                        },
                        onCommitScrub = {
                            exoPlayer.seekTo(scrubPositionMs)
                            currentPositionMs = scrubPositionMs
                            isScrubbing = false
                            exoPlayer.play()
                        },
                        onCancelScrub = {
                            isScrubbing = false
                            scrubPositionMs = anchorPositionMs
                        },
                        onTogglePlayPause = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        topBarFocusRequester = when (lastFocusedTopBarButton) {
                            "restart" -> restartFocusRequester
                            "server" -> serverFocusRequester
                            else -> backFocusRequester
                        },
                        bottomControlFocusRequester = when (lastFocusedBottomControlButton) {
                            "audio" -> audioFocusRequester
                            "subtitles" -> subtitleFocusRequester
                            "quality" -> qualityFocusRequester
                            else -> playPauseFocusRequester
                        },
                        focusRequester = timelineFocusRequester,
                        onFocusChanged = { focused ->
                            isTimelineFocused = focused
                        }
                    )

                    // Bottom Action Controls
                    TvPlayerBottomControls(
                        isPlaying = isPlaying,
                        currentAudioLabel = currentAudioLabel,
                        currentSubtitleLabel = currentSubtitleLabel,
                        currentQualityLabel = currentQualityLabel,
                        onTogglePlayPause = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        onOpenAudio = { activeMenu = PlayerActiveMenu.Audio },
                        onOpenSubtitles = { activeMenu = PlayerActiveMenu.Subtitles },
                        onOpenQuality = { activeMenu = PlayerActiveMenu.Quality },
                        timelineFocusRequester = timelineFocusRequester,
                        playPauseFocusRequester = playPauseFocusRequester,
                        audioFocusRequester = audioFocusRequester,
                        subtitleFocusRequester = subtitleFocusRequester,
                        qualityFocusRequester = qualityFocusRequester,
                        onButtonFocused = { btnId -> lastFocusedBottomControlButton = btnId }
                    )
                }
            }
        }

        // Layer 5: Submenu Overlay Drawer (Audio, Subtitles, Quality, Servers)
        TvPlayerSubmenu(
            activeMenu = activeMenu,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            qualityTracks = qualityTracks,
            servers = viewModel.availableServers,
            currentServerId = currentServerId,
            currentSubtitleFont = subtitleFont,
            currentSubtitleSize = subtitleSize,
            autoSyncStatus = autoSyncStatus,
            onSelectAudioTrack = { track ->
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .setOverrideForType(TrackSelectionOverride(track.mediaTrackGroup, track.trackIndex))
                    .build()
            },
            onSelectSubtitleTrack = { track ->
                if (track.isOff) {
                    viewModel.loadSubtitleTrack(null)
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .build()
                } else {
                    val readyState = viewModel.streamState.value as? PlayerStreamState.Ready
                    val matchedTrack = readyState?.captions?.find { it.label == track.label }
                    viewModel.loadSubtitleTrack(matchedTrack)
                    if (track.mediaTrackGroup != null && track.trackIndex >= 0) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setOverrideForType(TrackSelectionOverride(track.mediaTrackGroup, track.trackIndex))
                            .build()
                    }
                }
            },
            onSelectSubtitleFont = { font ->
                coroutineScope.launch { subtitlePrefs.setFont(font) }
            },
            onSelectSubtitleSize = { size ->
                coroutineScope.launch { subtitlePrefs.setSize(size) }
            },
            onNudgeSubtitleSync = { deltaMs ->
                if (deltaMs == 0L) {
                    subtitleSyncEngine.reset()
                } else {
                    subtitleSyncEngine.nudgeOffset(deltaMs)
                }
            },
            onSelectQualityTrack = { track ->
                if (track.isAuto) {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .build()
                } else if (track.mediaTrackGroup != null && track.trackIndex >= 0) {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                        .setOverrideForType(TrackSelectionOverride(track.mediaTrackGroup, track.trackIndex))
                        .build()
                }
            },
            onSelectServer = { serverId ->
                viewModel.selectServer(serverId)
            },
            onClose = {
                val closingMenu = activeMenu
                activeMenu = PlayerActiveMenu.None
                coroutineScope.launch {
                    delay(50)
                    when (closingMenu) {
                        PlayerActiveMenu.Audio -> audioFocusRequester.requestFocus()
                        PlayerActiveMenu.Subtitles -> subtitleFocusRequester.requestFocus()
                        PlayerActiveMenu.Quality -> qualityFocusRequester.requestFocus()
                        PlayerActiveMenu.Servers -> serverFocusRequester.requestFocus()
                        else -> playPauseFocusRequester.requestFocus()
                    }
                }
            }
        )
    }
}

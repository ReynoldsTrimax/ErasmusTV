@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.erasmustv.app.ui.screens.player

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.BorderFocused
import com.erasmustv.app.core.theme.BorderSubtle
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.SurfaceElevated
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.StreamServer
import kotlinx.coroutines.delay

import com.erasmustv.app.data.local.SubtitleFont
import com.erasmustv.app.data.local.SubtitleSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@Composable
fun TvPlayerSubmenu(
    activeMenu: PlayerActiveMenu,
    audioTracks: List<TvAudioTrack>,
    subtitleTracks: List<TvSubtitleTrack>,
    qualityTracks: List<TvQualityTrack>,
    servers: List<StreamServer>,
    currentServerId: String,
    currentSubtitleFont: SubtitleFont = SubtitleFont.SANS_SERIF,
    currentSubtitleSize: SubtitleSize = SubtitleSize.MEDIUM,
    autoSyncStatus: String = "Auto-sync: Active (±0.0s)",
    onSelectAudioTrack: (TvAudioTrack) -> Unit,
    onSelectSubtitleTrack: (TvSubtitleTrack) -> Unit,
    onSelectQualityTrack: (TvQualityTrack) -> Unit,
    onSelectServer: (String) -> Unit,
    onSelectSubtitleFont: (SubtitleFont) -> Unit = {},
    onSelectSubtitleSize: (SubtitleSize) -> Unit = {},
    onNudgeSubtitleSync: (Long) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeMenu != PlayerActiveMenu.None && activeMenu != PlayerActiveMenu.Episodes,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 },
        modifier = modifier
    ) {
        // Semi-translucent dark backdrop for contrast behind drawer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack.copy(alpha = 0.70f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Elegant right panel drawer with trapped focus
            Box(
                modifier = Modifier
                    .width(440.dp)
                    .fillMaxHeight()
                    .background(SurfaceDark)
                    .clickable(enabled = false) {}
                    .focusProperties {
                        onExit = { FocusRequester.Cancel }
                    }
                    .padding(28.dp)
            ) {
                when (activeMenu) {
                    PlayerActiveMenu.Audio -> {
                        val displayTracks = if (audioTracks.isNotEmpty()) audioTracks else listOf(
                            TvAudioTrack(
                                id = "audio_default",
                                mediaTrackGroup = androidx.media3.common.TrackGroup(androidx.media3.common.Format.Builder().build()),
                                trackIndex = 0,
                                label = "Default Audio",
                                language = "STEREO",
                                isSelected = true
                            )
                        )
                        val selectedIndex = displayTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0)
                        SubmenuContent(
                            title = "Audio Tracks",
                            subtitle = if (audioTracks.size > 1) "${audioTracks.size} audio tracks available" else "Playback audio language",
                            itemCount = displayTracks.size,
                            initialSelectedIndex = selectedIndex,
                            onClose = onClose
                        ) { itemFocusRequesters ->
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(displayTracks) { index, track ->
                                    SubmenuRowItem(
                                        title = track.label,
                                        subtitle = if (track.language.isNotBlank() && !track.language.equals("und", ignoreCase = true)) track.language.uppercase() else null,
                                        isSelected = track.isSelected,
                                        isFirstItem = (index == 0),
                                        isLastItem = (index == displayTracks.size - 1),
                                        focusRequester = itemFocusRequesters.getOrNull(index),
                                        onClick = {
                                            if (audioTracks.isNotEmpty()) {
                                                onSelectAudioTrack(track)
                                            }
                                            onClose()
                                        },
                                        onBackOrLeft = onClose
                                    )
                                }
                            }
                        }
                    }

                    PlayerActiveMenu.Subtitles -> {
                        var subtitleTab by remember { mutableIntStateOf(0) }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Subtitles",
                                        style = ErasmusTvTypography.SectionTitle.copy(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (subtitleTab == 0) "Select language or turn off" else "Customize font, size & sync",
                                        style = ErasmusTvTypography.Badge.copy(fontSize = 12.sp),
                                        color = TextMuted
                                    )
                                }

                                SubmenuCloseButton(onClose = onClose)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Tab Switcher (Tracks vs Appearance - de-boxified)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SubmenuTabButton(
                                    title = "Tracks",
                                    isSelected = subtitleTab == 0,
                                    onClick = { subtitleTab = 0 },
                                    modifier = Modifier.weight(1f)
                                )
                                SubmenuTabButton(
                                    title = "Appearance & Sync",
                                    isSelected = subtitleTab == 1,
                                    onClick = { subtitleTab = 1 },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (subtitleTab == 0) {
                                // Subtitle Tracks List
                                val selectedIndex = subtitleTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0)
                                val trackFocusRequesters = remember(subtitleTracks.size) {
                                    List(subtitleTracks.size) { FocusRequester() }
                                }
                                LaunchedEffect(Unit) {
                                    delay(50)
                                    trackFocusRequesters.getOrNull(selectedIndex)?.requestFocus()
                                }
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(subtitleTracks) { index, track ->
                                        SubmenuRowItem(
                                            title = track.label,
                                            subtitle = if (!track.isOff && track.language.isNotBlank()) track.language.uppercase() else null,
                                            isSelected = track.isSelected,
                                            isFirstItem = (index == 0),
                                            isLastItem = (index == subtitleTracks.size - 1),
                                            focusRequester = trackFocusRequesters.getOrNull(index),
                                            onClick = {
                                                onSelectSubtitleTrack(track)
                                                onClose()
                                            },
                                            onBackOrLeft = onClose
                                        )
                                    }
                                }
                            } else {
                                // Subtitle Appearance & Sync Settings
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // 1. Font Size (3 options)
                                    item {
                                        Text(
                                            text = "FONT SIZE",
                                            style = ErasmusTvTypography.Badge.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            SubtitleSize.entries.forEach { size ->
                                                SubmenuOptionChip(
                                                    title = size.displayName,
                                                    isSelected = currentSubtitleSize == size,
                                                    onClick = { onSelectSubtitleSize(size) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    // 2. Font Style (5 options)
                                    item {
                                        Text(
                                            text = "FONT STYLE",
                                            style = ErasmusTvTypography.Badge.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            SubtitleFont.entries.forEachIndexed { fontIndex, font ->
                                                SubmenuRowItem(
                                                    title = font.displayName,
                                                    subtitle = if (font == SubtitleFont.BOSTONE) "Signature Erasmus Typography" else null,
                                                    isSelected = currentSubtitleFont == font,
                                                    isFirstItem = false,
                                                    isLastItem = false,
                                                    focusRequester = null,
                                                    onClick = { onSelectSubtitleFont(font) },
                                                    onBackOrLeft = onClose
                                                )
                                            }
                                        }
                                    }

                                    // 3. Autonomous Background Sync & Manual Fine-Tune
                                    item {
                                        Text(
                                            text = "SUBTITLE SYNC",
                                            style = ErasmusTvTypography.Badge.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Auto-Sync Engine",
                                                    style = ErasmusTvTypography.ButtonText.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Active",
                                                    style = ErasmusTvTypography.Badge.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF4CAF50)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = autoSyncStatus,
                                                style = ErasmusTvTypography.Body,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                SubmenuOptionChip(
                                                    title = "-0.5s",
                                                    isSelected = false,
                                                    onClick = { onNudgeSubtitleSync(-500L) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                SubmenuOptionChip(
                                                    title = "Reset",
                                                    isSelected = false,
                                                    onClick = { onNudgeSubtitleSync(0L) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                SubmenuOptionChip(
                                                    title = "+0.5s",
                                                    isSelected = false,
                                                    isLastItem = true,
                                                    onClick = { onNudgeSubtitleSync(500L) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }
                            }
                        }
                    }

                    PlayerActiveMenu.Quality -> {
                        val selectedIndex = qualityTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0)
                        SubmenuContent(
                            title = "Video Quality",
                            subtitle = "Select video rendition or automatic adaptive streaming",
                            itemCount = qualityTracks.size,
                            initialSelectedIndex = selectedIndex,
                            onClose = onClose
                        ) { itemFocusRequesters ->
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(qualityTracks) { index, track ->
                                    SubmenuRowItem(
                                        title = track.label,
                                        subtitle = if (track.bitrate > 0) "${track.bitrate / 1000} kbps" else null,
                                        isSelected = track.isSelected,
                                        isFirstItem = (index == 0),
                                        isLastItem = (index == qualityTracks.size - 1),
                                        focusRequester = itemFocusRequesters.getOrNull(index),
                                        onClick = {
                                            onSelectQualityTrack(track)
                                            onClose()
                                        },
                                        onBackOrLeft = onClose
                                    )
                                }
                            }
                        }
                    }

                    PlayerActiveMenu.Servers -> {
                        val selectedIndex = servers.indexOfFirst { it.id == currentServerId }.coerceAtLeast(0)
                        SubmenuContent(
                            title = "Streaming Server",
                            subtitle = "Switch cluster source if experiencing buffering",
                            itemCount = servers.size,
                            initialSelectedIndex = selectedIndex,
                            onClose = onClose
                        ) { itemFocusRequesters ->
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(servers) { index, server ->
                                    val isSelected = server.id == currentServerId
                                    SubmenuRowItem(
                                        title = "${server.flag}  ${server.name}",
                                        subtitle = "${server.badge} · ${server.description}",
                                        isSelected = isSelected,
                                        isFirstItem = (index == 0),
                                        isLastItem = (index == servers.size - 1),
                                        focusRequester = itemFocusRequesters.getOrNull(index),
                                        onClick = {
                                            onSelectServer(server.id)
                                            onClose()
                                        },
                                        onBackOrLeft = onClose
                                    )
                                }
                            }
                        }
                    }

                    PlayerActiveMenu.Episodes,
                    PlayerActiveMenu.None -> {}
                }
            }
        }
    }
}

@Composable
private fun SubmenuContent(
    title: String,
    subtitle: String,
    itemCount: Int,
    initialSelectedIndex: Int,
    onClose: () -> Unit,
    content: @Composable (focusRequesters: List<FocusRequester>) -> Unit
) {
    val focusRequesters = remember(itemCount) {
        List(itemCount) { FocusRequester() }
    }

    LaunchedEffect(itemCount, initialSelectedIndex) {
        if (focusRequesters.isNotEmpty()) {
            delay(80)
            val target = focusRequesters.getOrNull(initialSelectedIndex) ?: focusRequesters.firstOrNull()
            target?.requestFocus()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ErasmusTvTypography.SectionTitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = ErasmusTvTypography.Badge.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }

            SubmenuCloseButton(onClose = onClose)
        }

        Spacer(modifier = Modifier.height(20.dp))

        content(focusRequesters)
    }
}

@Composable
private fun SubmenuCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .size(36.dp)
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
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            // Lock focus inside drawer
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // Lock focus inside drawer
                            true
                        }
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE,
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
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
            contentDescription = "Close Menu",
            tint = if (isFocused) FocusWhite else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SubmenuRowItem(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onBackOrLeft: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val reqModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else Modifier

    Box(
        modifier = reqModifier
            .fillMaxWidth()
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
                onClick = onClick
            )
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
                            // Lock focus inside: consume RIGHT so focus doesn't leave drawer
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE -> {
                            onBackOrLeft()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ErasmusTvTypography.ButtonText.copy(
                        fontSize = 14.5.sp,
                        fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isFocused || isSelected) FocusWhite else TextSecondary
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = ErasmusTvTypography.Badge.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isFocused || isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isFocused || isSelected) FocusWhite.copy(alpha = 0.8f) else TextMuted
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = TvPlayerIcons.Check,
                    contentDescription = "Selected",
                    tint = FocusWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SubmenuTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDpadDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
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
                onClick = onClick
            )
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
                            // Lock focus inside: consume UP
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (onDpadDown != null) {
                                onDpadDown()
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = ErasmusTvTypography.ButtonText.copy(
                fontSize = 13.5.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected || isFocused) FocusWhite else TextMuted
        )
    }
}

@Composable
private fun SubmenuOptionChip(
    title: String,
    isSelected: Boolean,
    isLastItem: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
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
                onClick = onClick
            )
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
                            if (isLastItem) true else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (isLastItem) true else false
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = ErasmusTvTypography.ButtonText.copy(
                fontSize = 12.5.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected || isFocused) FocusWhite else TextMuted
        )
    }
}


package com.erasmustv.app.ui.screens.player

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.BorderFocused
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.SurfaceElevated
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.core.theme.TvSpring
import com.erasmustv.app.core.theme.floatSpec

@Composable
fun TvPlayerTopBar(
    title: String,
    mediaType: String,
    season: Int?,
    episode: Int?,
    episodeTitle: String? = null,
    currentServerName: String,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onOpenServers: () -> Unit,
    backFocusRequester: FocusRequester,
    restartFocusRequester: FocusRequester,
    serverFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
    onButtonFocused: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Back button & Title Context
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TvPlayerButton(
                icon = TvPlayerIcons.Back,
                label = "Back",
                onClick = onExit,
                focusRequester = backFocusRequester,
                onFocused = { onButtonFocused("back") },
                onDpadRight = { restartFocusRequester.requestFocus() },
                onDpadDown = { timelineFocusRequester.requestFocus() },
                modifier = Modifier.focusProperties {
                    up = FocusRequester.Cancel
                    down = timelineFocusRequester
                    left = FocusRequester.Cancel
                    right = restartFocusRequester
                }
            )

            Column {
                Text(
                    text = title,
                    style = ErasmusTvTypography.SectionTitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextPrimary
                )
                if (mediaType.equals("tv", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val episodeLabel = when {
                        !episodeTitle.isNullOrBlank() -> episodeTitle
                        episode != null -> "Episode $episode"
                        else -> null
                    }
                    if (episodeLabel != null) {
                        Text(
                            text = episodeLabel,
                            style = ErasmusTvTypography.Badge.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Right Actions: Restart & Server
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvPlayerButton(
                icon = TvPlayerIcons.Restart,
                label = "Restart",
                onClick = onRestart,
                focusRequester = restartFocusRequester,
                onFocused = { onButtonFocused("restart") },
                onDpadLeft = { backFocusRequester.requestFocus() },
                onDpadRight = { serverFocusRequester.requestFocus() },
                onDpadDown = { timelineFocusRequester.requestFocus() },
                modifier = Modifier.focusProperties {
                    up = FocusRequester.Cancel
                    down = timelineFocusRequester
                    left = backFocusRequester
                    right = serverFocusRequester
                }
            )

            TvPlayerButton(
                icon = TvPlayerIcons.Server,
                label = "Server: $currentServerName",
                onClick = onOpenServers,
                focusRequester = serverFocusRequester,
                onFocused = { onButtonFocused("server") },
                onDpadLeft = { restartFocusRequester.requestFocus() },
                onDpadDown = { timelineFocusRequester.requestFocus() },
                modifier = Modifier.focusProperties {
                    up = FocusRequester.Cancel
                    down = timelineFocusRequester
                    left = restartFocusRequester
                    right = FocusRequester.Cancel
                }
            )
        }
    }
}

@Composable
fun TvPlayerBottomControls(
    isPlaying: Boolean,
    currentAudioLabel: String,
    currentSubtitleLabel: String,
    currentQualityLabel: String,
    onTogglePlayPause: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenQuality: () -> Unit,
    timelineFocusRequester: FocusRequester,
    playPauseFocusRequester: FocusRequester,
    audioFocusRequester: FocusRequester,
    subtitleFocusRequester: FocusRequester,
    qualityFocusRequester: FocusRequester,
    mediaType: String = "movie",
    season: Int? = null,
    episode: Int? = null,
    onOpenEpisodes: () -> Unit = {},
    episodesFocusRequester: FocusRequester = remember { FocusRequester() },
    onButtonFocused: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isTv = mediaType.equals("tv", ignoreCase = true) && season != null && episode != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Controls: Play/Pause
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvPlayerButton(
                icon = if (isPlaying) TvPlayerIcons.Pause else TvPlayerIcons.Play,
                label = if (isPlaying) "Pause" else "Play",
                onClick = onTogglePlayPause,
                focusRequester = playPauseFocusRequester,
                onFocused = { onButtonFocused("playPause") },
                onDpadRight = {
                    if (isTv) episodesFocusRequester.requestFocus() else audioFocusRequester.requestFocus()
                },
                onDpadUp = { timelineFocusRequester.requestFocus() },
                modifier = Modifier.focusProperties {
                    up = timelineFocusRequester
                    down = FocusRequester.Cancel
                    left = FocusRequester.Cancel
                    right = if (isTv) episodesFocusRequester else audioFocusRequester
                }
            )
        }

        // Right Controls: Episodes (TV only), Audio, Subtitles, Quality
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTv) {
                TvPlayerButton(
                    icon = TvPlayerIcons.Episodes,
                    label = "Episodes (S$season · E$episode)",
                    onClick = onOpenEpisodes,
                    focusRequester = episodesFocusRequester,
                    onFocused = { onButtonFocused("episodes") },
                    onDpadLeft = { playPauseFocusRequester.requestFocus() },
                    onDpadRight = { audioFocusRequester.requestFocus() },
                    onDpadUp = { timelineFocusRequester.requestFocus() },
                    modifier = Modifier.focusProperties {
                        up = timelineFocusRequester
                        down = FocusRequester.Cancel
                        left = playPauseFocusRequester
                        right = audioFocusRequester
                    }
                )
            }

            TvPlayerButton(
                icon = TvPlayerIcons.Audio,
                label = currentAudioLabel,
                onClick = onOpenAudio,
                focusRequester = audioFocusRequester,
                onFocused = { onButtonFocused("audio") },
                onDpadLeft = {
                    if (isTv) episodesFocusRequester.requestFocus() else playPauseFocusRequester.requestFocus()
                },
                onDpadRight = { subtitleFocusRequester.requestFocus() },
                onDpadUp = { timelineFocusRequester.requestFocus() },
                modifier = Modifier.focusProperties {
                    up = timelineFocusRequester
                    down = FocusRequester.Cancel
                    left = if (isTv) episodesFocusRequester else playPauseFocusRequester
                    right = subtitleFocusRequester
                }
            )

            TvPlayerButton(
                icon = TvPlayerIcons.Subtitles,
                label = currentSubtitleLabel,
                onClick = onOpenSubtitles,
                focusRequester = subtitleFocusRequester,
                onFocused = { onButtonFocused("subtitles") },
                onDpadLeft = { audioFocusRequester.requestFocus() },
                onDpadRight = { qualityFocusRequester.requestFocus() },
                onDpadUp = { timelineFocusRequester.requestFocus() },
                modifier = Modifier.focusProperties {
                    up = timelineFocusRequester
                    down = FocusRequester.Cancel
                    left = audioFocusRequester
                    right = qualityFocusRequester
                }
            )

            TvPlayerButton(
                icon = TvPlayerIcons.Quality,
                label = currentQualityLabel,
                onClick = onOpenQuality,
                focusRequester = qualityFocusRequester,
                onFocused = { onButtonFocused("quality") },
                onDpadLeft = { subtitleFocusRequester.requestFocus() },
                onDpadUp = { timelineFocusRequester.requestFocus() },
                modifier = Modifier.focusProperties {
                    up = timelineFocusRequester
                    down = FocusRequester.Cancel
                    left = subtitleFocusRequester
                    right = FocusRequester.Cancel
                }
            )
        }
    }
}

@Composable
fun TvPlayerButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onDpadLeft: (() -> Unit)? = null,
    onDpadRight: (() -> Unit)? = null,
    onDpadUp: (() -> Unit)? = null,
    onDpadDown: (() -> Unit)? = null,
    onFocused: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused()
        }
    }

    // Focus magnification on the snappiest spring in the vocabulary: during
    // playback this is the only thing on screen telling you where you are.
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.20f else 1.0f,
        animationSpec = TvSpring.FocusFast.floatSpec(),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (onDpadLeft != null) {
                                onDpadLeft()
                                true
                            } else true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (onDpadRight != null) {
                                onDpadRight()
                                true
                            } else true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (onDpadUp != null) {
                                onDpadUp()
                                true
                            } else true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (onDpadDown != null) {
                                onDpadDown()
                                true
                            } else true
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
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) FocusWhite else Color(0xB8FFFFFF),
                modifier = Modifier.size(22.dp)
            )

            val shouldShowLabel = label.contains(":") || label.contains("(") || label == "Restart" ||
                label == "Audio" || label == "Subtitles" || label == "Quality" || label.startsWith("Episodes")
            if (shouldShowLabel) {
                Text(
                    text = label,
                    style = ErasmusTvTypography.ButtonText.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isFocused) FocusWhite else Color(0xB8FFFFFF)
                )
            }
        }
    }
}

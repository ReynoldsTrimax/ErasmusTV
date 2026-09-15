package com.erasmustv.app.ui.screens.player

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.BorderFocused
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.SurfaceElevated
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import kotlin.math.abs

@Composable
fun TvPlayerTimeline(
    currentPositionMs: Long,
    durationMs: Long,
    isScrubbing: Boolean,
    scrubPositionMs: Long,
    anchorPositionMs: Long,
    onStartScrub: (initialPos: Long) -> Unit,
    onScrub: (targetPos: Long) -> Unit,
    onCommitScrub: () -> Unit,
    onCancelScrub: () -> Unit,
    onTogglePlayPause: () -> Unit,
    topBarFocusRequester: FocusRequester,
    bottomControlFocusRequester: FocusRequester,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
        if (!isFocused && isScrubbing) {
            onCancelScrub()
        }
    }

    var consecutiveSteps by remember { mutableIntStateOf(0) }
    var lastStepTimeMs by remember { mutableLongStateOf(0L) }

    val activePositionMs = if (isScrubbing) scrubPositionMs else currentPositionMs
    val progressRatio = if (durationMs > 0) {
        (activePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val trackHeight by animateDpAsState(
        targetValue = when {
            isScrubbing -> 6.dp
            isFocused -> 5.dp
            else -> 4.dp
        },
        animationSpec = tween(durationMillis = 140),
        label = "timelineTrackHeight"
    )

    val thumbSize by animateDpAsState(
        targetValue = when {
            isScrubbing -> 16.dp
            isFocused -> 13.dp
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = 140),
        label = "timelineThumbSize"
    )

    fun calculateStepMs(repeatCount: Int): Long {
        val now = System.currentTimeMillis()
        val isRapid = (now - lastStepTimeMs < 300)
        lastStepTimeMs = now

        return if (repeatCount > 0) {
            when {
                repeatCount < 4 -> 5_000L
                repeatCount in 4..10 -> 10_000L
                repeatCount in 11..24 -> 20_000L
                else -> 45_000L
            }
        } else {
            if (isRapid) {
                consecutiveSteps = (consecutiveSteps + 1).coerceAtMost(30)
                when {
                    consecutiveSteps < 3 -> 5_000L
                    consecutiveSteps in 3..7 -> 10_000L
                    consecutiveSteps in 8..16 -> 20_000L
                    else -> 45_000L
                }
            } else {
                consecutiveSteps = 0
                5_000L // Predictable 5-second single press
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusProperties {
                up = topBarFocusRequester
                down = bottomControlFocusRequester
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            val stepSizeMs = calculateStepMs(keyEvent.nativeKeyEvent.repeatCount)
                            if (!isScrubbing) {
                                onStartScrub(currentPositionMs)
                                val target = (currentPositionMs - stepSizeMs).coerceAtLeast(0L)
                                onScrub(target)
                            } else {
                                val target = (scrubPositionMs - stepSizeMs).coerceAtLeast(0L)
                                onScrub(target)
                            }
                            true
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val stepSizeMs = calculateStepMs(keyEvent.nativeKeyEvent.repeatCount)
                            if (!isScrubbing) {
                                onStartScrub(currentPositionMs)
                                val target = (currentPositionMs + stepSizeMs).coerceAtMost(durationMs)
                                onScrub(target)
                            } else {
                                val target = (scrubPositionMs + stepSizeMs).coerceAtMost(durationMs)
                                onScrub(target)
                            }
                            true
                        }

                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (isScrubbing) {
                                onCommitScrub()
                            } else {
                                onTogglePlayPause()
                            }
                            true
                        }

                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE -> {
                            if (isScrubbing) {
                                onCancelScrub()
                                true
                            } else false
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (isScrubbing) {
                                onCancelScrub()
                            }
                            try {
                                topBarFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                            true
                        }

                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (isScrubbing) {
                                onCancelScrub()
                            }
                            try {
                                bottomControlFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                            true
                        }

                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .background(
                if (isFocused || isScrubbing) Color(0x18FFFFFF) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isFocused || isScrubbing) 1.dp else 0.dp,
                color = if (isFocused || isScrubbing) Color(0x33FFFFFF) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Timecode & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Timestamps
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatPlayerTimestamp(activePositionMs / 1000L),
                        style = ErasmusTvTypography.SectionTitle.copy(
                            fontSize = 15.sp,
                            fontWeight = if (isFocused || isScrubbing) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isScrubbing) BorderFocused else if (isFocused) FocusWhite else TextPrimary
                    )

                    Text(
                        text = "/",
                        style = ErasmusTvTypography.Badge,
                        color = TextMuted
                    )

                    Text(
                        text = formatPlayerTimestamp(durationMs / 1000L),
                        style = ErasmusTvTypography.Badge.copy(fontSize = 14.sp),
                        color = TextSecondary
                    )

                    // Delta indicator while scrubbing
                    if (isScrubbing) {
                        val deltaSeconds = (scrubPositionMs - anchorPositionMs) / 1000L
                        val sign = if (deltaSeconds >= 0) "+" else "-"
                        val formattedDelta = "$sign${formatPlayerTimestamp(abs(deltaSeconds))}"
                        Box(
                            modifier = Modifier
                                .background(SurfaceElevated, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formattedDelta,
                                style = ErasmusTvTypography.Badge.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (deltaSeconds >= 0) FocusWhite else TextSecondary
                            )
                        }
                    }
                }

                // Right: Contextual Helper / Status
                AnimatedVisibility(
                    visible = isScrubbing,
                    enter = fadeIn(tween(100)),
                    exit = fadeOut(tween(100))
                ) {
                    Box(
                        modifier = Modifier
                            .background(SurfaceElevated.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Press OK to jump  ·  Back to cancel",
                            style = ErasmusTvTypography.Badge.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Track with Smoothly Positioned Playhead
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val totalWidth = maxWidth

                // Background unplayed track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .background(Color(0x38FFFFFF), RoundedCornerShape(3.dp))
                )

                // Active played progress bar
                Box(
                    modifier = Modifier
                        .width(totalWidth * progressRatio)
                        .height(trackHeight)
                        .background(
                            if (isScrubbing) BorderFocused else FocusWhite,
                            RoundedCornerShape(3.dp)
                        )
                )

                // Playhead Thumb (visible when focused or scrubbing)
                if (isFocused || isScrubbing) {
                    val availableTravel = (totalWidth - thumbSize).coerceAtLeast(0.dp)
                    val thumbOffset = availableTravel * progressRatio

                    Box(
                        modifier = Modifier
                            .offset(x = thumbOffset)
                            .size(thumbSize)
                            .background(FocusWhite, CircleShape)
                            .border(
                                width = if (isScrubbing) 2.dp else 1.dp,
                                color = if (isScrubbing) BorderFocused else Color(0x33000000),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

fun formatPlayerTimestamp(totalSeconds: Long): String {
    val sec = (totalSeconds % 60).coerceAtLeast(0)
    val totalMinutes = totalSeconds / 60
    val min = (totalMinutes % 60).coerceAtLeast(0)
    val hours = totalMinutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, min, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}

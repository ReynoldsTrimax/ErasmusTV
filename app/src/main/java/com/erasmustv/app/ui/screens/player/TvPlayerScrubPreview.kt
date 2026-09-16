package com.erasmustv.app.ui.screens.player

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.erasmustv.app.core.theme.BorderFocused
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary

@Composable
fun TvPlayerScrubPreview(
    visible: Boolean,
    scrubPositionMs: Long,
    anchorPositionMs: Long,
    durationMs: Long,
    previewBitmap: Bitmap?,
    isFetchingFrame: Boolean,
    backdropUrl: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val progressRatio = if (durationMs > 0) {
            (scrubPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        // Calculate horizontal bias from -1f (left) to 1f (right), clamped to avoid edge overflow
        val horizontalBias = ((progressRatio * 2f) - 1f).coerceIn(-0.82f, 0.82f)

        val deltaMs = scrubPositionMs - anchorPositionMs
        val deltaSeconds = deltaMs / 1000L
        val deltaSign = if (deltaSeconds >= 0) "+" else "-"
        val deltaFormatted = "$deltaSign${formatPreviewTimecode(Math.abs(deltaSeconds))}"

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            contentAlignment = BiasAlignment(horizontalBias, 0f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 16:9 Sharp Preview Frame Container
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(146.dp)
                        .background(SurfaceDark, RectangleShape)
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Preview Frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!backdropUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = backdropUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PitchBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = ErasmusTvTypography.Badge,
                                color = TextMuted
                            )
                        }
                    }

                    // Scrim gradient for contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        PitchBlack.copy(alpha = 0.85f)
                                    ),
                                    startY = 60f
                                )
                            )
                    )

                    // Loading spinner in corner if fetching exact frame
                    if (isFetchingFrame) {
                        CircularProgressIndicator(
                            color = FocusWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        )
                    }

                    // Overlay details at bottom of preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = formatPreviewTimecode(scrubPositionMs / 1000L),
                                style = ErasmusTvTypography.BillboardTitle.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )

                            Text(
                                text = deltaFormatted,
                                style = ErasmusTvTypography.Badge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (deltaSeconds >= 0) FocusWhite else TextSecondary
                            )
                        }
                    }
                }

                // Sharp geometric notch pointing down to timeline
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(FocusWhite)
                )
            }
        }
    }
}

fun formatPreviewTimecode(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, sec)
    } else {
        String.format("%02d:%02d", m, sec)
    }
}

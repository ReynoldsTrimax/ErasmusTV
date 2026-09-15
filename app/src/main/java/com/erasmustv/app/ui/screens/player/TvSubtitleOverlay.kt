package com.erasmustv.app.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.R
import com.erasmustv.app.data.local.SubtitleFont
import com.erasmustv.app.data.local.SubtitleSize

@Composable
fun TvSubtitleOverlay(
    activeText: String?,
    font: SubtitleFont,
    size: SubtitleSize,
    isControlsVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val fontFamily = when (font) {
        SubtitleFont.SANS_SERIF -> FontFamily.SansSerif
        SubtitleFont.SERIF -> FontFamily.Serif
        SubtitleFont.MONOSPACE -> FontFamily.Monospace
        SubtitleFont.CASUAL -> FontFamily.Cursive
        SubtitleFont.BOSTONE -> FontFamily(Font(R.font.bostone))
    }

    val fontSize = when (size) {
        SubtitleSize.SMALL -> 20.sp
        SubtitleSize.MEDIUM -> 26.sp
        SubtitleSize.LARGE -> 32.sp
    }

    val bottomPadding = if (isControlsVisible) 118.dp else 42.dp

    AnimatedVisibility(
        visible = !activeText.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding, start = 64.dp, end = 64.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (!activeText.isNullOrBlank()) {
                // High contrast shadow with pure transparent background (no background box)
                Box {
                    // Deep drop-shadow layer for extreme clarity against any bright background
                    Text(
                        text = activeText,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = fontSize * 1.3f,
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(0f, 3f),
                                blurRadius = 6f
                            )
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Foreground crisp white text
                    Text(
                        text = activeText,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = fontSize * 1.3f,
                            shadow = Shadow(
                                color = Color(0xCC000000),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 3f
                            )
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

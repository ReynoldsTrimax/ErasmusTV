package com.erasmustv.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite

/**
 * Ambient "YOU ARE WATCHING" pause overlay centered on screen.
 *
 * Appears during paused video with a smooth 2-3 second cinematic fade-in.
 * Displays:
 * 1. "YOU ARE WATCHING" uppercase tracked-out micro-header
 * 2. Authentic original title logo from TMDB
 * 3. Movie/show tagline rendered in classic serif italic font in quotation marks
 *
 * All HUD clutter (progress bar, timecodes, paused badge) is omitted for a clean cinematic view.
 */
@Composable
fun TvPlayerPauseOverlay(
    title: String,
    logoPath: String?,
    tagline: String?,
    mediaType: String,
    season: Int? = null,
    episode: Int? = null,
    episodeTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLogoError by remember(logoPath) { mutableStateOf(false) }
    val isTv = mediaType.equals("tv", ignoreCase = true)

    val effectiveTagline = remember(tagline, isTv, season, episode, episodeTitle) {
        if (!tagline.isNullOrBlank()) {
            val trimmed = tagline.trim()
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("“") && trimmed.endsWith("”"))
            ) {
                trimmed
            } else {
                "“$trimmed”"
            }
        } else if (isTv && season != null && episode != null) {
            if (!episodeTitle.isNullOrBlank()) {
                "“Season $season, Episode $episode • $episodeTitle”"
            } else {
                "“Season $season, Episode $episode”"
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp)
        ) {
            // "YOU ARE WATCHING" Micro-Header
            Text(
                text = "YOU ARE WATCHING",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Authentic Title Logo (or Fallback Title Text)
            if (!logoPath.isNullOrBlank() && !isLogoError) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(AppConfig.logoUrl(logoPath))
                        .memoryCacheKey(logoPath)
                        .diskCacheKey(logoPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .heightIn(min = 60.dp, max = 150.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    onError = { isLogoError = true }
                )
            } else {
                Text(
                    text = title,
                    style = ErasmusTvTypography.BillboardTitle.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = FocusWhite,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tagline in Serif Italic font matching the reference design
            if (!effectiveTagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = effectiveTagline,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

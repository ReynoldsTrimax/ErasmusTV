package com.erasmustv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.erasmustv.app.core.theme.PitchBlack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.RatingGold
import com.erasmustv.app.core.theme.SurfaceCard
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.MediaItem

import androidx.compose.ui.semantics.Role

/**
 * Movie & TV Poster Card.
 * Artwork-dominated presentation with clean 2:3 ratio, sharp square geometry,
 * subtle surface borders, and understated metadata beneath the poster.
 */
@Composable
fun MediaPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    cardWidth: Int = 138,
    rank: Int? = null,
    badge: String? = null
) {
    val context = LocalContext.current
    val imageRequest = remember(item.posterPath) {
        ImageRequest.Builder(context)
            .data(AppConfig.posterUrl(item.posterPath))
            .crossfade(200)
            .build()
    }

    val a11yDescription = remember(item, rank, badge) {
        buildString {
            if (rank != null) {
                append("Number $rank, ")
            }
            if (badge != null) {
                append("$badge, ")
            }
            append(item.title)
            append(if (item.isTv) ", TV Series" else ", Movie")
            item.year?.let { append(", $it") }
            if (item.voteAverage != null && item.voteAverage > 0) {
                append(", rated ${item.ratingFormatted} stars")
            }
        }
    }

    Column(
        modifier = modifier.width(cardWidth.dp)
    ) {
        TvFocusableCard(
            onClick = onClick,
            modifier = cardModifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = RectangleShape,
            focusedScale = com.erasmustv.app.core.theme.TvMotion.FocusScaleCard,
            focusedBorderColor = FocusWhite,
            focusedBorderWidth = 1.5.dp,
            contentDescription = a11yDescription,
            role = Role.Button
        ) { isFocused ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                if (isFocused) Color(0xFF1E1E28) else Color(0xFF141418),
                                if (isFocused) Color(0xFF131318) else Color(0xFF0C0C0F)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isFocused) Color.Transparent else com.erasmustv.app.core.theme.SurfaceCardBorder,
                        shape = RectangleShape
                    )
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Restrained corner badge (e.g., NEW, TOP 10)
                if (!badge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(PitchBlack.copy(alpha = 0.85f), RectangleShape)
                            .border(1.dp, Color(0x33FFFFFF), RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = ErasmusTvTypography.Badge.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (badge == "TOP 10") RatingGold else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Title Line
        Text(
            text = item.title,
            style = ErasmusTvTypography.CardTitle,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Metadata Line: ★ 6.7 · 2026 · Movie
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (item.voteAverage != null && item.voteAverage > 0) {
                Text(
                    text = "★ ${item.ratingFormatted}",
                    style = ErasmusTvTypography.Badge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = RatingGold,
                    maxLines = 1
                )
                Text(
                    text = " · ",
                    style = ErasmusTvTypography.Badge.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }

            item.year?.let { y ->
                Text(
                    text = y,
                    style = ErasmusTvTypography.Badge.copy(fontSize = 12.sp),
                    color = TextSecondary,
                    maxLines = 1
                )
                Text(
                    text = " · ",
                    style = ErasmusTvTypography.Badge.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }

            Text(
                text = if (item.isTv) "Series" else "Movie",
                style = ErasmusTvTypography.Badge.copy(fontSize = 12.sp),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

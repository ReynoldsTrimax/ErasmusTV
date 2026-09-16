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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
    cardWidth: Int = 138
) {
    val context = LocalContext.current
    val imageRequest = remember(item.posterPath) {
        ImageRequest.Builder(context)
            .data(AppConfig.posterUrl(item.posterPath))
            .crossfade(false)
            .build()
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
            focusedScale = 1.0f,
            focusedBorderColor = FocusWhite,
            focusedBorderWidth = 1.5.dp
        ) { isFocused ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .background(SurfaceCard)
                    .border(
                        width = 1.dp,
                        color = if (isFocused) FocusWhite else com.erasmustv.app.core.theme.SurfaceCardBorder,
                        shape = RectangleShape
                    )
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = RatingGold,
                    maxLines = 1
                )
                Text(
                    text = " · ",
                    style = ErasmusTvTypography.Badge.copy(fontSize = 10.sp),
                    color = TextMuted
                )
            }

            item.year?.let { y ->
                Text(
                    text = y,
                    style = ErasmusTvTypography.Badge.copy(fontSize = 10.sp),
                    color = TextSecondary,
                    maxLines = 1
                )
                Text(
                    text = " · ",
                    style = ErasmusTvTypography.Badge.copy(fontSize = 10.sp),
                    color = TextMuted
                )
            }

            Text(
                text = if (item.isTv) "Series" else "Movie",
                style = ErasmusTvTypography.Badge.copy(fontSize = 10.sp),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

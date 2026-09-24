package com.erasmustv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.data.model.MediaItem

/**
 * ERASMUS CARD TYPE 2 — the vertical poster card.
 *
 * This is the card used by *everything* except Continue Watching: trending,
 * Top 10, genre shelves, search results, recommendations, watch list, studio
 * catalogues. Its 2:3 artwork is the dominant element; the title and metadata
 * beneath it are intentionally quiet supporting detail.
 *
 * @param cardWidth artwork width; height follows from the fixed 2:3 ratio.
 *   Pass [Dp.Unspecified] inside a grid to let the card fill its cell instead.
 * @param rank surfaced to accessibility only — the visible numeral for Top 10
 *   rails is drawn by [RankedSectionRow], not stamped onto the artwork.
 * @param badge optional short overline such as `NEW`. Left null for most rails.
 */
@Composable
fun MediaPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    cardWidth: Dp = ErasmusDimens.PosterCardWidth,
    rank: Int? = null,
    badge: String? = null,
    showMetadata: Boolean = true
) {
    val imageRequest = rememberCardImageRequest(AppConfig.posterUrl(item.posterPath))

    val a11yDescription = remember(item, rank, badge) {
        buildString {
            if (rank != null) append("Number $rank, ")
            if (badge != null) append("$badge, ")
            append(item.title)
            append(if (item.isTv) ", TV Series" else ", Movie")
            item.year?.let { append(", $it") }
            if (item.voteAverage != null && item.voteAverage > 0) {
                append(", rated ${item.ratingFormatted} out of 10")
            }
        }
    }

    // Rails give the card an explicit width; grids let the cell decide.
    val widthModifier = if (cardWidth == Dp.Unspecified) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(cardWidth)
    }

    Column(modifier = modifier.then(widthModifier)) {
        TvFocusableCard(
            onClick = onClick,
            modifier = cardModifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = ErasmusShapes.Card,
            focusedScale = TvMotion.FocusScaleCard,
            focusedBorderColor = FocusWhite,
            focusedBorderWidth = 1.5.dp,
            contentDescription = a11yDescription,
            role = Role.Button
        ) { isFocused ->
            ErasmusCardArtwork(
                model = imageRequest,
                isFocused = isFocused,
                shape = ErasmusShapes.Card,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!badge.isNullOrBlank()) {
                    PosterBadge(
                        text = badge,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(7.dp)
                    )
                }
            }
        }

        if (showMetadata) {
            Spacer(modifier = Modifier.height(ErasmusDimens.CardMetadataGap))
            ErasmusCardTitle(title = item.title)
            Spacer(modifier = Modifier.height(3.dp))
            ErasmusCardMetadata(item = item)
        }
    }
}

/**
 * A short overline marker (`NEW`) on poster artwork. Small, rounded, and
 * near-black so it labels the poster without becoming a graphic element.
 */
@Composable
private fun PosterBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(PitchBlack.copy(alpha = 0.72f), ErasmusShapes.CardSmall)
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = text,
            style = ErasmusTvTypography.Badge.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = TextPrimary.copy(alpha = 0.92f)
        )
    }
}

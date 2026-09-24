package com.erasmustv.app.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.erasmustv.app.R

val BostoneFontFamily = FontFamily(
    Font(R.font.bostone, FontWeight.Normal)
)

object ErasmusTvTypography {
    // Tracking is a function of size, never one value for everything. Large type
    // reads too loose as it grows, small type too tight at 10 feet, so the ladder
    // runs negative at display sizes and slightly positive for supporting copy:
    //
    //   ≥ 30sp   −0.018em   (34sp → −0.6sp, 32sp → −0.6sp)
    //   24–29sp  −0.012em   (24sp → −0.3sp)
    //   14–18sp  −0.007em   (14sp → −0.1sp)
    //   ≤ 13.5sp +0.015em   (12.5–13.5sp → +0.2sp)
    //
    // Display numerals and the Bostone wordmark sit outside the ladder: both are
    // graphic marks, tracked by eye.

    // Wordmark typography
    val Wordmark = TextStyle(
        fontFamily = BostoneFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = 2.5.sp,
        color = TextPrimary
    )

    val BillboardTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        color = TextPrimary
    )

    // Shelf headings. Strong enough to anchor a section from across a room,
    // deliberately held at the low end of the spec range (24-30sp) so headings
    // never start competing with the hero or with the artwork below them.
    val SectionTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        color = TextPrimary
    )

    val SectionAction = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
        color = TextMuted
    )

    val CardTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp,
        color = TextPrimary
    )

    /** Muted supporting line beneath a card title: rating · year · type. */
    val CardMeta = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        color = TextSecondary
    )

    /**
     * Top-level page heading (Watch List, Studios, Search). Larger than a shelf
     * heading so a page reads as a place, but well below hero scale.
     */
    val PageTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
        color = TextPrimary
    )

    /** Quiet one-line subtitle under a [PageTitle]. */
    val PageSubtitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSecondary
    )

    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
        color = TextSecondary
    )

    val BodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.2.sp,
        color = TextSecondary
    )

    val Badge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    )

    val ButtonText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
        color = TextPrimary
    )

    val BrandBadge = TextStyle(
        fontFamily = BostoneFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        letterSpacing = 1.5.sp,
        color = TextPrimary
    )

    val MatchScore = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = MatchGreen
    )

    val HeroTitleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
        color = TextPrimary
    )

    val HeroMeta = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        letterSpacing = 0.2.sp,
        color = TextSecondary
    )

    // Oversized graphic rank numbers (1, 2, 3, etc.) matching Reference 3 & 5
    val HeroRankNumber = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 80.sp,
        lineHeight = 70.sp,
        letterSpacing = (-5).sp,
        color = TextPrimary
    )

    val CastSummary = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp,
        color = TextSecondary
    )

    val Top10Badge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = TextPrimary
    )
}


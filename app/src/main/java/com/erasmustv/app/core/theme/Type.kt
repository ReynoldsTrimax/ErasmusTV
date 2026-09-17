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
        color = TextPrimary
    )

    val SectionTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
        color = TextPrimary
    )

    val SectionAction = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        color = TextMuted
    )

    val CardTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = TextPrimary
    )

    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        color = TextSecondary
    )

    val BodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
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
        letterSpacing = (-0.3).sp,
        color = TextPrimary
    )

    val HeroMeta = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
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


package com.erasmustv.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom vector icons matching the TV sidebar design:
 * 1. Home (Outline house)
 * 2. Search (Magnifying glass)
 * 3. TV (Television monitor — matches uploaded thin-outline TV icon)
 * 4. Movies (Popcorn bucket — matches uploaded popcorn icon)
 * 5. Anime (Wave/swirl crescent — matches uploaded anime icon)
 * 6. Categories (Split panel layout: left card + two stacked right cards)
 * 7. MySpace (Person inside circle)
 */
object TvNavIcons {

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvNavHome",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 10.5f)
            lineTo(12f, 3.5f)
            lineTo(21f, 10.5f)
            verticalLineTo(20f)
            curveTo(21f, 20.6f, 20.6f, 21f, 20f, 21f)
            horizontalLineTo(15f)
            verticalLineTo(14.5f)
            curveTo(15f, 14.2f, 14.8f, 14f, 14.5f, 14f)
            horizontalLineTo(9.5f)
            curveTo(9.2f, 14f, 9f, 14.2f, 9f, 14.5f)
            verticalLineTo(21f)
            horizontalLineTo(4f)
            curveTo(3.4f, 21f, 3f, 20.6f, 3f, 20f)
            close()
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvNavSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Circle lens
            moveTo(11f, 3.5f)
            curveTo(15.14f, 3.5f, 18.5f, 6.86f, 18.5f, 11f)
            curveTo(18.5f, 15.14f, 15.14f, 18.5f, 11f, 18.5f)
            curveTo(6.86f, 18.5f, 3.5f, 15.14f, 3.5f, 11f)
            curveTo(3.5f, 6.86f, 6.86f, 3.5f, 11f, 3.5f)
            close()
            // Handle
            moveTo(16.5f, 16.5f)
            lineTo(21f, 21f)
        }.build()
    }

    // TV icon — matches uploaded thin-outline monitor with small centered stand
    val Tv: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvNavTv",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Screen rounded rectangle
            moveTo(2f, 5.5f)
            curveTo(2f, 4.67f, 2.67f, 4f, 3.5f, 4f)
            horizontalLineTo(20.5f)
            curveTo(21.33f, 4f, 22f, 4.67f, 22f, 5.5f)
            verticalLineTo(15.5f)
            curveTo(22f, 16.33f, 21.33f, 17f, 20.5f, 17f)
            horizontalLineTo(3.5f)
            curveTo(2.67f, 17f, 2f, 16.33f, 2f, 15.5f)
            close()
            // Short stand neck
            moveTo(12f, 17f)
            verticalLineTo(20f)
            // Base
            moveTo(8.5f, 20f)
            horizontalLineTo(15.5f)
        }.build()
    }

    // Movies icon — matches uploaded clean popcorn bucket with stripes
    val Movies: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvNavMovies",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Popcorn puffs on top (bumpy cloud shape)
            moveTo(5.5f, 9.5f)
            curveTo(4.8f, 9.5f, 4f, 9f, 4f, 8f)
            curveTo(4f, 7f, 4.9f, 6.3f, 5.9f, 6.5f)
            curveTo(6.1f, 5.3f, 7.2f, 4.5f, 8.3f, 4.5f)
            curveTo(8.8f, 4.5f, 9.3f, 4.7f, 9.7f, 5f)
            curveTo(10.1f, 4.1f, 11f, 3.5f, 12f, 3.5f)
            curveTo(13f, 3.5f, 13.9f, 4.1f, 14.3f, 5f)
            curveTo(14.7f, 4.7f, 15.2f, 4.5f, 15.7f, 4.5f)
            curveTo(16.8f, 4.5f, 17.9f, 5.3f, 18.1f, 6.5f)
            curveTo(19.1f, 6.3f, 20f, 7f, 20f, 8f)
            curveTo(20f, 9f, 19.2f, 9.5f, 18.5f, 9.5f)
            // Bucket body (trapezoid)
            lineTo(17f, 20.5f)
            horizontalLineTo(7f)
            close()
            // Vertical stripes on the bucket
            moveTo(9.5f, 9.5f)
            lineTo(8.5f, 20.5f)
            moveTo(12f, 9.5f)
            verticalLineTo(20.5f)
            moveTo(14.5f, 9.5f)
            lineTo(15.5f, 20.5f)
        }.build()
    }

    // Anime icon — matches uploaded wave/swirl crescent (Crunchyroll-style)
    val Anime: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvNavAnime",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Outer circle arc forming the large C/crescent shape
            moveTo(19.5f, 7f)
            curveTo(18f, 4.8f, 15.2f, 3.5f, 12f, 3.5f)
            curveTo(7.3f, 3.5f, 3.5f, 7.3f, 3.5f, 12f)
            curveTo(3.5f, 16.7f, 7.3f, 20.5f, 12f, 20.5f)
            curveTo(15.2f, 20.5f, 18f, 19.2f, 19.5f, 17f)
            // Inner circle (smaller concentric) — creates the "C-with-inner-C" wave look
            curveTo(18.5f, 15.8f, 17f, 15f, 15.5f, 15f)
            curveTo(13f, 15f, 11f, 13f, 11f, 10.5f)
            curveTo(11f, 9f, 11.8f, 7.5f, 12.8f, 6.5f)
            curveTo(11.8f, 6.2f, 10.8f, 6f, 9.8f, 6f)
        }.build()
    }

    val Sports: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvNavSports",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Ball circle
            moveTo(12f, 3.5f)
            curveTo(16.7f, 3.5f, 20.5f, 7.3f, 20.5f, 12f)
            curveTo(20.5f, 16.7f, 16.7f, 20.5f, 12f, 20.5f)
            curveTo(7.3f, 20.5f, 3.5f, 16.7f, 3.5f, 12f)
            curveTo(3.5f, 7.3f, 7.3f, 3.5f, 12f, 3.5f)
            close()
            // Diagonal stripes
            moveTo(6f, 7f)
            lineTo(17f, 18f)
            moveTo(9.5f, 4.5f)
            lineTo(19.5f, 14.5f)
            moveTo(4.5f, 10f)
            lineTo(14f, 19.5f)
        }.build()
    }

    val Sparks: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvNavSparks",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Lightning bolt outline
            moveTo(13.5f, 2.5f)
            lineTo(5.5f, 13f)
            horizontalLineTo(12f)
            lineTo(10.5f, 21.5f)
            lineTo(18.5f, 11f)
            horizontalLineTo(12f)
            close()
        }.build()
    }

    val Categories: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvNavCategories",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Tall rounded rectangle on left
            moveTo(5.5f, 4f)
            curveTo(4.7f, 4f, 4f, 4.7f, 4f, 5.5f)
            verticalLineTo(18.5f)
            curveTo(4f, 19.3f, 4.7f, 20f, 5.5f, 20f)
            horizontalLineTo(8.5f)
            curveTo(9.3f, 20f, 10f, 19.3f, 10f, 18.5f)
            verticalLineTo(5.5f)
            curveTo(10f, 4.7f, 9.3f, 4f, 8.5f, 4f)
            close()
            // Top rounded rectangle on right
            moveTo(15.5f, 4f)
            curveTo(14.7f, 4f, 14f, 4.7f, 14f, 5.5f)
            verticalLineTo(9.5f)
            curveTo(14f, 10.3f, 14.7f, 11f, 15.5f, 11f)
            horizontalLineTo(18.5f)
            curveTo(19.3f, 11f, 20f, 10.3f, 20f, 9.5f)
            verticalLineTo(5.5f)
            curveTo(20f, 4.7f, 19.3f, 4f, 18.5f, 4f)
            close()
            // Bottom rounded rectangle on right
            moveTo(15.5f, 13f)
            curveTo(14.7f, 13f, 14f, 13.7f, 14f, 14.5f)
            verticalLineTo(18.5f)
            curveTo(14f, 19.3f, 14.7f, 20f, 15.5f, 20f)
            horizontalLineTo(18.5f)
            curveTo(19.3f, 20f, 20f, 19.3f, 20f, 18.5f)
            verticalLineTo(14.5f)
            curveTo(20f, 13.7f, 19.3f, 13f, 18.5f, 13f)
            close()
        }.build()
    }

    val MySpace: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvNavMySpace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Outer circle
            moveTo(12f, 2.5f)
            curveTo(17.2f, 2.5f, 21.5f, 6.8f, 21.5f, 12f)
            curveTo(21.5f, 17.2f, 17.2f, 21.5f, 12f, 21.5f)
            curveTo(6.8f, 21.5f, 2.5f, 17.2f, 2.5f, 12f)
            curveTo(2.5f, 6.8f, 6.8f, 2.5f, 12f, 2.5f)
            close()
            // Head
            moveTo(12f, 7f)
            curveTo(13.4f, 7f, 14.5f, 8.1f, 14.5f, 9.5f)
            curveTo(14.5f, 10.9f, 13.4f, 12f, 12f, 12f)
            curveTo(10.6f, 12f, 9.5f, 10.9f, 9.5f, 9.5f)
            curveTo(9.5f, 8.1f, 10.6f, 7f, 12f, 7f)
            close()
            // Torso / shoulders arc
            moveTo(6.8f, 18.2f)
            curveTo(7.8f, 15.7f, 9.8f, 14.5f, 12f, 14.5f)
            curveTo(14.2f, 14.5f, 16.2f, 15.7f, 17.2f, 18.2f)
        }.build()
    }
}

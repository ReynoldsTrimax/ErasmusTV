package com.erasmustv.app.ui.screens.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Cohesive, minimal TV player vector icons with consistent 1.8f stroke weight,
 * rounded caps, and professional visual scale.
 */
object TvPlayerIcons {

    val Back: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
            moveTo(12f, 5f)
            lineTo(5f, 12f)
            lineTo(12f, 19f)
        }.build()
    }

    val Restart: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerRestart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Circular replay arc
            moveTo(12f, 4f)
            curveTo(7.6f, 4f, 4f, 7.6f, 4f, 12f)
            curveTo(4f, 16.4f, 7.6f, 20f, 12f, 20f)
            curveTo(16.4f, 20f, 20f, 16.4f, 20f, 12f)
            curveTo(20f, 9.8f, 19.1f, 7.8f, 17.6f, 6.4f)
            lineTo(17f, 7f)
            // Arrowhead at top left
            moveTo(12f, 1.5f)
            lineTo(12f, 6.5f)
            lineTo(7f, 4f)
            close()
        }.build()
    }

    val Server: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerServer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Top rack unit
            moveTo(4f, 5f)
            curveTo(4f, 4.4f, 4.4f, 4f, 5f, 4f)
            horizontalLineTo(19f)
            curveTo(19.6f, 4f, 20f, 4.4f, 20f, 5f)
            verticalLineTo(9f)
            curveTo(20f, 9.6f, 19.6f, 10f, 19f, 10f)
            horizontalLineTo(5f)
            curveTo(4.4f, 10f, 4f, 9.6f, 4f, 9f)
            close()
            // Indicator lights top
            moveTo(7.5f, 7f)
            lineTo(7.55f, 7f)
            moveTo(10.5f, 7f)
            lineTo(10.55f, 7f)

            // Bottom rack unit
            moveTo(4f, 14f)
            curveTo(4f, 13.4f, 4.4f, 13f, 5f, 13f)
            horizontalLineTo(19f)
            curveTo(19.6f, 13f, 20f, 13.4f, 20f, 14f)
            verticalLineTo(18f)
            curveTo(20f, 18.6f, 19.6f, 19f, 19f, 19f)
            horizontalLineTo(5f)
            curveTo(4.4f, 19f, 4f, 18.6f, 4f, 18f)
            close()
            // Indicator lights bottom
            moveTo(7.5f, 16f)
            lineTo(7.55f, 16f)
            moveTo(10.5f, 16f)
            lineTo(10.55f, 16f)
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerPlay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7.5f, 5.5f)
            lineTo(18.5f, 12f)
            lineTo(7.5f, 18.5f)
            close()
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerPause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Left bar
            moveTo(7f, 5.5f)
            curveTo(6.5f, 5.5f, 6f, 6f, 6f, 6.5f)
            verticalLineTo(17.5f)
            curveTo(6f, 18f, 6.5f, 18.5f, 7f, 18.5f)
            horizontalLineTo(9.5f)
            curveTo(10f, 18.5f, 10.5f, 18f, 10.5f, 17.5f)
            verticalLineTo(6.5f)
            curveTo(10.5f, 6f, 10f, 5.5f, 9.5f, 5.5f)
            close()

            // Right bar
            moveTo(14.5f, 5.5f)
            curveTo(14f, 5.5f, 13.5f, 6f, 13.5f, 6.5f)
            verticalLineTo(17.5f)
            curveTo(13.5f, 18f, 14f, 18.5f, 14.5f, 18.5f)
            horizontalLineTo(17f)
            curveTo(17.5f, 18.5f, 18f, 18f, 18f, 17.5f)
            verticalLineTo(6.5f)
            curveTo(18f, 6f, 17.5f, 5.5f, 17f, 5.5f)
            close()
        }.build()
    }

    val Replay10: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerReplay10",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 270-degree CCW circular arc
            moveTo(12.5f, 4f)
            curveTo(7.8f, 4f, 4f, 7.8f, 4f, 12.5f)
            curveTo(4f, 17.2f, 7.8f, 21f, 12.5f, 21f)
            curveTo(17.2f, 21f, 21f, 17.2f, 21f, 12.5f)
            curveTo(21f, 10.5f, 20.3f, 8.7f, 19.1f, 7.3f)
            // Arrowhead at top left
            moveTo(12.5f, 1.5f)
            lineTo(12.5f, 6.5f)
            lineTo(7.5f, 4f)
            close()
        }.path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Digit "1"
            moveTo(9.5f, 11f)
            lineTo(11f, 9.5f)
            verticalLineTo(16f)
            // Digit "0"
            moveTo(14f, 10f)
            curveTo(13.2f, 10f, 12.8f, 10.8f, 12.8f, 12.7f)
            verticalLineTo(13.3f)
            curveTo(12.8f, 15.2f, 13.2f, 16f, 14f, 16f)
            curveTo(14.8f, 16f, 15.2f, 15.2f, 15.2f, 13.3f)
            verticalLineTo(12.7f)
            curveTo(15.2f, 10.8f, 14.8f, 10f, 14f, 10f)
            close()
        }.build()
    }

    val Forward10: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerForward10",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 270-degree CW circular arc
            moveTo(11.5f, 4f)
            curveTo(16.2f, 4f, 20f, 7.8f, 20f, 12.5f)
            curveTo(20f, 17.2f, 16.2f, 21f, 11.5f, 21f)
            curveTo(6.8f, 21f, 3f, 17.2f, 3f, 12.5f)
            curveTo(3f, 10.5f, 3.7f, 8.7f, 4.9f, 7.3f)
            // Arrowhead at top right
            moveTo(11.5f, 1.5f)
            lineTo(11.5f, 6.5f)
            lineTo(16.5f, 4f)
            close()
        }.path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Digit "1"
            moveTo(9.5f, 11f)
            lineTo(11f, 9.5f)
            verticalLineTo(16f)
            // Digit "0"
            moveTo(14f, 10f)
            curveTo(13.2f, 10f, 12.8f, 10.8f, 12.8f, 12.7f)
            verticalLineTo(13.3f)
            curveTo(12.8f, 15.2f, 13.2f, 16f, 14f, 16f)
            curveTo(14.8f, 16f, 15.2f, 15.2f, 15.2f, 13.3f)
            verticalLineTo(12.7f)
            curveTo(15.2f, 10.8f, 14.8f, 10f, 14f, 10f)
            close()
        }.build()
    }

    val Audio: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerAudio",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Speaker cone
            moveTo(3f, 9.5f)
            horizontalLineTo(7f)
            lineTo(12f, 5f)
            verticalLineTo(19f)
            lineTo(7f, 14.5f)
            horizontalLineTo(3f)
            close()
            // Soundwaves
            moveTo(15.5f, 9f)
            curveTo(16.8f, 10.2f, 17.5f, 11.8f, 17.5f, 12f)
            curveTo(17.5f, 12.2f, 16.8f, 13.8f, 15.5f, 15f)
            moveTo(18.5f, 6.5f)
            curveTo(20.5f, 8.5f, 21.5f, 10.8f, 21.5f, 12f)
            curveTo(21.5f, 13.2f, 20.5f, 15.5f, 18.5f, 17.5f)
        }.build()
    }

    val Subtitles: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerSubtitles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Outer rectangle
            moveTo(4.5f, 5.5f)
            curveTo(3.7f, 5.5f, 3f, 6.2f, 3f, 7f)
            verticalLineTo(17f)
            curveTo(3f, 17.8f, 3.7f, 18.5f, 4.5f, 18.5f)
            horizontalLineTo(19.5f)
            curveTo(20.3f, 18.5f, 21f, 17.8f, 21f, 17f)
            verticalLineTo(7f)
            curveTo(21f, 6.2f, 20.3f, 5.5f, 19.5f, 5.5f)
            close()
        }.path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // First CC "C"
            moveTo(10f, 10f)
            curveTo(9.4f, 9.5f, 8.5f, 9.5f, 8f, 10f)
            curveTo(7.2f, 10.8f, 7.2f, 13.2f, 8f, 14f)
            curveTo(8.5f, 14.5f, 9.4f, 14.5f, 10f, 14f)
            // Second CC "C"
            moveTo(16f, 10f)
            curveTo(15.4f, 9.5f, 14.5f, 9.5f, 14f, 10f)
            curveTo(13.2f, 10.8f, 13.2f, 13.2f, 14f, 14f)
            curveTo(14.5f, 14.5f, 15.4f, 14.5f, 16f, 14f)
        }.build()
    }

    val Quality: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerQuality",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Outer rectangle
            moveTo(4f, 5.5f)
            curveTo(3.4f, 5.5f, 3f, 5.9f, 3f, 6.5f)
            verticalLineTo(17.5f)
            curveTo(3f, 18.1f, 3.4f, 18.5f, 4f, 18.5f)
            horizontalLineTo(20f)
            curveTo(20.6f, 18.5f, 21f, 18.1f, 21f, 17.5f)
            verticalLineTo(6.5f)
            curveTo(21f, 5.9f, 20.6f, 5.5f, 20f, 5.5f)
            close()
        }.path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // "H"
            moveTo(6.5f, 9.5f)
            verticalLineTo(14.5f)
            moveTo(6.5f, 12f)
            horizontalLineTo(9.5f)
            moveTo(9.5f, 9.5f)
            verticalLineTo(14.5f)
            // "D"
            moveTo(13f, 9.5f)
            verticalLineTo(14.5f)
            moveTo(13f, 9.5f)
            horizontalLineTo(15f)
            curveTo(16.5f, 9.5f, 17.5f, 10.5f, 17.5f, 12f)
            curveTo(17.5f, 13.5f, 16.5f, 14.5f, 15f, 14.5f)
            horizontalLineTo(13f)
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerClose",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4.5f, 12f)
            lineTo(9.5f, 17f)
            lineTo(19.5f, 7f)
        }.build()
    }

    val Episodes: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerEpisodes",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Back stacked window (offset top-right)
            moveTo(8.5f, 4f)
            horizontalLineTo(19.5f)
            curveTo(20.3f, 4f, 21f, 4.7f, 21f, 5.5f)
            verticalLineTo(15f)

            // Front window
            moveTo(4.5f, 7.5f)
            curveTo(3.7f, 7.5f, 3f, 8.2f, 3f, 9f)
            verticalLineTo(18.5f)
            curveTo(3f, 19.3f, 3.7f, 20f, 4.5f, 20f)
            horizontalLineTo(15.5f)
            curveTo(16.3f, 20f, 17f, 19.3f, 17f, 18.5f)
            verticalLineTo(9f)
            curveTo(17f, 8.2f, 16.3f, 7.5f, 15.5f, 7.5f)
            close()
        }.build()
    }

    val Soundwave: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerSoundwave",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(2.5f, 6.5f)
            lineTo(2.5f, 9.5f)
            moveTo(5.5f, 4f)
            lineTo(5.5f, 12f)
            moveTo(8.5f, 2f)
            lineTo(8.5f, 14f)
            moveTo(11.5f, 4.5f)
            lineTo(11.5f, 11.5f)
            moveTo(14.5f, 7f)
            lineTo(14.5f, 9f)
        }.build()
    }

    val ArrowForward: ImageVector by lazy {
        ImageVector.Builder(
            name = "TvPlayerArrowForward",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7.5f, 4.5f)
            lineTo(13.5f, 10f)
            lineTo(7.5f, 15.5f)
        }.build()
    }
}

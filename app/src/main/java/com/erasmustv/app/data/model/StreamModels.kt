package com.erasmustv.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamServer(
    val id: String,
    val name: String,
    val flag: String,
    val country: String,
    val badge: String,
    val description: String,
    val isPrimary: Boolean = false
)

val STREAM_SERVERS = listOf(
    StreamServer(
        id = "lisbon",
        name = "Lisbon",
        flag = "\uD83C\uDDFA\uD83C\uDDF8",
        country = "US",
        badge = "4K / 1080p",
        description = "Primary flagship server with ultra-high bitrate and multi-audio",
        isPrimary = true
    ),
    StreamServer(
        id = "sakura",
        name = "Sakura",
        flag = "\uD83C\uDDEF\uD83C\uDDF5",
        country = "JP",
        badge = "1080p Full HD",
        description = "Dedicated anime and Asian media cluster with dual audio and subtitles"
    ),
    StreamServer(
        id = "nebula",
        name = "Nebula",
        flag = "\uD83C\uDDFA\uD83C\uDDF8",
        country = "US",
        badge = "1080p High Speed",
        description = "High-speed US edge CDN cluster for instant start times"
    ),
    StreamServer(
        id = "solara",
        name = "Solara",
        flag = "\uD83C\uDDFA\uD83C\uDDF8",
        country = "US",
        badge = "1080p Full HD",
        description = "Full-library universal cloud player"
    ),
    StreamServer(
        id = "athens",
        name = "Athens",
        flag = "\uD83C\uDDEC\uD83C\uDDF7",
        country = "GR",
        badge = "4K Cinema",
        description = "High-availability 4K cinema mirror"
    ),
    StreamServer(
        id = "joy",
        name = "Joy",
        flag = "\uD83C\uDF10",
        country = "EU",
        badge = "Direct Cloud",
        description = "Direct video cloud stream"
    ),
    StreamServer(
        id = "castle",
        name = "Castle",
        flag = "\uD83C\uDDF7\uD83C\uDDF4",
        country = "RO",
        badge = "1080p Mirror",
        description = "Alternate ArtPlayer media mirror"
    ),
    StreamServer(
        id = "canaias",
        name = "Canaias",
        flag = "\uD83C\uDDEA\uD83C\uDDF8",
        country = "ES",
        badge = "Global Edge",
        description = "Global low-latency edge mirror"
    )
)

@Serializable
data class DirectServer(
    val name: String,
    val url: String,
    val ms: Long? = null,
    val kind: String = "hls"
)

@Serializable
data class CinejoyCaption(
    val label: String,
    val language: String,
    val url: String,
    val mimeType: String = "text/vtt"
)

@Serializable
data class DirectStreamResult(
    val ok: Boolean,
    val error: String? = null,
    val referer: String? = null,
    val captions: List<CinejoyCaption> = emptyList(),
    val servers: List<DirectServer> = emptyList()
)

@Serializable
data class SubtitleTrack(
    val label: String,
    val language: String,
    val url: String,
    val mimeType: String = "application/x-subrip"
)

@Serializable
data class PlaybackProgress(
    val seconds: Long,
    val duration: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val title: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val logoPath: String? = null
)

@Serializable
data class ContinueWatchingItem(
    val mediaType: String,
    val tmdbId: String,
    val season: Int? = null,
    val episode: Int? = null,
    val title: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val logoPath: String? = null,
    val seconds: Long,
    val duration: Long? = null,
    val updatedAt: Long
) {
    val progressRatio: Float get() {
        val d = duration ?: return 0f
        if (d <= 0) return 0f
        return (seconds.toFloat() / d.toFloat()).coerceIn(0f, 1f)
    }

    val resumeLabel: String get() {
        val s = seconds
        val m = s / 60
        val remaining = (duration?.minus(seconds))?.coerceAtLeast(0) ?: 0
        val remM = remaining / 60
        return if (remM > 0) "${remM}m left" else "${m}m elapsed"
    }
}

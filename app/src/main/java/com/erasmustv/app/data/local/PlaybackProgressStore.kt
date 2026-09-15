package com.erasmustv.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.data.model.ContinueWatchingItem
import com.erasmustv.app.data.model.PlaybackProgress
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlaybackProgressStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("erasmus_playback_progress", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private fun buildKey(
        profileId: String,
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null
    ): String {
        return if (mediaType.equals("tv", ignoreCase = true)) {
            val s = (season ?: 1).coerceAtLeast(1)
            val e = (episode ?: 1).coerceAtLeast(1)
            "progress:${profileId}:tv:${tmdbId}:s${s}:e${e}"
        } else {
            "progress:${profileId}:movie:${tmdbId}"
        }
    }

    fun saveProgress(
        profileId: String,
        mediaType: String,
        tmdbId: String,
        title: String,
        posterPath: String?,
        backdropPath: String?,
        seconds: Long,
        duration: Long?,
        season: Int? = null,
        episode: Int? = null,
        logoPath: String? = null
    ) {
        val key = buildKey(profileId, mediaType, tmdbId, season, episode)

        // Check if finished (>= 90% or within 30 seconds of end)
        if (duration != null && duration > 0) {
            val ratio = seconds.toFloat() / duration.toFloat()
            val remaining = duration - seconds
            if (ratio >= AppConfig.COMPLETE_RATIO || remaining < AppConfig.COMPLETE_REMAINING_SECONDS) {
                prefs.edit().remove(key).apply()
                return
            }
        }

        // Only save if >= 15 seconds watched
        if (seconds < AppConfig.MIN_RESUME_SECONDS) return

        val progress = PlaybackProgress(
            seconds = seconds,
            duration = duration,
            updatedAt = System.currentTimeMillis(),
            title = title,
            posterPath = posterPath,
            backdropPath = backdropPath,
            logoPath = logoPath
        )

        prefs.edit().putString(key, json.encodeToString(progress)).apply()
    }

    fun getProgress(
        profileId: String,
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null
    ): PlaybackProgress? {
        val key = buildKey(profileId, mediaType, tmdbId, season, episode)
        val raw = prefs.getString(key, null) ?: return null
        return try {
            json.decodeFromString<PlaybackProgress>(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun getResumePosition(
        profileId: String,
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null
    ): Long {
        val progress = getProgress(profileId, mediaType, tmdbId, season, episode) ?: return 0L
        return progress.seconds
    }

    fun listContinueWatching(profileId: String, limit: Int = 20): List<ContinueWatchingItem> {
        val prefix = "progress:${profileId}:"
        val items = mutableListOf<ContinueWatchingItem>()

        for ((key, value) in prefs.all) {
            if (!key.startsWith(prefix) || value !is String) continue
            try {
                val progress = json.decodeFromString<PlaybackProgress>(value)
                val parts = key.removePrefix(prefix).split(":")
                val mediaType = parts.getOrNull(0) ?: continue
                val tmdbId = parts.getOrNull(1) ?: continue

                var season: Int? = null
                var episode: Int? = null
                if (mediaType == "tv" && parts.size >= 4) {
                    season = parts[2].removePrefix("s").toIntOrNull()
                    episode = parts[3].removePrefix("e").toIntOrNull()
                }

                items.add(
                    ContinueWatchingItem(
                        mediaType = mediaType,
                        tmdbId = tmdbId,
                        season = season,
                        episode = episode,
                        title = progress.title ?: "Untitled",
                        posterPath = progress.posterPath,
                        backdropPath = progress.backdropPath,
                        logoPath = progress.logoPath,
                        seconds = progress.seconds,
                        duration = progress.duration,
                        updatedAt = progress.updatedAt
                    )
                )
            } catch (_: Exception) {
                // Ignore corrupt entry
            }
        }

        // Sort by most recently played
        return items.sortedByDescending { it.updatedAt }.take(limit)
    }

    fun clearProgress(
        profileId: String,
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null
    ) {
        val key = buildKey(profileId, mediaType, tmdbId, season, episode)
        prefs.edit().remove(key).apply()
    }
}

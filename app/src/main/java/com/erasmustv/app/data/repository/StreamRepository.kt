package com.erasmustv.app.data.repository

import com.erasmustv.app.data.local.PlaybackProgressStore
import com.erasmustv.app.data.model.CinejoyCaption
import com.erasmustv.app.data.model.ContinueWatchingItem
import com.erasmustv.app.data.model.DirectStreamResult
import com.erasmustv.app.data.model.SubtitleTrack
import com.erasmustv.app.data.remote.CinejoyStreamResolver
import com.erasmustv.app.data.remote.SubtitleResolver

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class StreamRepository(
    private val cinejoyResolver: CinejoyStreamResolver,
    private val subtitleResolver: SubtitleResolver,
    private val progressStore: PlaybackProgressStore
) {

    suspend fun extractStream(
        mediaType: String,
        tmdbId: String,
        server: String = "lisbon",
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        year: String? = null,
        imdbId: String? = null
    ): Result<DirectStreamResult> = runCatching {
        coroutineScope {
            val streamDeferred = async {
                cinejoyResolver.resolveStream(
                    mediaType = mediaType,
                    tmdbId = tmdbId,
                    title = title ?: "Video",
                    season = season,
                    episode = episode,
                    preferredServer = server,
                    year = year,
                    imdbId = imdbId
                )
            }

            val subsDeferred = async {
                subtitleResolver.getSubtitles(
                    tmdbId = tmdbId,
                    mediaType = mediaType,
                    imdbId = imdbId,
                    season = season,
                    episode = episode
                )
            }

            val streamResult = streamDeferred.await()
            val externalSubs = try {
                subsDeferred.await()
            } catch (e: Exception) {
                emptyList()
            }

            if (!streamResult.ok) {
                return@coroutineScope streamResult
            }

            val combinedCaptions = mutableListOf<CinejoyCaption>()
            val seenUrls = mutableSetOf<String>()
            val seenLangs = mutableSetOf<String>()

            // 1. Keep valid Cinejoy embedded captions
            for (cap in streamResult.captions) {
                if (cap.url.isNotBlank() && seenUrls.add(cap.url)) {
                    seenLangs.add(cap.language.lowercase())
                    combinedCaptions.add(cap)
                }
            }

            // 2. Add external subtitles
            for (sub in externalSubs) {
                val lang = sub.language.lowercase()
                if (sub.url.isNotBlank() && seenUrls.add(sub.url)) {
                    if (lang.startsWith("en") || seenLangs.add(lang)) {
                        combinedCaptions.add(
                            CinejoyCaption(
                                label = sub.label,
                                language = sub.language,
                                url = sub.url,
                                mimeType = sub.mimeType
                            )
                        )
                    }
                }
            }

            combinedCaptions.sortWith(compareBy<CinejoyCaption> {
                if (it.language.startsWith("en", ignoreCase = true)) 0 else 1
            }.thenBy { it.label })

            streamResult.copy(captions = combinedCaptions)
        }
    }

    suspend fun getSubtitles(
        tmdbId: String,
        mediaType: String = "movie",
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): Result<List<SubtitleTrack>> = runCatching {
        subtitleResolver.getSubtitles(
            tmdbId = tmdbId,
            mediaType = mediaType,
            imdbId = imdbId,
            season = season,
            episode = episode
        )
    }

    fun savePlaybackProgress(
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
        progressStore.saveProgress(
            profileId = profileId,
            mediaType = mediaType,
            tmdbId = tmdbId,
            title = title,
            posterPath = posterPath,
            backdropPath = backdropPath,
            seconds = seconds,
            duration = duration,
            season = season,
            episode = episode,
            logoPath = logoPath
        )
    }

    fun getResumePosition(
        profileId: String,
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null
    ): Long {
        return progressStore.getResumePosition(profileId, mediaType, tmdbId, season, episode)
    }

    fun listContinueWatching(profileId: String): List<ContinueWatchingItem> {
        return progressStore.listContinueWatching(profileId)
    }

    fun clearProgress(
        profileId: String,
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null
    ) {
        progressStore.clearProgress(profileId, mediaType, tmdbId, season, episode)
    }
}

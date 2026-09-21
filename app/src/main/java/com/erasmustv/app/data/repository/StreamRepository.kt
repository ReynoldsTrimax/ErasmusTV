package com.erasmustv.app.data.repository

import com.erasmustv.app.data.local.PlaybackProgressStore
import com.erasmustv.app.data.model.CinejoyCaption
import com.erasmustv.app.data.model.ContinueWatchingItem
import com.erasmustv.app.data.model.DirectStreamResult
import com.erasmustv.app.data.model.PlaybackProgress
import com.erasmustv.app.data.model.SubtitleTrack
import com.erasmustv.app.data.remote.BingrStreamResolver
import com.erasmustv.app.data.remote.CinejoyStreamResolver
import com.erasmustv.app.data.remote.SubtitleResolver

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class StreamRepository(
    private val cinejoyResolver: CinejoyStreamResolver,
    private val bingrResolver: BingrStreamResolver = BingrStreamResolver(),
    private val subtitleResolver: SubtitleResolver,
    private val progressStore: PlaybackProgressStore
) {

    constructor(
        cinejoyResolver: CinejoyStreamResolver,
        subtitleResolver: SubtitleResolver,
        progressStore: PlaybackProgressStore
    ) : this(
        cinejoyResolver = cinejoyResolver,
        bingrResolver = BingrStreamResolver(),
        subtitleResolver = subtitleResolver,
        progressStore = progressStore
    )

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
            // Check Bingr cluster first if explicitly requested
            if (bingrResolver.isBingrServer(server)) {
                val bingrHit = bingrResolver.resolveBingrStream(
                    mediaType = mediaType,
                    tmdbId = tmdbId,
                    title = title ?: "Video",
                    season = season,
                    episode = episode,
                    year = year,
                    imdbId = imdbId,
                    preferredServer = server
                )
                if (bingrHit != null && bingrHit.ok && bingrHit.servers.isNotEmpty()) {
                    val subsDeferred = async {
                        subtitleResolver.getSubtitles(
                            tmdbId = tmdbId,
                            mediaType = mediaType,
                            imdbId = imdbId,
                            season = season,
                            episode = episode
                        )
                    }
                    val externalSubs = try {
                        subsDeferred.await()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    return@coroutineScope mergeCaptions(bingrHit, externalSubs)
                }
            }

            // Fallback to legacy Cinejoy/Shegu/Vidfast resolver
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
            } catch (_: Exception) {
                emptyList()
            }

            if (!streamResult.ok) {
                // Secondary cluster-wide fallback: try Aphelion from Bingr cluster if not already queried
                if (!bingrResolver.isBingrServer(server)) {
                    val fallbackHit = bingrResolver.resolveBingrStream(
                        mediaType = mediaType,
                        tmdbId = tmdbId,
                        title = title ?: "Video",
                        season = season,
                        episode = episode,
                        year = year,
                        imdbId = imdbId,
                        preferredServer = "aphelion"
                    )
                    if (fallbackHit != null && fallbackHit.ok && fallbackHit.servers.isNotEmpty()) {
                        return@coroutineScope mergeCaptions(fallbackHit, externalSubs)
                    }
                }
                return@coroutineScope streamResult
            }

            mergeCaptions(streamResult, externalSubs)
        }
    }

    private fun mergeCaptions(
        streamResult: DirectStreamResult,
        externalSubs: List<SubtitleTrack>
    ): DirectStreamResult {
        val combinedCaptions = mutableListOf<CinejoyCaption>()
        val seenUrls = mutableSetOf<String>()

        // 1. Keep valid embedded captions from stream
        for (cap in streamResult.captions) {
            if (cap.url.isNotBlank() && seenUrls.add(cap.url)) {
                val normLang = SubtitleResolver.normalizeLangCode(cap.language)
                val normLabelLang = SubtitleResolver.normalizeLangCode(cap.label)
                val displayFromLabel = SubtitleResolver.getLanguageDisplayName(normLabelLang)
                val displayFromLang = SubtitleResolver.getLanguageDisplayName(normLang)
                
                val finalLabel = when {
                    SubtitleResolver.LANG_NAMES.containsKey(cap.label.lowercase().trim()) -> displayFromLabel
                    cap.label.length <= 3 -> displayFromLang
                    cap.label.isNotBlank() -> cap.label
                    else -> displayFromLang
                }
                val finalLang = if (normLang.length > 3 && normLabelLang.length <= 3) normLabelLang else normLang
                combinedCaptions.add(cap.copy(label = finalLabel, language = finalLang))
            }
        }

        // 2. Add all external subtitles (Wyzie, OpenSubtitles Stremio, etc.)
        for (sub in externalSubs) {
            if (sub.url.isNotBlank() && seenUrls.add(sub.url)) {
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

        // 3. Disambiguate duplicate labels across embedded and external captions
        val labelCounts = mutableMapOf<String, Int>()
        val disambiguated = mutableListOf<CinejoyCaption>()
        for (cap in combinedCaptions) {
            val base = cap.label.trim()
            val count = labelCounts.getOrDefault(base, 0) + 1
            labelCounts[base] = count
            val finalLabel = if (count > 1) {
                if (base.contains("#")) "$base-$count" else "$base #$count"
            } else {
                base
            }
            disambiguated.add(cap.copy(label = finalLabel))
        }

        // 4. Sort: English first (Standard then CC), then alphabetical by label
        disambiguated.sortWith(
            compareBy<CinejoyCaption> {
                val isEn = it.language.startsWith("en", ignoreCase = true) || it.label.contains("English", ignoreCase = true)
                if (!isEn) 2
                else if (it.label.contains("[CC]", ignoreCase = true)) 1
                else 0
            }.thenBy { it.label }
        )

        return streamResult.copy(captions = disambiguated)
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

    fun getPlaybackProgress(
        profileId: String,
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null
    ): PlaybackProgress? {
        return progressStore.getProgress(profileId, mediaType, tmdbId, season, episode)
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

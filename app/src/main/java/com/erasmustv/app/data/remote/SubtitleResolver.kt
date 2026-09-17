package com.erasmustv.app.data.remote

import android.util.Log
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.data.model.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SubtitleResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    companion object {
        private const val TAG = "SubtitleResolver"
        private val LANG_NAMES = mapOf(
            "en" to "English",
            "eng" to "English",
            "es" to "Spanish",
            "spa" to "Spanish",
            "fr" to "French",
            "fre" to "French",
            "fra" to "French",
            "de" to "German",
            "deu" to "German",
            "ger" to "German",
            "it" to "Italian",
            "ita" to "Italian",
            "pt" to "Portuguese",
            "por" to "Portuguese",
            "pob" to "Portuguese (BR)",
            "ru" to "Russian",
            "rus" to "Russian",
            "ja" to "Japanese",
            "jpn" to "Japanese",
            "ko" to "Korean",
            "kor" to "Korean",
            "zh" to "Chinese",
            "chi" to "Chinese",
            "zho" to "Chinese",
            "ar" to "Arabic",
            "ara" to "Arabic",
            "hi" to "Hindi",
            "hin" to "Hindi",
            "tr" to "Turkish",
            "tur" to "Turkish",
            "nl" to "Dutch",
            "dut" to "Dutch",
            "nld" to "Dutch",
            "pl" to "Polish",
            "pol" to "Polish",
            "sv" to "Swedish",
            "swe" to "Swedish",
            "el" to "Greek",
            "ell" to "Greek",
            "gre" to "Greek"
        )
    }

    private val imdbIdCache = ConcurrentHashMap<String, String>()

    suspend fun resolveImdbId(mediaType: String, tmdbId: String): String? = withContext(Dispatchers.IO) {
        val cacheKey = "$mediaType:$tmdbId"
        imdbIdCache[cacheKey]?.let { return@withContext it }

        try {
            val endpoint = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
            val url = "${AppConfig.TMDB_BASE_URL}$endpoint/$tmdbId/external_ids?api_key=${AppConfig.TMDB_API_KEY}"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val id = json.optString("imdb_id").takeIf { it.isNotBlank() && it != "null" }
                        if (id != null) {
                            imdbIdCache[cacheKey] = id
                        }
                        return@withContext id
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve IMDb ID for $mediaType/$tmdbId: ${e.message}")
        }
        null
    }

    private fun fetchWyzie(
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?
    ): List<SubtitleTrack> {
        val results = mutableListOf<SubtitleTrack>()
        try {
            val isTv = mediaType.equals("tv", ignoreCase = true)
            val urlBuilder = StringBuilder("https://vidfast.vc/wyzie?id=").append(tmdbId)
            if (isTv) {
                urlBuilder.append("&season=").append(season ?: 1)
                urlBuilder.append("&episode=").append(episode ?: 1)
            }
            val req = Request.Builder()
                .url(urlBuilder.toString())
                .header("Referer", "https://vidfast.vc/")
                .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string()
                    if (!body.isNullOrBlank()) {
                        val arr = JSONArray(body)
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val subUrl = item.optString("url")
                            if (subUrl.isNotBlank()) {
                                val lang = item.optString("language").lowercase()
                                val display = item.optString("display").takeIf { it.isNotBlank() }
                                    ?: LANG_NAMES[lang] ?: lang.uppercase()
                                val mime = when {
                                    subUrl.contains(".vtt", ignoreCase = true) -> "text/vtt"
                                    subUrl.contains(".ass", ignoreCase = true) || subUrl.contains(".ssa", ignoreCase = true) || subUrl.contains("wyzie", ignoreCase = true) -> "text/x-ssa"
                                    else -> "application/x-subrip"
                                }
                                results.add(SubtitleTrack(label = display, language = lang, url = subUrl, mimeType = mime))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vidfast Wyzie fetch failed: ${e.message}")
        }
        return results
    }

    private fun fetchStremio(
        imdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?
    ): List<SubtitleTrack> {
        val results = mutableListOf<SubtitleTrack>()
        try {
            val isTv = mediaType.equals("tv", ignoreCase = true)
            val path = if (isTv) {
                "series/$imdbId:${season ?: 1}:${episode ?: 1}"
            } else {
                "movie/$imdbId"
            }
            val req = Request.Builder()
                .url("https://opensubtitles-v3.strem.io/subtitles/$path.json")
                .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val subs = json.optJSONArray("subtitles")
                        if (subs != null) {
                            for (i in 0 until subs.length()) {
                                val item = subs.optJSONObject(i) ?: continue
                                val subUrl = item.optString("url")
                                if (subUrl.isNotBlank()) {
                                    val lang = item.optString("lang").lowercase()
                                    val fileName = item.optString("subtitleFileName")
                                    val isSdh = fileName.contains("SDH", ignoreCase = true) ||
                                            fileName.contains(".HI.", ignoreCase = true) ||
                                            fileName.contains("hearing", ignoreCase = true) ||
                                            fileName.contains("[CC]", ignoreCase = true)
                                    val baseDisplay = LANG_NAMES[lang] ?: lang.uppercase()
                                    val display = if (isSdh) "$baseDisplay [CC]" else baseDisplay
                                    val mime = when {
                                        fileName.endsWith(".vtt", ignoreCase = true) || subUrl.contains(".vtt", ignoreCase = true) -> "text/vtt"
                                        fileName.endsWith(".ass", ignoreCase = true) || fileName.endsWith(".ssa", ignoreCase = true) ||
                                                subUrl.contains(".ass", ignoreCase = true) || subUrl.contains(".ssa", ignoreCase = true) -> "text/x-ssa"
                                        else -> "application/x-subrip"
                                    }
                                    results.add(SubtitleTrack(label = display, language = lang, url = subUrl, mimeType = mime))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Stremio OpenSubtitles fetch failed: ${e.message}")
        }
        return results
    }

    suspend fun getSubtitles(
        tmdbId: String,
        mediaType: String,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        val resolvedImdb = imdbId?.takeIf { it.isNotBlank() } ?: resolveImdbId(mediaType, tmdbId)

        // Query both Wyzie and Stremio concurrently for sub-second responses
        val wyzieDeferred = async { fetchWyzie(tmdbId, mediaType, season, episode) }
        val stremioDeferred = async {
            if (!resolvedImdb.isNullOrBlank()) {
                fetchStremio(resolvedImdb, mediaType, season, episode)
            } else {
                emptyList()
            }
        }

        var allRaw = wyzieDeferred.await() + stremioDeferred.await()

        // Fallback for multi-cour anime where external subtitles are cataloged under Season 2 (e.g., Solo Leveling S1E13..25 -> S2E1..13)
        if (allRaw.isEmpty() && mediaType.equals("tv", ignoreCase = true) && season == 1 && episode != null && episode > 12) {
            val fbSeason = 2
            val fbEpisode = episode - 12
            val wyzieFb = async { fetchWyzie(tmdbId, mediaType, fbSeason, fbEpisode) }
            val stremioFb = async {
                if (!resolvedImdb.isNullOrBlank()) {
                    fetchStremio(resolvedImdb, mediaType, fbSeason, fbEpisode)
                } else {
                    emptyList()
                }
            }
            allRaw = wyzieFb.await() + stremioFb.await()
        }

        // Deduplicate and rank tracks cleanly
        val seenUrls = mutableSetOf<String>()
        val langCounts = mutableMapOf<String, Int>()
        val filtered = mutableListOf<SubtitleTrack>()

        for (sub in allRaw) {
            if (!seenUrls.add(sub.url)) continue
            val normLang = when (sub.language.lowercase()) {
                "eng" -> "en"
                "spa" -> "es"
                "fre", "fra" -> "fr"
                "ger", "deu" -> "de"
                "ita" -> "it"
                "por", "pob" -> "pt"
                "rus" -> "ru"
                "jpn" -> "ja"
                "kor" -> "ko"
                "chi", "zho" -> "zh"
                "hin" -> "hi"
                "ara" -> "ar"
                "tur" -> "tr"
                "dut", "nld" -> "nl"
                "pol" -> "pl"
                "swe" -> "sv"
                "ell", "gre" -> "el"
                else -> sub.language.lowercase()
            }

            // Keep up to 2 distinct tracks for English (e.g. Standard and CC), and 1 best track for other languages
            val maxAllowed = if (normLang == "en") 2 else 1
            val count = langCounts.getOrDefault(normLang, 0)
            if (count < maxAllowed) {
                langCounts[normLang] = count + 1
                filtered.add(sub)
            }
        }

        // Sort: English first, then alphabetical by label
        filtered.sortedWith(compareBy<SubtitleTrack> {
            if (it.language.startsWith("en", ignoreCase = true)) 0 else 1
        }.thenBy { it.label })
    }
}

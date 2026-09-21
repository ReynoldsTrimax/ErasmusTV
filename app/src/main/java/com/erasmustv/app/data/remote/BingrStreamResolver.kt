package com.erasmustv.app.data.remote

import android.net.Uri
import android.util.Log
import com.erasmustv.app.data.model.CinejoyCaption
import com.erasmustv.app.data.model.DirectServer
import com.erasmustv.app.data.model.DirectStreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class BingrStreamResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "BingrResolver"
        private const val BINGR_API_BASE = "https://api.bingr.one/api"
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val SERVER_MAP = mapOf(
            "aphelion" to Pair("s40", "Aphelion"),
            "bastion" to Pair("s62", "Bastion"),
            "orion" to Pair("s31", "Orion"),
            "nova" to Pair("s30", "Nova"),
            "edmunds" to Pair("s3", "Edmunds"),
            "hallyu" to Pair("s63", "Hallyu"),
            "polaris" to Pair("s70", "Polaris"),
            "animesalt" to Pair("animesalt", "AnimeSalt"),
            "ryuu" to Pair("ryuu", "Ryuu")
        )
    }

    @Volatile
    private var cooldownUntil = 0L

    fun getCooldownUntil(): Long = cooldownUntil

    fun setCooldown(ms: Long) {
        cooldownUntil = ms
    }

    fun isBingrServer(serverId: String): Boolean {
        val s = serverId.lowercase()
        return SERVER_MAP.containsKey(s) || s == "animesalt" || s == "ryuu"
    }

    fun unwrapUrl(rawUrl: String): String {
        return try {
            if (rawUrl.contains("manifest?url=") || rawUrl.contains("proxy/m3u8?url=")) {
                val inner = try {
                    Uri.parse(rawUrl).getQueryParameter("url")
                } catch (_: Throwable) {
                    null
                } ?: run {
                    val idx = rawUrl.indexOf("url=")
                    if (idx != -1) {
                        val encoded = rawUrl.substring(idx + 4).split("&")[0]
                        URLDecoder.decode(encoded, "UTF-8")
                    } else null
                }
                if (!inner.isNullOrBlank() && inner.startsWith("http")) inner else rawUrl
            } else {
                rawUrl
            }
        } catch (_: Exception) {
            rawUrl
        }
    }

    suspend fun resolveBingrStream(
        mediaType: String,
        tmdbId: String,
        title: String,
        season: Int? = 1,
        episode: Int? = 1,
        year: String? = null,
        imdbId: String? = null,
        preferredServer: String = "aphelion"
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() < cooldownUntil) {
            Log.w(TAG, "Bingr resolver currently in rate-limit cooldown until $cooldownUntil")
            return@withContext null
        }

        val targetKey = preferredServer.lowercase()
        val candidates = mutableListOf<String>()
        if (SERVER_MAP.containsKey(targetKey)) candidates.add(targetKey)
        listOf("bastion", "aphelion", "polaris", "orion", "nova", "edmunds", "hallyu").forEach {
            if (!candidates.contains(it)) candidates.add(it)
        }



        val isTv = mediaType.equals("tv", ignoreCase = true)
        val s = season ?: 1
        val e = episode ?: 1

        for (srvKey in candidates.take(3)) {
            val srvPair = SERVER_MAP[srvKey] ?: continue
            val srvCode = srvPair.first
            val srvName = srvPair.second

            try {
                var responseBody: String? = null

                // 1. Fast path for Aphelion TV
                if (srvCode == "s40" && isTv) {
                    val fastUrl = "$BINGR_API_BASE/stream/aphelion-tv/$tmdbId/$s/$e"
                    val req = Request.Builder()
                        .url(fastUrl)
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", "https://bingr.one/")
                        .header("Origin", "https://bingr.one")
                        .header("Accept", "application/json")
                        .get()
                        .build()

                    client.newCall(req).execute().use { res ->
                        if (res.code == 429) {
                            Log.w(TAG, "Bingr rate limited (429) on fast path. Setting 25s cooldown.")
                            cooldownUntil = System.currentTimeMillis() + 25_000L
                            return@withContext null
                        }
                        if (res.isSuccessful) {
                            responseBody = res.body?.string()
                        }
                    }
                }

                // 2. Universal POST fallback
                if (responseBody.isNullOrBlank()) {
                    val queryObj = JSONObject().apply {
                        put("title", title)
                        put("year", year ?: "")
                        put("imdbId", imdbId ?: "")
                        if (isTv) {
                            put("season", s.toString())
                            put("episode", e.toString())
                        }
                    }
                    val postObj = JSONObject().apply {
                        put("srv", srvCode)
                        put("t", if (isTv) "tv" else "movie")
                        put("id", tmdbId)
                        if (!imdbId.isNullOrBlank()) put("imdbId", imdbId)
                        put("query", queryObj)
                    }

                    val postReq = Request.Builder()
                        .url("$BINGR_API_BASE/stream")
                        .header("Content-Type", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", "https://bingr.one/")
                        .header("Origin", "https://bingr.one")
                        .header("Accept", "application/json")
                        .post(postObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    client.newCall(postReq).execute().use { res ->
                        if (res.code == 429) {
                            Log.w(TAG, "Bingr rate limited (429) on POST /stream. Setting 25s cooldown.")
                            cooldownUntil = System.currentTimeMillis() + 25_000L
                            return@withContext null
                        }
                        if (res.isSuccessful) {
                            responseBody = res.body?.string()
                        }
                    }
                }

                if (responseBody.isNullOrBlank()) continue
                val json = JSONObject(responseBody!!)
                val sources = json.optJSONArray("sources") ?: continue
                if (sources.length() == 0) continue

                // Find valid playable video sources, rejecting storyboard manifests (e.g. img.rousav.tech / tiles.m3u8)
                val validSources = mutableListOf<Pair<JSONObject, String>>()
                for (sIdx in 0 until sources.length()) {
                    val src = sources.optJSONObject(sIdx) ?: continue
                    val u = src.optString("url")
                    if (u.isBlank() || u.contains("img.rousav.tech") || u.contains("tiles.m3u8") ||
                        u.endsWith(".jpg", ignoreCase = true) || u.endsWith(".png", ignoreCase = true) || u.endsWith(".webp", ignoreCase = true)) {
                        continue
                    }
                    validSources.add(Pair(src, u))
                }

                if (validSources.isEmpty()) {
                    Log.w(TAG, "No valid video sources found for server $srvCode, cascading to next candidate")
                    continue
                }

                val (source, rawUrl) = validSources.first()
                val streamUrl = unwrapUrl(rawUrl)
                if (streamUrl.isBlank() || streamUrl.contains("img.rousav.tech") || streamUrl.contains("tiles.m3u8")) {
                    Log.w(TAG, "Unwrapped stream URL invalid from $srvCode ($streamUrl), cascading to next candidate")
                    continue
                }

                val isMp4 = source.optBoolean("isMP4", false) ||
                            source.optString("type") == "video/mp4" ||
                            streamUrl.contains(".mp4", ignoreCase = true)

                val headersObj = source.optJSONObject("headers")
                val referer = headersObj?.optString("Referer")?.takeIf { it.isNotBlank() }
                    ?: headersObj?.optString("referer")?.takeIf { it.isNotBlank() }
                    ?: "https://bingr.one/"

                val captions = mutableListOf<CinejoyCaption>()
                val subArray = json.optJSONArray("subtitles")
                if (subArray != null) {
                    for (i in 0 until subArray.length()) {
                        val subObj = subArray.getJSONObject(i)
                        val subUrl = subObj.optString("url")
                        if (subUrl.isNotBlank()) {
                            val lang = subObj.optString("lang", "en").ifBlank { "en" }
                            val label = subObj.optString("label", lang.uppercase()).ifBlank { lang.uppercase() }
                            captions.add(
                                CinejoyCaption(
                                    label = label,
                                    language = lang,
                                    url = subUrl,
                                    mimeType = "text/vtt"
                                )
                            )
                        }
                    }
                }

                val directServers = mutableListOf<DirectServer>()
                directServers.add(
                    DirectServer(
                        name = srvName,
                        url = streamUrl,
                        kind = if (isMp4) "file" else "hls"
                    )
                )

                // Attach additional source variants if provided by Bingr cluster
                for (vIdx in 1 until validSources.size) {
                    val (extraSrc, extraRaw) = validSources[vIdx]
                    val extraUrl = unwrapUrl(extraRaw)
                    if (extraUrl.isNotBlank() && extraUrl != streamUrl && !extraUrl.contains("tiles.m3u8") && !extraUrl.contains("img.rousav.tech")) {
                        val extraQuality = extraSrc.optString("quality")
                        val extraName = if (extraQuality.isNotBlank()) "$srvName ($extraQuality)" else "$srvName Mirror"
                        val extraMp4 = extraSrc.optBoolean("isMP4", false) || extraUrl.contains(".mp4", ignoreCase = true)
                        directServers.add(
                            DirectServer(
                                name = extraName,
                                url = extraUrl,
                                kind = if (extraMp4) "file" else "hls"
                            )
                        )
                    }
                }

                return@withContext DirectStreamResult(
                    ok = true,
                    referer = referer,
                    captions = captions,
                    servers = directServers
                )
            } catch (ex: Exception) {
                Log.w(TAG, "Bingr server $srvKey failed: ${ex.message}")
            }
        }
        null
    }
}

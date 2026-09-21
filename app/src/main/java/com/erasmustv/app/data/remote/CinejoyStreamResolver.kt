package com.erasmustv.app.data.remote

import android.util.Log
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.util.DeviceCodecCapability
import com.erasmustv.app.data.model.CinejoyCaption
import com.erasmustv.app.data.model.DirectServer
import com.erasmustv.app.data.model.DirectStreamResult
import com.erasmustv.app.data.model.STREAM_SERVERS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class CinejoyStreamResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "CinejoyResolver"
        private const val ENC_API = "https://enc-dec.app/api"
        private const val SHEGU = "https://api.shegu.st"
        const val CINEJOY_REFERER = "https://cinejoy.to/"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val OCTET_MEDIA_TYPE = "application/octet-stream".toMediaType()

        fun b64Decode(data: String): ByteArray {
            val pad = "=".repeat((4 - (data.length % 4)) % 4)
            val normalized = data.replace('-', '+').replace('_', '/') + pad
            return try {
                java.util.Base64.getDecoder().decode(normalized)
            } catch (_: Throwable) {
                android.util.Base64.decode(normalized, android.util.Base64.DEFAULT)
            }
        }

        fun b64UrlEncode(bytes: ByteArray): String {
            return try {
                java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            } catch (_: Throwable) {
                android.util.Base64.encodeToString(
                    bytes,
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                ).trim()
            }
        }
    }

    private val cache = ConcurrentHashMap<String, CachedResult>()
    private val CACHE_MS = 10 * 60 * 1000L // 10 minutes

    fun clearCache() {
        cache.clear()
    }

    private data class CachedResult(
        val timestamp: Long,
        val result: DirectStreamResult
    )

    fun sheguServerName(serverId: String): String {
        return STREAM_SERVERS.find { it.id.equals(serverId, ignoreCase = true) }?.name ?: "Lisbon"
    }

    suspend fun resolveStream(
        mediaType: String,
        tmdbId: String,
        title: String,
        season: Int? = null,
        episode: Int? = null,
        year: String? = null,
        imdbId: String? = null,
        preferredServer: String = "lisbon"
    ): DirectStreamResult = withContext(Dispatchers.IO) {
        val isTv = mediaType.equals("tv", ignoreCase = true)
        val s = if (isTv) season ?: 1 else null
        val e = if (isTv) episode ?: 1 else null

        val isHevc10 = DeviceCodecCapability.isHevcMain10Supported()
        val cacheKey = "$preferredServer:$mediaType:$tmdbId:$s:$e:hevc10=$isHevc10"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_MS) {
            return@withContext cached.result
        }

        val legacyCinejoyServers = listOf("lisbon", "sakura", "nebula", "solara", "athens", "joy", "castle", "canaias")
        // Preferred server first, then cluster fallback sequence
        val serverOrder = buildList {
            add(preferredServer)
            STREAM_SERVERS.map { it.id }
                .filter { legacyCinejoyServers.contains(it.lowercase()) && !it.equals(preferredServer, ignoreCase = true) }
                .forEach {
                    add(it)
                }
        }

        for (serverId in serverOrder) {
            try {
                val hit = resolveSingleServer(
                    mediaType = mediaType,
                    isTv = isTv,
                    tmdbId = tmdbId,
                    title = title,
                    serverId = serverId,
                    season = s,
                    episode = e,
                    year = year,
                    imdbId = imdbId
                )
                if (hit != null && hit.ok && hit.servers.isNotEmpty()) {
                    cache[cacheKey] = CachedResult(System.currentTimeMillis(), hit)
                    return@withContext hit
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Server $serverId failed for $title (TMDB $tmdbId): ${ex.message}")
            }

            // Fast-path split-cour fallback: If preferredServer failed on s=1 and e > 12 (e.g. Solo Leveling Ep 14-25),
            // immediately attempt Season 2 mapping on preferredServer to avoid 7 server timeouts
            if (serverId.equals(preferredServer, ignoreCase = true) && isTv && s == 1 && e != null && e > 12) {
                val fbSeason = 2
                val fbEpisode = e - 12
                try {
                    val fbHit = resolveSingleServer(
                        mediaType = mediaType,
                        isTv = isTv,
                        tmdbId = tmdbId,
                        title = title,
                        serverId = preferredServer,
                        season = fbSeason,
                        episode = fbEpisode,
                        year = year,
                        imdbId = imdbId
                    )
                    if (fbHit != null && fbHit.ok && fbHit.servers.isNotEmpty()) {
                        cache[cacheKey] = CachedResult(System.currentTimeMillis(), fbHit)
                        return@withContext fbHit
                    }
                } catch (ex: Exception) {
                    Log.w(TAG, "Fast fallback S$fbSeason E$fbEpisode failed on $preferredServer: ${ex.message}")
                }
            }
        }

        // Secondary cluster-wide fallback for multi-cour anime where TMDB merges cours into Season 1 (e.g., Solo Leveling 13-25)
        // while upstream providers index Cour 2 as Season 2 (e.g., S1E13 -> S2E1, S1E25 -> S2E13).
        if (isTv && s == 1 && e != null && e > 12) {
            val fallbackMappings = listOf(
                Pair(2, e - 12),
                Pair(2, e - 13)
            )
            for ((fbSeason, fbEpisode) in fallbackMappings) {
                if (fbEpisode <= 0) continue
                Log.i(TAG, "Attempting split-cour fallback for $title: S$s E$e -> S$fbSeason E$fbEpisode across cluster")
                for (serverId in serverOrder) {
                    try {
                        val hit = resolveSingleServer(
                            mediaType = mediaType,
                            isTv = isTv,
                            tmdbId = tmdbId,
                            title = title,
                            serverId = serverId,
                            season = fbSeason,
                            episode = fbEpisode,
                            year = year,
                            imdbId = imdbId
                        )
                        if (hit != null && hit.ok && hit.servers.isNotEmpty()) {
                            cache[cacheKey] = CachedResult(System.currentTimeMillis(), hit)
                            return@withContext hit
                        }
                    } catch (ex: Exception) {
                        Log.w(TAG, "Fallback S$fbSeason E$fbEpisode server $serverId failed: ${ex.message}")
                    }
                }
            }
        }

        // Reverse fallback: If requested Season 2 Episode X, but upstream merged all into Season 1
        if (isTv && s != null && s > 1 && e != null) {
            val fbEpisode = e + 12
            for (serverId in serverOrder) {
                try {
                    val hit = resolveSingleServer(
                        mediaType = mediaType,
                        isTv = isTv,
                        tmdbId = tmdbId,
                        title = title,
                        serverId = serverId,
                        season = 1,
                        episode = fbEpisode,
                        year = year,
                        imdbId = imdbId
                    )
                    if (hit != null && hit.ok && hit.servers.isNotEmpty()) {
                        cache[cacheKey] = CachedResult(System.currentTimeMillis(), hit)
                        return@withContext hit
                    }
                } catch (_: Exception) {}
            }
        }

        DirectStreamResult(
            ok = false,
            error = "Could not resolve stream for $title across any cluster servers",
            servers = emptyList()
        )
    }

    private fun resolveSingleServer(
        mediaType: String,
        isTv: Boolean,
        tmdbId: String,
        title: String,
        serverId: String,
        season: Int?,
        episode: Int?,
        year: String?,
        imdbId: String?
    ): DirectStreamResult? {
        val serverName = sheguServerName(serverId)

        val hasVidfastOffset = isTv && tmdbId == "66732"

        // Cluster priority routing
        when (serverId.lowercase()) {
            "lisbon" -> {
                if (hasVidfastOffset) {
                    Log.i(TAG, "Bypassing Vidfast for $title due to known upstream episode index offset. Routing to verified cluster mirrors.")
                    val vidlinkHit = resolveVidlink(mediaType, tmdbId, season, episode, serverName = serverName)
                    if (vidlinkHit != null && vidlinkHit.ok && vidlinkHit.servers.isNotEmpty()) {
                        val vidloveHit = resolveVidlove(mediaType, tmdbId, season, episode, serverName = "$serverName (Cloud)")
                        val merged = vidlinkHit.servers + (vidloveHit?.servers ?: emptyList())
                        return vidlinkHit.copy(servers = merged)
                    }
                    val vidloveHit = resolveVidlove(mediaType, tmdbId, season, episode, serverName = serverName)
                    if (vidloveHit != null && vidloveHit.ok && vidloveHit.servers.isNotEmpty()) {
                        return vidloveHit
                    }
                }

                // Lisbon: 4K / HLS flagship (4K Master adaptive ladder primary with 4K UHD Direct fallback)
                val vidfastHit = resolveVidfast(mediaType, tmdbId, season, episode, serverName, preferredSubServer = "4k_master")
                val isHevc10 = DeviceCodecCapability.isHevcMain10Supported()
                val primaryUrl = vidfastHit?.servers?.firstOrNull()?.url.orEmpty()
                val isOnlyRawCdn1 = primaryUrl.contains("/r2/cdn1/") && vidfastHit?.servers?.none { it.url.contains("/vd/") || it.url.contains("/r2/cdn2/") } == true

                if (vidfastHit != null && vidfastHit.ok && !isOnlyRawCdn1) {
                    return vidfastHit
                }

                // If vidfast returned only raw cdn1 (single-variant 10-bit HEVC) or null, query mirrors for resilient fallback
                val vidlinkHit = resolveVidlink(mediaType, tmdbId, season, episode, serverName = "$serverName (Mirror)")
                val vidloveHit = resolveVidlove(mediaType, tmdbId, season, episode, serverName = "$serverName (Cloud)")

                if (!isHevc10 && isOnlyRawCdn1) {
                    // Device cannot decode HEVC Main 10! Elevate Vidlink/Vidlove over unplayable r2/cdn1
                    if (vidlinkHit != null && vidlinkHit.ok && vidlinkHit.servers.isNotEmpty()) {
                        val merged = vidlinkHit.servers + (vidloveHit?.servers ?: emptyList()) + (vidfastHit?.servers ?: emptyList())
                        return vidlinkHit.copy(servers = merged)
                    }
                    if (vidloveHit != null && vidloveHit.ok && vidloveHit.servers.isNotEmpty()) {
                        val merged = vidloveHit.servers + (vidfastHit?.servers ?: emptyList())
                        return vidloveHit.copy(servers = merged)
                    }
                }

                if (vidfastHit != null && vidfastHit.ok && vidfastHit.servers.isNotEmpty()) {
                    val fallbacks = (vidlinkHit?.servers ?: emptyList()) + (vidloveHit?.servers ?: emptyList())
                    return vidfastHit.copy(servers = vidfastHit.servers + fallbacks)
                }

                vidloveHit?.let { return it }
                vidlinkHit?.let { return it }
            }
            "athens" -> {
                if (hasVidfastOffset) {
                    val vidlinkHit = resolveVidlink(mediaType, tmdbId, season, episode, serverName = serverName)
                    if (vidlinkHit != null && vidlinkHit.ok && vidlinkHit.servers.isNotEmpty()) {
                        val vidloveHit = resolveVidlove(mediaType, tmdbId, season, episode, serverName = "$serverName (Cloud)")
                        return vidlinkHit.copy(servers = vidlinkHit.servers + (vidloveHit?.servers ?: emptyList()))
                    }
                    val vidloveHit = resolveVidlove(mediaType, tmdbId, season, episode, serverName = serverName)
                    if (vidloveHit != null && vidloveHit.ok && vidloveHit.servers.isNotEmpty()) {
                        return vidloveHit
                    }
                }

                // Athens: 4K Cinema Mirror (4K UHD Direct primary with 4K Master ladder fallback)
                val vidfastHit = resolveVidfast(mediaType, tmdbId, season, episode, serverName, preferredSubServer = "4k_direct")
                val isHevc10 = DeviceCodecCapability.isHevcMain10Supported()
                val primaryUrl = vidfastHit?.servers?.firstOrNull()?.url.orEmpty()
                val isOnlyRawCdn1 = primaryUrl.contains("/r2/cdn1/") && vidfastHit?.servers?.none { it.url.contains("/vd/") || it.url.contains("/r2/cdn2/") } == true

                if (vidfastHit != null && vidfastHit.ok && !isOnlyRawCdn1) {
                    return vidfastHit
                }

                val vidlinkHit = resolveVidlink(mediaType, tmdbId, season, episode, serverName = "$serverName (Mirror)")
                val vidloveHit = resolveVidlove(mediaType, tmdbId, season, episode, serverName = "$serverName (Cloud)")

                if (!isHevc10 && isOnlyRawCdn1) {
                    if (vidlinkHit != null && vidlinkHit.ok && vidlinkHit.servers.isNotEmpty()) {
                        val merged = vidlinkHit.servers + (vidloveHit?.servers ?: emptyList()) + (vidfastHit?.servers ?: emptyList())
                        return vidlinkHit.copy(servers = merged)
                    }
                    if (vidloveHit != null && vidloveHit.ok && vidloveHit.servers.isNotEmpty()) {
                        val merged = vidloveHit.servers + (vidfastHit?.servers ?: emptyList())
                        return vidloveHit.copy(servers = merged)
                    }
                }

                if (vidfastHit != null && vidfastHit.ok && vidfastHit.servers.isNotEmpty()) {
                    val fallbacks = (vidlinkHit?.servers ?: emptyList()) + (vidloveHit?.servers ?: emptyList())
                    return vidfastHit.copy(servers = vidfastHit.servers + fallbacks)
                }

                vidloveHit?.let { return it }
                vidlinkHit?.let { return it }
            }
            "nebula" -> {
                // Nebula: High-speed US edge CDN (Vidlink direct MP4, fallback to Vidlove and Vidfast 1080p/4K)
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidfast(mediaType, tmdbId, season, episode, serverName, preferredSubServer = "vFast")?.let { return it }
            }
            "solara" -> {
                // Solara: Full-library universal cloud player (Vidlove direct, fallback to Vidfast 4K)
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidfast(mediaType, tmdbId, season, episode, serverName, preferredSubServer = "vRapid")?.let { return it }
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
            }
            "sakura" -> {
                // Sakura: Dedicated Anime & Asian media cluster with dual/multi-audio
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidfast(mediaType, tmdbId, season, episode, serverName, preferredSubServer = "vFast")?.let { return it }
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
            }
            "joy" -> {
                // Joy: Direct video cloud stream (Vidlove VidAPI multi-audio stream)
                resolveVidlove(mediaType, tmdbId, season, episode, serverName, source = "vidapi")?.let { return it }
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidfast(mediaType, tmdbId, season, episode, serverName, preferredSubServer = "vFast")?.let { return it }
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
            }
            "castle" -> {
                // Castle: Alternate media mirror
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidfast(mediaType, tmdbId, season, episode, serverName, preferredSubServer = "vRapid")?.let { return it }
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
            }
            "canaias" -> {
                // Canaias: Global low-latency edge mirror
                if (!hasVidfastOffset) {
                    resolveVidfast(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                }
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
            }
            else -> {
                if (!hasVidfastOffset) {
                    resolveVidfast(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                }
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
            }
        }

        // Secondary fallback: Shegu / Cinejoy mirror
        return resolveShegu(
            isTv = isTv,
            tmdbId = tmdbId,
            title = title,
            serverName = serverName,
            season = season,
            episode = episode,
            year = year,
            imdbId = imdbId
        )
    }

    fun resolveVidlove(
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null,
        serverName: String = "Sakura",
        source: String? = null
    ): DirectStreamResult? {
        try {
            val isTv = mediaType.equals("tv", ignoreCase = true)
            val base = if (isTv) {
                val s = season ?: 1
                val e = episode ?: 1
                "https://api.vidlove.cc/tv?id=$tmdbId&season=$s&episode=$e&mode=json"
            } else {
                "https://api.vidlove.cc/movie?id=$tmdbId&mode=json"
            }
            val url = if (!source.isNullOrBlank()) "$base&sources=$source" else base

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                .header("Referer", "https://player.vidlove.cc/")
                .header("Accept", "application/json")
                .get()
                .build()

            val body = client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                res.body?.string() ?: return null
            }
            if (body.isBlank() || body.trim() == "null" || !body.trim().startsWith("{")) return null

            val json = JSONObject(body)
            val sourceObj = json.optJSONObject("source") ?: return null
            val streamUrl = sourceObj.optString("url").takeIf { it.isNotBlank() } ?: return null

            val captions = mutableListOf<CinejoyCaption>()
            val subtitlesArray = json.optJSONArray("subtitles")
            if (subtitlesArray != null) {
                for (i in 0 until subtitlesArray.length()) {
                    val sub = subtitlesArray.optJSONObject(i) ?: continue
                    val file = sub.optString("file").takeIf { it.isNotBlank() }
                        ?: sub.optString("url").takeIf { it.isNotBlank() }
                        ?: continue
                    val label = sub.optString("label").takeIf { it.isNotBlank() }
                        ?: sub.optString("display").takeIf { it.isNotBlank() }
                        ?: "Subtitle"
                    val lang = sub.optString("language").takeIf { it.isNotBlank() }
                        ?: label.lowercase().take(2)
                    captions.add(CinejoyCaption(label = label, language = lang, url = file, mimeType = "text/vtt"))
                }
            }

            return DirectStreamResult(
                ok = true,
                referer = "https://player.vidlove.cc/",
                captions = captions,
                servers = listOf(
                    DirectServer(
                        name = serverName,
                        url = streamUrl,
                        kind = if (streamUrl.contains(".mp4", ignoreCase = true)) "file" else "hls"
                    )
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Vidlove resolution failed for $tmdbId: ${e.message}")
            return null
        }
    }

    fun resolveVidlink(
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null,
        serverName: String = "Nebula"
    ): DirectStreamResult? {
        try {
            val encUrl = "$ENC_API/enc-vidlink?text=$tmdbId"
            val encReq = Request.Builder()
                .url(encUrl)
                .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                .get()
                .build()

            val encKey = client.newCall(encReq).execute().use { res ->
                if (!res.isSuccessful) return null
                val body = res.body?.string() ?: return null
                if (body.isBlank() || !body.trim().startsWith("{")) return null
                val json = JSONObject(body)
                if (json.optInt("status") != 200) return null
                json.optString("result").takeIf { it.isNotBlank() }
            } ?: return null

            val isTv = mediaType.equals("tv", ignoreCase = true)
            val apiUrl = if (isTv) {
                val s = season ?: 1
                val e = episode ?: 1
                "https://vidlink.pro/api/b/tv/$encKey/$s/$e"
            } else {
                "https://vidlink.pro/api/b/movie/$encKey"
            }

            val vidlinkReq = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                .header("Origin", "https://vidlink.pro")
                .header("Referer", "https://vidlink.pro/")
                .get()
                .build()

            val body = client.newCall(vidlinkReq).execute().use { res ->
                if (!res.isSuccessful) return null
                res.body?.string() ?: return null
            }

            if (body.isBlank() || body.trim() == "null" || !body.trim().startsWith("{")) return null

            val json = JSONObject(body)
            val streamObj = json.optJSONObject("stream") ?: return null

            val qualitiesObj = streamObj.optJSONObject("qualities")
            val playlist = streamObj.optString("playlist").takeIf { it.isNotBlank() }

            val streamUrl = (qualitiesObj?.optJSONObject("2160")
                ?: qualitiesObj?.optJSONObject("1080")
                ?: qualitiesObj?.optJSONObject("720")
                ?: qualitiesObj?.optJSONObject("480")
                ?: qualitiesObj?.optJSONObject("360"))?.optString("url")?.takeIf { it.isNotBlank() }
                ?: playlist
                ?: return null

            val kind = if (streamUrl.contains(".mp4", ignoreCase = true)) "file" else "hls"

            val captions = mutableListOf<CinejoyCaption>()
            val captionsArray = streamObj.optJSONArray("captions")
                ?: json.optJSONArray("tracks")
                ?: json.optJSONArray("captions")

            if (captionsArray != null) {
                for (i in 0 until captionsArray.length()) {
                    val cap = captionsArray.optJSONObject(i) ?: continue
                    val capUrl = cap.optString("url").takeIf { it.isNotBlank() }
                        ?: cap.optString("file").takeIf { it.isNotBlank() }
                        ?: continue
                    val lang = cap.optString("language").takeIf { it.isNotBlank() } ?: "English"
                    val label = cap.optString("label").takeIf { it.isNotBlank() } ?: lang
                    val capMime = if (capUrl.contains(".srt", ignoreCase = true) ||
                        cap.optString("type").equals("srt", ignoreCase = true)
                    ) {
                        "application/x-subrip"
                    } else {
                        "text/vtt"
                    }
                    captions.add(CinejoyCaption(label = label, language = lang, url = capUrl, mimeType = capMime))
                }
            }

            return DirectStreamResult(
                ok = true,
                referer = null, // Direct CDN (hakunaymatata.com) requires NO referer
                captions = captions,
                servers = listOf(
                    DirectServer(
                        name = serverName,
                        url = streamUrl,
                        kind = kind
                    )
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.w(TAG, "Vidlink resolution failed for $tmdbId: ${e.message}")
            return null
        }
    }

    fun resolveVidfast(
        mediaType: String,
        tmdbId: String,
        season: Int? = null,
        episode: Int? = null,
        serverName: String = "Lisbon",
        preferredSubServer: String? = null
    ): DirectStreamResult? {
        try {
            val isTv = mediaType.equals("tv", ignoreCase = true)
            val embedUrl = if (isTv) {
                val s = season ?: 1
                val e = episode ?: 1
                "https://vidfast.vc/tv/$tmdbId/$s/$e"
            } else {
                "https://vidfast.vc/movie/$tmdbId"
            }

            val pageReq = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                .get()
                .build()

            val html = client.newCall(pageReq).execute().use { res ->
                if (!res.isSuccessful) return null
                res.body?.string() ?: return null
            }

            // Next.js RSC Flight stream encodes tokens with escaped quotes: \"en\":\"...\" or \\"en\\":\\"...\\"
            val tokenPattern = java.util.regex.Pattern.compile("""(?:en|token)[\\]*":[\\]*"([a-zA-Z0-9_\-]{25,})""")
            var matcher = tokenPattern.matcher(html)
            val token = if (matcher.find()) {
                matcher.group(1)
            } else {
                val fallbackPattern = java.util.regex.Pattern.compile("""(?:\\*\"|")en(?:\\*\"|")\s*:\s*(?:\\*\"|")([a-zA-Z0-9_\-]{25,})""")
                matcher = fallbackPattern.matcher(html)
                if (matcher.find()) matcher.group(1) else null
            } ?: return null

            val encReq = Request.Builder()
                .url("$ENC_API/enc-vidfast?text=${URLEncoder.encode(token, "UTF-8")}")
                .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                .get()
                .build()

            val encJson = client.newCall(encReq).execute().use { res ->
                if (!res.isSuccessful) return null
                val body = res.body?.string() ?: return null
                if (body.isBlank() || !body.trim().startsWith("{")) return null
                JSONObject(body)
            } ?: return null
            if (encJson.optInt("status") != 200) return null
            val resultObj = encJson.optJSONObject("result") ?: return null
            val serversEndpoint = resultObj.optString("servers")
            val streamEndpoint = resultObj.optString("stream")
            val csrfToken = resultObj.optString("token")
            if (serversEndpoint.isBlank() || streamEndpoint.isBlank()) return null

            val vidfastHeaders = mapOf(
                "User-Agent" to AppConfig.STREAM_USER_AGENT,
                "Referer" to "https://vidfast.vc/",
                "X-Requested-With" to "XMLHttpRequest",
                "X-CSRF-Token" to csrfToken
            )

            val serversReqBuilder = Request.Builder().url(serversEndpoint).post("".toRequestBody())
            vidfastHeaders.forEach { (k, v) -> serversReqBuilder.header(k, v) }
            val serversEncrypted = client.newCall(serversReqBuilder.build()).execute().use { res ->
                if (!res.isSuccessful) return null
                res.body?.string() ?: return null
            }

            val decServersBody = JSONObject().apply { put("text", serversEncrypted) }
            val decServersReq = Request.Builder()
                .url("$ENC_API/dec-vidfast")
                .header("Content-Type", "application/json")
                .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                .post(decServersBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val decServersJson = client.newCall(decServersReq).execute().use { res ->
                if (!res.isSuccessful) return null
                val body = res.body?.string() ?: return null
                if (body.isBlank() || !body.trim().startsWith("{")) return null
                JSONObject(body)
            } ?: return null
            val serversList = decServersJson.optJSONArray("result") ?: return null
            if (serversList.length() == 0) return null
            println("[$tmdbId] VIDFAST SERVERS LIST: $serversList")

            // Sort servers according to preferred sub-cluster, 4K availability, and hardware codec support
            data class VidfastCandidate(
                val name: String,
                val payload: String,
                val is4k: Boolean,
                val isDirect: Boolean,
                val isMaster: Boolean
            )
            val fourKCandidates = mutableListOf<VidfastCandidate>()
            val adaptiveCandidates = mutableListOf<VidfastCandidate>()
            val otherCandidates = mutableListOf<VidfastCandidate>()

            for (i in 0 until serversList.length()) {
                val s = serversList.optJSONObject(i) ?: continue
                val sName = s.optString("name")
                val sData = s.optString("data")
                val sImg = s.optString("image")
                val sDesc = s.optString("description")
                if (sData.isNotBlank()) {
                    val is4k = sImg.contains("4k", ignoreCase = true) ||
                               sDesc.contains("4k", ignoreCase = true) ||
                               sName.contains("4k", ignoreCase = true) ||
                               sName.contains("vFast", ignoreCase = true)
                    val isDirect = sName.contains("vFast", ignoreCase = true) || sDesc.contains("direct", ignoreCase = true)
                    val isMaster = sName.contains("vRapid", ignoreCase = true) || sName.contains("vBlaze", ignoreCase = true)
                    val candidate = VidfastCandidate(sName, sData, is4k, isDirect, isMaster)
                    if (is4k) {
                        fourKCandidates.add(candidate)
                    } else if (isMaster || sName.contains("vEdge", ignoreCase = true) || sName.contains("Cobra", ignoreCase = true)) {
                        adaptiveCandidates.add(candidate)
                    } else {
                        otherCandidates.add(candidate)
                    }
                }
            }

            val isHevc10 = DeviceCodecCapability.isHevcMain10Supported()
            val candidatePayloads = mutableListOf<VidfastCandidate>()

            val prefersDirect = preferredSubServer?.contains("direct", ignoreCase = true) == true ||
                                preferredSubServer?.equals("vFast", ignoreCase = true) == true ||
                                serverName.contains("athens", ignoreCase = true)

            if (prefersDirect) {
                // Athens / Direct preference: 4K UHD Direct first, then 4K Master ladder, then adaptives
                candidatePayloads.addAll(fourKCandidates.filter { it.isDirect })
                candidatePayloads.addAll(fourKCandidates.filter { it.isMaster })
                candidatePayloads.addAll(fourKCandidates.filter { !it.isDirect && !it.isMaster })
                candidatePayloads.addAll(adaptiveCandidates)
                candidatePayloads.addAll(otherCandidates)
            } else {
                // Lisbon / Master preference: 4K Master ladder first, then 4K UHD Direct, then adaptives
                candidatePayloads.addAll(fourKCandidates.filter { it.isMaster })
                candidatePayloads.addAll(fourKCandidates.filter { it.isDirect })
                candidatePayloads.addAll(fourKCandidates.filter { !it.isDirect && !it.isMaster })
                candidatePayloads.addAll(adaptiveCandidates)
                candidatePayloads.addAll(otherCandidates)
            }

            data class DiscoveredVidfastStream(
                val server: DirectServer,
                val captions: List<CinejoyCaption>,
                val score: Int,
                val is4k: Boolean,
                val isMaster: Boolean,
                val isDirect: Boolean
            )

            val discoveredStreams = mutableListOf<DiscoveredVidfastStream>()
            var attempts = 0
            for (candidate in candidatePayloads) {
                val has4kMaster = discoveredStreams.any { it.is4k && it.isMaster }
                val has4kDirect = discoveredStreams.any { it.is4k && it.isDirect }
                if (has4kMaster && has4kDirect) {
                    break
                }
                // Skip redundant master mirrors if we already have a functional 4K master ladder (e.g. vBlaze when vRapid succeeded)
                if (candidate.isMaster && has4kMaster) {
                    continue
                }
                // Skip redundant direct streams if we already have a functional 4K direct rip
                if (candidate.isDirect && has4kDirect) {
                    continue
                }
                if (attempts >= 6 || (discoveredStreams.size >= 3 && discoveredStreams.any { it.score >= 95 })) {
                    break
                }
                attempts++
                try {
                    val streamReqBuilder = Request.Builder().url("$streamEndpoint/${candidate.payload}").post("".toRequestBody())
                    vidfastHeaders.forEach { (k, v) -> streamReqBuilder.header(k, v) }
                    val streamEncrypted = client.newCall(streamReqBuilder.build()).execute().use { res ->
                        if (!res.isSuccessful) return@use null
                        res.body?.string()?.takeIf { it.isNotBlank() }
                    }
                    if (streamEncrypted.isNullOrBlank()) continue

                    val decStreamBody = JSONObject().apply { put("text", streamEncrypted) }
                    val decStreamReq = Request.Builder()
                        .url("$ENC_API/dec-vidfast")
                        .header("Content-Type", "application/json")
                        .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                        .post(decStreamBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    val decJson = client.newCall(decStreamReq).execute().use { res ->
                        if (!res.isSuccessful) return@use null
                        val body = res.body?.string() ?: return@use null
                        JSONObject(body)
                    }

                    if (decJson?.optInt("status") == 200 && decJson.optJSONObject("result") != null) {
                        val streamResult = decJson.optJSONObject("result") ?: continue
                        val playlistUrl = streamResult.optString("url").takeIf { it.isNotBlank() } ?: continue

                        val captions = mutableListOf<CinejoyCaption>()
                        val tracksArray = streamResult.optJSONArray("tracks")
                        if (tracksArray != null) {
                            for (i in 0 until tracksArray.length()) {
                                val tr = tracksArray.optJSONObject(i) ?: continue
                                val file = tr.optString("file").takeIf { it.isNotBlank() } ?: continue
                                val label = tr.optString("label").takeIf { it.isNotBlank() } ?: "Subtitle"
                                val lang = label.lowercase().take(2)
                                captions.add(CinejoyCaption(label = label, language = lang, url = file, mimeType = "text/vtt"))
                            }
                        }

                        val hasMasterM3u8 = playlistUrl.contains("master.m3u8", ignoreCase = true)
                        val hasDirect2160p = playlistUrl.contains("2160p", ignoreCase = true) || playlistUrl.contains("/r2/cdn1/") || candidate.isDirect
                        val isMasterStream = hasMasterM3u8 || (candidate.isMaster && !hasDirect2160p)
                        val isDirectStream = hasDirect2160p || (!hasMasterM3u8 && candidate.isDirect)
                        val is4kStream = candidate.is4k || hasDirect2160p || playlistUrl.contains("/vd/")

                        val score = when {
                            prefersDirect && isDirectStream && isHevc10 -> 100
                            prefersDirect && isMasterStream -> 98
                            !prefersDirect && isMasterStream -> 100
                            !prefersDirect && isDirectStream && isHevc10 -> 98
                            playlistUrl.contains("/r2/cdn2/") -> 85
                            isDirectStream && !isHevc10 -> 30
                            playlistUrl.contains(".mp4", ignoreCase = true) -> 50
                            else -> 40
                        }

                        val serverLabel = when {
                            is4kStream && isMasterStream -> "$serverName (4K Master)"
                            is4kStream && isDirectStream -> "$serverName (4K UHD Direct)"
                            is4kStream -> "$serverName (4K Cinema)"
                            isMasterStream -> "$serverName (Master HD)"
                            playlistUrl.contains("/r2/cdn2/") -> "$serverName (Adaptive HD)"
                            else -> "$serverName (${candidate.name})"
                        }

                        discoveredStreams.add(
                            DiscoveredVidfastStream(
                                server = DirectServer(
                                    name = serverLabel,
                                    url = playlistUrl,
                                    kind = if (playlistUrl.contains(".mp4", ignoreCase = true)) "file" else "hls"
                                ),
                                captions = captions,
                                score = score,
                                is4k = is4kStream,
                                isMaster = isMasterStream,
                                isDirect = isDirectStream
                            )
                        )

                        // If we found both top-tier 4K streams (Master + Direct), break early
                        if (discoveredStreams.any { it.is4k && it.isMaster } && discoveredStreams.any { it.is4k && it.isDirect }) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Vidfast candidate payload error: ${e.message}")
                }
            }

            if (discoveredStreams.isEmpty()) return null

            // Prioritize highest compatibility and quality score
            discoveredStreams.sortByDescending { it.score }

            // Consolidate captions across streams, taking the richest subtitle track set
            val bestCaptions = discoveredStreams.maxByOrNull { it.captions.size }?.captions ?: emptyList()

            // Keep descriptive labels for all streams so both 4K Master and 4K UHD Direct are distinct and selectable
            val servers = discoveredStreams.map { it.server }

            return DirectStreamResult(
                ok = true,
                referer = "https://vidfast.vc/",
                captions = bestCaptions,
                servers = servers
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.w(TAG, "Vidfast resolution failed for $tmdbId: ${e.message}")
            return null
        }
    }

    private fun resolveShegu(
        isTv: Boolean,
        tmdbId: String,
        title: String,
        serverName: String,
        season: Int?,
        episode: Int?,
        year: String?,
        imdbId: String?
    ): DirectStreamResult? {
        val queryUrl = buildSheguQuery(
            title = title,
            isTv = isTv,
            tmdbId = tmdbId,
            serverName = serverName,
            season = season,
            episode = episode,
            year = year,
            imdbId = imdbId
        )

        // Step 1: Request encryption from enc-dec.app
        val encUrl = "$ENC_API/enc-cinejoy?url=${URLEncoder.encode(queryUrl, "UTF-8")}"
        val encReq = Request.Builder()
            .url(encUrl)
            .header("User-Agent", AppConfig.STREAM_USER_AGENT)
            .get()
            .build()

        val (encData, encState) = client.newCall(encReq).execute().use { res ->
            if (!res.isSuccessful) return null
            val body = res.body?.string() ?: return null
            val json = JSONObject(body)
            if (json.optInt("status") != 200) return null
            val resultObj = json.optJSONObject("result") ?: return null
            val data = resultObj.optString("data")
            val state = resultObj.opt("state")
            if (data.isNullOrBlank() || state == null) return null
            Pair(data, state)
        }

        // Step 2: Base64URL decode data payload
        val decodedPayload = try {
            b64Decode(encData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to base64 decode encData", e)
            return null
        }

        // Step 3: POST binary payload to Shegu gateway
        val sheguReq = Request.Builder()
            .url("$SHEGU/g")
            .header("Accept", "*/*")
            .header("Origin", "https://cinejoy.to")
            .header("Referer", CINEJOY_REFERER)
            .header("User-Agent", AppConfig.STREAM_USER_AGENT)
            .post(decodedPayload.toRequestBody(OCTET_MEDIA_TYPE))
            .build()

        val packedBytes = client.newCall(sheguReq).execute().use { res ->
            if (!res.isSuccessful) return null
            res.body?.bytes()
        } ?: return null

        // Step 4: Base64URL encode packedBytes
        val packedB64 = b64UrlEncode(packedBytes)

        // Step 5: Decrypt response with encState
        val decBodyObj = JSONObject().apply {
            put("text", packedB64)
            put("state", encState)
        }
        val decReq = Request.Builder()
            .url("$ENC_API/dec-cinejoy")
            .header("Content-Type", "application/json")
            .header("User-Agent", AppConfig.STREAM_USER_AGENT)
            .post(decBodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val decJson = client.newCall(decReq).execute().use { res ->
            if (!res.isSuccessful) return null
            val body = res.body?.string() ?: return null
            JSONObject(body)
        }

        if (decJson.optInt("status") != 200) return null
        val decResult = decJson.opt("result") ?: return null

        return parseDecryptedStream(decResult, serverName)
    }

    private fun parseDecryptedStream(result: Any, serverName: String): DirectStreamResult? {
        val json = when (result) {
            is JSONObject -> result
            is String -> JSONObject(result)
            else -> return null
        }

        val dataObj = json.optJSONObject("data") ?: json
        val streamArray = dataObj.optJSONArray("stream") ?: json.optJSONArray("stream") ?: return null

        for (i in 0 until streamArray.length()) {
            val item = streamArray.optJSONObject(i) ?: continue
            val playlist = item.optString("playlist").takeIf { it.isNotBlank() }
            val url = item.optString("url").takeIf { it.isNotBlank() }
            val file = item.optString("file").takeIf { it.isNotBlank() }
            val streamUrl = playlist ?: url ?: file ?: continue

            val captions = mutableListOf<CinejoyCaption>()
            val captionsArray = item.optJSONArray("captions")
            if (captionsArray != null) {
                for (j in 0 until captionsArray.length()) {
                    val cap = captionsArray.optJSONObject(j) ?: continue
                    val capUrl = cap.optString("file").takeIf { it.isNotBlank() }
                        ?: cap.optString("url").takeIf { it.isNotBlank() }
                        ?: continue
                    val label = cap.optString("label").takeIf { it.isNotBlank() }
                        ?: cap.optString("language").takeIf { it.isNotBlank() }
                        ?: "Subtitle"
                    val lang = cap.optString("language").takeIf { it.isNotBlank() } ?: "en"
                    val capMime = if (capUrl.contains(".srt", ignoreCase = true)) "application/x-subrip" else "text/vtt"
                    captions.add(CinejoyCaption(label = label, language = lang, url = capUrl, mimeType = capMime))
                }
            }

            val kind = if (streamUrl.contains(".mp4", ignoreCase = true)) "file" else "hls"

            return DirectStreamResult(
                ok = true,
                referer = CINEJOY_REFERER,
                captions = captions,
                servers = listOf(
                    DirectServer(
                        name = serverName,
                        url = streamUrl,
                        kind = kind
                    )
                )
            )
        }
        return null
    }

    private fun buildSheguQuery(
        title: String,
        isTv: Boolean,
        tmdbId: String,
        serverName: String,
        season: Int?,
        episode: Int?,
        year: String?,
        imdbId: String?
    ): String {
        val params = StringBuilder()
        params.append("title=").append(URLEncoder.encode(title, "UTF-8"))
        params.append("&type=").append(if (isTv) "series" else "movie")
        params.append("&tmdb=").append(tmdbId)
        params.append("&server=").append(URLEncoder.encode(serverName, "UTF-8"))
        if (!year.isNullOrBlank()) {
            params.append("&year=").append(URLEncoder.encode(year, "UTF-8"))
        }
        if (!imdbId.isNullOrBlank()) {
            params.append("&imdb=").append(URLEncoder.encode(imdbId, "UTF-8"))
        }
        if (isTv) {
            params.append("&season=").append(season ?: 1)
            params.append("&episode=").append(episode ?: 1)
        }
        return "$SHEGU/?$params"
    }
}

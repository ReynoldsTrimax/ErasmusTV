package com.erasmustv.app.data.remote

import android.util.Log
import com.erasmustv.app.core.config.AppConfig
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
        preferredServer: String = "lisbon",
        year: String? = null,
        imdbId: String? = null
    ): DirectStreamResult = withContext(Dispatchers.IO) {
        val isTv = mediaType.equals("tv", ignoreCase = true)
        val s = if (isTv) season ?: 1 else null
        val e = if (isTv) episode ?: 1 else null

        val cacheKey = "$preferredServer:$mediaType:$tmdbId:$s:$e"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_MS) {
            return@withContext cached.result
        }

        // Preferred server first, then cluster fallback sequence
        val serverOrder = buildList {
            add(preferredServer)
            STREAM_SERVERS.map { it.id }.filter { !it.equals(preferredServer, ignoreCase = true) }.forEach {
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

        // Cluster priority routing
        when (serverId.lowercase()) {
            "lisbon" -> {
                // Lisbon: 4K / HLS flagship (vFast primary sub-server with internal vRapid fallback)
                resolveVidfast(mediaType, tmdbId, season, episode, serverName, preferredSubServer = "vFast")?.let { return it }
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
            }
            "athens" -> {
                // Athens: 4K Cinema Mirror (vRapid primary sub-server for genuine 4K with internal vFast fallback)
                resolveVidfast(mediaType, tmdbId, season, episode, serverName, preferredSubServer = "vRapid")?.let { return it }
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
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
                resolveVidfast(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidlove(mediaType, tmdbId, season, episode, serverName)?.let { return it }
                resolveVidlink(mediaType, tmdbId, season, episode, serverName)?.let { return it }
            }
            else -> {
                resolveVidfast(mediaType, tmdbId, season, episode, serverName)?.let { return it }
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

            // Next.js RSC Flight stream encodes tokens with escaped quotes: \"en\":\"...\"
            val escapedTokenPattern = java.util.regex.Pattern.compile("""(?:\\\"|")en(?:\\\"|")\s*:\s*(?:\\\"|")([^\\\"]+)(?:\\\"|")""")
            var matcher = escapedTokenPattern.matcher(html)
            val token = if (matcher.find()) {
                matcher.group(1)
            } else {
                val standardPattern = java.util.regex.Pattern.compile("\"(?:en|token)\":\"(.*?)\"")
                matcher = standardPattern.matcher(html)
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

            // Sort servers according to preferred sub-cluster (vFast vs vRapid)
            val vFastPayloads = mutableListOf<String>()
            val vRapidPayloads = mutableListOf<String>()
            val otherPayloads = mutableListOf<String>()

            for (i in 0 until serversList.length()) {
                val s = serversList.optJSONObject(i) ?: continue
                val sName = s.optString("name")
                val sData = s.optString("data")
                if (sData.isNotBlank()) {
                    if (sName.contains("vFast", ignoreCase = true)) {
                        vFastPayloads.add(sData)
                    } else if (sName.contains("vRapid", ignoreCase = true)) {
                        vRapidPayloads.add(sData)
                    } else {
                        otherPayloads.add(sData)
                    }
                }
            }

            val candidatePayloads = mutableListOf<String>()
            if (preferredSubServer?.equals("vRapid", ignoreCase = true) == true) {
                // vRapid priority for Athens / Cinema 4K
                candidatePayloads.addAll(vRapidPayloads)
                candidatePayloads.addAll(vFastPayloads)
                candidatePayloads.addAll(otherPayloads)
            } else {
                // Default / vFast priority for Lisbon & flagship
                candidatePayloads.addAll(vFastPayloads)
                candidatePayloads.addAll(vRapidPayloads)
                candidatePayloads.addAll(otherPayloads)
            }

            var decryptedStreamJson: JSONObject? = null
            for (dataPayload in candidatePayloads.take(4)) {
                try {
                    val streamReqBuilder = Request.Builder().url("$streamEndpoint/$dataPayload").post("".toRequestBody())
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
                        decryptedStreamJson = decJson
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Vidfast candidate payload error: ${e.message}")
                }
            }

            val streamResult = decryptedStreamJson?.optJSONObject("result") ?: return null
            val playlistUrl = streamResult.optString("url").takeIf { it.isNotBlank() } ?: return null

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

            return DirectStreamResult(
                ok = true,
                referer = "https://vidfast.vc/",
                captions = captions,
                servers = listOf(
                    DirectServer(
                        name = serverName,
                        url = playlistUrl,
                        kind = if (playlistUrl.contains(".mp4", ignoreCase = true)) "file" else "hls"
                    )
                )
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

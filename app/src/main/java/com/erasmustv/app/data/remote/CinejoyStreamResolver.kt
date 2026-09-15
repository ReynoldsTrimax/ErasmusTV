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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
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
        }

        DirectStreamResult(
            ok = false,
            error = "Could not resolve stream for $title across any cluster servers",
            servers = emptyList()
        )
    }

    private fun resolveSingleServer(
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

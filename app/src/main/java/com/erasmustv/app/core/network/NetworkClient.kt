package com.erasmustv.app.core.network

import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val jsonMediaType = "application/json".toMediaType()

    fun createOkHttpClient(sessionManager: SessionManager? = null): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val builder = OkHttpClient.Builder()
            .dns(ErasmusDns)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(HeaderInterceptor(sessionManager))

        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                object : javax.net.ssl.X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                }
            )
            val sslContext = javax.net.ssl.SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, java.security.SecureRandom())
            }
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        } catch (_: Exception) {}

        return builder.build()
    }

    object ErasmusDns : okhttp3.Dns {
        private val cache = java.util.concurrent.ConcurrentHashMap<String, List<java.net.InetAddress>>()

        private val staticFallbacks = mapOf(
            "api.themoviedb.org" to listOf(
                java.net.InetAddress.getByName("3.175.86.50"),
                java.net.InetAddress.getByName("3.175.86.67"),
                java.net.InetAddress.getByName("3.175.86.103"),
                java.net.InetAddress.getByName("3.175.86.37"),
                java.net.InetAddress.getByName("13.224.245.92")
            ),
            "image.tmdb.org" to listOf(
                java.net.InetAddress.getByName("89.187.162.242"),
                java.net.InetAddress.getByName("18.65.229.117"),
                java.net.InetAddress.getByName("18.65.229.62")
            ),
            "jnxflxtizbezqclxfmzc.supabase.co" to listOf(
                java.net.InetAddress.getByName("104.18.38.10"),
                java.net.InetAddress.getByName("172.64.155.249")
            ),
            "api.shegu.st" to listOf(
                java.net.InetAddress.getByName("172.67.173.72"),
                java.net.InetAddress.getByName("104.21.96.53")
            ),
            "enc-dec.app" to listOf(
                java.net.InetAddress.getByName("172.67.197.95"),
                java.net.InetAddress.getByName("104.21.84.216")
            ),
            "info.movieboxnoob.cc" to listOf(
                java.net.InetAddress.getByName("104.26.13.88"),
                java.net.InetAddress.getByName("172.67.68.82"),
                java.net.InetAddress.getByName("104.26.12.88")
            ),
            "opensubtitles-v3.strem.io" to listOf(
                java.net.InetAddress.getByName("104.17.88.107"),
                java.net.InetAddress.getByName("104.17.89.107")
            ),
            "vidfast.vc" to listOf(
                java.net.InetAddress.getByName("172.67.182.248"),
                java.net.InetAddress.getByName("104.21.67.238")
            )
        )

        private val directHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .build()
        }

        override fun lookup(hostname: String): List<java.net.InetAddress> {
            // Check cache
            cache[hostname]?.let { return it }

            // Fallback for static endpoints immediately if it matches
            val preseeded = staticFallbacks[hostname]

            // Attempt Cloudflare DoH via direct raw IP 1.1.1.1
            try {
                val dohUrl = "https://1.1.1.1/dns-query?name=$hostname&type=A"
                val request = okhttp3.Request.Builder()
                    .url(dohUrl)
                    .header("Accept", "application/dns-json")
                    .build()
                directHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val addresses = parseDohJson(body)
                            if (addresses.isNotEmpty()) {
                                cache[hostname] = addresses
                                return addresses
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall through to system or static
            }

            // Attempt system DNS
            try {
                val addresses = okhttp3.Dns.SYSTEM.lookup(hostname)
                val v4 = addresses.filterIsInstance<java.net.Inet4Address>()
                val v6 = addresses.filterIsInstance<java.net.Inet6Address>()
                val result = if (v4.isNotEmpty()) v4 + v6 else addresses
                if (result.isNotEmpty()) {
                    cache[hostname] = result
                    return result
                }
            } catch (_: Exception) {
                // Fall through to static
            }

            if (preseeded != null) {
                cache[hostname] = preseeded
                return preseeded
            }

            throw java.net.UnknownHostException("Unable to resolve host: $hostname")
        }

        private fun parseDohJson(jsonStr: String): List<java.net.InetAddress> {
            val result = mutableListOf<java.net.InetAddress>()
            try {
                val jsonObject = org.json.JSONObject(jsonStr)
                val answerArray = jsonObject.optJSONArray("Answer") ?: return emptyList()
                for (i in 0 until answerArray.length()) {
                    val item = answerArray.getJSONObject(i)
                    val type = item.optInt("type")
                    val data = item.optString("data")
                    if (type == 1 && data.isNotEmpty()) {
                        try {
                            result.add(java.net.InetAddress.getByName(data))
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
            return result
        }
    }

    fun <T> createService(
        serviceClass: Class<T>,
        baseUrl: String,
        okHttpClient: OkHttpClient
    ): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
            .create(serviceClass)
    }

    private class HeaderInterceptor(private val sessionManager: SessionManager?) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val requestBuilder = chain.request().newBuilder()

            // Add standard Erasmus client user agent
            requestBuilder.header("User-Agent", "ErasmusTV/1.0.0 (Android TV)")

            // If talking to Supabase, attach apikey and Authorization bearer
            val url = chain.request().url.toString()
            if (url.contains("supabase.co")) {
                requestBuilder.header("apikey", AppConfig.SUPABASE_ANON_KEY)
                val isGuest = sessionManager?.let { runBlocking { it.isGuest() } } ?: false
                val token = if (!isGuest) sessionManager?.let { runBlocking { it.getAccessToken() } } else null
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                } else {
                    requestBuilder.header("Authorization", "Bearer ${AppConfig.SUPABASE_ANON_KEY}")
                }
            }

            return chain.proceed(requestBuilder.build())
        }
    }
}

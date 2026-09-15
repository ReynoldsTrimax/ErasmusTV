package com.erasmustv.app.core.config

import com.erasmustv.app.BuildConfig

object AppConfig {
    const val APP_NAME = "Erasmus"

    // Supabase & Auth
    val SUPABASE_URL = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
    val GOOGLE_WEB_CLIENT_ID = BuildConfig.GOOGLE_WEB_CLIENT_ID

    // TMDB
    val TMDB_API_KEY = BuildConfig.TMDB_API_KEY
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    const val TMDB_IMAGE_BASE_W500 = "https://image.tmdb.org/t/p/w500"
    const val TMDB_IMAGE_BASE_W780 = "https://image.tmdb.org/t/p/w780"
    const val TMDB_IMAGE_BASE_W1280 = "https://image.tmdb.org/t/p/w1280"
    const val TMDB_IMAGE_BASE_ORIGINAL = "https://image.tmdb.org/t/p/original"

    // Erasmus Stream & Backend API
    val BACKEND_BASE_URL = BuildConfig.BACKEND_BASE_URL
    const val DEFAULT_STREAM_REFERER = "https://cinejoy.to/"
    const val STREAM_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    // OMDb API
    val OMDB_API_KEY = BuildConfig.OMDB_API_KEY
    const val OMDB_BASE_URL = "https://www.omdbapi.com/"

    // Playback thresholds
    const val MIN_RESUME_SECONDS = 15L
    const val COMPLETE_RATIO = 0.90f
    const val COMPLETE_REMAINING_SECONDS = 30L

    fun posterUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val clean = if (path.startsWith("/")) path else "/$path"
        return "$TMDB_IMAGE_BASE_W500$clean"
    }

    fun backdropUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val clean = if (path.startsWith("/")) path else "/$path"
        return "$TMDB_IMAGE_BASE_W1280$clean"
    }

    fun stillUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val clean = if (path.startsWith("/")) path else "/$path"
        return "$TMDB_IMAGE_BASE_W780$clean"
    }

    fun logoUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val clean = if (path.startsWith("/")) path else "/$path"
        return "$TMDB_IMAGE_BASE_W500$clean"
    }
}

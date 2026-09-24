package com.erasmustv.app.ui.navigation

import android.net.Uri

object NavRoutes {
    const val LOGIN = "login"
    const val PROFILES = "profiles"
    const val HOME = "home"
    const val MOVIES = "movies"
    const val TV = "tv"
    const val ANIME = "anime"
    const val SEARCH = "search"
    const val WATCHLIST = "watchlist"

    const val DETAILS = "details/{mediaType}/{id}"
    fun details(mediaType: String, id: String) = "details/$mediaType/$id"

    const val PLAYER = "player/{mediaType}/{id}/{title}?season={season}&episode={episode}&posterPath={posterPath}&backdropPath={backdropPath}&logoPath={logoPath}&tagline={tagline}"
    fun player(
        mediaType: String,
        id: String,
        title: String,
        season: Int? = null,
        episode: Int? = null,
        posterPath: String? = null,
        backdropPath: String? = null,
        logoPath: String? = null,
        tagline: String? = null
    ): String {
        val encodedTitle = Uri.encode(title)
        val s = season ?: 1
        val e = episode ?: 1
        val encodedPoster = Uri.encode(posterPath ?: "")
        val encodedBackdrop = Uri.encode(backdropPath ?: "")
        val encodedLogo = Uri.encode(logoPath ?: "")
        val encodedTagline = Uri.encode(tagline ?: "")
        return "player/$mediaType/$id/$encodedTitle?season=$s&episode=$e&posterPath=$encodedPoster&backdropPath=$encodedBackdrop&logoPath=$encodedLogo&tagline=$encodedTagline"
    }
}


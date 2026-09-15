package com.erasmustv.app.ui.navigation

import android.net.Uri

object NavRoutes {
    const val LOGIN = "login"
    const val PROFILES = "profiles"
    const val HOME = "home"
    const val MOVIES = "movies"
    const val TV = "tv"
    const val ANIME = "anime"
    const val CATEGORIES = "categories"
    const val STUDIOS = "studios"
    const val SPORTS = "sports"
    const val SPARKS = "sparks"
    const val SEARCH = "search"
    const val WATCHLIST = "watchlist"

    const val DETAILS = "details/{mediaType}/{id}"
    fun details(mediaType: String, id: String) = "details/$mediaType/$id"

    const val PLAYER = "player/{mediaType}/{id}/{title}?season={season}&episode={episode}&posterPath={posterPath}&backdropPath={backdropPath}"
    fun player(
        mediaType: String,
        id: String,
        title: String,
        season: Int? = null,
        episode: Int? = null,
        posterPath: String? = null,
        backdropPath: String? = null
    ): String {
        val encodedTitle = Uri.encode(title)
        val s = season ?: 1
        val e = episode ?: 1
        val encodedPoster = Uri.encode(posterPath ?: "")
        val encodedBackdrop = Uri.encode(backdropPath ?: "")
        return "player/$mediaType/$id/$encodedTitle?season=$s&episode=$e&posterPath=$encodedPoster&backdropPath=$encodedBackdrop"
    }
}


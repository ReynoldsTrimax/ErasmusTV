package com.erasmustv.app.data.repository

import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.data.model.Genre
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.MediaRating
import com.erasmustv.app.data.model.MovieDetails
import com.erasmustv.app.data.model.TvDetails
import com.erasmustv.app.data.model.TvSeason
import com.erasmustv.app.data.model.WatchProvider
import com.erasmustv.app.data.remote.OmdbApiService
import com.erasmustv.app.data.remote.OmdbResponse
import com.erasmustv.app.data.remote.TmdbApiService
import java.util.Locale

class MediaRepository(
    private val tmdbApi: TmdbApiService,
    private val omdbApi: OmdbApiService? = null
) {
    private val apiKey = AppConfig.TMDB_API_KEY
    private val omdbApiKey = AppConfig.OMDB_API_KEY.ifBlank { "trilogy" }
    private val logoCache = java.util.concurrent.ConcurrentHashMap<String, String?>()

    suspend fun resolveRatings(
        voteAverage: Double?,
        voteCount: Int?,
        imdbId: String?,
        title: String,
        year: String?,
        isTv: Boolean
    ): List<MediaRating> {
        val tmdbScore = if (voteAverage != null && voteAverage > 0) {
            String.format(Locale.US, "%.1f/10", voteAverage)
        } else "—"
        val tmdbSub = if (voteCount != null && voteCount > 0) "$voteCount votes" else "—"

        val tmdbRating = MediaRating(
            provider = "tmdb",
            label = "TMDB",
            score = tmdbScore,
            subText = tmdbSub
        )

        var omdb: OmdbResponse? = null
        if (omdbApi != null) {
            try {
                if (!imdbId.isNullOrBlank()) {
                    val cleanImdb = if (imdbId.startsWith("tt")) imdbId else "tt$imdbId"
                    val res = omdbApi.getByImdbId(cleanImdb, omdbApiKey)
                    if (res.response.equals("True", ignoreCase = true)) {
                        omdb = res
                    }
                }
                if (omdb == null && title.isNotBlank()) {
                    val res = omdbApi.getByTitle(
                        title = title,
                        apiKey = omdbApiKey,
                        year = year,
                        type = if (isTv) "series" else "movie"
                    )
                    if (res.response.equals("True", ignoreCase = true)) {
                        omdb = res
                    }
                }
            } catch (_: Exception) {}
        }

        // IMDb Card
        val imdbRatingVal = omdb?.imdbRating?.takeIf { it != "N/A" && it.isNotBlank() }
        val imdbVotesVal = omdb?.imdbVotes?.takeIf { it != "N/A" && it.isNotBlank() }
        val imdbRating = MediaRating(
            provider = "imdb",
            label = "IMDb",
            score = if (imdbRatingVal != null) "$imdbRatingVal/10" else "—",
            subText = if (imdbVotesVal != null) "$imdbVotesVal votes" else "—"
        )

        // Rotten Tomatoes Card
        val rtVal = omdb?.ratings?.find { it.source.contains("Rotten Tomatoes", ignoreCase = true) }?.value
            ?.takeIf { it != "N/A" && it.isNotBlank() }
        val rtRating = MediaRating(
            provider = "rotten_tomatoes",
            label = "ROTTEN TOMATOES",
            score = rtVal ?: "—",
            subText = if (rtVal != null) "Tomatometer" else "—"
        )

        // Metacritic Card
        val metaVal = omdb?.ratings?.find { it.source.contains("Metacritic", ignoreCase = true) }?.value
            ?: omdb?.metascore?.takeIf { it != "N/A" && it.isNotBlank() }?.let { "$it/100" }
        val metaClean = metaVal?.takeIf { it != "N/A" && it.isNotBlank() }
        val metaRating = MediaRating(
            provider = "metacritic",
            label = "METACRITIC",
            score = metaClean ?: "—",
            subText = if (metaClean != null) "Metascore" else "—"
        )

        return listOf(tmdbRating, imdbRating, rtRating, metaRating)
    }

    suspend fun getMediaLogo(mediaType: String, id: String): Result<String?> = runCatching {
        val mType = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
        val cacheKey = "$mType:$id"
        if (logoCache.containsKey(cacheKey)) {
            return@runCatching logoCache[cacheKey]
        }
        val response = tmdbApi.getMediaImages(mType, id, apiKey, "en,null")
        val enLogos = response.logos.filter { it.iso6391 == "en" }
        val bestLogo = (enLogos.ifEmpty { response.logos })
            .maxByOrNull { (it.voteAverage ?: 0.0) * 10 + (it.voteCount ?: 0) }
            ?.filePath
        logoCache[cacheKey] = bestLogo
        bestLogo
    }

    suspend fun getTrending(): Result<List<MediaItem>> = runCatching {
        tmdbApi.getTrending("all", "day", apiKey).results
    }

    suspend fun getPopularMovies(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.getPopularMovies(apiKey, page).results.map {
            it.copy(mediaType = "movie")
        }
    }

    suspend fun getTopRatedMovies(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.getTopRatedMovies(apiKey, page).results.map {
            it.copy(mediaType = "movie")
        }
    }

    suspend fun getNowPlayingMovies(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.getNowPlayingMovies(apiKey, page).results.map {
            it.copy(mediaType = "movie")
        }
    }

    suspend fun getUpcomingMovies(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.getUpcomingMovies(apiKey, page).results.map {
            it.copy(mediaType = "movie")
        }
    }

    suspend fun getPopularTv(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.getPopularTv(apiKey, page).results.map {
            it.copy(mediaType = "tv")
        }
    }

    suspend fun getTopRatedTv(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.getTopRatedTv(apiKey, page).results.map {
            it.copy(mediaType = "tv")
        }
    }

    suspend fun getTrendingAnime(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.discoverTv(
            apiKey = apiKey,
            withGenres = "16",
            withOriginalLanguage = "ja",
            sortBy = "popularity.desc",
            page = page
        ).results.map { it.copy(mediaType = "tv") }
    }

    suspend fun getTopRatedAnime(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.discoverTv(
            apiKey = apiKey,
            withGenres = "16",
            withOriginalLanguage = "ja",
            sortBy = "vote_average.desc",
            page = page
        ).results.map { it.copy(mediaType = "tv") }
    }

    suspend fun getShonenAnime(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.discoverTv(
            apiKey = apiKey,
            withGenres = "16,10759",
            withOriginalLanguage = "ja",
            sortBy = "popularity.desc",
            page = page
        ).results.map { it.copy(mediaType = "tv") }
    }

    suspend fun getAnimeMovies(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.discoverMovie(
            apiKey = apiKey,
            withGenres = "16",
            withOriginalLanguage = "ja",
            sortBy = "popularity.desc",
            page = page
        ).results.map { it.copy(mediaType = "movie") }
    }

    suspend fun getMovieDetails(id: String): Result<MovieDetails> = runCatching {
        val raw = tmdbApi.getMovieDetails(id, apiKey)
        val bestLogo = raw.images?.logos?.let { logos ->
            val enLogos = logos.filter { it.iso6391 == "en" }
            (enLogos.ifEmpty { logos })
                .maxByOrNull { (it.voteAverage ?: 0.0) * 10 + (it.voteCount ?: 0) }
                ?.filePath
        }
        if (bestLogo != null) {
            logoCache["movie:${raw.id}"] = bestLogo
        }
        val logo = bestLogo ?: logoCache["movie:${raw.id}"]

        val ratings = resolveRatings(
            voteAverage = raw.voteAverage,
            voteCount = raw.voteCount,
            imdbId = raw.imdbId,
            title = raw.title,
            year = raw.releaseDate?.take(4),
            isTv = false
        )

        MovieDetails(
            id = raw.id,
            title = raw.title,
            overview = raw.overview,
            posterPath = raw.posterPath,
            backdropPath = raw.backdropPath,
            logoPath = logo,
            releaseDate = raw.releaseDate,
            runtime = raw.runtime,
            genres = raw.genres,
            voteAverage = raw.voteAverage,
            voteCount = raw.voteCount,
            imdbId = raw.imdbId,
            status = raw.status,
            budget = raw.budget,
            revenue = raw.revenue,
            cast = raw.credits?.cast ?: emptyList(),
            similar = raw.similar?.results?.map { it.copy(mediaType = "movie") } ?: emptyList(),
            ratings = ratings,
            tagline = raw.tagline
        )
    }

    suspend fun getTvDetails(id: String): Result<TvDetails> = runCatching {
        val raw = tmdbApi.getTvDetails(id, apiKey)
        val bestLogo = raw.images?.logos?.let { logos ->
            val enLogos = logos.filter { it.iso6391 == "en" }
            (enLogos.ifEmpty { logos })
                .maxByOrNull { (it.voteAverage ?: 0.0) * 10 + (it.voteCount ?: 0) }
                ?.filePath
        }
        if (bestLogo != null) {
            logoCache["tv:${raw.id}"] = bestLogo
        }
        val logo = bestLogo ?: logoCache["tv:${raw.id}"]

        val imdbId = raw.externalIds?.imdbId
        val ratings = resolveRatings(
            voteAverage = raw.voteAverage,
            voteCount = raw.voteCount,
            imdbId = imdbId,
            title = raw.title,
            year = raw.firstAirDate?.take(4),
            isTv = true
        )

        TvDetails(
            id = raw.id,
            title = raw.title,
            overview = raw.overview,
            posterPath = raw.posterPath,
            backdropPath = raw.backdropPath,
            logoPath = logo,
            firstAirDate = raw.firstAirDate,
            numberOfSeasons = raw.numberOfSeasons,
            numberOfEpisodes = raw.numberOfEpisodes,
            seasons = raw.seasons,
            genres = raw.genres,
            voteAverage = raw.voteAverage,
            voteCount = raw.voteCount,
            imdbId = imdbId,
            status = raw.status,
            cast = raw.credits?.cast ?: emptyList(),
            similar = raw.similar?.results?.map { it.copy(mediaType = "tv") } ?: emptyList(),
            ratings = ratings,
            tagline = raw.tagline
        )
    }

    suspend fun getTvSeason(showId: String, seasonNumber: Int): Result<TvSeason> = runCatching {
        tmdbApi.getTvSeason(showId, seasonNumber, apiKey)
    }

    suspend fun search(query: String, page: Int = 1): Result<List<MediaItem>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        tmdbApi.searchMulti(query.trim(), apiKey, page).results.filter {
            it.mediaType == "movie" || it.mediaType == "tv"
        }
    }

    suspend fun getMovieGenres(): Result<List<Genre>> = runCatching {
        tmdbApi.getMovieGenres(apiKey).genres
    }

    suspend fun getTvGenres(): Result<List<Genre>> = runCatching {
        tmdbApi.getTvGenres(apiKey).genres
    }

    suspend fun getStudioContent(providerId: Int, page: Int = 1): Result<Pair<List<MediaItem>, List<MediaItem>>> = runCatching {
        val provStr = providerId.toString()
        val movies = tmdbApi.discoverMovie(
            apiKey = apiKey,
            withWatchProviders = provStr,
            watchRegion = "US",
            sortBy = "popularity.desc",
            page = page
        ).results.map { it.copy(mediaType = "movie") }

        val tv = tmdbApi.discoverTv(
            apiKey = apiKey,
            withWatchProviders = provStr,
            watchRegion = "US",
            sortBy = "popularity.desc",
            page = page
        ).results.map { it.copy(mediaType = "tv") }

        Pair(movies, tv)
    }

    suspend fun discoverByGenre(
        movieGenreId: Int?,
        tvGenreId: Int?,
        page: Int = 1
    ): Result<List<MediaItem>> = runCatching {
        val movies = if (movieGenreId != null) {
            tmdbApi.discoverMovie(
                apiKey = apiKey,
                withGenres = movieGenreId.toString(),
                sortBy = "popularity.desc",
                page = page
            ).results.map { it.copy(mediaType = "movie") }
        } else emptyList()

        val tv = if (tvGenreId != null) {
            tmdbApi.discoverTv(
                apiKey = apiKey,
                withGenres = tvGenreId.toString(),
                sortBy = "popularity.desc",
                page = page
            ).results.map { it.copy(mediaType = "tv") }
        } else emptyList()

        // Interleave movies and tv shows for diverse genre catalog
        val combined = mutableListOf<MediaItem>()
        val maxLen = maxOf(movies.size, tv.size)
        for (i in 0 until maxLen) {
            if (i < movies.size) combined.add(movies[i])
            if (i < tv.size) combined.add(tv[i])
        }
        combined
    }
}


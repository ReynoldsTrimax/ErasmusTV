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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Locale

class MediaRepository(
    private val tmdbApi: TmdbApiService,
    private val omdbApi: OmdbApiService? = null
) {
    private val apiKey = AppConfig.TMDB_API_KEY
    private val omdbApiKey = AppConfig.OMDB_API_KEY.ifBlank { "trilogy" }
    // Artwork lookup caches.
    //
    // Values are non-null with `""` standing for "looked this up, there is
    // nothing". `ConcurrentHashMap` throws on a null value, so the previous
    // `<String, String?>` maps threw inside their enclosing `runCatching` every
    // time a title had no logo — a miss was never cached, and every hero slide
    // re-requested the same absent artwork on every single visit.
    private val logoCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val titledBackdropCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val taglineCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Reads a sentinel-backed cache: absent → null, `""` → known-absent → null. */
    private fun cached(
        cache: java.util.concurrent.ConcurrentHashMap<String, String>,
        key: String
    ): String? = cache[key]?.takeIf { it.isNotBlank() }

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

    /**
     * Resolves the title's own logo artwork (the "title treatment").
     *
     * Two passes, because one is not enough:
     *
     *  1. `en,null` — an English logo, or a language-neutral one.
     *  2. **Every** language TMDB holds. Anime and other non-English titles
     *     frequently carry no English logo at all, only the original Japanese
     *     (or French, Korean…) one. That original logo is the real thing and is
     *     exactly what should be shown; falling back to plain text instead was
     *     why so many anime and older shows rendered as a bare string.
     *
     * Raster files are preferred over `.svg` within an equally-ranked group:
     * both render (an SVG decoder is registered), but a PNG costs a fraction of
     * the work to decode on a TV chipset.
     */
    suspend fun getMediaLogo(mediaType: String, id: String): Result<String?> = runCatching {
        val mType = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
        val cacheKey = "$mType:$id"
        if (logoCache.containsKey(cacheKey)) {
            return@runCatching cached(logoCache, cacheKey)
        }

        var logos = tmdbApi.getMediaImages(mType, id, apiKey, "en,null").logos
        if (logos.isEmpty()) {
            // Omitting the language filter entirely is what returns the
            // original-language logo.
            logos = runCatching {
                tmdbApi.getMediaImages(mType, id, apiKey, null).logos
            }.getOrDefault(emptyList())
        }

        val bestLogo = pickBestLogo(logos)
        logoCache[cacheKey] = bestLogo.orEmpty()
        bestLogo
    }

    /**
     * Ranks logo candidates: English first, then language-neutral, then any
     * original-language artwork. Within a group, raster before vector, then by
     * community score.
     */
    private fun pickBestLogo(logos: List<com.erasmustv.app.data.remote.TmdbLogoItem>): String? {
        if (logos.isEmpty()) return null

        fun rank(items: List<com.erasmustv.app.data.remote.TmdbLogoItem>) =
            items.sortedWith(
                compareByDescending<com.erasmustv.app.data.remote.TmdbLogoItem> {
                    !it.filePath.endsWith(".svg", ignoreCase = true)
                }.thenByDescending { (it.voteAverage ?: 0.0) * 10 + (it.voteCount ?: 0) }
            )

        val english = logos.filter { it.iso6391 == "en" }
        val neutral = logos.filter { it.iso6391 == null }
        val rest = logos.filter { it.iso6391 != null && it.iso6391 != "en" }

        return rank(english).firstOrNull()?.filePath
            ?: rank(neutral).firstOrNull()?.filePath
            ?: rank(rest).firstOrNull()?.filePath
    }

    /**
     * A backdrop that already has the title treatment printed on it, for the
     * landscape Continue Watching cards.
     *
     * TMDB tags a backdrop with a language exactly when it contains text, and
     * leaves `iso_639_1` null for the clean, textless artwork used behind heroes.
     * So "find a thumbnail that has the logo in it" is a matter of asking for the
     * language-tagged backdrops rather than compositing anything: the logo is
     * part of the original image as the studio published it.
     *
     * Returns null when a title has no such artwork, and the caller keeps the
     * plain backdrop — never a synthetic overlay.
     */
    suspend fun getTitledBackdrop(mediaType: String, id: String): Result<String?> = runCatching {
        val mType = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
        val cacheKey = "$mType:$id"
        if (titledBackdropCache.containsKey(cacheKey)) {
            return@runCatching cached(titledBackdropCache, cacheKey)
        }

        val backdrops = tmdbApi.getMediaImages(mType, id, apiKey, "en").backdrops
        val best = backdrops
            .filter { it.iso6391 != null }
            .sortedWith(
                compareByDescending<com.erasmustv.app.data.remote.TmdbImageItem> {
                    (it.voteAverage ?: 0.0) * 10 + (it.voteCount ?: 0)
                }.thenByDescending { it.width ?: 0 }
            )
            .firstOrNull()
            ?.filePath

        titledBackdropCache[cacheKey] = best.orEmpty()
        best
    }

    /**
     * Replaces an item's artwork with whatever TMDB currently serves.
     *
     * Necessary because hand-written artwork paths rot. TMDB reissues image file
     * paths when artwork is replaced, and the old path then 404s — which is why
     * the curated anime carousel rendered on a black background: every one of its
     * six hardcoded backdrops had been superseded upstream, so the hero had
     * nothing to draw. Anything hardcoded here is treated as a *last resort*,
     * used only if the live lookup fails outright.
     *
     * The logo comes back from the same request (details already appends the
     * images response), so this doubles as logo enrichment and [withLogos] then
     * has nothing left to fetch for these items.
     */
    suspend fun refreshArtwork(items: List<MediaItem>): List<MediaItem> {
        if (items.isEmpty()) return items
        return coroutineScope {
            items.map { item ->
                async {
                    runCatching {
                        if (item.mediaType.equals("tv", ignoreCase = true) || item.isTv) {
                            val live = getTvDetails(item.id).getOrNull() ?: return@runCatching item
                            item.copy(
                                posterPath = live.posterPath ?: item.posterPath,
                                backdropPath = live.backdropPath ?: item.backdropPath,
                                logoPath = live.logoPath ?: item.logoPath
                            )
                        } else {
                            val live = getMovieDetails(item.id).getOrNull() ?: return@runCatching item
                            item.copy(
                                posterPath = live.posterPath ?: item.posterPath,
                                backdropPath = live.backdropPath ?: item.backdropPath,
                                logoPath = live.logoPath ?: item.logoPath
                            )
                        }
                    }.getOrDefault(item)
                }
            }.map { it.await() }
        }
    }

    /**
     * Fills in logos for the first [limit] items of a feed, concurrently.
     *
     * The hero is a carousel of five, but only its first slide was ever given a
     * logo — every other slide fell back to plain text. Enrichment is done in
     * parallel because five sequential round trips before a page can render is
     * exactly the kind of wait that makes a TV app feel broken.
     */
    suspend fun withLogos(items: List<MediaItem>, limit: Int = 5): List<MediaItem> {
        if (items.isEmpty()) return items
        return coroutineScope {
            val enriched = items.take(limit).map { item ->
                async {
                    if (!item.logoPath.isNullOrBlank()) item
                    else item.copy(logoPath = getMediaLogo(item.mediaType, item.id).getOrNull())
                }
            }.map { it.await() }
            enriched + items.drop(limit)
        }
    }

    suspend fun getMediaTagline(mediaType: String, id: String): Result<String?> = runCatching {
        val mType = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
        val cacheKey = "$mType:$id"
        if (taglineCache.containsKey(cacheKey)) {
            return@runCatching cached(taglineCache, cacheKey)
        }
        val tagline = if (mType == "tv") {
            getTvDetails(id).getOrNull()?.tagline
        } else {
            getMovieDetails(id).getOrNull()?.tagline
        }
        taglineCache[cacheKey] = tagline.orEmpty()
        tagline
    }

    suspend fun getTrending(): Result<List<MediaItem>> = runCatching {
        tmdbApi.getTrending("all", "day", apiKey).results
    }

    suspend fun getTop10Movies(): Result<List<MediaItem>> = runCatching {
        tmdbApi.getTrending("movie", "day", apiKey).results.take(10).map {
            it.copy(mediaType = "movie")
        }
    }

    suspend fun getTop10Tv(): Result<List<MediaItem>> = runCatching {
        tmdbApi.getTrending("tv", "day", apiKey).results.take(10).map {
            it.copy(mediaType = "tv")
        }
    }

    suspend fun getDiscoverMovies(
        withGenres: String? = null,
        withOriginalLanguage: String? = null,
        sortBy: String = "popularity.desc",
        voteAverageGte: Double? = null,
        voteCountGte: Int? = null,
        page: Int = 1
    ): Result<List<MediaItem>> = runCatching {
        tmdbApi.discoverMovie(
            apiKey = apiKey,
            withGenres = withGenres,
            withOriginalLanguage = withOriginalLanguage,
            sortBy = sortBy,
            voteAverageGte = voteAverageGte,
            voteCountGte = voteCountGte,
            page = page
        ).results.map { it.copy(mediaType = "movie") }
    }

    suspend fun getDiscoverTv(
        withGenres: String? = null,
        withOriginalLanguage: String? = null,
        sortBy: String = "popularity.desc",
        voteAverageGte: Double? = null,
        voteCountGte: Int? = null,
        page: Int = 1
    ): Result<List<MediaItem>> = runCatching {
        tmdbApi.discoverTv(
            apiKey = apiKey,
            withGenres = withGenres,
            withOriginalLanguage = withOriginalLanguage,
            sortBy = sortBy,
            voteAverageGte = voteAverageGte,
            voteCountGte = voteCountGte,
            page = page
        ).results.map { it.copy(mediaType = "tv") }
    }

    suspend fun getKdramas(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.discoverTv(
            apiKey = apiKey,
            withOriginalLanguage = "ko",
            sortBy = "popularity.desc",
            page = page
        ).results.map { it.copy(mediaType = "tv") }
    }

    suspend fun getCriticallyAcclaimedMovies(page: Int = 1): Result<List<MediaItem>> = runCatching {
        tmdbApi.discoverMovie(
            apiKey = apiKey,
            voteAverageGte = 7.5,
            voteCountGte = 300,
            sortBy = "vote_average.desc",
            page = page
        ).results.map { it.copy(mediaType = "movie") }
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
        // Same ranking as the standalone lookup, so a title's logo does not
        // change depending on which screen asked for it.
        val bestLogo = raw.images?.logos?.let { pickBestLogo(it) }
        if (bestLogo != null) {
            logoCache["movie:${raw.id}"] = bestLogo
        }
        val logo = bestLogo ?: cached(logoCache, "movie:${raw.id}")
        if (!raw.tagline.isNullOrBlank()) {
            taglineCache["movie:${raw.id}"] = raw.tagline
        }

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
        val bestLogo = raw.images?.logos?.let { pickBestLogo(it) }
        if (bestLogo != null) {
            logoCache["tv:${raw.id}"] = bestLogo
        }
        val logo = bestLogo ?: cached(logoCache, "tv:${raw.id}")
        if (!raw.tagline.isNullOrBlank()) {
            taglineCache["tv:${raw.id}"] = raw.tagline
        }

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


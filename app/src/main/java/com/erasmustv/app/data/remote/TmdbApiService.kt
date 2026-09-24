package com.erasmustv.app.data.remote

import com.erasmustv.app.data.model.Genre
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.MovieDetails
import com.erasmustv.app.data.model.TvDetails
import com.erasmustv.app.data.model.TvSeason
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class TmdbPaginatedResponse<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0
)

@Serializable
data class GenreResponse(
    val genres: List<Genre> = emptyList()
)

@Serializable
data class WatchProvidersResponse(
    val results: Map<String, CountryProviders> = emptyMap()
)

@Serializable
data class CountryProviders(
    val flatrate: List<WatchProviderItem> = emptyList(),
    val rent: List<WatchProviderItem> = emptyList(),
    val buy: List<WatchProviderItem> = emptyList()
)

@Serializable
data class WatchProviderItem(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
    @SerialName("logo_path") val logoPath: String? = null
)

@Serializable
data class TmdbImagesResponse(
    val backdrops: List<TmdbImageItem> = emptyList(),
    val posters: List<TmdbImageItem> = emptyList(),
    val logos: List<TmdbLogoItem> = emptyList()
)

@Serializable
data class TmdbImageItem(
    @SerialName("aspect_ratio") val aspectRatio: Double? = null,
    @SerialName("file_path") val filePath: String,
    val height: Int? = null,
    val width: Int? = null,
    @SerialName("iso_639_1") val iso6391: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null
)

@Serializable
data class TmdbLogoItem(
    @SerialName("aspect_ratio") val aspectRatio: Double? = null,
    @SerialName("file_path") val filePath: String,
    val height: Int? = null,
    val width: Int? = null,
    @SerialName("iso_639_1") val iso6391: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null
)

interface TmdbApiService {

    @GET("{media_type}/{id}/images")
    suspend fun getMediaImages(
        @Path("media_type") mediaType: String,
        @Path("id") id: String,
        @Query("api_key") apiKey: String,
        // Nullable so the filter can be omitted entirely: TMDB then returns
        // artwork in every language it holds, which is the only way to reach a
        // title's original-language logo when no English one exists.
        @Query("include_image_language") imageLanguage: String? = "en,null"
    ): TmdbImagesResponse

    @GET("trending/{media_type}/{time_window}")
    suspend fun getTrending(
        @Path("media_type") mediaType: String = "all",
        @Path("time_window") timeWindow: String = "day",
        @Query("api_key") apiKey: String
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("tv/popular")
    suspend fun getPopularTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("tv/top_rated")
    suspend fun getTopRatedTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "credits,similar,release_dates,images",
        @Query("include_image_language") imageLanguage: String = "en,null"
    ): com.erasmustv.app.data.model.MovieDetailsRaw

    @GET("tv/{series_id}")
    suspend fun getTvDetails(
        @Path("series_id") seriesId: String,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "credits,similar,content_ratings,images,external_ids",
        @Query("include_image_language") imageLanguage: String = "en,null"
    ): com.erasmustv.app.data.model.TvDetailsRaw

    @GET("tv/{series_id}/season/{season_number}")
    suspend fun getTvSeason(
        @Path("series_id") seriesId: String,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String
    ): TvSeason

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("api_key") apiKey: String
    ): GenreResponse

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("api_key") apiKey: String
    ): GenreResponse

    @GET("movie/{movie_id}/watch/providers")
    suspend fun getMovieWatchProviders(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String
    ): WatchProvidersResponse

    @GET("tv/{series_id}/watch/providers")
    suspend fun getTvWatchProviders(
        @Path("series_id") seriesId: String,
        @Query("api_key") apiKey: String
    ): WatchProvidersResponse

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("api_key") apiKey: String,
        @Query("with_genres") withGenres: String? = null,
        @Query("with_watch_providers") withWatchProviders: String? = null,
        @Query("watch_region") watchRegion: String? = "US",
        @Query("with_original_language") withOriginalLanguage: String? = null,
        @Query("with_keywords") withKeywords: String? = null,
        @Query("vote_average.gte") voteAverageGte: Double? = null,
        @Query("vote_count.gte") voteCountGte: Int? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("discover/movie")
    suspend fun discoverMovie(
        @Query("api_key") apiKey: String,
        @Query("with_genres") withGenres: String? = null,
        @Query("with_watch_providers") withWatchProviders: String? = null,
        @Query("watch_region") watchRegion: String? = "US",
        @Query("with_original_language") withOriginalLanguage: String? = null,
        @Query("with_keywords") withKeywords: String? = null,
        @Query("vote_average.gte") voteAverageGte: Double? = null,
        @Query("vote_count.gte") voteCountGte: Int? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>
}

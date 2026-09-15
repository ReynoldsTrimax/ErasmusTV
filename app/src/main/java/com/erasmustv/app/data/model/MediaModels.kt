package com.erasmustv.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: String,
    @SerialName("title") val rawTitle: String? = null,
    @SerialName("name") val rawName: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("media_type") val mediaType: String = "movie",
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("release_date") val rawReleaseDate: String? = null,
    @SerialName("first_air_date") val rawFirstAirDate: String? = null,
    val overview: String? = null,
    val isAnime: Boolean = false,
    val tagline: String? = null
) {
    constructor(
        id: String,
        title: String,
        posterPath: String? = null,
        backdropPath: String? = null,
        mediaType: String = "movie",
        voteAverage: Double? = null,
        releaseDate: String? = null,
        overview: String? = null,
        isAnime: Boolean = false,
        logoPath: String? = null,
        tagline: String? = null
    ) : this(
        id = id,
        rawTitle = title,
        rawName = null,
        posterPath = posterPath,
        backdropPath = backdropPath,
        logoPath = logoPath,
        mediaType = mediaType,
        voteAverage = voteAverage,
        rawReleaseDate = releaseDate,
        rawFirstAirDate = null,
        overview = overview,
        isAnime = isAnime,
        tagline = tagline
    )

    val title: String get() = rawTitle ?: rawName ?: "Untitled"
    val releaseDate: String? get() = rawReleaseDate ?: rawFirstAirDate
    val isTv: Boolean get() = mediaType.equals("tv", ignoreCase = true)
    val year: String? get() = releaseDate?.take(4)
    val ratingFormatted: String? get() = voteAverage?.let { String.format("%.1f", it) }
}

@Serializable
data class MovieDetails(
    val id: String,
    val title: String,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: List<Genre> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    val status: String? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    val cast: List<CastMember> = emptyList(),
    val similar: List<MediaItem> = emptyList(),
    val ratings: List<MediaRating> = emptyList(),
    val whereToWatch: List<WatchProvider> = emptyList(),
    val certification: String? = null,
    val tagline: String? = null
) {
    val year: String? get() = releaseDate?.take(4)
    val durationFormatted: String get() {
        val m = runtime ?: return ""
        val hours = m / 60
        val mins = m % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }
}

@Serializable
data class TvDetails(
    val id: String,
    @SerialName("name") val title: String,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    val seasons: List<TvSeason> = emptyList(),
    val genres: List<Genre> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    val status: String? = null,
    val cast: List<CastMember> = emptyList(),
    val similar: List<MediaItem> = emptyList(),
    val ratings: List<MediaRating> = emptyList(),
    val whereToWatch: List<WatchProvider> = emptyList(),
    val certification: String? = null,
    val tagline: String? = null
) {
    val year: String? get() = firstAirDate?.take(4)
}

@Serializable
data class CreditsResponse(
    val cast: List<CastMember> = emptyList()
)

@Serializable
data class MovieDetailsRaw(
    val id: String,
    val title: String,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: List<Genre> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    val status: String? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    val credits: CreditsResponse? = null,
    val similar: com.erasmustv.app.data.remote.TmdbPaginatedResponse<MediaItem>? = null,
    val images: com.erasmustv.app.data.remote.TmdbImagesResponse? = null,
    val tagline: String? = null
)

@Serializable
data class TvDetailsRaw(
    val id: String,
    @SerialName("name") val title: String,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    val seasons: List<TvSeason> = emptyList(),
    val genres: List<Genre> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val status: String? = null,
    val credits: CreditsResponse? = null,
    val similar: com.erasmustv.app.data.remote.TmdbPaginatedResponse<MediaItem>? = null,
    val images: com.erasmustv.app.data.remote.TmdbImagesResponse? = null,
    val tagline: String? = null
)

@Serializable
data class TvSeason(
    val id: String,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    val episodes: List<TvEpisode> = emptyList()
)

@Serializable
data class TvEpisode(
    val id: String,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_number") val episodeNumber: Int,
    val name: String,
    val overview: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("runtime") val runtime: Int? = null
) {
    val episodeCode: String get() = "S${seasonNumber} · E${episodeNumber}"
    val durationFormatted: String get() {
        val m = runtime ?: return ""
        return "${m}m"
    }
}

@Serializable
data class CastMember(
    val id: String,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class Genre(
    val id: Int,
    val name: String
)

@Serializable
data class MediaRating(
    val provider: String,
    val label: String,
    val value: String,
    val scale: Int = 10
)

@Serializable
data class WatchProvider(
    @SerialName("provider_id") val id: Int,
    @SerialName("provider_name") val name: String,
    @SerialName("logo_path") val logoPath: String? = null,
    val offerType: String = "flatrate"
)

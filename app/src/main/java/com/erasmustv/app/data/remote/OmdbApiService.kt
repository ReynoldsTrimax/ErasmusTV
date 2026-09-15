package com.erasmustv.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class OmdbRating(
    @SerialName("Source") val source: String = "",
    @SerialName("Value") val value: String = ""
)

@Serializable
data class OmdbResponse(
    @SerialName("Response") val response: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("Year") val year: String? = null,
    @SerialName("imdbID") val imdbId: String? = null,
    val imdbRating: String? = null,
    val imdbVotes: String? = null,
    @SerialName("Metascore") val metascore: String? = null,
    @SerialName("Ratings") val ratings: List<OmdbRating> = emptyList(),
    @SerialName("Error") val error: String? = null
)

interface OmdbApiService {

    @GET("/")
    suspend fun getByImdbId(
        @Query("i") imdbId: String,
        @Query("apikey") apiKey: String,
        @Query("plot") plot: String = "short"
    ): OmdbResponse

    @GET("/")
    suspend fun getByTitle(
        @Query("t") title: String,
        @Query("apikey") apiKey: String,
        @Query("y") year: String? = null,
        @Query("type") type: String? = null,
        @Query("plot") plot: String = "short"
    ): OmdbResponse
}

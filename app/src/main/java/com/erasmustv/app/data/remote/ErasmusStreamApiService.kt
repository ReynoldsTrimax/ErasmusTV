package com.erasmustv.app.data.remote

import com.erasmustv.app.data.model.DirectStreamResult
import com.erasmustv.app.data.model.SubtitleTrack
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class SubtitlesResponse(
    val tracks: List<SubtitleTrack> = emptyList()
)

interface ErasmusStreamApiService {

    @GET("api/stream/direct")
    suspend fun extractDirectStream(
        @Query("type") type: String,
        @Query("id") tmdbId: String,
        @Query("server") server: String = "lisbon",
        @Query("title") title: String? = null,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null
    ): DirectStreamResult

    @GET("api/stream/subs")
    suspend fun getSubtitles(
        @Query("id") tmdbId: String,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null
    ): SubtitlesResponse
}

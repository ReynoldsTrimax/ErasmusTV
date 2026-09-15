package com.erasmustv.app.data.remote

import com.erasmustv.app.data.model.AuthSession
import com.erasmustv.app.data.model.LoginRequest
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.model.WatchlistItemEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    // Auth
    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthSession

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Body request: com.erasmustv.app.data.model.RefreshTokenRequest
    ): AuthSession

    @POST("auth/v1/token?grant_type=id_token")
    suspend fun loginWithIdToken(
        @Body request: com.erasmustv.app.data.model.IdTokenLoginRequest
    ): AuthSession

    @GET("auth/v1/user")
    suspend fun getUser(): com.erasmustv.app.data.model.AuthUser

    @POST("auth/v1/logout")
    suspend fun logout(): Response<Unit>

    // Profiles (watch_profiles table with RLS)
    @GET("rest/v1/watch_profiles?select=*&order=created_at.asc")
    suspend fun getWatchProfiles(): List<WatchProfile>

    @POST("rest/v1/watch_profiles")
    suspend fun createWatchProfile(
        @Body profile: com.erasmustv.app.data.model.CreateProfileRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<WatchProfile>

    @PATCH("rest/v1/watch_profiles")
    suspend fun updateWatchProfile(
        @Query("id") idFilter: String, // format "eq.UUID"
        @Body profile: WatchProfile,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<WatchProfile>

    @DELETE("rest/v1/watch_profiles")
    suspend fun deleteWatchProfile(
        @Query("id") idFilter: String // format "eq.UUID"
    ): Response<Unit>

    // Watchlist (watchlist_items table with RLS)
    @GET("rest/v1/watchlist_items?select=*&order=created_at.desc")
    suspend fun getWatchlist(
        @Query("profile_id") profileIdFilter: String // format "eq.UUID"
    ): List<WatchlistItemEntity>

    @POST("rest/v1/watchlist_items")
    suspend fun addToWatchlist(
        @Body item: WatchlistItemEntity,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<WatchlistItemEntity>

    @DELETE("rest/v1/watchlist_items")
    suspend fun removeFromWatchlist(
        @Query("profile_id") profileIdFilter: String,
        @Query("media_type") mediaTypeFilter: String,
        @Query("tmdb_id") tmdbIdFilter: String
    ): Response<Unit>
}

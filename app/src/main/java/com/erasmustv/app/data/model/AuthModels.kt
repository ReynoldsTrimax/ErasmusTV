package com.erasmustv.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val user: AuthUser? = null
) {
    val expiryTimestampMs: Long
        get() {
            if (expiresAt != null && expiresAt > 0) {
                return expiresAt * 1000L
            }
            val seconds = expiresIn ?: 3600L
            return System.currentTimeMillis() + (seconds * 1000L)
        }

    fun isExpired(bufferMs: Long = 60_000L): Boolean {
        return System.currentTimeMillis() + bufferMs >= expiryTimestampMs
    }
}

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: Map<String, String>? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class IdTokenLoginRequest(
    val provider: String,
    @SerialName("id_token") val idToken: String,
    val nonce: String? = null
)

@Serializable
data class SupabaseAuthError(
    val code: Int? = null,
    @SerialName("error_code") val errorCode: String? = null,
    val msg: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
) {
    val userFacingMessage: String
        get() {
            val code = errorCode ?: error ?: ""
            val message = msg ?: errorDescription ?: ""
            return when {
                code == "invalid_credentials" || message.contains("Invalid login credentials", ignoreCase = true) ->
                    "Incorrect email or password. Please try again."
                code == "email_not_confirmed" || message.contains("not confirmed", ignoreCase = true) ->
                    "Your email has not been confirmed. Please check your inbox."
                code == "over_request_rate_limit" || message.contains("rate limit", ignoreCase = true) ->
                    "Too many attempts. Please wait a minute and try again."
                code == "user_not_found" ->
                    "No account found with this email."
                message.isNotBlank() ->
                    message
                else ->
                    "Unable to sign in. Please verify your credentials."
            }
        }
}

sealed interface SessionCheckResult {
    data object Checking : SessionCheckResult
    data class Authenticated(val user: AuthUser, val accessToken: String) : SessionCheckResult
    data object Guest : SessionCheckResult
    data object Unauthenticated : SessionCheckResult
}

@Serializable
data class WatchlistItemEntity(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("profile_id") val profileId: String = "",
    @SerialName("media_type") val mediaType: String,
    @SerialName("tmdb_id") val tmdbId: String,
    val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

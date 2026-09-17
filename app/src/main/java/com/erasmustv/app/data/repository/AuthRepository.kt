package com.erasmustv.app.data.repository

import com.erasmustv.app.data.local.SessionManager
import com.erasmustv.app.data.model.AuthSession
import com.erasmustv.app.data.model.AuthUser
import com.erasmustv.app.data.model.LoginRequest
import com.erasmustv.app.data.model.RefreshTokenRequest
import com.erasmustv.app.data.model.SessionCheckResult
import com.erasmustv.app.data.model.SupabaseAuthError
import com.erasmustv.app.data.remote.SupabaseApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val supabaseApi: SupabaseApiService,
    private val sessionManager: SessionManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    val isLoggedInFlow: Flow<Boolean> = sessionManager.isLoggedInFlow
    val isGuestFlow: Flow<Boolean> = sessionManager.isGuestFlow
    val userEmailFlow: Flow<String?> = sessionManager.userEmailFlow

    suspend fun login(email: String, password: String): Result<AuthSession> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your email and password."))
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        return try {
            val session = supabaseApi.login(LoginRequest(email = trimmedEmail, password = password))
            sessionManager.saveSession(session, isGuest = false)
            Result.success(session)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val friendlyMessage = parseSupabaseError(errorBody) ?: when (e.code()) {
                400, 401 -> "Incorrect email or password. Please try again."
                429 -> "Too many attempts. Please wait a moment and try again."
                500, 502, 503 -> "Erasmus server is currently unavailable. Please try again later."
                else -> "Authentication failed (${e.code()}). Please verify your credentials."
            }
            Result.failure(Exception(friendlyMessage))
        } catch (e: IOException) {
            Result.failure(Exception("Unable to connect to the Erasmus server. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "An unexpected error occurred during sign in."))
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<AuthSession> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Google ID token is missing."))
        }

        return try {
            val session = supabaseApi.loginWithIdToken(
                com.erasmustv.app.data.model.IdTokenLoginRequest(provider = "google", idToken = idToken)
            )
            sessionManager.saveSession(session, isGuest = false)
            Result.success(session)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val friendlyMessage = parseSupabaseError(errorBody) ?: when (e.code()) {
                400, 401 -> "Google authentication failed. Please try again."
                else -> "Google sign in failed (${e.code()})."
            }
            Result.failure(Exception(friendlyMessage))
        } catch (e: IOException) {
            Result.failure(Exception("Unable to connect to the Erasmus server. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Google sign in failed."))
        }
    }

    suspend fun checkSession(): SessionCheckResult {
        if (sessionManager.isGuest()) {
            return SessionCheckResult.Guest
        }

        val accessToken = sessionManager.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            return SessionCheckResult.Unauthenticated
        }

        val refreshToken = sessionManager.getRefreshToken()
        val isExpired = sessionManager.isTokenExpired(bufferMs = 60_000L)

        if (isExpired && !refreshToken.isNullOrBlank()) {
            // Attempt to refresh the session
            return tryRefresh(refreshToken)
        }

        // Token is not expired according to timestamp. Verify with Supabase.
        return try {
            val user = supabaseApi.getUser()
            SessionCheckResult.Authenticated(user = user, accessToken = accessToken)
        } catch (e: HttpException) {
            if ((e.code() == 401 || e.code() == 403) && !refreshToken.isNullOrBlank()) {
                // Token may have been invalidated on the server, attempt refresh
                tryRefresh(refreshToken)
            } else if (e.code() == 401 || e.code() == 403) {
                sessionManager.clearSession()
                SessionCheckResult.Unauthenticated
            } else {
                // Other HTTP error (e.g. 500) — allow cached session
                val cachedUser = AuthUser(
                    id = sessionManager.getUserId() ?: "",
                    email = sessionManager.getUserEmail()
                )
                SessionCheckResult.Authenticated(user = cachedUser, accessToken = accessToken)
            }
        } catch (_: IOException) {
            // Offline / network issue — allow cached session if not expired
            val cachedUser = AuthUser(
                id = sessionManager.getUserId() ?: "",
                email = sessionManager.getUserEmail()
            )
            SessionCheckResult.Authenticated(user = cachedUser, accessToken = accessToken)
        } catch (_: Exception) {
            sessionManager.clearSession()
            SessionCheckResult.Unauthenticated
        }
    }

    private suspend fun tryRefresh(refreshToken: String): SessionCheckResult {
        return try {
            val refreshedSession = supabaseApi.refreshToken(RefreshTokenRequest(refreshToken = refreshToken))
            sessionManager.saveSession(refreshedSession, isGuest = false)
            val user = refreshedSession.user ?: AuthUser(
                id = sessionManager.getUserId() ?: "",
                email = sessionManager.getUserEmail()
            )
            SessionCheckResult.Authenticated(user = user, accessToken = refreshedSession.accessToken)
        } catch (_: Exception) {
            // Refresh token revoked or invalid
            sessionManager.clearSession()
            SessionCheckResult.Unauthenticated
        }
    }

    suspend fun logout() {
        try {
            if (!sessionManager.isGuest()) {
                supabaseApi.logout()
            }
        } catch (_: Exception) {
            // Best effort logout on server
        } finally {
            sessionManager.clearSession()
        }
    }

    suspend fun continueAsGuest(): Result<AuthSession> {
        return try {
            sessionManager.saveGuestSession()
            val guestSession = AuthSession(
                accessToken = "",
                user = AuthUser(
                    id = "guest_user_erasmus",
                    email = "guest@erasmustv.app"
                )
            )
            Result.success(guestSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserId(): String? = sessionManager.getUserId()
    suspend fun getUserEmail(): String? = sessionManager.getUserEmail()
    suspend fun isGuest(): Boolean = sessionManager.isGuest()

    private fun parseSupabaseError(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val err = json.decodeFromString<SupabaseAuthError>(errorBody)
            err.userFacingMessage
        } catch (_: Exception) {
            null
        }
    }
}


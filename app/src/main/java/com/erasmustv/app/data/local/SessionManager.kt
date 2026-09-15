package com.erasmustv.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.erasmustv.app.data.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "erasmus_session")

class SessionManager(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val EXPIRES_AT = longPreferencesKey("expires_at")
    }

    val accessTokenFlow: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[Keys.ACCESS_TOKEN]
    }

    val userIdFlow: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[Keys.USER_ID]
    }

    val userEmailFlow: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[Keys.USER_EMAIL]
    }

    val isGuestFlow: Flow<Boolean> = context.sessionDataStore.data.map { prefs ->
        prefs[Keys.IS_GUEST] ?: false
    }

    val isLoggedInFlow: Flow<Boolean> = context.sessionDataStore.data.map { prefs ->
        val isGuest = prefs[Keys.IS_GUEST] ?: false
        val hasToken = !prefs[Keys.ACCESS_TOKEN].isNullOrBlank()
        isGuest || hasToken
    }

    suspend fun getAccessToken(): String? {
        return context.sessionDataStore.data.first()[Keys.ACCESS_TOKEN]
    }

    suspend fun getRefreshToken(): String? {
        return context.sessionDataStore.data.first()[Keys.REFRESH_TOKEN]
    }

    suspend fun getExpiresAt(): Long? {
        return context.sessionDataStore.data.first()[Keys.EXPIRES_AT]
    }

    suspend fun getUserId(): String? {
        return context.sessionDataStore.data.first()[Keys.USER_ID]
    }

    suspend fun getUserEmail(): String? {
        return context.sessionDataStore.data.first()[Keys.USER_EMAIL]
    }

    suspend fun isGuest(): Boolean {
        return context.sessionDataStore.data.first()[Keys.IS_GUEST] ?: false
    }

    suspend fun isTokenExpired(bufferMs: Long = 60_000L): Boolean {
        val expiresAt = getExpiresAt() ?: return false
        if (expiresAt <= 0) return false
        return System.currentTimeMillis() + bufferMs >= expiresAt
    }

    suspend fun saveSession(session: AuthSession, isGuest: Boolean = false) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_GUEST] = isGuest
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            session.refreshToken?.let { prefs[Keys.REFRESH_TOKEN] = it }
            prefs[Keys.EXPIRES_AT] = session.expiryTimestampMs
            session.user?.let { user ->
                prefs[Keys.USER_ID] = user.id
                user.email?.let { prefs[Keys.USER_EMAIL] = it }
            }
        }
    }

    suspend fun saveGuestSession() {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_GUEST] = true
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs[Keys.USER_ID] = "guest_user_erasmus"
            prefs[Keys.USER_EMAIL] = "guest@erasmustv.app"
            prefs.remove(Keys.EXPIRES_AT)
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}


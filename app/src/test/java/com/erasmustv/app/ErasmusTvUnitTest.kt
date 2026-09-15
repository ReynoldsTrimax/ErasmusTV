package com.erasmustv.app

import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.data.model.ContinueWatchingItem
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.MovieDetails
import com.erasmustv.app.data.model.TvEpisode
import com.erasmustv.app.data.model.WatchProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ErasmusTvUnitTest {

    @Test
    fun testMediaItemYearAndRating() {
        val item = MediaItem(
            id = "123",
            title = "Inception",
            releaseDate = "2010-07-16",
            voteAverage = 8.357
        )
        assertEquals("2010", item.year)
        assertEquals("8.4", item.ratingFormatted)
        assertFalse(item.isTv)
    }

    @Test
    fun testTvItemAttributes() {
        val item = MediaItem(
            id = "456",
            title = "Breaking Bad",
            mediaType = "tv",
            releaseDate = "2008-01-20"
        )
        assertTrue(item.isTv)
        assertEquals("2008", item.year)
    }

    @Test
    fun testMovieDetailsDurationFormatted() {
        val movie = MovieDetails(
            id = "1",
            title = "Movie",
            runtime = 148
        )
        assertEquals("2h 28m", movie.durationFormatted)

        val shortFilm = MovieDetails(
            id = "2",
            title = "Short",
            runtime = 45
        )
        assertEquals("45m", shortFilm.durationFormatted)
    }

    @Test
    fun testTvEpisodeCode() {
        val ep = TvEpisode(
            id = "ep1",
            seasonNumber = 2,
            episodeNumber = 7,
            name = "One Minute",
            runtime = 47
        )
        assertEquals("S2 · E7", ep.episodeCode)
        assertEquals("47m", ep.durationFormatted)
    }

    @Test
    fun testWatchProfileAgeDerivation() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val profileAdult = WatchProfile(
            id = "p1",
            name = "Adult",
            birthYear = currentYear - 25
        )
        assertEquals(25, profileAdult.derivedAge)
        assertFalse(profileAdult.isKidsProfile)

        val profileKid = WatchProfile(
            id = "p2",
            name = "Kid",
            birthYear = currentYear - 8
        )
        assertEquals(8, profileKid.derivedAge)
        assertTrue(profileKid.isKidsProfile)
    }

    @Test
    fun testContinueWatchingProgressCalculations() {
        val item = ContinueWatchingItem(
            mediaType = "movie",
            tmdbId = "100",
            title = "Dune",
            seconds = 1800,
            duration = 7200,
            updatedAt = System.currentTimeMillis()
        )
        assertEquals(7200L, item.duration)
        assertEquals(0.25f, item.progressRatio, 0.001f)
        assertEquals("90m left", item.resumeLabel)
    }

    @Test
    fun testTvSeasonJsonDecoding() {
        val jsonStr = """
        {
            "_id": "5256c89f19c2956ff6046d47",
            "air_date": "2011-04-17",
            "episodes": [
                {
                    "air_date": "2011-04-17",
                    "episode_number": 1,
                    "id": 63056,
                    "name": "Winter Is Coming",
                    "overview": "Ned Stark is torn.",
                    "runtime": 62,
                    "season_number": 1,
                    "still_path": "/path.jpg"
                }
            ],
            "name": "Season 1",
            "overview": "",
            "id": 3624,
            "poster_path": "/poster.jpg",
            "season_number": 1
        }
        """.trimIndent()

        // Test decoding
        try {
            val season = com.erasmustv.app.core.network.NetworkClient.json.decodeFromString<com.erasmustv.app.data.model.TvSeason>(jsonStr)
            assertEquals("Season 1", season.name)
            assertEquals(1, season.episodes.size)
            assertEquals("S1 · E1", season.episodes[0].episodeCode)
        } catch (e: Exception) {
            println("Caught exception during TvSeason decode: ${e.message}")
            throw e
        }
    }

    @Test
    fun testAppConfigImageUrls() {
        val fullUrl = "https://example.com/poster.jpg"
        assertEquals(fullUrl, AppConfig.posterUrl(fullUrl))

        val relativePath = "/abc123xyz.jpg"
        assertEquals("https://image.tmdb.org/t/p/w500/abc123xyz.jpg", AppConfig.posterUrl(relativePath))
        assertEquals("https://image.tmdb.org/t/p/w1280/abc123xyz.jpg", AppConfig.backdropUrl(relativePath))
        assertEquals("https://image.tmdb.org/t/p/w780/abc123xyz.jpg", AppConfig.stillUrl(relativePath))
        assertNull(AppConfig.posterUrl(null))
    }

    @Test
    fun testCinejoyBase64RoundTrip() {
        val original = "Hello Erasmus TV Streaming Cluster!".toByteArray(Charsets.UTF_8)
        val encoded = com.erasmustv.app.data.remote.CinejoyStreamResolver.b64UrlEncode(original)
        assertFalse(encoded.contains("+"))
        assertFalse(encoded.contains("/"))
        assertFalse(encoded.contains("="))
        val decoded = com.erasmustv.app.data.remote.CinejoyStreamResolver.b64Decode(encoded)
        assertEquals(String(original, Charsets.UTF_8), String(decoded, Charsets.UTF_8))
    }

    @Test
    fun testSheguServerNameMapping() {
        val resolver = com.erasmustv.app.data.remote.CinejoyStreamResolver()
        assertEquals("Lisbon", resolver.sheguServerName("lisbon"))
        assertEquals("Sakura", resolver.sheguServerName("sakura"))
        assertEquals("Nebula", resolver.sheguServerName("nebula"))
        assertEquals("Athens", resolver.sheguServerName("athens"))
        assertEquals("Lisbon", resolver.sheguServerName("unknown_server"))
    }

    @Test
    fun testAuthSessionExpiryDetection() {
        val nowSeconds = System.currentTimeMillis() / 1000L

        // Session expiring in 30 seconds (within 60s buffer) -> should be considered expired
        val sessionNearExpiry = com.erasmustv.app.data.model.AuthSession(
            accessToken = "token_near_expiry",
            expiresAt = nowSeconds + 30L
        )
        assertTrue(sessionNearExpiry.isExpired(bufferMs = 60_000L))

        // Session expiring in 2 hours -> not expired
        val sessionValid = com.erasmustv.app.data.model.AuthSession(
            accessToken = "token_valid",
            expiresAt = nowSeconds + 7200L
        )
        assertFalse(sessionValid.isExpired(bufferMs = 60_000L))

        // Session with expires_in = 3600
        val sessionWithExpiresIn = com.erasmustv.app.data.model.AuthSession(
            accessToken = "token_expires_in",
            expiresIn = 3600L
        )
        assertFalse(sessionWithExpiresIn.isExpired(bufferMs = 60_000L))
    }

    @Test
    fun testSupabaseAuthErrorUserFacingMessages() {
        val invalidCreds = com.erasmustv.app.data.model.SupabaseAuthError(
            errorCode = "invalid_credentials",
            msg = "Invalid login credentials"
        )
        assertEquals("Incorrect email or password. Please try again.", invalidCreds.userFacingMessage)

        val rateLimit = com.erasmustv.app.data.model.SupabaseAuthError(
            errorCode = "over_request_rate_limit",
            msg = "Rate limit exceeded"
        )
        assertEquals("Too many attempts. Please wait a minute and try again.", rateLimit.userFacingMessage)

        val unconfirmed = com.erasmustv.app.data.model.SupabaseAuthError(
            msg = "Email not confirmed"
        )
        assertEquals("Your email has not been confirmed. Please check your inbox.", unconfirmed.userFacingMessage)

        val userNotFound = com.erasmustv.app.data.model.SupabaseAuthError(
            errorCode = "user_not_found"
        )
        assertEquals("No account found with this email.", userNotFound.userFacingMessage)
    }

    @Test
    fun testCreateProfileRequestSerialization() {
        val req = com.erasmustv.app.data.model.CreateProfileRequest(
            userId = "12345-6789",
            name = "Living Room TV",
            avatarKey = "crimson",
            birthYear = 1995
        )
        val json = com.erasmustv.app.core.network.NetworkClient.json.encodeToString(
            com.erasmustv.app.data.model.CreateProfileRequest.serializer(),
            req
        )
        assertTrue(json.contains("\"user_id\":\"12345-6789\""))
        assertTrue(json.contains("\"name\":\"Living Room TV\""))
        assertTrue(json.contains("\"avatar_key\":\"crimson\""))
        assertTrue(json.contains("\"birth_year\":1995"))
        assertFalse(json.contains("\"id\""))
    }

    @Test
    fun testGoogleIdTokenRequestSerialization() {
        val req = com.erasmustv.app.data.model.IdTokenLoginRequest(
            provider = "google",
            idToken = "mock_google_id_token_123"
        )
        val json = com.erasmustv.app.core.network.NetworkClient.json.encodeToString(
            com.erasmustv.app.data.model.IdTokenLoginRequest.serializer(),
            req
        )
        assertTrue(json.contains("\"provider\":\"google\""))
        assertTrue(json.contains("\"id_token\":\"mock_google_id_token_123\""))
    }

    @Test
    fun testSubtitleTrackAndCaptionMimeDefaults() {
        val subTrack = com.erasmustv.app.data.model.SubtitleTrack(
            label = "English",
            language = "en",
            url = "https://example.com/sub.srt"
        )
        assertEquals("application/x-subrip", subTrack.mimeType)

        val caption = com.erasmustv.app.data.model.CinejoyCaption(
            label = "English",
            language = "en",
            url = "https://example.com/sub.vtt"
        )
        assertEquals("text/vtt", caption.mimeType)
    }
}



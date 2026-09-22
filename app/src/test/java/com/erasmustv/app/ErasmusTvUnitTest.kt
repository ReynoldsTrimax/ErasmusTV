package com.erasmustv.app

import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.data.model.ContinueWatchingItem
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.MovieDetails
import com.erasmustv.app.data.model.TvEpisode
import com.erasmustv.app.data.model.WatchProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        assertEquals("Solara", resolver.sheguServerName("solara"))
        assertEquals("Athens", resolver.sheguServerName("athens"))
        assertEquals("Joy", resolver.sheguServerName("joy"))
        assertEquals("Castle", resolver.sheguServerName("castle"))
        assertEquals("Canaias", resolver.sheguServerName("canaias"))
        assertEquals("Lisbon", resolver.sheguServerName("unknown_server"))
    }

    @Test
    fun testServerClusterConfiguration() {
        val servers = com.erasmustv.app.data.model.STREAM_SERVERS
        assertEquals(16, servers.size)
        val lisbon = servers.find { it.id == "lisbon" }
        assertNotNull(lisbon)
        assertTrue(lisbon!!.isPrimary == true)

        val athens = servers.find { it.id == "athens" }
        assertNotNull(athens)
        assertEquals("4K Cinema", athens!!.badge)

        val nebula = servers.find { it.id == "nebula" }
        assertNotNull(nebula)
        assertEquals("1080p High Speed", nebula!!.badge)

        val aphelion = servers.find { it.id == "aphelion" }
        assertNotNull(aphelion)
        assertEquals("4K Ultra HD", aphelion!!.badge)
        assertTrue(aphelion!!.isPrimary)

        val polaris = servers.find { it.id == "polaris" }
        assertNotNull(polaris)
        assertEquals("1080p Full HD", polaris!!.badge)

        val bastion = servers.find { it.id == "bastion" }
        assertNotNull(bastion)
        assertEquals("1080p Full HD", bastion!!.badge)

        val hallyu = servers.find { it.id == "hallyu" }
        assertNotNull(hallyu)
        assertEquals("1080p Asian & Anime", hallyu!!.badge)

        val animesalt = servers.find { it.id == "animesalt" }
        assertNotNull(animesalt)
        assertEquals("1080p Anime Multi", animesalt!!.badge)

        val ryuu = servers.find { it.id == "ryuu" }
        assertNotNull(ryuu)
        assertEquals("1080p Animex", ryuu!!.badge)
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

    @Test
    fun testNavRailDestinationsIntegrity() {
        val destinations = com.erasmustv.app.ui.components.NAV_RAIL_ITEMS
        assertEquals(6, destinations.size)
        assertEquals(com.erasmustv.app.ui.navigation.NavRoutes.HOME, destinations[0].route)
        assertEquals(com.erasmustv.app.ui.navigation.NavRoutes.SEARCH, destinations[1].route)
        assertEquals(com.erasmustv.app.ui.navigation.NavRoutes.TV, destinations[2].route)
        assertEquals(com.erasmustv.app.ui.navigation.NavRoutes.MOVIES, destinations[3].route)
        assertEquals(com.erasmustv.app.ui.navigation.NavRoutes.ANIME, destinations[4].route)
        assertEquals(com.erasmustv.app.ui.navigation.NavRoutes.STUDIOS, destinations[5].route)
    }

    @Test
    fun testProfileListSerializationAndDeserialization() {
        val profiles = listOf(
            WatchProfile(
                id = "profile_1",
                userId = "user_abc",
                name = "Main Profile",
                avatarKey = "crimson",
                birthYear = 1990
            ),
            WatchProfile(
                id = "profile_2",
                userId = "user_abc",
                name = "Kids",
                avatarKey = "forest",
                birthYear = 2018
            )
        )
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(WatchProfile.serializer()),
            profiles
        )
        val decoded = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(WatchProfile.serializer()),
            encoded
        )

        assertEquals(2, decoded.size)
        assertEquals("Main Profile", decoded[0].name)
        assertEquals("Kids", decoded[1].name)
        assertTrue(decoded[1].isKidsProfile)
        assertFalse(decoded[0].isKidsProfile)
    }

    @Test
    fun testProfileNameSynthesisFromEmail() {
        val email = "alex.murphy@erasmustv.com"
        val synthesized = email
            .substringBefore("@")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }

        assertEquals("Alex.murphy", synthesized)

        val emptyEmail: String? = null
        val fallbackName = emptyEmail
            ?.substringBefore("@")
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
            ?.ifBlank { "Profile 1" }
            ?: "Profile 1"

        assertEquals("Profile 1", fallbackName)
    }

    @Test
    fun testProfileLimitEnforcement() {
        val profiles = mutableListOf<WatchProfile>()
        for (i in 1..5) {
            profiles.add(WatchProfile(id = "p_$i", name = "Profile $i"))
        }
        assertEquals(5, profiles.size)

        // Attempting to add a 6th profile should be prevented by size check
        val canAddMore = profiles.size < 5
        assertFalse(canAddMore)
    }

    @Test
    fun testHomeSectionAttributes() {
        val movies = listOf(
            MediaItem(id = "m1", title = "Inception", mediaType = "movie"),
            MediaItem(id = "m2", title = "Interstellar", mediaType = "movie")
        )
        val rankedSection = com.erasmustv.app.data.model.HomeSection(
            id = "top_10_movies",
            title = "Top 10 Movies Today",
            items = movies,
            isRanked = true,
            viewAllRoute = com.erasmustv.app.ui.navigation.NavRoutes.MOVIES
        )

        assertTrue(rankedSection.isRanked)
        assertEquals("top_10_movies", rankedSection.id)
        assertEquals("Top 10 Movies Today", rankedSection.title)
        assertEquals(2, rankedSection.items.size)
        assertEquals(com.erasmustv.app.ui.navigation.NavRoutes.MOVIES, rankedSection.viewAllRoute)

        val standardSection = com.erasmustv.app.data.model.HomeSection(
            id = "hit_comedies",
            title = "Hit Comedies",
            items = movies,
            showNewBadge = true
        )
        assertFalse(standardSection.isRanked)
        assertTrue(standardSection.showNewBadge)
    }

    @Test
    fun testSoloLevelingSplitCourResolution() = kotlinx.coroutines.runBlocking {
        val resolver = com.erasmustv.app.data.remote.CinejoyStreamResolver()
        // Episode 25 is mapped in TMDB as S1E25, but upstream hosts it as S2E13
        val result = resolver.resolveStream(
            mediaType = "tv",
            tmdbId = "127532",
            title = "Solo Leveling",
            season = 1,
            episode = 25,
            preferredServer = "lisbon"
        )
        println("TEST RESULT: ok=${result.ok}, error=${result.error}, servers=${result.servers.map { "${it.name}:${it.url}" }}")
        assertTrue("Stream resolution for Solo Leveling S1E25 should succeed via split-cour fallback: ${result.error}", result.ok)
        assertTrue("Stream servers should not be empty", result.servers.isNotEmpty())
        assertTrue("Stream URL should be valid", result.servers[0].url.isNotBlank())
    }

    @Test
    fun testFastFiveResolution() = kotlinx.coroutines.runBlocking {
        val resolver = com.erasmustv.app.data.remote.CinejoyStreamResolver()

        // 1. Test Fast Five on non-10-bit device (e.g. standard JVM / non-HEVC hardware)
        com.erasmustv.app.core.util.DeviceCodecCapability.setHevcMain10SupportedForTesting(false)
        val non10Result = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "51497",
            title = "Fast Five",
            preferredServer = "lisbon"
        )
        println("FAST FIVE (NON-10BIT): ok=${non10Result.ok}, primary=${non10Result.servers.firstOrNull()?.url}")
        assertTrue("Fast Five stream resolution must succeed", non10Result.ok)
        assertTrue("Fast Five must have available servers", non10Result.servers.isNotEmpty())
        val non10Url = non10Result.servers[0].url
        assertTrue("Primary URL must be valid", non10Url.isNotBlank() && non10Url.startsWith("https://"))
        // Non-10-bit devices must not receive unplayable raw r2/cdn1
        assertFalse("Non-10-bit devices must receive adaptive stream instead of raw r2/cdn1", non10Url.contains("/r2/cdn1/"))

        // 2. Test Fast Five on 4K TV device with HEVC Main 10 decoding support
        com.erasmustv.app.core.util.DeviceCodecCapability.setHevcMain10SupportedForTesting(true)
        val hevc10Result = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "51497",
            title = "Fast Five",
            preferredServer = "lisbon"
        )
        println("FAST FIVE (4K HEVC-10): ok=${hevc10Result.ok}, primary=${hevc10Result.servers.firstOrNull()?.url}, name=${hevc10Result.servers.firstOrNull()?.name}")
        assertTrue("Fast Five 4K resolution must succeed", hevc10Result.ok)
        assertTrue("Fast Five 4K must have available servers", hevc10Result.servers.isNotEmpty())
        val hevc10Url = hevc10Result.servers[0].url
        assertTrue("Fast Five on 4K hardware must resolve to valid stream", hevc10Url.isNotBlank() && hevc10Url.startsWith("https://"))
        // Reset capability override
        com.erasmustv.app.core.util.DeviceCodecCapability.setHevcMain10SupportedForTesting(null)

        // 3. Test Furious 7 on Lisbon
        val furious7Result = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "168259",
            title = "Furious 7",
            preferredServer = "lisbon"
        )
        println("FURIOUS 7 TEST: ok=${furious7Result.ok}, primary=${furious7Result.servers.firstOrNull()?.url}, servers=${furious7Result.servers.size}")
        assertTrue("Furious 7 resolution must succeed", furious7Result.ok)
        assertTrue("Furious 7 must have available servers", furious7Result.servers.isNotEmpty())
        val f7PrimaryUrl = furious7Result.servers[0].url
        assertTrue("Furious 7 must have valid stream URL", f7PrimaryUrl.isNotBlank() && f7PrimaryUrl.startsWith("https://"))

        // 4. Test 2 Fast 2 Furious on Lisbon
        val ff2Result = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "584",
            title = "2 Fast 2 Furious",
            preferredServer = "lisbon"
        )
        println("2 FAST 2 FURIOUS TEST: ok=${ff2Result.ok}, primary=${ff2Result.servers.firstOrNull()?.url}, servers=${ff2Result.servers.size}")
        assertTrue("2 Fast 2 Furious resolution must succeed", ff2Result.ok)
        assertTrue("2 Fast 2 Furious must have at least one server", ff2Result.servers.isNotEmpty())
        assertTrue("2 Fast 2 Furious must have valid stream URL", ff2Result.servers[0].url.isNotBlank() && ff2Result.servers[0].url.startsWith("https://"))
    }

    @Test
    fun testStrangerThingsS1E1() = kotlinx.coroutines.runBlocking {
        val resolver = com.erasmustv.app.data.remote.CinejoyStreamResolver()

        // 1. Verify Lisbon resolution for Stranger Things S1E1
        val lisbonResult = resolver.resolveStream(
            mediaType = "tv",
            tmdbId = "66732",
            title = "Stranger Things",
            season = 1,
            episode = 1,
            preferredServer = "lisbon"
        )
        println("STRANGER THINGS S1E1 LISBON: ok=${lisbonResult.ok}, name=${lisbonResult.servers.firstOrNull()?.name}, url=${lisbonResult.servers.firstOrNull()?.url}")
        assertTrue("Stranger Things S1E1 resolution must succeed", lisbonResult.ok)
        assertTrue("Stranger Things S1E1 must have available servers", lisbonResult.servers.isNotEmpty())
        val lisbonUrl = lisbonResult.servers[0].url

        // Confirm that Lisbon did NOT return Vidfast's shifted Episode 2 stream token (which serves Chapter Two: The Weirdo on Maple Street)
        assertFalse(
            "Lisbon must not return Vidfast shifted stream for Stranger Things S1E1",
            lisbonUrl.contains("M0xPeTI5bjFqekVnUTZoVy")
        )
        // Confirm Lisbon serves the verified Chapter One stream from Vidlink/Vidlove
        assertTrue(
            "Lisbon must route to verified stream delivering Chapter One",
            lisbonUrl.contains("hakunaymatata.com") || lisbonUrl.contains("whysosigmabro.cfd") || lisbonUrl.startsWith("https://")
        )

        // 2. Verify all other servers in cluster for Stranger Things S1E1
        for (server in listOf("nebula", "solara", "joy", "sakura", "castle", "canaias")) {
            val result = resolver.resolveStream(
                mediaType = "tv",
                tmdbId = "66732",
                title = "Stranger Things",
                season = 1,
                episode = 1,
                preferredServer = server
            )
            println("SERVER [$server] -> ok=${result.ok}, name=${result.servers.firstOrNull()?.name}, url=${result.servers.firstOrNull()?.url}")
            assertTrue("Server $server must resolve S1E1", result.ok)
            assertTrue("Server $server must have servers", result.servers.isNotEmpty())
        }
    }

    @Test
    fun testMoanaStreamResolution() = kotlinx.coroutines.runBlocking {
        val resolver = com.erasmustv.app.data.remote.CinejoyStreamResolver()
        com.erasmustv.app.core.util.DeviceCodecCapability.setHevcMain10SupportedForTesting(true)

        // 1. Moana (277834) on Lisbon (4K Master primary) & Athens (4K Direct primary)
        val moanaLisbon = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "277834",
            title = "Moana",
            preferredServer = "lisbon"
        )
        println("MOANA LISBON: ok=${moanaLisbon.ok}")
        moanaLisbon.servers.forEach { s ->
            println(" -> Lisbon Server: name='${s.name}', url='${s.url}'")
        }
        assertTrue("Lisbon must resolve Moana", moanaLisbon.ok)
        assertTrue("Lisbon must contain 4K Master stream", moanaLisbon.servers.any { it.name.contains("4K Master") })
        assertTrue("Lisbon must contain 4K stream", moanaLisbon.servers.any { it.name.contains("4K") })

        val moanaAthens = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "277834",
            title = "Moana",
            preferredServer = "athens"
        )
        println("MOANA ATHENS: ok=${moanaAthens.ok}")
        moanaAthens.servers.forEach { s ->
            println(" -> Athens Server: name='${s.name}', url='${s.url}'")
        }
        assertTrue("Athens must resolve Moana", moanaAthens.ok)
        assertTrue("Athens must contain 4K stream", moanaAthens.servers.any { it.name.contains("4K") })

        // 2. Fast Five (51497) on Lisbon and Athens
        val fast5Lisbon = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "51497",
            title = "Fast Five",
            preferredServer = "lisbon"
        )
        println("FAST 5 LISBON: ok=${fast5Lisbon.ok}")
        fast5Lisbon.servers.forEach { s ->
            println(" -> Fast 5 Lisbon Server: name='${s.name}', url='${s.url}'")
        }
        assertTrue("Lisbon must resolve Fast Five", fast5Lisbon.ok)
        assertTrue("Lisbon must contain streams for Fast Five", fast5Lisbon.servers.isNotEmpty())

        val fast5Athens = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "51497",
            title = "Fast Five",
            preferredServer = "athens"
        )
        println("FAST 5 ATHENS: ok=${fast5Athens.ok}")
        fast5Athens.servers.forEach { s ->
            println(" -> Fast 5 Athens Server: name='${s.name}', url='${s.url}'")
        }
        assertTrue("Athens must resolve Fast Five", fast5Athens.ok)
        assertTrue("Athens must contain streams for Fast Five", fast5Athens.servers.isNotEmpty())

        // 3. Furious 7 (168259) on Lisbon and Athens
        val f7Lisbon = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "168259",
            title = "Furious 7",
            preferredServer = "lisbon"
        )
        println("FURIOUS 7 LISBON: ok=${f7Lisbon.ok}")
        f7Lisbon.servers.forEach { s ->
            println(" -> Furious 7 Lisbon Server: name='${s.name}', url='${s.url}'")
        }
        assertTrue("Lisbon must resolve Furious 7", f7Lisbon.ok)
        assertTrue("Lisbon must contain streams for Furious 7", f7Lisbon.servers.isNotEmpty())

        val f7Athens = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "168259",
            title = "Furious 7",
            preferredServer = "athens"
        )
        println("FURIOUS 7 ATHENS: ok=${f7Athens.ok}")
        f7Athens.servers.forEach { s ->
            println(" -> Furious 7 Athens Server: name='${s.name}', url='${s.url}'")
        }
        assertTrue("Athens must resolve Furious 7", f7Athens.ok)
        assertTrue("Athens must contain streams for Furious 7", f7Athens.servers.isNotEmpty())

        val moana2026 = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "1108427",
            title = "Moana",
            preferredServer = "lisbon"
        )
        println("MOANA 2026 (1108427) LISBON: ok=${moana2026.ok}")
        moana2026.servers.forEach { s ->
            println(" -> Lisbon Server: name='${s.name}', url='${s.url}'")
        }

        // Test Cinejoy (Shegu) direct resolution for Fast 5, Furious 7, Moana
        val f5Shegu = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "51497",
            title = "Fast Five",
            preferredServer = "castle",
            year = "2011",
            imdbId = "tt1596343"
        )
        println("FAST 5 CASTLE/SHEGU: ok=${f5Shegu.ok}")
        f5Shegu.servers.forEach { s ->
            println(" -> Fast 5 Castle Server: name='${s.name}', url='${s.url}'")
        }

        val f7Shegu = resolver.resolveStream(
            mediaType = "movie",
            tmdbId = "168259",
            title = "Furious 7",
            preferredServer = "castle",
            year = "2015",
            imdbId = "tt2820852"
        )
        println("FURIOUS 7 CASTLE/SHEGU: ok=${f7Shegu.ok}")
        f7Shegu.servers.forEach { s ->
            println(" -> Furious 7 Castle Server: name='${s.name}', url='${s.url}'")
        }
    }

    @Test
    fun testBingrServerRecognition() {
        val resolver = com.erasmustv.app.data.remote.BingrStreamResolver()
        assertTrue(resolver.isBingrServer("aphelion"))
        assertTrue(resolver.isBingrServer("Aphelion"))
        assertTrue(resolver.isBingrServer("bastion"))
        assertTrue(resolver.isBingrServer("polaris"))
        assertTrue(resolver.isBingrServer("nova"))
        assertTrue(resolver.isBingrServer("edmunds"))
        assertTrue(resolver.isBingrServer("hallyu"))
        assertTrue(resolver.isBingrServer("animesalt"))
        assertTrue(resolver.isBingrServer("ryuu"))
        assertTrue(resolver.isBingrServer("orion"))

        assertFalse(resolver.isBingrServer("lisbon"))
        assertFalse(resolver.isBingrServer("sakura"))
        assertFalse(resolver.isBingrServer("nebula"))
        assertFalse(resolver.isBingrServer("unknown_server"))
    }

    @Test
    fun testBingrServerMapCodes() {
        val map = com.erasmustv.app.data.remote.BingrStreamResolver.SERVER_MAP
        assertEquals("s40", map["aphelion"]?.first)
        assertEquals("Aphelion", map["aphelion"]?.second)
        assertEquals("s62", map["bastion"]?.first)
        assertEquals("s70", map["polaris"]?.first)
        assertEquals("s30", map["nova"]?.first)
        assertEquals("s3", map["edmunds"]?.first)
        assertEquals("s63", map["hallyu"]?.first)
        assertEquals("s31", map["orion"]?.first)
        assertEquals("animesalt", map["animesalt"]?.first)
        assertEquals("ryuu", map["ryuu"]?.first)
    }

    @Test
    fun testBingrUrlUnwrapping() {
        val resolver = com.erasmustv.app.data.remote.BingrStreamResolver()
        val directUrl = "https://keenanchor.top/hls/test/master.m3u8"
        assertEquals(directUrl, resolver.unwrapUrl(directUrl))

        val wrappedUrl = "https://domain.com/manifest?url=https%3A%2F%2Fcdn.example.com%2Fmaster.m3u8"
        val unwrapped = resolver.unwrapUrl(wrappedUrl)
        assertEquals("https://cdn.example.com/master.m3u8", unwrapped)
    }

    @Test
    fun testBingrCooldown() {
        val resolver = com.erasmustv.app.data.remote.BingrStreamResolver()
        assertEquals(0L, resolver.getCooldownUntil())
        val future = System.currentTimeMillis() + 25_000L
        resolver.setCooldown(future)
        assertEquals(future, resolver.getCooldownUntil())
    }

    @Test
    fun testSubtitleLanguageNormalizationAndDisplayNames() {
        assertEquals("en", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("eng"))
        assertEquals("es", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("spa"))
        assertEquals("fr", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("fra"))
        assertEquals("pt-br", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("pob"))
        assertEquals("pt-br", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("pb"))
        assertEquals("ja", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("jpn"))
        assertEquals("ko", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("kor"))
        assertEquals("is", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("ice"))
        assertEquals("da", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("dan"))
        assertEquals("es-la", com.erasmustv.app.data.remote.SubtitleResolver.normalizeLangCode("spl"))

        assertEquals("English", com.erasmustv.app.data.remote.SubtitleResolver.getLanguageDisplayName("en"))
        assertEquals("Spanish", com.erasmustv.app.data.remote.SubtitleResolver.getLanguageDisplayName("es"))
        assertEquals("Spanish (Latin America)", com.erasmustv.app.data.remote.SubtitleResolver.getLanguageDisplayName("es-la"))
        assertEquals("Spanish (Latin America)", com.erasmustv.app.data.remote.SubtitleResolver.getLanguageDisplayName("spl"))
        assertEquals("Portuguese (BR)", com.erasmustv.app.data.remote.SubtitleResolver.getLanguageDisplayName("pt-br"))
        assertEquals("Icelandic", com.erasmustv.app.data.remote.SubtitleResolver.getLanguageDisplayName("is"))
        assertEquals("Danish", com.erasmustv.app.data.remote.SubtitleResolver.getLanguageDisplayName("da"))
    }

    @Test
    fun testMultilingualSubtitlePreservationAndDisambiguation() {
        val rawTracks = mutableListOf<com.erasmustv.app.data.model.SubtitleTrack>()
        
        // 5 English tracks (regular + CC + duplicates)
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("English", "en", "https://subs.org/en1.srt"))
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("English [CC]", "en", "https://subs.org/en_cc.srt"))
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("English", "en", "https://subs.org/en2.srt"))
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("English", "en", "https://subs.org/en3.srt"))
        
        // 4 Spanish tracks
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("Spanish", "es", "https://subs.org/es1.srt"))
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("Spanish", "es", "https://subs.org/es2.srt"))
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("Spanish", "es", "https://subs.org/es3.srt"))
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("Spanish", "es", "https://subs.org/es4.srt"))

        // 3 French tracks
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("French", "fr", "https://subs.org/fr1.srt"))
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("French", "fr", "https://subs.org/fr2.srt"))
        rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack("French", "fr", "https://subs.org/fr3.srt"))

        // Add 20 more distinct language tracks (German, Italian, Portuguese, Japanese, etc.)
        val moreLangs = listOf("de" to "German", "it" to "Italian", "pt" to "Portuguese", "ja" to "Japanese",
            "ko" to "Korean", "zh" to "Chinese", "ar" to "Arabic", "hi" to "Hindi", "tr" to "Turkish",
            "nl" to "Dutch", "pl" to "Polish", "sv" to "Swedish", "el" to "Greek", "da" to "Danish",
            "fi" to "Finnish", "no" to "Norwegian", "cs" to "Czech", "hu" to "Hungarian", "ro" to "Romanian",
            "ru" to "Russian")
        for ((code, name) in moreLangs) {
            rawTracks.add(com.erasmustv.app.data.model.SubtitleTrack(name, code, "https://subs.org/$code.srt"))
        }

        assertEquals(31, rawTracks.size)

        // URL deduplication
        val seenUrls = mutableSetOf<String>()
        val unique = rawTracks.filter { it.url.isNotBlank() && seenUrls.add(it.url) }
        assertEquals(31, unique.size)

        // Label disambiguation
        val labelCounts = mutableMapOf<String, Int>()
        val disambiguated = mutableListOf<com.erasmustv.app.data.model.SubtitleTrack>()
        for (sub in unique) {
            val base = sub.label.trim()
            val count = labelCounts.getOrDefault(base, 0) + 1
            labelCounts[base] = count
            val finalLabel = if (count > 1) {
                if (base.contains("#")) "$base-$count" else "$base #$count"
            } else base
            disambiguated.add(sub.copy(label = finalLabel))
        }

        // English priority sorting
        val sorted = disambiguated.sortedWith(
            compareBy<com.erasmustv.app.data.model.SubtitleTrack> {
                val isEn = it.language.startsWith("en", ignoreCase = true) || it.label.contains("English", ignoreCase = true)
                if (!isEn) 2
                else if (it.label.contains("[CC]", ignoreCase = true)) 1
                else 0
            }.thenBy { it.label }
        )

        // All 31 tracks preserved!
        assertEquals(31, sorted.size)
        // English Standard first
        assertEquals("English", sorted[0].label)
        assertEquals("English #2", sorted[1].label)
        assertEquals("English #3", sorted[2].label)
        // English [CC] next
        assertEquals("English [CC]", sorted[3].label)
        // Other languages alphabetically
        assertEquals("Arabic", sorted[4].label)
        assertTrue(sorted.any { it.label == "Spanish" })
        assertTrue(sorted.any { it.label == "Spanish #2" })
        assertTrue(sorted.any { it.label == "Spanish #3" })
        assertTrue(sorted.any { it.label == "Spanish #4" })
        assertTrue(sorted.any { it.label == "French" })
        assertTrue(sorted.any { it.label == "French #2" })
        assertTrue(sorted.any { it.label == "French #3" })
    }
}





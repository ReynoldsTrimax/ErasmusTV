package com.erasmustv.app

import com.erasmustv.app.data.model.JokeCategory
import com.erasmustv.app.data.model.TvLoadingJokes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TvLoadingJokeTest {

    @Test
    fun testJokeCounts() {
        assertEquals(66, TvLoadingJokes.BOTH_JOKES.size)
        assertEquals(64, TvLoadingJokes.MOVIE_JOKES.size)
        assertEquals(64, TvLoadingJokes.TV_JOKES.size)
        assertEquals(194, TvLoadingJokes.ALL_JOKES.size)
    }

    @Test
    fun testNoEmptyOrBlankJokes() {
        for (joke in TvLoadingJokes.ALL_JOKES) {
            assertTrue("Joke text should not be blank: '${joke.text}'", joke.text.isNotBlank())
            assertEquals(joke.text.trim(), joke.text)
        }
    }

    @Test
    fun testMoviePoolIsolation() {
        val tvOnlyTexts = TvLoadingJokes.TV_JOKES.map { it.text }.toSet()
        val movieEligibleTexts = (TvLoadingJokes.MOVIE_JOKES + TvLoadingJokes.BOTH_JOKES).map { it.text }.toSet()

        val seedRandom = Random(42)
        val drawn = mutableSetOf<String>()

        for (i in 0 until 1000) {
            val joke = TvLoadingJokes.getRandomJoke(mediaType = "movie", random = seedRandom)
            assertTrue("Movie joke must be in movie/both pool", movieEligibleTexts.contains(joke))
            assertFalse("Movie joke must NEVER be in TV-only pool", tvOnlyTexts.contains(joke))
            drawn.add(joke)
        }

        // Verify broad coverage across iterations
        assertTrue("Expected broad coverage of jokes drawn", drawn.size > 80)
    }

    @Test
    fun testTvPoolIsolation() {
        val movieOnlyTexts = TvLoadingJokes.MOVIE_JOKES.map { it.text }.toSet()
        val tvEligibleTexts = (TvLoadingJokes.TV_JOKES + TvLoadingJokes.BOTH_JOKES).map { it.text }.toSet()

        val seedRandom = Random(99)
        val drawn = mutableSetOf<String>()

        for (i in 0 until 1000) {
            val joke = TvLoadingJokes.getRandomJoke(mediaType = "tv", random = seedRandom)
            assertTrue("TV joke must be in tv/both pool", tvEligibleTexts.contains(joke))
            assertFalse("TV joke must NEVER be in Movie-only pool", movieOnlyTexts.contains(joke))
            drawn.add(joke)
        }

        // Verify broad coverage across iterations
        assertTrue("Expected broad coverage of jokes drawn", drawn.size > 80)
    }

    @Test
    fun testCaseInsensitiveMediaTypeHandling() {
        val tvOnlyTexts = TvLoadingJokes.TV_JOKES.map { it.text }.toSet()
        val movieOnlyTexts = TvLoadingJokes.MOVIE_JOKES.map { it.text }.toSet()

        val movieJokeUpper = TvLoadingJokes.getRandomJoke("MOVIE")
        assertFalse(tvOnlyTexts.contains(movieJokeUpper))

        val tvJokeUpper = TvLoadingJokes.getRandomJoke("TV")
        assertFalse(movieOnlyTexts.contains(tvJokeUpper))
    }
}

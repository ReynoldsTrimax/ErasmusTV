package com.erasmustv.app.data.model

/**
 * Represents a dynamic horizontal content rail on the ErasmusTV home screen.
 *
 * @param id Unique identifier for the section (e.g. "top_10_movies", "action_blockbusters").
 * @param title User-facing category header displayed above the rail.
 * @param items List of media items populated in this rail.
 * @param isRanked True if this rail displays oversized 1-10 typographic numbers (Top 10 rows).
 * @param showNewBadge True if cards in this rail should display the "NEW" badge overlay.
 * @param viewAllRoute Optional navigation route for the "View All ›" action header.
 */
data class HomeSection(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
    val isRanked: Boolean = false,
    val showNewBadge: Boolean = false,
    val viewAllRoute: String? = null
)

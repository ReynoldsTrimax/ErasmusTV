package com.erasmustv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceElevated
import com.erasmustv.app.core.theme.rememberShimmerBrush

/**
 * Android TV Shimmer Skeleton Placeholder for Feed Screens (Home, Movies, TV, Anime).
 * Renders a full-width hero billboard placeholder followed by horizontal card rows.
 * Uses a luminous diagonal shimmer sweep matching Apple TV & Netflix TV ergonomics.
 */
@Composable
fun TvFeedSkeleton(
    modifier: Modifier = Modifier,
    contentShift: Dp = 0.dp
) {
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .graphicsLayer { translationX = contentShift.toPx() }
    ) {
        // Hero Billboard Skeleton (synchronized to Phase 14 335dp height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(335.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceElevated.copy(alpha = 0.55f),
                            PitchBlack
                        )
                    )
                )
                .padding(start = 64.dp, top = 54.dp)
        ) {
            Column(modifier = Modifier.width(520.dp)) {
                // Category / Tag pill
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 18.dp)
                        .background(shimmerBrush, RectangleShape)
                        .border(1.dp, BorderHairline, RectangleShape)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title bar
                Box(
                    modifier = Modifier
                        .size(width = 340.dp, height = 36.dp)
                        .background(shimmerBrush, RectangleShape)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata tags row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 16.dp)
                                .background(shimmerBrush, RectangleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Overview lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .background(shimmerBrush, RectangleShape)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(12.dp)
                        .background(shimmerBrush, RectangleShape)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Button placeholders
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 38.dp)
                            .background(shimmerBrush, RectangleShape)
                            .border(1.dp, BorderHairline, RectangleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 38.dp)
                            .background(shimmerBrush, RectangleShape)
                            .border(1.dp, BorderHairline, RectangleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // First Horizontal Poster Shelf
        TvSkeletonShelf(titleWidth = 140.dp, shimmerBrush = shimmerBrush)

        Spacer(modifier = Modifier.height(20.dp))

        // Second Horizontal Poster Shelf
        TvSkeletonShelf(titleWidth = 110.dp, shimmerBrush = shimmerBrush)
    }
}

@Composable
private fun TvSkeletonShelf(
    titleWidth: Dp,
    shimmerBrush: Brush,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Shelf Title
        Box(
            modifier = Modifier
                .padding(start = 64.dp, bottom = 10.dp)
                .size(width = titleWidth, height = 18.dp)
                .background(shimmerBrush, RectangleShape)
        )

        // Poster Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(8) {
                Box(
                    modifier = Modifier
                        .size(width = 138.dp, height = 207.dp)
                        .background(shimmerBrush, RectangleShape)
                        .border(1.dp, BorderHairline, RectangleShape)
                )
            }
        }
    }
}

/**
 * Android TV Shimmer Skeleton Placeholder for Grid Screens (Watchlist, Catalog).
 */
@Composable
fun TvGridSkeleton(
    modifier: Modifier = Modifier,
    contentShift: Dp = 0.dp
) {
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .graphicsLayer { translationX = contentShift.toPx() }
            .padding(start = 64.dp, top = 36.dp, end = 48.dp)
    ) {
        // Title placeholder
        Box(
            modifier = Modifier
                .padding(bottom = 22.dp)
                .size(width = 160.dp, height = 24.dp)
                .background(shimmerBrush, RectangleShape)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(5) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .background(shimmerBrush, RectangleShape)
                                .border(1.dp, BorderHairline, RectangleShape)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Android TV Shimmer Skeleton Placeholder for Media Detail Screen.
 */
@Composable
fun TvDetailSkeleton(
    modifier: Modifier = Modifier,
    contentShift: Dp = 0.dp
) {
    val shimmerBrush = rememberShimmerBrush()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .graphicsLayer { translationX = contentShift.toPx() }
    ) {
        // Backdrop wash
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceElevated.copy(alpha = 0.5f),
                            PitchBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 82.dp, top = 70.dp, end = 60.dp)
        ) {
            // Title
            Box(
                modifier = Modifier
                    .size(width = 380.dp, height = 38.dp)
                    .background(shimmerBrush, RectangleShape)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata row (Year, Rating, Duration, Quality)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 20.dp)
                            .background(shimmerBrush, RectangleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overview lines
            Box(
                modifier = Modifier
                    .width(540.dp)
                    .height(14.dp)
                    .background(shimmerBrush, RectangleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(480.dp)
                    .height(14.dp)
                    .background(shimmerBrush, RectangleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .height(14.dp)
                    .background(shimmerBrush, RectangleShape)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 140.dp, height = 44.dp)
                        .background(shimmerBrush, RectangleShape)
                        .border(1.dp, BorderHairline, RectangleShape)
                )
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 44.dp)
                        .background(shimmerBrush, RectangleShape)
                        .border(1.dp, BorderHairline, RectangleShape)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Cast / Similar shelf shimmer
            Box(
                modifier = Modifier
                    .size(width = 150.dp, height = 20.dp)
                    .background(shimmerBrush, RectangleShape)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(7) {
                    Box(
                        modifier = Modifier
                            .size(width = 130.dp, height = 195.dp)
                            .background(shimmerBrush, RectangleShape)
                            .border(1.dp, BorderHairline, RectangleShape)
                    )
                }
            }
        }
    }
}

/**
 * Android TV Shimmer Skeleton Placeholder for Search Results Panel.
 */
@Composable
fun TvSearchSkeleton(modifier: Modifier = Modifier) {
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 160.dp, height = 22.dp)
                .background(shimmerBrush, RectangleShape)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(4) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .background(shimmerBrush, RectangleShape)
                                .border(1.dp, BorderHairline, RectangleShape)
                        )
                    }
                }
            }
        }
    }
}

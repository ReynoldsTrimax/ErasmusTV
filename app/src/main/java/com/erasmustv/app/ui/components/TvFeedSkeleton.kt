package com.erasmustv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.rememberSkeletonBrush

/**
 * ERASMUS SKELETON LOADING STATES.
 *
 * Skeletons mirror the real layout's geometry — same gutters, same card sizes,
 * same corner radii — so content arriving causes no visible reflow or jump.
 * They are intentionally dull: a calm breathing tone rather than a travelling
 * highlight, because the loading state should not be the most animated thing
 * the user ever sees.
 */

/** Rounded placeholder block. Text lines use a pill radius, cards use card radius. */
@Composable
private fun SkeletonBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp)
) {
    Box(modifier = modifier.background(brush, shape))
}

/**
 * Feed skeleton for hero-led pages (Home, Movies, TV Shows, Anime).
 */
@Composable
fun TvFeedSkeleton(
    modifier: Modifier = Modifier,
    contentShift: Dp = 0.dp
) {
    val brush = rememberSkeletonBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .graphicsLayer { translationX = contentShift.toPx() }
    ) {
        // Hero region — matches the real hero height so the first shelf does
        // not shift downward when content resolves.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ErasmusDimens.HeroHeight)
                .padding(
                    start = ErasmusDimens.HeroContentStartInset,
                    top = ErasmusDimens.HeroHeight * 0.42f
                )
        ) {
            Column(modifier = Modifier.width(ErasmusDimens.HeroContentMaxWidth)) {
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier.size(width = 300.dp, height = 34.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        SkeletonBlock(
                            brush = brush,
                            modifier = Modifier.size(width = 54.dp, height = 14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .height(11.dp)
                )
                Spacer(modifier = Modifier.height(7.dp))
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .height(11.dp)
                )
                Spacer(modifier = Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonBlock(
                        brush = brush,
                        modifier = Modifier.size(
                            width = 132.dp,
                            height = ErasmusDimens.HeroButtonHeight
                        ),
                        shape = ErasmusShapes.ButtonLarge
                    )
                    SkeletonBlock(
                        brush = brush,
                        modifier = Modifier.size(
                            width = 108.dp,
                            height = ErasmusDimens.HeroButtonHeight
                        ),
                        shape = ErasmusShapes.ButtonLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
        TvSkeletonShelf(titleWidth = 180.dp, brush = brush)
        Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
        TvSkeletonShelf(titleWidth = 140.dp, brush = brush)
    }
}

@Composable
private fun TvSkeletonShelf(
    titleWidth: Dp,
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SkeletonBlock(
            brush = brush,
            modifier = Modifier
                .padding(start = ErasmusDimens.RailStartGutter)
                .size(width = titleWidth, height = 24.dp),
            shape = RoundedCornerShape(5.dp)
        )

        Spacer(modifier = Modifier.height(ErasmusDimens.RailTitleGap))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ErasmusDimens.RailStartGutter),
            horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.CardSpacing)
        ) {
            repeat(7) {
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier.size(
                        width = ErasmusDimens.PosterCardWidth,
                        height = ErasmusDimens.PosterCardHeight
                    ),
                    shape = ErasmusShapes.Card
                )
            }
        }
    }
}

/**
 * Skeleton for rail-based pages that have no hero (e.g. a studio catalogue).
 * Using the feed skeleton there would flash a hero block that never arrives.
 */
@Composable
fun TvRailsSkeleton(
    modifier: Modifier = Modifier,
    railCount: Int = 2,
    showPageTitle: Boolean = true
) {
    val brush = rememberSkeletonBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(top = ErasmusDimens.NavPillContentClearance)
    ) {
        if (showPageTitle) {
            SkeletonBlock(
                brush = brush,
                modifier = Modifier
                    .padding(start = ErasmusDimens.RailStartGutter)
                    .size(width = 240.dp, height = 34.dp),
                shape = RoundedCornerShape(6.dp)
            )
            Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
        }

        repeat(railCount) { index ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))
            }
            TvSkeletonShelf(titleWidth = if (index == 0) 200.dp else 150.dp, brush = brush)
        }
    }
}

/**
 * Grid skeleton for poster-grid pages (Watch List, Categories).
 */
@Composable
fun TvGridSkeleton(
    modifier: Modifier = Modifier,
    contentShift: Dp = 0.dp,
    columns: Int = 6
) {
    val brush = rememberSkeletonBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .graphicsLayer { translationX = contentShift.toPx() }
            .padding(
                start = ErasmusDimens.RailStartGutter,
                top = ErasmusDimens.NavPillContentClearance,
                end = ErasmusDimens.RailEndGutter
            )
    ) {
        SkeletonBlock(
            brush = brush,
            modifier = Modifier.size(width = 220.dp, height = 34.dp),
            shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.height(ErasmusDimens.GridRowSpacing))

        repeat(2) { rowIndex ->
            if (rowIndex > 0) {
                Spacer(modifier = Modifier.height(ErasmusDimens.GridRowSpacing))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.GridItemSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(columns) {
                    SkeletonBlock(
                        brush = brush,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(2f / 3f),
                        shape = ErasmusShapes.Card
                    )
                }
            }
        }
    }
}

/**
 * Detail page skeleton — mirrors the cinematic detail hero layout.
 */
@Composable
fun TvDetailSkeleton(
    modifier: Modifier = Modifier,
    contentShift: Dp = 0.dp
) {
    val brush = rememberSkeletonBrush()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .graphicsLayer { translationX = contentShift.toPx() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = ErasmusDimens.HeroContentStartInset,
                    top = ErasmusDimens.HeroHeight * 0.38f,
                    end = ErasmusDimens.RailEndGutter
                )
        ) {
            SkeletonBlock(
                brush = brush,
                modifier = Modifier.size(width = 360.dp, height = 38.dp),
                shape = RoundedCornerShape(6.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) {
                    SkeletonBlock(
                        brush = brush,
                        modifier = Modifier.size(width = 58.dp, height = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SkeletonBlock(
                brush = brush,
                modifier = Modifier.size(width = 520.dp, height = 12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBlock(
                brush = brush,
                modifier = Modifier.size(width = 460.dp, height = 12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBlock(
                brush = brush,
                modifier = Modifier.size(width = 330.dp, height = 12.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier.size(width = 150.dp, height = ErasmusDimens.HeroButtonHeight),
                    shape = ErasmusShapes.ButtonLarge
                )
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier.size(width = 170.dp, height = ErasmusDimens.HeroButtonHeight),
                    shape = ErasmusShapes.ButtonLarge
                )
            }

            Spacer(modifier = Modifier.height(ErasmusDimens.RailSpacing))

            SkeletonBlock(
                brush = brush,
                modifier = Modifier.size(width = 170.dp, height = 24.dp),
                shape = RoundedCornerShape(5.dp)
            )

            Spacer(modifier = Modifier.height(ErasmusDimens.RailTitleGap))

            Row(horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.CardSpacing)) {
                repeat(6) {
                    SkeletonBlock(
                        brush = brush,
                        modifier = Modifier.size(
                            width = ErasmusDimens.PosterCardWidth,
                            height = ErasmusDimens.PosterCardHeight
                        ),
                        shape = ErasmusShapes.Card
                    )
                }
            }
        }
    }
}

/**
 * Search results skeleton — a poster grid matching the real results grid.
 */
@Composable
fun TvSearchSkeleton(
    modifier: Modifier = Modifier,
    columns: Int = 4
) {
    val brush = rememberSkeletonBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ErasmusDimens.GridItemSpacing, vertical = 16.dp)
    ) {
        SkeletonBlock(
            brush = brush,
            modifier = Modifier.size(width = 160.dp, height = 20.dp)
        )
        Spacer(modifier = Modifier.height(ErasmusDimens.GridRowSpacing))
        Row(
            horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.GridItemSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(columns) {
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(2f / 3f),
                    shape = ErasmusShapes.Card
                )
            }
        }
    }
}

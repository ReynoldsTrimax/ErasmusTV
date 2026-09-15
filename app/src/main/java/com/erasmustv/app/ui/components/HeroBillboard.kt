package com.erasmustv.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.RatingGold
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.MediaItem
import kotlinx.coroutines.delay

// Smooth multi-stop cinematic gradients dissolving seamlessly into PitchBlack
private val HeroHorizontalGradient = Brush.horizontalGradient(
    colors = listOf(
        PitchBlack.copy(alpha = 0.92f),
        PitchBlack.copy(alpha = 0.80f),
        PitchBlack.copy(alpha = 0.50f),
        PitchBlack.copy(alpha = 0.15f),
        Color.Transparent
    ),
    startX = 0f,
    endX = 1300f
)

private val HeroVerticalGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Transparent,
        PitchBlack.copy(alpha = 0.20f),
        PitchBlack.copy(alpha = 0.55f),
        PitchBlack.copy(alpha = 0.88f),
        PitchBlack
    )
)

private val ElectricBlue = Color(0xFF1D90F5)

/**
 * Cinematic Hero Billboard matching Reference Image 2 (The End of Oak Street).
 * Features edge-to-edge full-bleed artwork, top badge chips (FEATURED, MOVIE/SERIES, YEAR, RATING),
 * large authentic title logo, italicized tagline, spacious synopsis, Electric Blue Watch Now pill,
 * frosted Details pill, and carousel slider indicator.
 */
@Composable
fun HeroBillboard(
    item: MediaItem?,
    onPlayClick: (MediaItem) -> Unit,
    onDetailsClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    heroFocusRequester: FocusRequester? = null,
    featuredItems: List<MediaItem> = emptyList(),
    onFeaturedSelect: ((MediaItem) -> Unit)? = null,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null
) {
    if (item == null) return

    val context = LocalContext.current
    val backdropKey = item.backdropPath ?: item.posterPath
    val imageRequest = remember(backdropKey) {
        ImageRequest.Builder(context)
            .data(AppConfig.backdropUrl(backdropKey))
            .memoryCacheKey(backdropKey)
            .diskCacheKey(backdropKey)
            .crossfade(350)
            .build()
    }

    val featuredList = remember(featuredItems) { featuredItems.take(8) }
    var isPrimaryFocused by remember { mutableStateOf(false) }
    var isSecondaryFocused by remember { mutableStateOf(false) }
    val isAnyFocused = isPrimaryFocused || isSecondaryFocused

    // Auto-advance carousel every 8 seconds when user is idle
    LaunchedEffect(item.id, isAnyFocused, featuredList) {
        if (featuredList.size > 1 && onFeaturedSelect != null && !isAnyFocused) {
            delay(8000L)
            val currentIndex = featuredList.indexOfFirst { it.id == item.id }
            val nextIndex = if (currentIndex in 0 until featuredList.size - 1) {
                currentIndex + 1
            } else {
                0
            }
            onFeaturedSelect(featuredList[nextIndex])
        }
    }

    val detailsFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(460.dp)
    ) {
        // Full-bleed Backdrop Image extending to screen edges
        AsyncImage(
            model = imageRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Horizontal vignette from left
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HeroHorizontalGradient)
        )

        // Vertical fade to bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HeroVerticalGradient)
        )

        // Content Area positioned with generous cinematic headroom for artwork
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.58f)
                .padding(start = 82.dp, top = 110.dp, bottom = 16.dp)
        ) {
            Crossfade(
                targetState = item,
                animationSpec = tween(220),
                label = "hero_content_crossfade"
            ) { currentItem ->
                Column {
                    // Top Badges Row (Reference Image 2: [FEATURED] [MOVIE] [2026] [★ 6.4])
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // FEATURED Badge
                        Box(
                            modifier = Modifier
                                .background(Color.White, RectangleShape)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FEATURED",
                                style = ErasmusTvTypography.Badge.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = PitchBlack
                            )
                        }

                        // MOVIE / SERIES Badge
                        Box(
                            modifier = Modifier
                                .background(Color(0x3DFFFFFF), RectangleShape)
                                .border(1.dp, Color(0x2BFFFFFF), RectangleShape)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentItem.isTv) "SERIES" else "MOVIE",
                                style = ErasmusTvTypography.Badge.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                ),
                                color = Color.White
                            )
                        }

                        // Year Badge
                        currentItem.year?.let { yr ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0x3DFFFFFF), RectangleShape)
                                    .border(1.dp, Color(0x2BFFFFFF), RectangleShape)
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = yr,
                                    style = ErasmusTvTypography.Badge.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        // Rating Badge
                        if (currentItem.voteAverage != null && currentItem.voteAverage > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x3DFFFFFF), RectangleShape)
                                    .border(1.dp, Color(0x2BFFFFFF), RectangleShape)
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "★",
                                        style = ErasmusTvTypography.Badge.copy(fontSize = 10.sp),
                                        color = RatingGold
                                    )
                                    Text(
                                        text = currentItem.ratingFormatted ?: "",
                                        style = ErasmusTvTypography.Badge.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title / Logo treatment (enlarged height ~70-100dp for bold impact)
                    var isLogoError by remember(currentItem.logoPath) { mutableStateOf(false) }

                    if (!currentItem.logoPath.isNullOrBlank() && !isLogoError) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 60.dp, max = 100.dp)
                                .fillMaxWidth(0.95f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(AppConfig.logoUrl(currentItem.logoPath))
                                    .memoryCacheKey(currentItem.logoPath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = currentItem.title,
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .heightIn(min = 60.dp, max = 100.dp)
                                    .fillMaxWidth(),
                                onError = { isLogoError = true }
                            )
                        }
                    } else {
                        Text(
                            text = currentItem.title,
                            style = ErasmusTvTypography.HeroTitleLarge.copy(
                                fontSize = 34.sp,
                                lineHeight = 40.sp,
                                fontWeight = FontWeight.Black
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Italic Tagline (Reference Image 2: "Where goes the neighborhood.")
                    if (!currentItem.tagline.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentItem.tagline,
                            style = ErasmusTvTypography.Body.copy(
                                fontSize = 14.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = TextSecondary.copy(alpha = 0.95f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Concise synopsis with generous line spacing
                    if (currentItem.overview != null) {
                        Text(
                            text = currentItem.overview,
                            style = ErasmusTvTypography.Body.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = TextPrimary.copy(alpha = 0.88f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons & Carousel Slider Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Watch Now (Solid White Rectangular Button)
                HeroRectangleButton(
                    text = "Watch Now",
                    icon = Icons.Default.PlayArrow,
                    isPrimary = true,
                    onClick = { onPlayClick(item) },
                    onFocusChanged = { isPrimaryFocused = it },
                    onNavigateLeft = onNavigateLeft,
                    onNavigateRight = { detailsFocusRequester.requestFocus() },
                    onNavigateDown = onNavigateDown,
                    modifier = heroFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                )

                // Details (Frosted Dark Rectangular Button)
                HeroRectangleButton(
                    text = "Details",
                    icon = Icons.Default.Info,
                    isPrimary = false,
                    onClick = { onDetailsClick(item) },
                    modifier = Modifier.focusRequester(detailsFocusRequester),
                    onFocusChanged = { isSecondaryFocused = it },
                    onNavigateLeft = { heroFocusRequester?.requestFocus() },
                    onNavigateRight = null,
                    onNavigateDown = onNavigateDown
                )

                // Inline Carousel Indicator (< [====] · · · · >)
                if (featuredList.size > 1 && onFeaturedSelect != null) {
                    val currentIndex = featuredList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                    HeroCarouselIndicator(
                        items = featuredList,
                        currentIndex = currentIndex,
                        onPrev = {
                            val prev = if (currentIndex > 0) currentIndex - 1 else featuredList.size - 1
                            onFeaturedSelect(featuredList[prev])
                        },
                        onNext = {
                            val next = if (currentIndex < featuredList.size - 1) currentIndex + 1 else 0
                            onFeaturedSelect(featuredList[next])
                        }
                    )
                }
            }
        }
    }
}

/**
 * Rectangular action button matching modern OLED design language.
 * Solid white background for primary button, frosted dark for secondary.
 */
@Composable
private fun HeroRectangleButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChanged?.invoke(isFocused)
    }

    val backgroundColor = when {
        isPrimary -> if (isFocused) Color.White else Color(0xEBFFFFFF)
        else -> if (isFocused) Color(0x66FFFFFF) else Color(0x26FFFFFF)
    }

    val contentColor = when {
        isPrimary -> PitchBlack
        else -> Color.White
    }

    Box(
        modifier = modifier
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionRight -> {
                            if (onNavigateRight != null) {
                                onNavigateRight()
                                true
                            } else false
                        }
                        Key.DirectionLeft -> {
                            if (onNavigateLeft != null) {
                                onNavigateLeft()
                                true
                            } else false
                        }
                        Key.DirectionDown -> {
                            if (onNavigateDown != null) {
                                onNavigateDown()
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            // Top boundary of billboard: consume to prevent hopping to the sidebar
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clip(RectangleShape)
            .background(backgroundColor, RectangleShape)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color(0x33FFFFFF),
                shape = RectangleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = ErasmusTvTypography.ButtonText.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Rectangular carousel indicator (< [====] · · · · >).
 */
@Composable
private fun HeroCarouselIndicator(
    items: List<MediaItem>,
    currentIndex: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(Color(0x2B080810), RectangleShape)
            .border(1.dp, Color(0x26FFFFFF), RectangleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Arrow
            Text(
                text = "‹",
                style = ErasmusTvTypography.Badge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0x80FFFFFF)
            )

            // Indicators
            items.forEachIndexed { index, _ ->
                val isActive = index == currentIndex
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(if (isActive) 16.dp else 4.dp)
                        .background(if (isActive) Color.White else Color(0x40FFFFFF), RectangleShape)
                )
            }

            // Right Arrow
            Text(
                text = "›",
                style = ErasmusTvTypography.Badge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0x80FFFFFF)
            )
        }
    }
}



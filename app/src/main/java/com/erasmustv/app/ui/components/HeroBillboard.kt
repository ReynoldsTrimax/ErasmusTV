package com.erasmustv.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.RatingGold
import com.erasmustv.app.core.theme.SurfaceElevated
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.MediaItem
import kotlinx.coroutines.delay

import androidx.compose.foundation.shape.RoundedCornerShape
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.rememberReducedMotion

// Smooth multi-stop cinematic gradients dissolving seamlessly into PitchBlack
private val HeroHorizontalGradient = Brush.horizontalGradient(
    colors = listOf(
        PitchBlack.copy(alpha = 0.94f),
        PitchBlack.copy(alpha = 0.82f),
        PitchBlack.copy(alpha = 0.52f),
        PitchBlack.copy(alpha = 0.18f),
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
    onNavigateDown: (() -> Unit)? = null,
    isAutoAdvanceEnabled: Boolean = true
) {
    val isReducedMotion = rememberReducedMotion()
    val displayItem = item ?: featuredItems.firstOrNull()

    if (displayItem == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(335.dp)
                .background(HeroVerticalGradient),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .padding(start = 64.dp, top = 54.dp, bottom = 12.dp)
            ) {
                // FEATURED SPOTLIGHT Clean Inline Label
                Text(
                    text = "FEATURED SPOTLIGHT",
                    style = ErasmusTvTypography.Badge.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(9.dp))

                Text(
                    text = "Explore Erasmus Catalog",
                    style = ErasmusTvTypography.HeroTitleLarge.copy(
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Black
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Stream trending films, series, and anime with multi-source playback and subtitle synchronization.",
                    style = ErasmusTvTypography.Body.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = TextPrimary.copy(alpha = 0.88f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                HeroRectangleButton(
                    text = "Browse Catalog",
                    icon = Icons.Default.PlayArrow,
                    isPrimary = true,
                    onClick = {},
                    onNavigateLeft = onNavigateLeft,
                    onNavigateDown = onNavigateDown,
                    modifier = heroFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val backdropKey = displayItem.backdropPath ?: displayItem.posterPath
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

    // Auto-advance carousel every 8 seconds when user is idle and hero is visible
    LaunchedEffect(displayItem.id, isAnyFocused, featuredList, isAutoAdvanceEnabled) {
        if (isAutoAdvanceEnabled && featuredList.size > 1 && onFeaturedSelect != null && !isAnyFocused) {
            delay(8000L)
            val currentIndex = featuredList.indexOfFirst { it.id == displayItem.id }
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
            .height(335.dp) // Reduced height by ~15% for cinematic density (Recommendation #3)
    ) {
        // Full-bleed Backdrop Image extending to screen edges with restrained cinematic crossfade
        Crossfade(
            targetState = backdropKey,
            animationSpec = tween(
                durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_CAROUSEL,
                easing = TvMotion.EasingSilk
            ),
            label = "hero_backdrop_crossfade",
            modifier = Modifier.fillMaxSize()
        ) { key ->
            val bgRequest = remember(key) {
                ImageRequest.Builder(context)
                    .data(AppConfig.backdropUrl(key))
                    .memoryCacheKey(key)
                    .diskCacheKey(key)
                    .crossfade(300)
                    .build()
            }
            var isImageLoaded by remember(key) { mutableStateOf(false) }
            val imageScale by animateFloatAsState(
                targetValue = if (isImageLoaded && !isReducedMotion) 1.0f else 1.025f,
                animationSpec = tween(durationMillis = 600, easing = TvMotion.EasingSilk),
                label = "heroBackdropDrift"
            )
            AsyncImage(
                model = bgRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { isImageLoaded = true },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = imageScale
                        scaleY = imageScale
                    }
            )
        }

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

        // Content Area positioned with comfortable cinematic headroom (Recommendation #3 & #18)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.58f)
                .padding(start = 64.dp, top = 54.dp, bottom = 10.dp)
        ) {
            Crossfade(
                targetState = displayItem,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "hero_content_crossfade"
            ) { currentItem ->
                var contentVisible by remember(currentItem.id) { mutableStateOf(false) }
                LaunchedEffect(currentItem.id) {
                    contentVisible = true
                }
                val translateY by animateDpAsState(
                    targetValue = if (contentVisible) 0.dp else 8.dp,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "hero_content_slide"
                )
                val contentAlpha by animateFloatAsState(
                    targetValue = if (contentVisible) 1f else 0.4f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "hero_content_alpha"
                )

                Column(
                    modifier = Modifier.graphicsLayer {
                        translationY = translateY.toPx()
                        alpha = contentAlpha
                    }
                ) {
                    // Inline Clean Metadata Row without heavy box containers (Recommendation #21 & #30)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = "FEATURED",
                            style = ErasmusTvTypography.Badge.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = Color.White
                        )

                        Text(
                            text = "·",
                            style = ErasmusTvTypography.Badge.copy(fontSize = 11.5.sp),
                            color = TextSecondary.copy(alpha = 0.6f)
                        )

                        Text(
                            text = if (currentItem.isTv) "SERIES" else "MOVIE",
                            style = ErasmusTvTypography.Badge.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.4.sp
                            ),
                            color = TextSecondary
                        )

                        currentItem.year?.let { yr ->
                            Text(
                                text = "·",
                                style = ErasmusTvTypography.Badge.copy(fontSize = 11.5.sp),
                                color = TextSecondary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = yr,
                                style = ErasmusTvTypography.Badge.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = TextSecondary
                            )
                        }

                        if (currentItem.voteAverage != null && currentItem.voteAverage > 0) {
                            Text(
                                text = "·",
                                style = ErasmusTvTypography.Badge.copy(fontSize = 11.5.sp),
                                color = TextSecondary.copy(alpha = 0.6f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "★",
                                    style = ErasmusTvTypography.Badge.copy(fontSize = 11.5.sp),
                                    color = RatingGold
                                )
                                Text(
                                    text = currentItem.ratingFormatted ?: "",
                                    style = ErasmusTvTypography.Badge.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(9.dp))

                    // Title / Logo treatment (normalized height 44-68dp for balanced vertical density)
                    var isLogoError by remember(currentItem.logoPath) { mutableStateOf(false) }

                    if (!currentItem.logoPath.isNullOrBlank() && !isLogoError) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 44.dp, max = 68.dp)
                                .fillMaxWidth(0.92f),
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
                                    .heightIn(min = 44.dp, max = 68.dp)
                                    .fillMaxWidth(),
                                onError = { isLogoError = true }
                            )
                        }
                    } else {
                        Text(
                            text = currentItem.title,
                            style = ErasmusTvTypography.HeroTitleLarge.copy(
                                fontSize = 28.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.Black
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Italic Tagline
                    if (!currentItem.tagline.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = currentItem.tagline,
                            style = ErasmusTvTypography.Body.copy(
                                fontSize = 13.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = TextSecondary.copy(alpha = 0.95f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Concise synopsis capped to two lines & ~110 characters (Recommendation #5)
                    val overviewText = remember(currentItem.overview) {
                        val raw = currentItem.overview ?: ""
                        if (raw.length > 115) {
                            raw.take(112).trimEnd() + "..."
                        } else raw
                    }
                    if (overviewText.isNotBlank()) {
                        Text(
                            text = overviewText,
                            style = ErasmusTvTypography.Body.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = TextSecondary.copy(alpha = 0.92f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Decoupled from carousel indicator, Recommendation #6)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Watch Now (Crisp Solid White TV Button, 48dp height)
                HeroRectangleButton(
                    text = "Watch Now",
                    icon = Icons.Default.PlayArrow,
                    isPrimary = true,
                    onClick = { onPlayClick(displayItem) },
                    onFocusChanged = { isPrimaryFocused = it },
                    onNavigateLeft = onNavigateLeft,
                    onNavigateRight = { detailsFocusRequester.requestFocus() },
                    onNavigateDown = onNavigateDown,
                    modifier = heroFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                )

                // Details (Visually quieter translucent dark button, Recommendation #6)
                HeroRectangleButton(
                    text = "Details",
                    icon = Icons.Default.Info,
                    isPrimary = false,
                    onClick = { onDetailsClick(displayItem) },
                    modifier = Modifier.focusRequester(detailsFocusRequester),
                    onFocusChanged = { isSecondaryFocused = it },
                    onNavigateLeft = { heroFocusRequester?.requestFocus() },
                    onNavigateRight = null,
                    onNavigateDown = onNavigateDown
                )
            }
        }

        // Dedicated Subtler Carousel Indicator aligned at bottom-right (Recommendation #7)
        if (featuredList.size > 1 && onFeaturedSelect != null) {
            val currentIndex = featuredList.indexOfFirst { it.id == displayItem.id }.coerceAtLeast(0)
            HeroCarouselIndicator(
                items = featuredList,
                currentIndex = currentIndex,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 56.dp, bottom = 18.dp)
            )
        }
    }
}

/**
 * Rectangular action button matching TV viewing ergonomics (Recommendation #6).
 * Solid white background for primary button, visually quieter frosted dark for secondary.
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
    val isReducedMotion = rememberReducedMotion()

    LaunchedEffect(isFocused) {
        onFocusChanged?.invoke(isFocused)
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) TvMotion.FocusScaleButton else 1.0f,
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FAST,
            easing = TvMotion.EasingSilk
        ),
        label = "heroButtonScale"
    )

    val buttonElevation by animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) 10f else 0f,
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FAST,
            easing = TvMotion.EasingSilk
        ),
        label = "heroButtonElevation"
    )

    val targetBgColor = when {
        isPrimary -> if (isFocused) Color.White else Color(0xEEFFFFFF)
        else -> if (isFocused) Color(0x38FFFFFF) else Color(0x221E1E24)
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FAST),
        label = "heroButtonBg"
    )

    val targetBorderColor = when {
        isFocused -> Color.White
        isPrimary -> Color.Transparent
        else -> Color(0x22FFFFFF)
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FAST),
        label = "heroButtonBorder"
    )

    val contentColor = when {
        isPrimary -> PitchBlack
        else -> Color.White
    }

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = text
            }
            .height(48.dp) // Crisp TV button height (Recommendation #6)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
                this.shadowElevation = buttonElevation
            }
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
                width = if (isFocused) 2.dp else if (!isPrimary) 0.dp else 1.dp,
                color = borderColor,
                shape = RectangleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = text,
                style = ErasmusTvTypography.ButtonText.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isPrimary) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Subtler floating carousel indicator with wider active bar (Recommendation #7).
 */
@Composable
private fun HeroCarouselIndicator(
    items: List<MediaItem>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = rememberReducedMotion()

    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Featured title ${currentIndex + 1} of ${items.size}"
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEachIndexed { index, _ ->
                val isActive = index == currentIndex
                val barWidth by animateDpAsState(
                    targetValue = if (isActive) 26.dp else 6.dp, // Wider active indicator (Recommendation #7)
                    animationSpec = tween(
                        durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_MEDIUM,
                        easing = TvMotion.EasingSilk
                    ),
                    label = "carouselBarWidth"
                )
                val barColor by animateColorAsState(
                    targetValue = if (isActive) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.28f),
                    animationSpec = tween(
                        durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_MEDIUM
                    ),
                    label = "carouselBarColor"
                )
                Box(
                    modifier = Modifier
                        .height(2.5.dp)
                        .width(barWidth)
                        .background(barColor, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}



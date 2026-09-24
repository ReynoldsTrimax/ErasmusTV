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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusRadius
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.rememberReducedMotion

/**
 * Hero artwork/content transition duration. Deliberately at the calm end of
 * the 300-700ms range: the hero should feel expensive, never flashy.
 */
private const val HERO_ARTWORK_TRANSITION_MS = TvMotion.DURATION_HERO_CROSSFADE
private const val HERO_CONTENT_TRANSITION_MS = 420

// ---------------------------------------------------------------------------
// Cinematic hero gradients.
//
// Note these deliberately never terminate in opaque PitchBlack. The hero's
// artwork composite is alpha-masked at its lower edge so the screen-level
// ambient wash (HeroAmbientWash) shows through from behind; ending any of
// these in solid black would reintroduce the hard hero/content seam this
// design exists to remove.
// ---------------------------------------------------------------------------

/** Left-to-right readability ramp so hero copy stays legible over artwork. */
private val HeroReadabilityGradient = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.00f to PitchBlack.copy(alpha = 0.92f),
        0.16f to PitchBlack.copy(alpha = 0.80f),
        0.34f to PitchBlack.copy(alpha = 0.56f),
        0.52f to PitchBlack.copy(alpha = 0.30f),
        0.72f to PitchBlack.copy(alpha = 0.10f),
        1.00f to Color.Transparent
    )
)

/** Gentle top-down darkening: shades the nav area and grounds the lower third. */
private val HeroVerticalShade = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to PitchBlack.copy(alpha = 0.55f),
        0.14f to PitchBlack.copy(alpha = 0.22f),
        0.34f to Color.Transparent,
        0.68f to PitchBlack.copy(alpha = 0.26f),
        0.88f to PitchBlack.copy(alpha = 0.52f),
        1.00f to PitchBlack.copy(alpha = 0.68f)
    )
)

/**
 * Alpha mask applied to the artwork composite via [BlendMode.DstIn]: keeps the
 * image opaque down past the hero copy and action buttons, then feathers it out
 * over the last stretch so the frosted layers behind become the visible surface.
 * This is the first stage of the hero's blur ramp — see [HeroFrostBridge] for
 * the mid stage and `HeroFrostedBackdrop` for the destination.
 */
private val HeroBottomFadeMask = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color.Black,
        // Hold the artwork fully sharp past the title, synopsis and buttons, so
        // everything the eye actually reads sits on crisp artwork, never frost.
        0.84f to Color.Black,
        0.90f to Color.Black.copy(alpha = 0.58f),
        0.95f to Color.Black.copy(alpha = 0.26f),
        1.00f to Color.Transparent
    )
)

/**
 * How far the hero's mid-blur bridge extends past the hero's bottom edge.
 *
 * The sharp artwork can only start feathering below the action buttons, which
 * leaves under 60dp of hero to ramp in — too short to read as anything but a
 * step. Letting the mid-blur stage continue past the hero boundary stretches the
 * ramp to roughly 170dp, so the blur climbs through the gap above the first
 * shelf rather than snapping at the hero's edge.
 */
private val HeroFrostBleed = 110.dp

/**
 * Alpha mask for the hero's mid-blur bridge, in fractions of the bridge's own
 * (taller than the hero) height. [heroFraction] is where the hero's bottom edge
 * falls inside it.
 *
 * Absent while the sharp artwork is still solid, full strength by the hero's
 * edge, then easing away through the bleed so the heavy page frost takes over
 * without a seam.
 */
private fun heroFrostBridgeMask(heroFraction: Float): Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color.Transparent,
        heroFraction * 0.83f to Color.Transparent,
        heroFraction * 0.92f to Color.Black.copy(alpha = 0.62f),
        heroFraction to Color.Black,
        (heroFraction + (1f - heroFraction) * 0.36f) to Color.Black.copy(alpha = 0.72f),
        (heroFraction + (1f - heroFraction) * 0.70f) to Color.Black.copy(alpha = 0.32f),
        1.00f to Color.Transparent
    )
)

/**
 * Shading for the bridge band. Picks up roughly where [HeroVerticalShade] has
 * reached by the point the sharp artwork starts to fade, deepens slightly at the
 * hero's edge, then relaxes toward the scrim level of the page-filling frost —
 * so the three blur stages read as one continuous surface, not three images.
 */
private fun heroFrostBridgeShade(heroFraction: Float): Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to PitchBlack.copy(alpha = 0.34f),
        (heroFraction * 0.86f) to PitchBlack.copy(alpha = 0.44f),
        heroFraction to PitchBlack.copy(alpha = 0.58f),
        1.00f to PitchBlack.copy(alpha = 0.48f)
    )
)

/** Soft edge vignette; corners recede without an obvious dark frame. */
private val HeroEdgeVignette = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.00f to PitchBlack.copy(alpha = 0.30f),
        0.10f to Color.Transparent,
        0.90f to Color.Transparent,
        1.00f to PitchBlack.copy(alpha = 0.34f)
    )
)

private val ElectricBlue = Color(0xFF1D90F5)

/**
 * Cinematic Hero Billboard matching Reference Image 2 (The End of Oak Street).
 * Features edge-to-edge full-bleed artwork, top badge chips (FEATURED, MOVIE/SERIES, YEAR, RATING),
 * large authentic title logo, italicized tagline, spacious synopsis, Electric Blue Watch Now pill,
 * frosted Details pill, and carousel slider indicator.
 *
 * ## Focus contract
 *
 * The hero contributes exactly two focusables — Watch Now and Details — and
 * nothing else. The artwork, the badge chips, the tagline, and the carousel
 * indicator are all deliberately non-focusable: a remote should never have to
 * step through decoration to reach an action.
 *
 * @param onFocused invoked when focus enters the hero, so the hosting screen's
 *   focus engine can record the hero as the active zone and send focus back here
 *   when the user drops out of the navigation.
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
    onFocused: (() -> Unit)? = null,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null,
    isAutoAdvanceEnabled: Boolean = true
) {
    val isReducedMotion = rememberReducedMotion()
    val displayItem = item ?: featuredItems.firstOrNull()

    if (displayItem == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(ErasmusDimens.HeroHeight)
                .background(HeroVerticalShade),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ErasmusDimens.HeroContentMaxWidth)
                    .padding(
                        start = ErasmusDimens.HeroContentStartInset,
                        bottom = 64.dp
                    )
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

                // Deliberately no action button here.
                //
                // The previous placeholder was a focusable "Browse Catalog"
                // control wired to `onClick = {}` — a focus target that swallows
                // OK and does nothing, which from six feet away reads as the
                // remote having failed. With nothing focusable in an empty hero
                // the screen's focus engine simply skips this zone and lands on
                // the first shelf that does have content.
            }
        }
        return
    }

    val context = LocalContext.current

    // Artwork resilience: some catalog/seed entries carry a stale backdrop path
    // that 404s upstream. Rather than render an empty hero, fall back to the
    // poster once the backdrop is known to have failed.
    var backdropFailed by remember(displayItem.id) { mutableStateOf(false) }
    val backdropKey = remember(displayItem.id, backdropFailed) {
        val backdrop = displayItem.backdropPath
        val poster = displayItem.posterPath
        when {
            !backdropFailed && !backdrop.isNullOrBlank() -> backdrop
            !poster.isNullOrBlank() -> poster
            else -> backdrop
        }
    }

    val featuredList = remember(featuredItems) { featuredItems.take(8) }
    var isPrimaryFocused by remember { mutableStateOf(false) }
    var isSecondaryFocused by remember { mutableStateOf(false) }

    var heroHasFocus by remember { mutableStateOf(false) }

    // Auto-advance the featured carousel every ~5.5s while the hero is visible.
    //
    // Crucially this does NOT pause while the hero buttons hold focus. Initial
    // focus lands on Watch Now, so gating on focus (as before) meant the
    // carousel was permanently frozen — it never cycled at all. It only stops
    // when the hero scrolls out of view. The displayed title and the buttons'
    // click targets both read from the same rotating state, so activating a
    // button always plays whatever is currently on screen.
    LaunchedEffect(displayItem.id, featuredList, isAutoAdvanceEnabled) {
        if (isAutoAdvanceEnabled && featuredList.size > 1 && onFeaturedSelect != null) {
            delay(5500L)
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
            .height(ErasmusDimens.HeroHeight)
            .onFocusChanged { state ->
                heroHasFocus = state.hasFocus
                if (state.hasFocus) onFocused?.invoke()
            }
    ) {
        // ------------------------------------------------------------------
        // Blur ramp, stage 2 of 3: a mid-radius frost sitting *under* the sharp
        // composite and continuing past the hero's bottom edge. It is revealed
        // exactly as the sharp artwork feathers out, and then dissolves into the
        // page-filling heavy frost below. Because it scrolls with the hero, the
        // whole ramp travels with the content — an equivalent ramp on the fixed
        // backdrop would strand a pale band (and, above it, black) across the
        // screen the moment the feed moved.
        // ------------------------------------------------------------------
        val bridgeHeight = ErasmusDimens.HeroHeight + HeroFrostBleed
        val heroFraction = ErasmusDimens.HeroHeight.value / bridgeHeight.value
        HeroFrostBridge(
            artworkUrl = AppConfig.backdropUrl(backdropKey),
            mask = remember(heroFraction) { heroFrostBridgeMask(heroFraction) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(bridgeHeight),
            overlays = {
                // The hero's own shading, reused verbatim so the bridge is the
                // same surface at a different focus — not a second, brighter image.
                Box(modifier = Modifier.fillMaxSize().background(HeroReadabilityGradient))
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(heroFrostBridgeShade(heroFraction))
                )
                Box(modifier = Modifier.fillMaxSize().background(HeroEdgeVignette))
            }
        )

        // ------------------------------------------------------------------
        // Artwork composite: image + readability/shade/vignette rendered into
        // one offscreen layer, then alpha-masked at the bottom so the whole
        // stack (not just the photo) dissolves into the ambient wash behind.
        // Masking the composite rather than the image alone is what prevents a
        // residual dark band at the seam.
        // ------------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = HeroBottomFadeMask, blendMode = BlendMode.DstIn)
                }
        ) {
            Crossfade(
                targetState = backdropKey,
                animationSpec = tween(
                    durationMillis = if (isReducedMotion) 0 else HERO_ARTWORK_TRANSITION_MS,
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
                        .crossfade(HERO_ARTWORK_TRANSITION_MS)
                        .build()
                }
                var isImageLoaded by remember(key) { mutableStateOf(false) }
                // Very slight settle on load; calm, not a zoom effect.
                val imageScale by animateFloatAsState(
                    targetValue = if (isImageLoaded && !isReducedMotion) 1.0f else 1.02f,
                    animationSpec = tween(durationMillis = 700, easing = TvMotion.EasingSilk),
                    label = "heroBackdropSettle"
                )
                AsyncImage(
                    model = bgRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { isImageLoaded = true },
                    onError = {
                        // Only escalate if the *backdrop* failed; prevents a
                        // loop when the poster fallback also fails.
                        if (key == displayItem.backdropPath) backdropFailed = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                        }
                )
            }

            // Left-weighted readability ramp
            Box(modifier = Modifier.fillMaxSize().background(HeroReadabilityGradient))
            // Top/bottom cinematic shading
            Box(modifier = Modifier.fillMaxSize().background(HeroVerticalShade))
            // Soft edge vignette
            Box(modifier = Modifier.fillMaxSize().background(HeroEdgeVignette))
        }

        // ------------------------------------------------------------------
        // Hero copy: left-aligned, sitting in the middle-to-lower third, and
        // width-capped so the title never sprawls across the screen.
        // Drawn outside the masked layer so text is never faded.
        // ------------------------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(max = ErasmusDimens.HeroContentMaxWidth)
                .padding(
                    start = ErasmusDimens.HeroContentStartInset,
                    bottom = 64.dp
                )
        ) {
            Crossfade(
                targetState = displayItem,
                animationSpec = tween(
                    durationMillis = if (isReducedMotion) 0 else HERO_CONTENT_TRANSITION_MS,
                    easing = TvMotion.EasingSilk
                ),
                label = "hero_content_crossfade"
            ) { currentItem ->
                var contentVisible by remember(currentItem.id) { mutableStateOf(false) }
                LaunchedEffect(currentItem.id) {
                    contentVisible = true
                }
                val translateY by animateDpAsState(
                    targetValue = if (contentVisible) 0.dp else 10.dp,
                    animationSpec = tween(
                        durationMillis = if (isReducedMotion) 0 else HERO_CONTENT_TRANSITION_MS,
                        easing = TvMotion.EasingSilk
                    ),
                    label = "hero_content_slide"
                )
                val contentAlpha by animateFloatAsState(
                    targetValue = if (contentVisible) 1f else 0.35f,
                    animationSpec = tween(
                        durationMillis = if (isReducedMotion) 0 else HERO_CONTENT_TRANSITION_MS,
                        easing = TvMotion.EasingSilk
                    ),
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
                                .heightIn(
                                    min = ErasmusDimens.HeroLogoMinHeight,
                                    max = ErasmusDimens.HeroLogoMaxHeight
                                )
                                .fillMaxWidth(),
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
                                    .heightIn(
                                        min = ErasmusDimens.HeroLogoMinHeight,
                                        max = ErasmusDimens.HeroLogoMaxHeight
                                    )
                                    .fillMaxWidth(),
                                onError = { isLogoError = true }
                            )
                        }
                    } else {
                        // Large but controlled: capped at two lines within the
                        // width-limited copy column so it can't dominate.
                        Text(
                            text = currentItem.title,
                            style = ErasmusTvTypography.HeroTitleLarge.copy(
                                fontSize = 34.sp,
                                lineHeight = 39.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.6).sp
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
                    onNavigateUp = onNavigateUp,
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
                    onNavigateDown = onNavigateDown,
                    onNavigateUp = onNavigateUp
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
 * Hero actions delegate to the shared [ErasmusActionButton] so the hero's
 * "Watch Now" and a detail page's "Add to Watch List" are literally the same
 * control. Only the hero's directional hand-off wiring lives here.
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
    onNavigateDown: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null
) {
    ErasmusActionButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        style = if (isPrimary) ErasmusButtonStyle.Primary else ErasmusButtonStyle.Secondary,
        shape = CircleShape,
        onFocusChanged = onFocusChanged,
        onNavigateLeft = onNavigateLeft,
        onNavigateRight = onNavigateRight,
        // UP/DOWN are always consumed at the hero boundary: without this, focus
        // can drift out of the hero into whatever happens to be geometrically
        // nearest, which on a TV feels like the remote stopped working.
        onNavigateUp = onNavigateUp ?: {},
        onNavigateDown = onNavigateDown ?: {}
    )
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



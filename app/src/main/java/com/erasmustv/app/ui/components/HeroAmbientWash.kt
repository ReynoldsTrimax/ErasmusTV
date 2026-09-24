package com.erasmustv.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.PitchBlack

/**
 * ERASMUS FROSTED HERO BACKDROP.
 *
 * A full-page continuation of the hero artwork: the same image, frosted so
 * heavily that its subject is unreadable but its colour is preserved, painted
 * behind the entire scrolling feed. It replaces the earlier synthetic colour
 * wash, which stopped a few hundred pixels below the hero and left the rest of
 * the page flat black — so the atmosphere no longer "runs out" partway down.
 *
 * ## One uninterrupted field, ramped inside the hero
 *
 * This layer is **fully opaque everywhere**, at one constant heavy radius. It
 * is deliberately *not* ramped in screen space: this surface does not scroll,
 * so any vertical alpha ramp on it is pinned to the display rather than to the
 * content. An earlier version faded it in below the hero's band, which looked
 * right at rest but opened a black gap across the top of the screen the moment
 * the feed scrolled and the sharp hero moved out of that band.
 *
 * The sharp → frosted ramp therefore lives in the hero itself, which *does*
 * scroll: [HeroFrostBridge] paints a mid-radius copy of the artwork across the
 * hero's lower edge, between the sharp artwork fading out and this layer
 * showing through. The apparent blur climbs sharp → mid → heavy, and because
 * every stage of that ramp is bounded by the hero item it travels with the
 * content instead of smearing across a fixed band of the screen.
 *
 * ## How the frost is achieved without a guaranteed blur
 *
 * `Modifier.blur` is API 31+ only (minSdk 26), so the frost also comes from
 * **downsampling**: the artwork is decoded at [SAMPLE_PX] and stretched across
 * the screen — a real, GPU-cheap blur that works on every API level. On 31+ the
 * heavy radius deepens it further; below 31 the downsample alone carries it.
 *
 * @param artworkUrl the hero's backdrop (or poster fallback) — the *same* URL
 *   the sharp hero uses, so the frosted field and the hero are the one image.
 * @param ambientColor artwork-derived tone, layered on at low alpha to
 *   guarantee a hero-matched hue even when a muddy downsample loses it.
 * @param heroHeight height of the sharp hero above, used to place the ramp so
 *   its full strength lands just past the hero and the first content row.
 */
@Composable
fun HeroFrostedBackdrop(
    artworkUrl: String?,
    ambientColor: Color,
    modifier: Modifier = Modifier,
    heroHeight: Dp = ErasmusDimens.HeroHeight,
    fallbackArtworkUrl: String? = null
) {
    // Artwork paths rot: TMDB 404s an old image path once the artwork behind it
    // is replaced, and a dead URL here left the entire page black. The hero's
    // sharp artwork already falls back to the poster on error; this layer now
    // does the same, so the page can never lose its background to one stale path.
    var useFallback by remember(artworkUrl, fallbackArtworkUrl) { mutableStateOf(false) }
    val effectiveUrl = if (useFallback && !fallbackArtworkUrl.isNullOrBlank()) {
        fallbackArtworkUrl
    } else {
        artworkUrl
    }
    val request = rememberFrostRequest(effectiveUrl, SAMPLE_PX)

    Box(modifier = modifier.fillMaxSize()) {
        if (request != null) {
            // One constant, fully opaque frost across the entire page. No
            // vertical ramp: this surface is pinned to the screen, so a ramp
            // here would leave a black band at the top as soon as the feed
            // scrolls. The ramp belongs to the hero — see HeroFrostBridge.
            FrostedArtworkLayer(
                request = request,
                blurRadius = BLUR_HEAVY,
                mask = null,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    if (!useFallback && !fallbackArtworkUrl.isNullOrBlank()) useFallback = true
                }
            )
        }

        // Scrim + tint. Kept close to uniform on purpose: this surface does not
        // scroll, so a steep vertical ramp here would be stranded against moving
        // content. It only has to keep rows legible over the frost at *any*
        // scroll position, and wash the hero's hue over the page so the colour
        // reads as deliberate atmosphere rather than a muddy photo.
        //
        // The gradients are built inside the draw scope, against the real
        // measured height. An optimisation pass moved them out to avoid the
        // per-draw allocation, but that meant guessing the hero's height
        // fraction on the first frame; this layer is static, so it redraws
        // almost never and the allocation costs nothing worth having a
        // one-frame difference in the backdrop for.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val heroPx = heroHeight.toPx()
                    val total = size.height.coerceAtLeast(1f)
                    val heroEnd = (heroPx / total).coerceIn(0.05f, 0.95f)

                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to PitchBlack.copy(alpha = 0.34f),
                                heroEnd to PitchBlack.copy(alpha = 0.46f),
                                1f to PitchBlack.copy(alpha = 0.66f)
                            )
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to ambientColor.copy(alpha = 0.08f),
                                heroEnd to ambientColor.copy(alpha = 0.12f),
                                1f to ambientColor.copy(alpha = 0.18f)
                            )
                        )
                    )
                }
        )
    }
}

/**
 * One frosted copy of the artwork at a fixed [blurRadius].
 *
 * The image and any [overlays] are composited into a single offscreen layer,
 * then — if [mask] is non-null — multiplied by that mask through
 * [BlendMode.DstIn], so a layer can be confined to a vertical band with its
 * shading intact. Masking the *composite* rather than the image alone is what
 * stops a residual dark edge appearing where a layer fades out.
 *
 * @param mask alpha mask; null means the layer is fully opaque everywhere.
 */
@Composable
internal fun FrostedArtworkLayer(
    request: ImageRequest,
    blurRadius: Dp,
    mask: Brush?,
    modifier: Modifier = Modifier,
    onError: (() -> Unit)? = null,
    overlays: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            // Left unconditional. Offscreen compositing is only strictly needed
            // when the mask below has to multiply the composite, but this layer
            // is the frost pipeline and the frost is being kept byte-for-byte as
            // designed — an offscreen buffer also clips its contents, so removing
            // it is only *probably* neutral, and "probably" is not good enough
            // for the app's most visible surface.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .then(
                if (mask != null) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(brush = mask, blendMode = BlendMode.DstIn)
                    }
                } else {
                    Modifier
                }
            )
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            onError = { onError?.invoke() },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    // Modifier.blur is a no-op below API 31; the decode-size
                    // downsample is what guarantees frost on every API level,
                    // and on 31+ this radius is what turns the downsample's
                    // colour blocks into one continuous field.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(blurRadius)
                    } else {
                        Modifier
                    }
                )
        )
        overlays()
    }
}

/**
 * Builds the downsampled request that makes the frost. [samplePx] is the decode
 * width in pixels: the smaller it is, the blurrier the stretched result, and the
 * cheaper it is to hold in memory.
 */
@Composable
internal fun rememberFrostRequest(artworkUrl: String?, samplePx: Int): ImageRequest? {
    val context = LocalContext.current
    return remember(artworkUrl, samplePx) {
        if (artworkUrl.isNullOrBlank()) null
        else ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(samplePx)
            // Deliberately full-colour. A 16-bit config saves memory but bands
            // badly on a surface this heavily stretched and blurred, which is
            // precisely where smooth colour gradation is the whole point.
            .crossfade(true)
            .build()
    }
}

/**
 * The middle stage of the hero's blur ramp.
 *
 * Drawn *beneath* the hero's sharp artwork composite and confined by [mask] to
 * the hero's lower edge, so it is revealed exactly as the sharp artwork feathers
 * out, and then itself hands over to the page-filling [HeroFrostedBackdrop]
 * behind it. Because it lives inside the hero item, its band scrolls with the
 * hero — which is why this ramp cannot leave a fixed grey or black stripe
 * stranded on the screen.
 *
 * @param artworkUrl the same backdrop the sharp hero is showing.
 * @param overlays the hero's own shading gradients, composited in so the bridge
 *   matches the tone of the artwork above it rather than reading as a bright band.
 */
@Composable
fun HeroFrostBridge(
    artworkUrl: String?,
    mask: Brush,
    modifier: Modifier = Modifier,
    overlays: @Composable BoxScope.() -> Unit = {}
) {
    val request = rememberFrostRequest(artworkUrl, BRIDGE_SAMPLE_PX) ?: return
    FrostedArtworkLayer(
        request = request,
        blurRadius = BLUR_BRIDGE,
        mask = mask,
        modifier = modifier,
        overlays = overlays
    )
}

/** Decode width of the page-filling frost. Tiny on purpose: the downsample IS the frost. */
private const val SAMPLE_PX = 56

/** Decode width of the hero's mid-blur bridge — frosted, but not yet obliterated. */
private const val BRIDGE_SAMPLE_PX = 190

/**
 * Radius of the page-filling frost on API 31+.
 *
 * Restored to its original heavy radius. An earlier optimisation pass set this
 * to zero on the theory that a 56px sample stretched across the panel is already
 * blurred past the point a GPU blur contributes — that theory was wrong in
 * practice: the stretch produces large, hard-edged colour blocks, and it is this
 * blur that dissolves them into a continuous field. The frost is load-bearing
 * for the design, so it stays and the performance work is done elsewhere.
 */
private val BLUR_HEAVY = 96.dp

/** Mid radius for the hero's bridge band. */
private val BLUR_BRIDGE = 20.dp

/**
 * ERASMUS HERO AMBIENCE — cinematic light spill.
 *
 * Renders the atmospheric colour wash that makes the hero flow into the
 * content below it, eliminating the hard horizontal seam where artwork would
 * otherwise stop and a flat black feed would begin.
 *
 * This is drawn at **screen level, beneath the scrolling content** — not
 * inside the hero. The hero is one item in a `LazyColumn`, so anything drawn
 * within it is bounded by that item and cannot bleed behind the rows below.
 * Painting underneath the whole feed is what lets the atmosphere genuinely
 * continue behind content.
 *
 * Composition (all low-alpha, heavily feathered):
 *  1. A broad elliptical spill anchored near the hero's lower-left, where the
 *     title and actions sit — the "light source" of the composition.
 *  2. A vertical bleed that carries the tone downward past the hero edge and
 *     decays to nothing over several hundred pixels.
 *  3. A horizontal falloff so the right side stays darker, preserving the
 *     left-weighted cinematic balance.
 *
 * Smoothness comes from many closely-spaced gradient stops rather than a real
 * blur: `Modifier.blur` is API 31+ (minSdk here is 26) and blurring a
 * full-width surface every frame is a non-starter on low-end TV chipsets.
 * Multi-stop gradients are resolved by the GPU in one pass.
 *
 * @param ambientColor artwork-derived tone (see `rememberHeroAmbientColor`).
 * @param intensity 0f..1f master strength, typically driven by scroll so the
 *   wash recedes as the hero scrolls away.
 * @param heroHeight height of the hero, used to anchor the spill's centre.
 * @param bleedBelow how far the atmosphere extends past the hero's bottom.
 */
@Composable
fun HeroAmbientWash(
    ambientColor: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    heroHeight: Dp = ErasmusDimens.HeroHeight,
    bleedBelow: Dp = ErasmusDimens.HeroAmbientBleed
) {
    if (intensity <= 0.01f) return
    val clamped = intensity.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight + bleedBelow)
            .drawBehind {
                val heroPx = heroHeight.toPx()
                val totalPx = size.height

                // --- 1. Elliptical light spill anchored at the hero's lower-left ---
                // Oversized radius keeps the falloff gentle and shapeless; a
                // tight radius would read as a visible coloured blob.
                val spillCenter = Offset(x = size.width * 0.26f, y = heroPx * 0.82f)
                val spillRadius = maxOf(size.width, totalPx) * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to ambientColor.copy(alpha = 0.34f * clamped),
                            0.18f to ambientColor.copy(alpha = 0.27f * clamped),
                            0.36f to ambientColor.copy(alpha = 0.185f * clamped),
                            0.54f to ambientColor.copy(alpha = 0.105f * clamped),
                            0.72f to ambientColor.copy(alpha = 0.045f * clamped),
                            0.88f to ambientColor.copy(alpha = 0.014f * clamped),
                            1.00f to Color.Transparent
                        ),
                        center = spillCenter,
                        radius = spillRadius
                    ),
                    radius = spillRadius,
                    center = spillCenter
                )

                // --- 2. Vertical bleed carrying the tone below the hero edge ---
                // Peaks just past the hero boundary so there is no step where
                // the artwork ends, then decays across the remaining bleed.
                val heroFraction = (heroPx / totalPx).coerceIn(0.05f, 0.95f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            (heroFraction * 0.55f) to ambientColor.copy(alpha = 0.03f * clamped),
                            (heroFraction * 0.82f) to ambientColor.copy(alpha = 0.12f * clamped),
                            heroFraction to ambientColor.copy(alpha = 0.19f * clamped),
                            (heroFraction + (1f - heroFraction) * 0.18f) to
                                ambientColor.copy(alpha = 0.14f * clamped),
                            (heroFraction + (1f - heroFraction) * 0.38f) to
                                ambientColor.copy(alpha = 0.085f * clamped),
                            (heroFraction + (1f - heroFraction) * 0.58f) to
                                ambientColor.copy(alpha = 0.042f * clamped),
                            (heroFraction + (1f - heroFraction) * 0.78f) to
                                ambientColor.copy(alpha = 0.015f * clamped),
                            0.94f to ambientColor.copy(alpha = 0.004f * clamped),
                            1f to Color.Transparent
                        )
                    ),
                    size = Size(size.width, totalPx)
                )

                // --- 3. Horizontal falloff keeping the right side recessive ---
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to ambientColor.copy(alpha = 0.06f * clamped),
                            0.30f to ambientColor.copy(alpha = 0.03f * clamped),
                            0.62f to ambientColor.copy(alpha = 0.008f * clamped),
                            1.00f to Color.Transparent
                        )
                    ),
                    size = Size(size.width, totalPx)
                )
            }
    )
}

/**
 * Computes the wash [intensity] from the feed's scroll position so the
 * atmosphere recedes once the hero is scrolled away, and returns fully when
 * the user comes back to the top.
 *
 * Kept as plain math on already-available scroll state — no measurement pass
 * and no image work, so it is safe to read on every frame.
 */
fun heroAmbientIntensity(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    heroHeightPx: Float
): Float {
    if (firstVisibleItemIndex > 0) return 0f
    if (heroHeightPx <= 0f) return 1f
    val progress = (firstVisibleItemScrollOffset / heroHeightPx).coerceIn(0f, 1f)
    // Hold near full strength early, then ease out.
    return (1f - progress).let { it * it }.coerceIn(0f, 1f)
}

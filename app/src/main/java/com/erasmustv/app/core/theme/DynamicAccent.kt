package com.erasmustv.app.core.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ERASMUS DESIGN SYSTEM — Dynamic Accent
 *
 * Erasmus intentionally avoids a single permanent bright accent color (no
 * fixed brand red/blue baked into every screen). Instead, screens that
 * display hero artwork may derive a subtle accent from that artwork and
 * provide it down the tree via [LocalAccentColor]. Screens/components that
 * don't opt in simply see [AccentNeutral], the muted warm-neutral default.
 *
 * The extracted color is deliberately desaturated and darkened before use —
 * raw palette swatches from posters/backdrops are often too saturated/bright
 * for a 10-foot dark UI, so [towardsSubtleAccent] tames it into something
 * usable as a focus glow tint or hairline accent rather than a loud hue.
 */
val LocalAccentColor = compositionLocalOf { AccentNeutral }

/**
 * Extracts a muted accent [Color] from [bitmap] using [Palette], preferring
 * a vibrant-but-dark swatch (better for a dark UI than a light/dominant one),
 * then tames it via [towardsSubtleAccent]. Falls back to [AccentNeutral] if
 * no usable swatch is found. Runs the Palette extraction off the main thread.
 */
suspend fun extractAccentColor(bitmap: Bitmap): Color = withContext(Dispatchers.Default) {
    runCatching {
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.darkVibrantSwatch
            ?: palette.vibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch
        swatch?.let { towardsSubtleAccent(Color(it.rgb)) } ?: AccentNeutral
    }.getOrDefault(AccentNeutral)
}

/**
 * Tames a raw extracted swatch into a restrained accent suitable for subtle
 * borders/glows on a dark canvas: desaturates slightly and blends toward the
 * neutral accent so the result never overpowers the near-black surfaces.
 */
fun towardsSubtleAccent(raw: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(raw.toArgbInt(), hsl)
    // Cap lightness/saturation so the accent stays "subtle" per design spec.
    hsl[1] = (hsl[1] * 0.65f).coerceIn(0f, 0.55f)
    hsl[2] = hsl[2].coerceIn(0.35f, 0.62f)
    val tamed = Color(ColorUtils.HSLToColor(hsl))
    return lerp(tamed, AccentNeutral, 0.2f)
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb(
    (alpha * 255f).toInt(),
    (red * 255f).toInt(),
    (green * 255f).toInt(),
    (blue * 255f).toInt()
)

// ===========================================================================
// HERO AMBIENCE — artwork-derived atmospheric color
// ===========================================================================
// The hero's "light spill" hue. Unlike [extractAccentColor] (which produces a
// mid-tone accent for borders/glows), this produces a *deep, desaturated*
// tone intended to be washed across a large area at low alpha behind content.
//
// Results are cached per artwork key for the process lifetime, so switching
// back to a previously-seen hero is instant and no bitmap work repeats. The
// bitmap is decoded at a tiny size (64px) purely for palette sampling — this
// never touches the full-resolution image and never runs per frame.

/** Default ambience when artwork is unavailable or still resolving. */
val AmbientNeutral = Color(0xFF14141A)

private val ambientCache = ConcurrentHashMap<String, Color>()

/** Lowered saturation/lightness so a large wash never reads as a colored website gradient. */
fun towardsAmbient(raw: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(raw.toArgbInt(), hsl)
    // Keep the hue; tame saturation and lightness hard so a large wash reads
    // as atmosphere rather than as a coloured panel.
    hsl[1] = hsl[1].coerceIn(0.10f, 0.30f)
    hsl[2] = hsl[2].coerceIn(0.14f, 0.24f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Extracts the ambient tone for [imageUrl], caching by [cacheKey].
 * Decodes a 64px sample off the main thread; returns [AmbientNeutral] on any failure.
 */
suspend fun extractAmbientColor(
    context: Context,
    imageUrl: String?,
    cacheKey: String,
    fallbackUrl: String? = null
): Color {
    if (imageUrl.isNullOrBlank() && fallbackUrl.isNullOrBlank()) return AmbientNeutral
    ambientCache[cacheKey]?.let { return it }

    return withContext(Dispatchers.IO) {
        // Try the primary artwork, then the fallback: catalog entries with a
        // stale backdrop path still yield the right ambience from their poster.
        val candidates = listOfNotNull(
            imageUrl?.takeIf { it.isNotBlank() },
            fallbackUrl?.takeIf { it.isNotBlank() }
        )

        var result = AmbientNeutral
        for (url in candidates) {
            val extracted = runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    // Palette requires a software bitmap; hardware bitmaps can't be read.
                    .allowHardware(false)
                    .size(64)
                    .build()
                val bitmap = (context.imageLoader.execute(request) as? SuccessResult)
                    ?.drawable
                    ?.toBitmap()
                    ?: return@runCatching null

                val palette = Palette.from(bitmap).clearFilters().maximumColorCount(16).generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.dominantSwatch
                    ?: palette.mutedSwatch
                swatch?.let { towardsAmbient(Color(it.rgb)) }
            }.getOrNull()

            if (extracted != null) {
                result = extracted
                break
            }
        }

        ambientCache[cacheKey] = result
        result
    }
}

/**
 * Resolves and smoothly animates the ambient tone for the current hero artwork.
 *
 * Extraction is keyed on [artworkPath], so it runs once per artwork and is
 * served from cache thereafter. The returned color animates over
 * [transitionMillis] so hero switches feel calm rather than snapping.
 */
@Composable
fun rememberHeroAmbientColor(
    artworkPath: String?,
    imageUrl: String? = artworkPath,
    fallbackImageUrl: String? = null,
    transitionMillis: Int = 600
): Color {
    val context = LocalContext.current
    val isReducedMotion = rememberReducedMotion()

    // Seed synchronously from cache so a revisited hero doesn't fade in again.
    var resolved by remember(artworkPath) {
        mutableStateOf(artworkPath?.let { ambientCache[it] } ?: AmbientNeutral)
    }

    LaunchedEffect(artworkPath, imageUrl, fallbackImageUrl) {
        resolved = if (artworkPath != null) {
            extractAmbientColor(context, imageUrl, artworkPath, fallbackImageUrl)
        } else {
            AmbientNeutral
        }
    }

    val animated by animateColorAsState(
        targetValue = resolved,
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 0 else transitionMillis,
            easing = TvMotion.EasingSilk
        ),
        label = "heroAmbientColor"
    )
    return animated
}

/**
 * Holds the current hero-derived accent as Compose state, defaulting to
 * [AccentNeutral] until [DynamicAccentState.update] is called with a bitmap.
 * Intended to be created with `remember { DynamicAccentState() }` at the
 * screen level and provided via `CompositionLocalProvider(LocalAccentColor
 * provides state.color) { ... }`; not wired into any screen in this phase.
 */
class DynamicAccentState {
    var color: Color by mutableStateOf(AccentNeutral)
        private set

    suspend fun update(bitmap: Bitmap?) {
        color = if (bitmap == null) AccentNeutral else extractAccentColor(bitmap)
    }

    fun reset() {
        color = AccentNeutral
    }
}

@Composable
fun rememberDynamicAccentState(): DynamicAccentState = remember { DynamicAccentState() }

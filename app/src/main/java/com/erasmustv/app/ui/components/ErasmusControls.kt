package com.erasmustv.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.ErasmusSpacing
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.TvSpring
import com.erasmustv.app.core.theme.floatSpec
import com.erasmustv.app.core.theme.rememberReducedMotion

/**
 * ERASMUS CONTROLS — buttons, chips, page headers, and empty states.
 *
 * One definition per control, shared by the hero, detail pages, search,
 * watch list, and every error/empty state. This is what keeps a "Watch Now"
 * on the hero and an "Add to Watch List" on a detail page from drifting into
 * two different button languages.
 */

enum class ErasmusButtonStyle {
    /** Solid warm-white fill with dark label — one per screen, maximum one. */
    Primary,

    /** Translucent fill with a hairline edge — supporting actions. */
    Secondary,

    /** No fill at rest; gains a quiet capsule on focus — tertiary actions. */
    Ghost
}

/**
 * The single Erasmus action button.
 *
 * Focus response is deliberately multi-signal but small: a 1.05 scale, a soft
 * elevation, a brightness gain on the fill, and a crisp outline. None of those
 * alone would be unmistakable at 10 feet; together they are, without any one
 * of them being loud.
 *
 * Directional callbacks are accepted because TV buttons frequently sit at the
 * boundary of a focus region (hero actions, dialog rows) and need to hand
 * focus off deterministically rather than relying on geometric traversal.
 */
@Composable
fun ErasmusActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: ErasmusButtonStyle = ErasmusButtonStyle.Secondary,
    height: Dp = ErasmusDimens.HeroButtonHeight,
    shape: Shape = ErasmusShapes.Button,
    contentDescriptionOverride: String? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isReducedMotion = rememberReducedMotion()

    LaunchedEffect(isFocused) { onFocusChanged?.invoke(isFocused) }

    // Lift and shadow on a snappy critically damped spring (1.0 / 0.25s) —
    // button focus is the shortest path between remote and screen, so it gets
    // the quickest response in the vocabulary.
    val liftSpec = TvSpring.FocusFast.floatSpec(isReducedMotion)

    val scale by animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) TvMotion.FocusScaleButton else 1f,
        animationSpec = liftSpec,
        label = "buttonScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) 10f else 0f,
        animationSpec = liftSpec,
        label = "buttonElevation"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (style) {
            ErasmusButtonStyle.Primary ->
                if (isFocused) Color.White else Color.White.copy(alpha = 0.93f)
            ErasmusButtonStyle.Secondary ->
                if (isFocused) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f)
            ErasmusButtonStyle.Ghost ->
                if (isFocused) Color.White.copy(alpha = 0.14f) else Color.Transparent
        },
        animationSpec = tween(if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS_FAST),
        label = "buttonBackground"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            style == ErasmusButtonStyle.Primary -> Color.Transparent
            style == ErasmusButtonStyle.Secondary -> Color.White.copy(alpha = 0.16f)
            else -> Color.Transparent
        },
        animationSpec = tween(if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS_FAST),
        label = "buttonBorder"
    )

    val contentColor = when (style) {
        ErasmusButtonStyle.Primary -> PitchBlack
        else -> if (isFocused) TextPrimary else TextPrimary.copy(alpha = 0.88f)
    }

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = contentDescriptionOverride ?: text
            }
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation
                // Without a shape, the elevation shadow is cast on the layer's
                // default rectangular outline — a rounded-rectangle halo behind a
                // pill-shaped button. Handing the layer the button's own shape
                // makes the shadow follow the pill; clip stays off so the border
                // and fill (applied below) are never sliced.
                this.shape = shape
                clip = false
            }
            .onKeyEvent { keyEvent ->
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> when (keyEvent.key) {
                        Key.DirectionRight -> onNavigateRight?.let { it(); true } ?: false
                        Key.DirectionLeft -> onNavigateLeft?.let { it(); true } ?: false
                        Key.DirectionUp -> onNavigateUp?.let { it(); true } ?: false
                        Key.DirectionDown -> onNavigateDown?.let { it(); true } ?: false
                        else -> false
                    }
                    else -> false
                }
            }
            .clip(shape)
            .background(backgroundColor, shape)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
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
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = text,
                style = ErasmusTvTypography.ButtonText.copy(
                    fontWeight = if (style == ErasmusButtonStyle.Primary) {
                        FontWeight.Bold
                    } else {
                        FontWeight.SemiBold
                    }
                ),
                color = contentColor,
                // A button label is a single word or short phrase; it must never
                // wrap. Without this, a narrow hero column broke "Details" onto a
                // second line and clipped it to "Detail / s".
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Compact rounded selection chip — genres, alphabet letters, filters, sorts.
 *
 * `isSelected` (a persistent state) and focus (a transient state) are rendered
 * differently on purpose: a selected chip carries a filled surface, while a
 * focused chip carries an outline. That way the user can always see both which
 * filter is active *and* where the cursor is.
 */
@Composable
fun ErasmusChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    minWidth: Dp = Dp.Unspecified,
    height: Dp = 36.dp,
    contentDescriptionOverride: String? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isReducedMotion = rememberReducedMotion()

    LaunchedEffect(isFocused) { onFocusChanged?.invoke(isFocused) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) 1.05f else 1f,
        animationSpec = TvSpring.FocusFast.floatSpec(isReducedMotion),
        label = "chipScale"
    )

    val background by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White.copy(alpha = 0.22f)
            isSelected -> Color.White.copy(alpha = 0.13f)
            else -> Color.White.copy(alpha = 0.05f)
        },
        animationSpec = tween(if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS_FAST),
        label = "chipBackground"
    )

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = contentDescriptionOverride ?: label
            }
            .height(height)
            .then(if (minWidth != Dp.Unspecified) Modifier.widthIn(min = minWidth) else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(ErasmusShapes.Button)
            .background(background, ErasmusShapes.Button)
            .border(
                width = 1.dp,
                color = when {
                    isFocused -> Color.White
                    isSelected -> Color.White.copy(alpha = 0.28f)
                    else -> Color.White.copy(alpha = 0.10f)
                },
                shape = ErasmusShapes.Button
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = ErasmusTvTypography.ButtonText.copy(
                fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = when {
                isFocused -> TextPrimary
                isSelected -> TextPrimary.copy(alpha = 0.95f)
                else -> TextSecondary
            },
            maxLines = 1
        )
    }
}

/**
 * Top-level page heading for non-hero pages (Watch List, Studios, Search).
 * Left-aligned and unadorned — no rule, no container, no icon.
 */
@Composable
fun ErasmusPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = ErasmusTvTypography.PageTitle
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(ErasmusSpacing.Small))
            Text(
                text = subtitle,
                style = ErasmusTvTypography.PageSubtitle,
                color = TextSecondary
            )
        }
    }
}

/**
 * Minimal empty state: a small muted glyph, a short line, and one instruction.
 *
 * Intentionally restrained — a large illustration in an empty state reads as
 * an apology, and on a TV it dominates a screen the user is passing through.
 */
@Composable
fun ErasmusEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(ErasmusSpacing.Medium))
        }
        Text(
            text = title,
            style = ErasmusTvTypography.SectionTitle,
            color = TextPrimary.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(ErasmusSpacing.Small))
        Text(
            text = message,
            style = ErasmusTvTypography.Body,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp)
        )
    }
}

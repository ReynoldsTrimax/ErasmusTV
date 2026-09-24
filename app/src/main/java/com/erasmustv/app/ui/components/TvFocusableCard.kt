package com.erasmustv.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.erasmustv.app.core.theme.ErasmusElevation
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.TvSpring
import com.erasmustv.app.core.theme.floatSpec
import com.erasmustv.app.core.theme.rememberReducedMotion

/**
 * The single focusable primitive behind every Erasmus card, tile, and poster.
 *
 * Focus is communicated through a combination of restrained signals rather
 * than one loud one: a small scale lift, soft elevation, a hairline outline,
 * and (in the card wrappers above this) a slight brightness gain. Nothing
 * here glows, pulses, or neon-outlines.
 *
 * Two behaviours matter for TV correctness:
 *  - `clip = false` on the scaling layer, so a lifted card is never sliced by
 *    its own bounds.
 *  - `zIndex` is raised while focused, so the lifted card draws above its
 *    neighbours in a rail instead of being overlapped by the next sibling.
 */
@Composable
fun TvFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ErasmusShapes.Card,
    focusedScale: Float = TvMotion.FocusScaleCard,
    focusedBorderWidth: Dp = 1.5.dp,
    unfocusedBorderWidth: Dp = 0.dp,
    focusedBorderColor: Color = FocusWhite,
    unfocusedBorderColor: Color = Color.Transparent,
    focusedElevation: Dp = ErasmusElevation.Focused,
    contentDescription: String? = null,
    role: Role? = Role.Button,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isReducedMotion = rememberReducedMotion()

    // Spatial focus signals ride a critically damped spring (damping 1.0,
    // response 0.30s). Held-down D-pad traversal retargets this far faster than
    // any duration could finish, and a spring picks up from the card's current
    // scale and velocity instead of restarting an interpolation.
    val liftSpec = remember(isReducedMotion) { TvSpring.Focus.floatSpec(isReducedMotion) }

    // The outline is pure opacity, so it stays on a short curve.
    val outlineSpec = remember(isReducedMotion) {
        tween<Float>(
            durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS,
            easing = TvMotion.EasingSilk
        )
    }

    // These are deliberately kept as State objects rather than destructured with
    // `by`. Reading `.value` inside the graphicsLayer lambda keeps the animation
    // in the layout/draw phase, so a focus change re-runs only the layer block —
    // not the composition of every card in the rail. With 20+ shelves on Home
    // that difference is the difference between smooth and janky traversal.
    val scale = animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) focusedScale else 1f,
        animationSpec = liftSpec,
        label = "tvCardScale"
    )

    val targetElevationPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        focusedElevation.toPx()
    }
    val shadowElevation = animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) targetElevationPx else 0f,
        animationSpec = liftSpec,
        label = "tvCardShadow"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else 0.0f,
        animationSpec = outlineSpec,
        label = "tvCardBorderAlpha"
    )

    val hasBorder = (isFocused && focusedBorderWidth > 0.dp && focusedBorderColor != Color.Transparent) ||
            (!isFocused && unfocusedBorderWidth > 0.dp && unfocusedBorderColor != Color.Transparent)

    Box(
        modifier = modifier
            // A lifted card must paint above its rail neighbours.
            .zIndex(if (isFocused) 1f else 0f)
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                if (role != null) {
                    this.role = role
                }
            }
            .graphicsLayer {
                val s = scale.value
                scaleX = s
                scaleY = s
                this.shape = shape
                this.shadowElevation = shadowElevation.value
                ambientShadowColor = ErasmusElevation.ShadowAmbientColor
                spotShadowColor = ErasmusElevation.ShadowSpotColor
                clip = false
            }
            .then(
                if (hasBorder) {
                    Modifier.border(
                        BorderStroke(
                            width = if (isFocused) focusedBorderWidth else unfocusedBorderWidth,
                            color = if (isFocused)
                                focusedBorderColor.copy(alpha = borderAlpha)
                            else
                                unfocusedBorderColor
                        ),
                        shape = shape
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
    ) {
        content(isFocused)
    }
}

package com.erasmustv.app.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.erasmustv.app.core.theme.FocusWhite

@Composable
fun TvFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    focusedScale: Float = 1.04f,
    focusedBorderWidth: Dp = 1.5.dp,
    unfocusedBorderWidth: Dp = 0.dp,
    focusedBorderColor: Color = FocusWhite,
    unfocusedBorderColor: Color = Color.Transparent,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Only run the scale animation when a real scale delta exists.
    // If focusedScale == 1.0f (most cards), skip the animator entirely to save
    // one animateFloatAsState instance per card — which on a full LazyRow is
    // 10-20 concurrent animators running for zero visual effect.
    val scale = if (focusedScale != 1.0f) {
        val animatedScale by animateFloatAsState(
            targetValue = if (isFocused) focusedScale else 1.0f,
            animationSpec = tween(durationMillis = 100, easing = FastOutLinearInEasing),
            label = "tvCardScale"
        )
        animatedScale
    } else {
        1.0f
    }

    // Border alpha drives focus feedback — keep it fast (80ms) so the
    // selection indicator appears immediately without feeling sluggish.
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutLinearInEasing),
        label = "tvCardBorderAlpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.shape = shape
                clip = false
            }
            .border(
                BorderStroke(
                    width = if (isFocused) focusedBorderWidth else unfocusedBorderWidth,
                    color = if (isFocused)
                        focusedBorderColor.copy(alpha = borderAlpha)
                    else
                        unfocusedBorderColor
                ),
                shape = shape
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

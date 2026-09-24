package com.erasmustv.app.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.BuildConfig

/**
 * ERASMUS FOCUS DEBUG OVERLAY — development only.
 *
 * Spatial focus is hard to debug from the couch: when DOWN lands somewhere
 * unexpected you cannot tell from the screen whether the travelling column was
 * wrong, the destination zone was not registered, or the zone order does not
 * match the layout. This surfaces exactly those three facts.
 *
 * Reads live engine state rather than logging, so holding a direction shows the
 * column updating in real time.
 *
 * ## Enabling it
 *
 * Gated twice over: once on [BuildConfig.DEBUG] so it cannot ship, and once on
 * [FocusDebug.enabled] so it is off even in a debug build until deliberately
 * switched on. Flip the flag from a debugger, or temporarily set it to
 * `BuildConfig.DEBUG` while working on navigation.
 */
object FocusDebug {
    /** Master switch. Compiled out of release builds regardless of its value. */
    var enabled: Boolean = false
}

@Composable
fun FocusDebugOverlay(
    coordinator: FeedFocusCoordinator,
    zoneOrder: List<String>,
    modifier: Modifier = Modifier
) {
    if (!BuildConfig.DEBUG || !FocusDebug.enabled) return

    val memory = coordinator.memory
    val activeZone = coordinator.activeZoneKey

    Box(
        modifier = modifier
            .widthIn(max = 260.dp)
            .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DebugLine("FOCUS", emphasis = true)
            DebugLine("zone: ${activeZone ?: "—"}")
            DebugLine("column: ${memory.columnOrNull()?.let { "%.0fpx".format(it) } ?: "unset"}")
            DebugLine("last: ${memory.lastZoneKey ?: "—"}")
            DebugLine("order (${zoneOrder.size}):")
            zoneOrder.forEach { key ->
                val index = memory.indexFor(key)
                val marker = if (key == activeZone) "▶" else " "
                DebugLine("  $marker $key @${index ?: "-"}")
            }
        }
    }
}

@Composable
private fun DebugLine(text: String, emphasis: Boolean = false) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = if (emphasis) 10.sp else 9.sp,
            color = if (emphasis) Color(0xFF7FD4FF) else Color.White.copy(alpha = 0.86f)
        ),
        maxLines = 1
    )
}

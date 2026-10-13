package dev.nheox.mintal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.domain.session.SessionRules
import dev.nheox.mintal.ui.theme.mintalPalette
import kotlin.math.roundToInt

/**
 * The duration picker (spec §5.1): a horizontal growth track — a stylised mint runner laid
 * left to right with leaf glyphs at the quarter hours — rather than a radial dial, which
 * was too close to the prior art this app deliberately diverges from (§13).
 */
@Composable
fun GrowthTrack(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = mintalPalette
    val minMinutes = SessionRules.MIN_COUNTDOWN_MINUTES
    val maxMinutes = SessionRules.MAX_MINUTES
    val span = (maxMinutes - minMinutes).toFloat()

    fun minutesAt(x: Float, width: Float): Int {
        val fraction = (x / width.coerceAtLeast(1f)).coerceIn(0f, 1f)
        return SessionRules.clampMinutes((minMinutes + fraction * span).roundToInt())
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .semantics {
                contentDescription = "Session length, in five minute steps"
                stateDescription = "$minutes minutes"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = minutes.toFloat(),
                    range = minMinutes.toFloat()..maxMinutes.toFloat(),
                    steps = ((maxMinutes - minMinutes) / SessionRules.STEP_MINUTES) - 1,
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    onMinutesChange(minutesAt(position.x, size.width.toFloat()))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onMinutesChange(minutesAt(change.position.x, size.width.toFloat()))
                }
            },
    ) {
        val width = size.width
        val baseline = size.height * 0.62f
        val fraction = ((minutes - minMinutes) / span).coerceIn(0f, 1f)
        val markerX = width * fraction

        // The runner: grown part, then the part still to grow.
        drawLine(
            color = palette.barTrack,
            start = Offset(0f, baseline),
            end = Offset(width, baseline),
            strokeWidth = 10f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = palette.groveRunner,
            start = Offset(0f, baseline),
            end = Offset(markerX, baseline),
            strokeWidth = 12f,
            cap = StrokeCap.Round,
        )

        // Five-minute ticks, with a taller mark every quarter hour.
        var tick = minMinutes
        while (tick <= maxMinutes) {
            val tickX = width * ((tick - minMinutes) / span)
            val quarter = tick % 15 == 0
            drawLine(
                color = if (tickX <= markerX) palette.groveRunner else palette.barTrack,
                start = Offset(tickX, baseline - if (quarter) 12f else 6f),
                end = Offset(tickX, baseline + if (quarter) 12f else 6f),
                strokeWidth = if (quarter) 3f else 1.5f,
                cap = StrokeCap.Round,
            )
            tick += SessionRules.STEP_MINUTES
        }

        // The marker is a bud at the growing tip.
        drawCircle(color = palette.barFill, radius = 16f, center = Offset(markerX, baseline))
        drawCircle(color = palette.groveGlow, radius = 8f, center = Offset(markerX, baseline))
    }
}

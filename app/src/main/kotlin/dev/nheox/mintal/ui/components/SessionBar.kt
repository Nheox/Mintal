package dev.nheox.mintal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.ui.theme.MintalTextStyles
import dev.nheox.mintal.ui.theme.mintalPalette

/**
 * The in-session indicator (spec §5.2): a solid, chunky, game-like horizontal bar in the
 * spirit of a Hearthstone mana bar — not a radial arc (§13). It reads the remaining time
 * better than a dial does, and it leaves room for the legend line underneath.
 */
@Composable
fun SessionBar(
    progress: Float,
    label: String,
    inGracePeriod: Boolean,
    modifier: Modifier = Modifier,
    segments: Int = 6,
) {
    val palette = mintalPalette
    val warning = MaterialTheme.colorScheme.error
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { contentDescription = "Session progress: $label" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            val corner = CornerRadius(size.height / 2f, size.height / 2f)
            drawRoundRect(color = palette.barTrack, size = size, cornerRadius = corner)

            val filledWidth = (size.width * progress.coerceIn(0f, 1f))
            if (filledWidth > 1f) {
                drawRoundRect(
                    color = if (inGracePeriod) warning else palette.barFill,
                    size = Size(filledWidth, size.height),
                    cornerRadius = corner,
                )
                // One flat highlight along the top of the fill: the game-bar look.
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(size.height * 0.4f, size.height * 0.28f),
                    end = Offset((filledWidth - size.height * 0.4f).coerceAtLeast(size.height * 0.4f), size.height * 0.28f),
                    strokeWidth = size.height * 0.16f,
                    cap = StrokeCap.Round,
                )
            }

            // Chunky notches, so progress is readable without reading the number.
            for (index in 1 until segments) {
                val x = size.width * index / segments
                drawLine(
                    color = palette.barFillSpent.copy(alpha = 0.55f),
                    start = Offset(x, size.height * 0.18f),
                    end = Offset(x, size.height * 0.82f),
                    strokeWidth = 2f,
                )
            }

            drawRoundRect(
                color = palette.barFillSpent,
                size = size,
                cornerRadius = corner,
                style = Stroke(width = 3f),
            )
        }
        Text(
            text = label,
            style = MintalTextStyles.timerSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

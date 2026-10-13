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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
            val track = Path().apply {
                addRoundRect(RoundRect(rect = Rect(Offset.Zero, size), cornerRadius = corner))
            }
            drawRoundRect(color = palette.barTrack, size = size, cornerRadius = corner)

            // Everything inside the bar is clipped to the bar. A rounded fill drawn on its
            // own keeps its own round cap, which bulges out past the track at small widths.
            clipPath(track) {
                val filledWidth = size.width * progress.coerceIn(0f, 1f)
                if (filledWidth > 0f) {
                    drawRect(
                        color = if (inGracePeriod) warning else palette.barFill,
                        size = Size(filledWidth, size.height),
                    )
                    // One flat highlight along the top of the fill: the game-bar look.
                    drawRect(
                        color = Color.White.copy(alpha = 0.16f),
                        topLeft = Offset(0f, size.height * 0.14f),
                        size = Size(filledWidth, size.height * 0.20f),
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

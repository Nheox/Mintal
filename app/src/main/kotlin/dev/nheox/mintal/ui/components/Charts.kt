package dev.nheox.mintal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.ui.theme.mintalPalette

/**
 * Charts are hand-drawn on a Compose canvas (spec §8), built to the same tokens as the
 * rest of the app — a borrowed chart library reads as borrowed. Axis labels are laid out
 * as real text below the canvas so a screen reader can reach them.
 */
@Composable
fun FocusBarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    labelEvery: Int = 1,
) {
    val palette = mintalPalette
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val total = values.sum()
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .semantics {
                    contentDescription = "Focus time per bucket, total ${MintalTime.formatDuration(total)}"
                },
        ) {
            val slot = size.width / values.size.coerceAtLeast(1)
            val barWidth = (slot * 0.62f).coerceAtLeast(2f)

            // Three guide lines, enough to read heights without a cluttered grid.
            for (step in 1..3) {
                val y = size.height * (1f - step / 4f)
                drawLine(
                    color = palette.chartGrid,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }

            values.forEachIndexed { index, value ->
                val barHeight = size.height * (value.toFloat() / maxValue)
                if (barHeight > 0.5f) {
                    drawRoundRect(
                        color = palette.chartBar,
                        topLeft = Offset(
                            x = slot * index + (slot - barWidth) / 2f,
                            y = size.height - barHeight,
                        ),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 3f, barWidth / 3f),
                    )
                }
            }

            drawLine(
                color = palette.chartGrid,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f,
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = if (index % labelEvery == 0) label else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Rating over time, as spec §5.5 asks for: trend visible at a glance. */
@Composable
fun RatingTrendChart(
    ratings: List<Int>,
    modifier: Modifier = Modifier,
    height: Dp = 148.dp,
) {
    val palette = mintalPalette
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .semantics {
                    val average = if (ratings.isEmpty()) 0f else ratings.average().toFloat()
                    contentDescription =
                        "Rating trend over ${ratings.size} rated sessions, average ${"%.1f".format(average)}"
                },
        ) {
            for (star in 1..5) {
                val y = size.height * (1f - (star - 1) / 4f)
                drawLine(
                    color = palette.chartGrid,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (star == 1) 2f else 1f,
                )
            }
            if (ratings.size < 2) {
                ratings.firstOrNull()?.let { rating ->
                    drawCircle(
                        color = palette.chartLine,
                        radius = 7f,
                        center = Offset(size.width / 2f, size.height * (1f - (rating - 1) / 4f)),
                    )
                }
                return@Canvas
            }

            val stepX = size.width / (ratings.size - 1)
            val path = Path()
            ratings.forEachIndexed { index, rating ->
                val x = stepX * index
                val y = size.height * (1f - (rating.coerceIn(1, 5) - 1) / 4f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = palette.chartLine,
                style = Stroke(width = 4f, cap = StrokeCap.Round),
            )
            ratings.forEachIndexed { index, rating ->
                drawCircle(
                    color = palette.chartLine,
                    radius = 5f,
                    center = Offset(
                        stepX * index,
                        size.height * (1f - (rating.coerceIn(1, 5) - 1) / 4f),
                    ),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "oldest",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "latest",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

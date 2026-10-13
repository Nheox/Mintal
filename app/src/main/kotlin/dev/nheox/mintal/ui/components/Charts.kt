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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.stats.AxisTicks
import dev.nheox.mintal.ui.theme.mintalPalette
import kotlin.math.roundToInt

/**
 * Charts are hand-drawn on a Compose canvas (spec §8), built to the same tokens as the
 * rest of the app — a borrowed chart library reads as borrowed. Axis labels are laid out
 * as real text so a screen reader can reach them.
 *
 * Both axes are laid out by measuring the labels rather than by dividing the width up
 * and hoping. The hour axis used to give each of twenty-four slots an equal sliver and
 * print every third label into it, so "03" was clipped to "0" and the axis read
 * "0 0 0 0 1 1 1 2". Here a label is drawn only if it fits beside the one before it.
 */

/** Gap a category label keeps from its neighbour before it is dropped instead. */
private val LABEL_GAP = 6.dp

@Composable
fun FocusBarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    axis: AxisTicks.Axis = AxisTicks.durations(values.maxOrNull() ?: 0),
    xAxisLabel: String? = null,
    describeValue: (Int) -> String = { MintalTime.formatDuration(it) },
) {
    val palette = mintalPalette
    val total = values.sum()
    val peak = values.maxOrNull() ?: 0
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ValueAxisLabels(
                ticks = axis.ticks.map { axis.fractionOf(it.value) to it.label },
                modifier = Modifier.height(height).padding(end = 6.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .semantics {
                            contentDescription = "Focus time per bucket, peak " +
                                "${describeValue(peak)}, total ${MintalTime.formatDuration(total)}"
                        },
                ) {
                    val slot = size.width / values.size.coerceAtLeast(1)
                    val barWidth = (slot * 0.62f).coerceAtLeast(2f)

                    for (tick in axis.ticks) {
                        if (tick.value == 0) continue
                        val y = size.height * (1f - axis.fractionOf(tick.value))
                        drawLine(
                            color = palette.chartGrid,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                        )
                    }

                    values.forEachIndexed { index, value ->
                        val barHeight = size.height * axis.fractionOf(value)
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

                CategoryAxisLabels(
                    labels = labels,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )

                if (xAxisLabel != null) {
                    Text(
                        text = xAxisLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    )
                }
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
        Row(modifier = Modifier.fillMaxWidth()) {
            // The stars are the scale here, so the axis is fixed at one to five rather
            // than derived from the data — a chart of three 4s is not a chart of 4s.
            ValueAxisLabels(
                ticks = (1..5).map { star -> (star - 1) / 4f to star.toString() },
                modifier = Modifier.height(height).padding(end = 6.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .semantics {
                            val average = if (ratings.isEmpty()) 0f else ratings.average().toFloat()
                            contentDescription = "Rating trend over ${ratings.size} rated " +
                                "sessions, average ${"%.1f".format(average)} out of 5"
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
                                center = Offset(
                                    size.width / 2f,
                                    size.height * (1f - (rating.coerceIn(1, 5) - 1) / 4f),
                                ),
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
                        text = "rated session, in order",
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
    }
}

/**
 * Right-aligned labels, each centred on the gridline it names. [ticks] are (fraction from
 * the baseline, label) pairs; a label never leaves the axis, so the top and bottom ones
 * sit flush rather than half off the edge.
 */
@Composable
private fun ValueAxisLabels(
    ticks: List<Pair<Float, String>>,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            ticks.forEach { (_, label) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = constraints.maxHeight
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val fraction = ticks[index].first.coerceIn(0f, 1f)
                val centre = (1f - fraction) * height
                val y = (centre - placeable.height / 2f).roundToInt()
                    .coerceIn(0, (height - placeable.height).coerceAtLeast(0))
                placeable.place(width - placeable.width, y)
            }
        }
    }
}

/**
 * One label per bar, centred under its bar — but only while there is room. Twenty-four
 * hour labels do not fit on a phone, so the ones that would collide with the last drawn
 * label are left out entirely rather than squeezed until they truncate.
 */
@Composable
private fun CategoryAxisLabels(
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val gap = LABEL_GAP.roundToPx()
        val slot = if (placeables.isEmpty()) 0f else width.toFloat() / placeables.size

        layout(width, height) {
            var nextFreeX = 0
            placeables.forEachIndexed { index, placeable ->
                val centre = slot * (index + 0.5f)
                val x = (centre - placeable.width / 2f).roundToInt()
                    .coerceIn(0, (width - placeable.width).coerceAtLeast(0))
                if (x >= nextFreeX) {
                    placeable.place(x, 0)
                    nextFreeX = x + placeable.width + gap
                }
            }
        }
    }
}

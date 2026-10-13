package dev.nheox.mintal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.grove.GroveLayout
import dev.nheox.mintal.ui.theme.mintalPalette
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * The Grove (spec §5.6): one continuous, ever-growing mint network rather than a grid of
 * potted plants. Each successful session is a runner segment grafted onto the growth that
 * came before it; a failed one is a wilted sprig. Panning and zooming along the network
 * replaces the old day-by-day grid, and tapping a day's growth opens Day detail — the
 * navigation question §11.10 leaves open is answered here by doing both: the tap target is
 * the rendered growth, and the Grove screen also lists days for anyone who would rather
 * scroll.
 */
@Composable
fun GroveCanvas(
    layout: GroveLayout,
    speciesById: Map<String, Species>,
    onDayTap: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = mintalPalette
    var userScale by remember { mutableFloatStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = buildString {
                    append("The Grove: ")
                    append(layout.segments.size)
                    append(" sessions across ")
                    append(layout.anchors.size)
                    append(" days. Double tap a day's growth to open it.")
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    userScale = (userScale * zoom).coerceIn(0.4f, 6f)
                    userOffset += pan
                }
            }
            .pointerInput(layout) {
                detectTapGestures { tap ->
                    if (layout.isEmpty) return@detectTapGestures
                    val canvasSize = size
                    val fit = fitScale(
                        canvasWidth = canvasSize.width.toFloat(),
                        canvasHeight = canvasSize.height.toFloat(),
                        layout = layout,
                    ) * userScale
                    val centreX = canvasSize.width / 2f + userOffset.x
                    val centreY = canvasSize.height / 2f + userOffset.y
                    val modelCentreX = (layout.bounds.minX + layout.bounds.maxX) / 2f
                    val modelCentreY = (layout.bounds.minY + layout.bounds.maxY) / 2f
                    val modelX = (tap.x - centreX) / fit + modelCentreX
                    val modelY = (tap.y - centreY) / fit + modelCentreY
                    val nearest = layout.anchors.minByOrNull { anchor ->
                        hypot((anchor.x - modelX).toDouble(), (anchor.y - modelY).toDouble())
                    }
                    if (nearest != null) onDayTap(nearest.epochDay)
                }
            },
    ) {
        if (layout.isEmpty) return@Canvas

        val fit = fitScale(size.width, size.height, layout) * userScale
        val centreX = size.width / 2f + userOffset.x
        val centreY = size.height / 2f + userOffset.y
        val modelCentreX = (layout.bounds.minX + layout.bounds.maxX) / 2f
        val modelCentreY = (layout.bounds.minY + layout.bounds.maxY) / 2f

        fun project(x: Float, y: Float) = Offset(
            x = centreX + (x - modelCentreX) * fit,
            y = centreY + (y - modelCentreY) * fit,
        )

        // The base the whole network spreads from.
        val root = project(0f, 0f)
        drawCircle(color = palette.groveGlow, radius = 10f * fit.coerceIn(0.5f, 3f), center = root)

        for (segment in layout.segments) {
            val start = project(segment.startX, segment.startY)
            val end = project(segment.endX, segment.endY)
            val species = speciesById[segment.speciesId]
            val runnerColour = when {
                segment.wilted -> palette.wilted
                species != null -> palette.stem(species.art.stemHue)
                else -> palette.groveRunner
            }
            val leafColour = when {
                segment.wilted -> palette.wilted
                species != null -> palette.leaf(species.art.leafHue)
                else -> palette.groveRunner
            }

            drawLine(
                color = runnerColour,
                start = start,
                end = end,
                strokeWidth = (2.5f * fit).coerceIn(1.5f, 14f),
                cap = StrokeCap.Round,
            )

            if (species == null) continue
            val axis = atan2(end.y - start.y, end.x - start.x)
            val segmentLength = hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble())
                .toFloat()
            val leafLength = (segmentLength * 0.34f * species.art.leafScale).coerceAtLeast(3f)
            val pairs = segment.leafCount / 2
            for (pair in 0 until pairs.coerceAtLeast(1)) {
                val along = 0.3f + 0.65f * (pair.toFloat() / pairs.coerceAtLeast(1))
                val node = Offset(
                    start.x + (end.x - start.x) * along,
                    start.y + (end.y - start.y) * along,
                )
                drawLeafPair(
                    node = node,
                    axisAngle = axis,
                    leafLength = leafLength,
                    species = species,
                    leafColour = leafColour,
                    stemColour = runnerColour,
                    wilted = segment.wilted,
                    outlineWidth = (0.6f * fit).coerceIn(0.5f, 3f),
                )
            }
        }
    }
}

/** Scale that fits the whole network into the canvas with a margin. */
private fun fitScale(canvasWidth: Float, canvasHeight: Float, layout: GroveLayout): Float =
    min(
        canvasWidth / layout.bounds.width,
        canvasHeight / layout.bounds.height,
    ).coerceIn(0.05f, 40f) * 0.82f

/** Short line for the Grove's day list, kept here so the canvas and list agree. */
fun groveDaySummary(seconds: Int, sessions: Int): String =
    "${MintalTime.formatDuration(seconds)} · $sessions session" + if (sessions == 1) "" else "s"

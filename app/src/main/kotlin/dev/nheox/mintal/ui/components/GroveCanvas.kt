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
import kotlin.math.hypot
import kotlin.math.min

/**
 * The Grove (spec §5.6, amended): a bed of real mints rather than a grid of potted
 * plants or one endlessly scaling specimen. Every successful session is one node — a
 * stem segment and the opposite leaf pair on top of it — added to that species' youngest
 * unfinished plant. A plant that reaches maturity stops, and the species' next session
 * starts another plant beside it, so a mint you use constantly becomes a clump.
 *
 * Panning and zooming move along the bed, and tapping a day's growth opens Day detail —
 * the navigation question §11.10 leaves open is answered by doing both: the tap target is
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
                    append(layout.plantCount)
                    append(" plants across ")
                    append(layout.anchors.size)
                    append(" days. Tap a day's growth to open it.")
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    userScale = (userScale * zoom).coerceIn(0.4f, 8f)
                    userOffset += pan
                }
            }
            .pointerInput(layout) {
                detectTapGestures { tap ->
                    if (layout.isEmpty) return@detectTapGestures
                    val view = viewFor(size.width.toFloat(), size.height.toFloat(), layout, userScale, userOffset)
                    val modelX = (tap.x - view.centreX) / view.scale + view.modelCentreX
                    val modelY = (tap.y - view.centreY) / view.scale + view.modelCentreY
                    val nearest = layout.anchors.minByOrNull { anchor ->
                        hypot((anchor.x - modelX).toDouble(), (anchor.y - modelY).toDouble())
                    }
                    if (nearest != null) onDayTap(nearest.epochDay)
                }
            },
    ) {
        if (layout.isEmpty) return@Canvas

        val view = viewFor(size.width, size.height, layout, userScale, userOffset)
        fun project(x: Float, y: Float) = Offset(
            x = view.centreX + (x - view.modelCentreX) * view.scale,
            y = view.centreY + (y - view.modelCentreY) * view.scale,
        )

        // The bed the whole thing stands in. Without a ground line the plants float.
        val ground = project(0f, 0f).y
        drawLine(
            color = palette.groveGround,
            start = Offset(0f, ground),
            end = Offset(size.width, ground),
            strokeWidth = (view.scale * 0.05f).coerceIn(1.5f, 8f),
            cap = StrokeCap.Round,
        )

        for (planting in layout.plantings) {
            val species = speciesById[planting.speciesId] ?: continue
            drawPlant(
                plant = planting.plant,
                origin = project(planting.baseX, 0f),
                unitPx = view.scale,
                species = species,
                colours = PlantColours.of(palette, species),
            )
        }

        for (stub in layout.stubs) {
            drawWiltedStub(
                origin = project(stub.baseX, 0f),
                unitPx = view.scale,
                species = speciesById[stub.speciesId],
                colours = PlantColours.wilted(palette),
            )
        }
    }
}

/** Where the bed sits on the canvas: one structural unit is [scale] pixels. */
private data class GroveView(
    val scale: Float,
    val centreX: Float,
    val centreY: Float,
    val modelCentreX: Float,
    val modelCentreY: Float,
)

/** A mature plant stands about this many units tall, and should fill the band. */
private const val VISIBLE_HEIGHT = 6.6f

/** However narrow the band, keep at least this much of the bed in frame. */
private const val MIN_PLANTS_ACROSS = 2.2f
private const val SLOT_WIDTH = 3.0f

/** Where the ground line sits down the canvas, leaving room to grow into. */
private const val GROUND_FRACTION = 0.84f

/**
 * A plant is always drawn at the same size, however big the Grove gets, and the reader
 * pans along the bed. Fitting the whole bed into the canvas instead means every plant
 * shrinks as the Grove succeeds — a hundred sessions in, the reward for using the app
 * is a row of specks.
 */
private fun viewFor(
    canvasWidth: Float,
    canvasHeight: Float,
    layout: GroveLayout,
    userScale: Float,
    userOffset: Offset,
): GroveView {
    // Height-driven: a plant should fill the band it is drawn in, and the reader pans
    // sideways along the bed. The width only ever reins it in on a very narrow screen.
    val scale = min(
        canvasHeight / VISIBLE_HEIGHT,
        canvasWidth / (MIN_PLANTS_ACROSS * SLOT_WIDTH),
    ) * userScale
    val visibleModelWidth = canvasWidth / scale
    // Open on the newest living plant. Anchoring on the far edge of the bed instead
    // opens on whatever happens to be last, and a run of failed sessions then greets
    // the reader with nothing but wilted stubs.
    val newest = layout.plantings.lastOrNull()?.baseX ?: layout.bounds.maxX
    val modelCentreX = if (layout.bounds.width <= visibleModelWidth) {
        (layout.bounds.minX + layout.bounds.maxX) / 2f
    } else {
        newest.coerceIn(
            layout.bounds.minX + visibleModelWidth / 2f,
            layout.bounds.maxX - visibleModelWidth / 2f,
        )
    }
    return GroveView(
        scale = scale,
        centreX = canvasWidth / 2f + userOffset.x,
        // Model y is 0 at the ground and negative upward, so anchoring the ground puts
        // every plant's feet on the same line whatever is in the bed.
        centreY = canvasHeight * GROUND_FRACTION + userOffset.y,
        modelCentreX = modelCentreX,
        modelCentreY = 0f,
    )
}

/** Short line for the Grove's day list, kept here so the canvas and list agree. */
fun groveDaySummary(seconds: Int, sessions: Int): String =
    "${MintalTime.formatDuration(seconds)} · $sessions session" + if (sessions == 1) "" else "s"

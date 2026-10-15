package dev.nheox.mintal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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
    val moved = userScale != 1f || userOffset != Offset.Zero

    Box(modifier = modifier.fillMaxSize()) {
    Canvas(
        modifier = Modifier
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
            .pointerInput(layout) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val scaled = (userScale * zoom).coerceIn(0.5f, 8f)
                    userScale = scaled
                    // Panning used to be unbounded, so the bed could be flung off the
                    // screen and left there with no way back but closing the screen.
                    userOffset = clampPan(
                        offset = userOffset + pan,
                        canvasWidth = size.width.toFloat(),
                        canvasHeight = size.height.toFloat(),
                        layout = layout,
                        userScale = scaled,
                    )
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
                colours = PlantColours.wilted(palette).fadedTo(stub.fade),
            )
        }
    }

        // Only once the view has actually been moved: an undo for a gesture, not a
        // permanent control sitting on top of the bed.
        if (moved) {
            FilledTonalButton(
                onClick = {
                    userScale = 1f
                    userOffset = Offset.Zero
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) { Text("Reset view") }
        }
    }
}

/**
 * Keeps the bed on screen. Whatever the pan, at least [EDGE_KEEP_FRACTION] of each axis
 * still shows something: you can drag to the far end of the Grove, but not into empty
 * space with nothing to drag back to.
 */
internal fun clampPan(
    offset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    layout: GroveLayout,
    userScale: Float,
): Offset {
    if (canvasWidth <= 0f || canvasHeight <= 0f) return offset
    val rect = groveContentRect(canvasWidth, canvasHeight, layout, userScale, Offset.Zero)
    val keepX = canvasWidth * EDGE_KEEP_FRACTION
    val keepY = canvasHeight * EDGE_KEEP_FRACTION

    return Offset(
        x = offset.x.clampBetween(keepX - rect.right, canvasWidth - keepX - rect.left),
        y = offset.y.clampBetween(keepY - rect.bottom, canvasHeight - keepY - rect.top),
    )
}

/** Where the whole bed lands on the canvas, in pixels, for a given pan. */
internal fun groveContentRect(
    canvasWidth: Float,
    canvasHeight: Float,
    layout: GroveLayout,
    userScale: Float,
    offset: Offset,
): Rect {
    val view = viewFor(canvasWidth, canvasHeight, layout, userScale, offset)
    return Rect(
        left = view.centreX + (layout.bounds.minX - view.modelCentreX) * view.scale,
        top = view.centreY + (layout.bounds.minY - view.modelCentreY) * view.scale,
        right = view.centreX + (layout.bounds.maxX - view.modelCentreX) * view.scale,
        bottom = view.centreY + (layout.bounds.maxY - view.modelCentreY) * view.scale,
    )
}

/** coerceIn with the ends the wrong way round means "no room to move": stay put. */
private fun Float.clampBetween(low: Float, high: Float): Float =
    if (low > high) (low + high) / 2f else coerceIn(low, high)

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

/** Below this, a young bed is drawn no larger: one seedling should not fill the sky. */
private const val MIN_VISIBLE_HEIGHT = 3.1f

/** However narrow the band, keep at least this much of the bed in frame. */
private const val MIN_PLANTS_ACROSS = 2.2f
private const val SLOT_WIDTH = 3.0f

/** Fraction of each axis that must still show something after a pan. */
private const val EDGE_KEEP_FRACTION = 0.3f

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
    //
    // The band is measured against what is actually in it, clamped between one mature
    // plant and one young one. A Grove of three seedlings framed for a plant they have
    // not grown into yet is three specks on an empty field; framed for themselves, they
    // are three seedlings.
    val unitsTall = layout.bounds.height.coerceIn(MIN_VISIBLE_HEIGHT, VISIBLE_HEIGHT)
    val plantsAcross = (layout.bounds.width / SLOT_WIDTH).coerceIn(1.2f, MIN_PLANTS_ACROSS)
    val scale = min(
        canvasHeight / unitsTall,
        canvasWidth / (plantsAcross * SLOT_WIDTH),
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

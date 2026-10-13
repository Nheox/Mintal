package dev.nheox.mintal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.core.model.GrowthHabit
import dev.nheox.mintal.core.model.LeafMargin
import dev.nheox.mintal.core.model.LeafShape
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.ui.theme.mintalPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/*
 * Mintal has no commissioned artwork yet (spec §8.3, §11.3), so every mint is drawn from
 * its real botanical traits: leaf shape, leaf margin, growth habit, relative size. One
 * construction grammar, one stroke treatment, and species told apart by shape rather than
 * by tint — which is the part of the brief that keeps a set from looking generic.
 *
 * When the illustrator's kit arrives, these functions are what it replaces.
 */

private const val PROFILE_STEPS = 12

/** Width at a point along the midrib: 0f at the base, 0f at the tip. */
private fun leafProfile(t: Float, shape: LeafShape): Float {
    val bell = sin(PI.toFloat() * t.coerceIn(0f, 1f))
    // Widths are generous on purpose: at emblem and Grove sizes a botanically accurate
    // narrow blade renders as a line, and the whole point is telling the species apart.
    return when (shape) {
        LeafShape.SPEAR -> bell.pow(0.75f) * (1f - t * 0.45f) * 0.44f
        LeafShape.OVATE -> bell.pow(0.80f) * 0.52f
        LeafShape.ROUND -> bell.pow(0.60f) * 0.68f
        LeafShape.LANCEOLATE -> bell.pow(1.25f) * 0.32f
        LeafShape.NEEDLE -> bell.pow(1.6f) * 0.17f
    }
}

/** Serrate teeth, crenate scallops, or a smooth edge. */
private fun marginFactor(step: Int, margin: LeafMargin): Float = when (margin) {
    LeafMargin.SERRATE -> if (step % 2 == 0) 1.14f else 0.88f
    LeafMargin.CRENATE -> 1f + 0.07f * sin(step * 1.9f)
    LeafMargin.ENTIRE -> 1f
}

/**
 * A single leaf, pointing from [base] to [tip]. The outline is sampled from a width
 * profile, so shape and margin are properties of the species, not of a drawing.
 */
fun leafPath(
    base: Offset,
    tip: Offset,
    halfWidth: Float,
    shape: LeafShape,
    margin: LeafMargin,
): Path {
    val axisX = tip.x - base.x
    val axisY = tip.y - base.y
    val path = Path()
    path.moveTo(base.x, base.y)
    for (side in intArrayOf(1, -1)) {
        val range = if (side == 1) 1..PROFILE_STEPS else (PROFILE_STEPS - 1) downTo 0
        for (step in range) {
            val t = step.toFloat() / PROFILE_STEPS
            val width = halfWidth * leafProfile(t, shape) * marginFactor(step, margin)
            val x = base.x + axisX * t - axisY * width * side
            val y = base.y + axisY * t + axisX * width * side
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

/** Leaves at one node: an opposite pair, as every mint carries them. */
fun DrawScope.drawLeafPair(
    node: Offset,
    axisAngle: Float,
    leafLength: Float,
    species: Species,
    leafColour: Color,
    stemColour: Color,
    spread: Float = 0.85f,
    wilted: Boolean = false,
    outlineWidth: Float = 1f,
) {
    for (side in intArrayOf(1, -1)) {
        val leafAngle = axisAngle + spread * side + if (wilted) 0.4f * side else 0f
        val tip = Offset(
            node.x + cos(leafAngle) * leafLength,
            node.y + sin(leafAngle) * leafLength,
        )
        val path = leafPath(
            base = node,
            tip = tip,
            halfWidth = leafLength,
            shape = species.art.leafShape,
            margin = species.art.margin,
        )
        drawPath(path = path, color = leafColour)
        drawPath(path = path, color = stemColour, style = Stroke(width = outlineWidth))
        // The midrib is what makes a green shape read as a leaf rather than a blob.
        drawLine(
            color = stemColour.copy(alpha = 0.65f),
            start = node,
            end = tip,
            strokeWidth = outlineWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** A sprig: one stem with leaf pairs that shrink toward the growing tip. */
fun DrawScope.drawSprig(
    origin: Offset,
    heightPx: Float,
    angle: Float,
    species: Species,
    leafColour: Color,
    stemColour: Color,
    leafPairs: Int,
    wilted: Boolean = false,
) {
    val lean = when (species.art.habit) {
        GrowthHabit.UPRIGHT -> 0f
        GrowthHabit.TALL -> -0.05f
        GrowthHabit.CREEPING -> 0.45f
    }
    val tipAngle = angle + lean + if (wilted) 0.55f else 0f
    val tip = Offset(
        origin.x + cos(tipAngle) * heightPx,
        origin.y + sin(tipAngle) * heightPx,
    )
    drawLine(
        color = stemColour,
        start = origin,
        end = tip,
        strokeWidth = (heightPx * 0.045f).coerceAtLeast(1.5f),
        cap = StrokeCap.Round,
    )

    val pairs = leafPairs.coerceAtLeast(1)
    val baseLength = heightPx * 0.50f * species.art.leafScale * if (wilted) 0.6f else 1f
    for (pair in 0 until pairs) {
        val fraction = pair.toFloat() / pairs
        val along = 0.25f + 0.72f * fraction
        val scale = 1f - 0.45f * fraction
        val node = Offset(
            origin.x + (tip.x - origin.x) * along,
            origin.y + (tip.y - origin.y) * along,
        )
        drawLeafPair(
            node = node,
            axisAngle = tipAngle,
            leafLength = baseLength * scale,
            species = species,
            leafColour = leafColour,
            stemColour = stemColour,
            spread = if (species.art.habit == GrowthHabit.CREEPING) 1.1f else 0.85f,
            wilted = wilted,
            outlineWidth = (heightPx * 0.012f).coerceAtLeast(0.8f),
        )
    }
}

/** The species emblem used on chips, the catalogue and the result screen. */
@Composable
fun SpeciesEmblem(
    species: Species,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    unlocked: Boolean = true,
) {
    val palette = mintalPalette
    val leaf = if (unlocked) palette.leaf(species.art.leafHue) else palette.chartGrid
    val stem = if (unlocked) palette.stem(species.art.stemHue) else palette.chartGrid
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "${species.commonName} sprig" },
    ) {
        drawSprig(
            origin = Offset(this.size.width / 2f, this.size.height * 0.95f),
            heightPx = this.size.height * 0.78f,
            angle = -(PI / 2).toFloat(),
            species = species,
            leafColour = leaf,
            stemColour = stem,
            leafPairs = 3,
        )
    }
}

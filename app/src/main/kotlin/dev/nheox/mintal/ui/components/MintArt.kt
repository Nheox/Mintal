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
import dev.nheox.mintal.core.model.FlowerForm
import dev.nheox.mintal.core.model.GrowthHabit
import dev.nheox.mintal.core.model.LeafAttachment
import dev.nheox.mintal.core.model.LeafMargin
import dev.nheox.mintal.core.model.LeafShape
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.ui.theme.mintalPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
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
    // sin(PI) in Float is -8.7e-8, not 0. Raising that to a fractional power gives NaN,
    // one NaN point makes the whole leaf path degenerate, and nothing is drawn at all —
    // which is why every mint rendered as a bare stem.
    val bell = sin(PI.toFloat() * t.coerceIn(0f, 1f)).coerceAtLeast(0f)
    // Half-width as a fraction of leaf length, so a value of 0.30 draws a blade roughly
    // 60% as wide as it is long — about right for an ovate mint leaf.
    return when (shape) {
        LeafShape.SPEAR -> bell.pow(0.75f) * (1f - t * 0.45f) * 0.34f
        LeafShape.OVATE -> bell.pow(0.80f) * 0.36f
        LeafShape.ROUND -> bell.pow(0.60f) * 0.46f
        LeafShape.LANCEOLATE -> bell.pow(1.25f) * 0.24f
        LeafShape.NEEDLE -> bell.pow(1.6f) * 0.13f
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
    val length = hypot(axisX, axisY)
    val path = Path()
    if (length < 0.01f) return path

    // The blade is offset along the UNIT perpendicular to the midrib. Offsetting along the
    // raw axis vector instead multiplies every width by the leaf's length in pixels, which
    // turns each leaf into a huge spike with only a sliver of it on screen — the "mints are
    // just green lines" bug.
    val alongX = axisX / length
    val alongY = axisY / length

    path.moveTo(base.x, base.y)
    for (side in intArrayOf(1, -1)) {
        val range = if (side == 1) 1..PROFILE_STEPS else (PROFILE_STEPS - 1) downTo 0
        for (step in range) {
            val t = step.toFloat() / PROFILE_STEPS
            val width = halfWidth * leafProfile(t, shape) * marginFactor(step, margin)
            val x = base.x + axisX * t - alongY * width * side
            val y = base.y + axisY * t + alongX * width * side
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
    // A stalked leaf holds its blade off the stem; a sessile one sits straight on it.
    // It is a real field mark — spearmint against peppermint turns on exactly this.
    val stalk = if (species.art.attachment == LeafAttachment.STALKED) leafLength * 0.22f else 0f

    for (side in intArrayOf(1, -1)) {
        val leafAngle = axisAngle + spread * side + if (wilted) 0.4f * side else 0f
        val bladeBase = Offset(
            node.x + cos(leafAngle) * stalk,
            node.y + sin(leafAngle) * stalk,
        )
        val tip = Offset(
            node.x + cos(leafAngle) * (leafLength + stalk),
            node.y + sin(leafAngle) * (leafLength + stalk),
        )
        if (stalk > 0f) {
            drawLine(
                color = stemColour,
                start = node,
                end = bladeBase,
                strokeWidth = outlineWidth * 1.4f,
                cap = StrokeCap.Round,
            )
        }
        val path = leafPath(
            base = bladeBase,
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
            start = bladeBase,
            end = tip,
            strokeWidth = outlineWidth,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * How a species flowers tells the mints apart faster than leaf shape does, so the sprig
 * carries its inflorescence: a slender or blunt spike above the tip, a round head, whorls
 * in the leaf axils with the stem continuing past them, or dense globes.
 */
private fun DrawScope.drawInflorescence(
    tip: Offset,
    nodes: List<Offset>,
    angle: Float,
    heightPx: Float,
    form: FlowerForm,
    colour: Color,
) {
    val unit = (heightPx * 0.05f).coerceAtLeast(1.2f)
    fun along(distance: Float) = Offset(
        tip.x + cos(angle) * distance,
        tip.y + sin(angle) * distance,
    )

    when (form) {
        FlowerForm.SLENDER_SPIKE -> for (step in 0 until 6) {
            val fraction = step / 5f
            drawCircle(colour, unit * (0.85f - 0.5f * fraction), along(unit * step * 0.85f))
        }
        FlowerForm.BLUNT_SPIKE -> for (step in 0 until 3) {
            drawCircle(colour, unit * 1.1f, along(unit * step * 1.3f))
        }
        FlowerForm.ROUND_HEAD -> drawCircle(colour, unit * 1.9f, along(unit * 1.4f))
        FlowerForm.GLOBE -> {
            val centre = along(unit * 1.3f)
            drawCircle(colour, unit * 1.6f, centre)
            // Protruding stamens: the reason this one does not read as a mint at all.
            for (spoke in 0 until 8) {
                val spokeAngle = spoke * 0.7854f
                drawLine(
                    color = colour,
                    start = centre,
                    end = Offset(
                        centre.x + cos(spokeAngle) * unit * 2.4f,
                        centre.y + sin(spokeAngle) * unit * 2.4f,
                    ),
                    strokeWidth = unit * 0.22f,
                )
            }
        }
        // Whorls sit at the leaf axils, and the stem carries on above them — no terminal head.
        FlowerForm.AXILLARY_WHORLS -> nodes.forEach { drawCircle(colour, unit * 0.9f, it) }
        FlowerForm.SPACED_WHORLS -> nodes.forEach { drawCircle(colour, unit * 1.15f, it) }
        FlowerForm.NONE -> Unit
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
    flowerColour: Color = leafColour,
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
    val baseLength = heightPx * 0.40f * species.art.leafScale * if (wilted) 0.6f else 1f
    val nodes = ArrayList<Offset>(pairs)
    for (pair in 0 until pairs) {
        val fraction = pair.toFloat() / pairs
        // Spread the nodes further apart than the leaves are long, so the stem stays
        // visible between the pairs and the axillary whorls are not buried.
        val along = 0.18f + 0.78f * fraction
        val scale = 1f - 0.45f * fraction
        val node = Offset(
            origin.x + (tip.x - origin.x) * along,
            origin.y + (tip.y - origin.y) * along,
        )
        nodes += node
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

    if (!wilted) {
        drawInflorescence(tip, nodes, tipAngle, heightPx, species.art.flower, flowerColour)
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
            origin = Offset(this.size.width / 2f, this.size.height * 0.80f),
            heightPx = this.size.height * 0.62f,
            angle = -(PI / 2).toFloat(),
            species = species,
            leafColour = leaf,
            stemColour = stem,
            leafPairs = 3,
            flowerColour = if (unlocked) palette.flower else palette.chartGrid,
        )
    }
}

package dev.nheox.mintal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.core.model.FlowerForm
import dev.nheox.mintal.core.model.GrowthHabit
import dev.nheox.mintal.core.model.LeafAttachment
import dev.nheox.mintal.core.model.LeafFlush
import dev.nheox.mintal.core.model.LeafMargin
import dev.nheox.mintal.core.model.LeafShape
import dev.nheox.mintal.core.model.LeafSurface
import dev.nheox.mintal.core.model.Pubescence
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.domain.grove.Plant
import dev.nheox.mintal.domain.grove.PlantBuilder
import dev.nheox.mintal.domain.grove.PlantNode
import dev.nheox.mintal.ui.theme.MintalPalette
import dev.nheox.mintal.ui.theme.mintalPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/*
 * Mintal has no commissioned artwork (spec §8.3), so every mint is drawn from the traits
 * in the author's photo reference: outline, attachment, margin, surface, hairiness, stem
 * colour, flower form. One construction grammar, and species told apart by shape and
 * texture rather than by tint — which is the part of the brief that keeps a set from
 * looking generic.
 *
 * The first pass was under-detailed and in places simply wrong: flat shapes with no
 * veining or margin detail, and lavender mint wearing apple mint's sessile, puckered,
 * crenate leaf when it should be short-stalked, smoother and softly hairy. Everything
 * here is per-species for that reason, and the species sheet screenshot is how it gets
 * checked.
 *
 * When the illustrator's kit arrives, these functions are what it replaces.
 */

private const val PROFILE_STEPS = 24

/**
 * How much of a leaf is worth drawing at its size on screen. A mature Grove is hundreds
 * of leaves; veins and hairs on a six-pixel blade cost time and show nothing.
 */
enum class LeafDetail { FLAT, SIMPLE, FULL }

fun detailFor(leafLengthPx: Float): LeafDetail = when {
    leafLengthPx >= 22f -> LeafDetail.FULL
    leafLengthPx >= 9f -> LeafDetail.SIMPLE
    else -> LeafDetail.FLAT
}

/** Every colour one plant needs, resolved once from the palette. */
data class PlantColours(
    val leaf: Color,
    val stem: Color,
    val flower: Color,
    val hair: Color,
    val variegation: Color,
    val flush: Color?,
) {
    companion object {
        fun of(palette: MintalPalette, species: Species, unlocked: Boolean = true): PlantColours {
            if (!unlocked) {
                return PlantColours(
                    leaf = palette.chartGrid,
                    stem = palette.chartGrid,
                    flower = palette.chartGrid,
                    hair = palette.chartGrid,
                    variegation = palette.chartGrid,
                    flush = null,
                )
            }
            return PlantColours(
                leaf = palette.leaf(species.art.leafHue),
                stem = palette.stem(species.art.stemHue),
                flower = palette.flower,
                hair = palette.leafHair,
                variegation = palette.variegation,
                flush = when (species.art.flush) {
                    LeafFlush.NONE -> null
                    LeafFlush.PURPLE -> palette.flushPurple
                    LeafFlush.BRONZE -> palette.flushBronze
                },
            )
        }

        fun wilted(palette: MintalPalette): PlantColours = PlantColours(
            leaf = palette.wilted,
            stem = palette.wilted,
            flower = palette.wilted,
            hair = palette.wilted,
            variegation = palette.wilted,
            flush = null,
        )
    }
}

/**
 * Half-width at a point along the midrib, as a fraction of leaf length. This is the
 * outline, and the outline is most of the identification: a lanceolate spearmint leaf and
 * a rounded-ovate lavender mint leaf are different plants at a glance only because these
 * numbers differ.
 */
private fun leafProfile(t: Float, shape: LeafShape): Float {
    // sin(PI) in Float is -8.7e-8, not 0. Raising that to a fractional power gives NaN,
    // one NaN point makes the whole leaf path degenerate, and nothing is drawn at all —
    // which is why every mint once rendered as a bare stem.
    val x = t.coerceIn(0f, 1f)
    val bell = sin(PI.toFloat() * x).coerceAtLeast(0f)
    // The (1 - k·t) term drags the widest point toward the base and sharpens the tip;
    // without it every shape is a symmetric lens.
    return when (shape) {
        LeafShape.LANCEOLATE -> bell.pow(1.15f) * (1f - 0.42f * x) * 0.30f
        LeafShape.OVATE_LANCEOLATE -> bell.pow(0.95f) * (1f - 0.34f * x) * 0.34f
        LeafShape.OVATE -> bell.pow(0.80f) * (1f - 0.22f * x) * 0.38f
        LeafShape.ROUNDED_OVATE -> bell.pow(0.62f) * (1f - 0.08f * x) * 0.42f
        LeafShape.OBLONG -> bell.pow(0.42f) * (1f - 0.06f * x) * 0.37f
        LeafShape.OVAL -> bell.pow(0.58f) * 0.40f
        LeafShape.ROUND -> bell.pow(0.48f) * 0.48f
        LeafShape.LINEAR -> bell.pow(0.30f) * 0.075f
    }
}

/**
 * Teeth, scallops or a smooth edge, faded out at both ends — a real leaf's margin is
 * entire for the first and last tenth whatever it does in between.
 */
private fun marginFactor(step: Int, steps: Int, margin: LeafMargin): Float {
    if (margin == LeafMargin.ENTIRE) return 1f
    val t = step.toFloat() / steps
    val taper = (sin(PI.toFloat() * t).coerceAtLeast(0f)).pow(0.5f)
    return when (margin) {
        // Sawteeth: out, in, out, in. Shallow enough to survive a 20px leaf.
        LeafMargin.SERRATE -> 1f + (if (step % 2 == 0) 0.13f else -0.07f) * taper
        LeafMargin.CRENATE -> 1f + 0.075f * sin(step * 1.75f) * taper
        LeafMargin.ENTIRE -> 1f
    }
}

/**
 * A single leaf blade, pointing from [base] to [tip]. The outline is sampled from a width
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
            val width = halfWidth * leafProfile(t, shape) * marginFactor(step, PROFILE_STEPS, margin)
            val x = base.x + axisX * t - alongY * width * side
            val y = base.y + axisY * t + alongX * width * side
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

/**
 * One blade with everything the reference says about its surface: veins, puckering,
 * gloss, hairs, variegation, a flush of another colour. [foreshorten] squashes the blade
 * across its width for a leaf turned away from the viewer.
 */
private fun DrawScope.drawBlade(
    base: Offset,
    angle: Float,
    length: Float,
    species: Species,
    colours: PlantColours,
    detail: LeafDetail,
    outlineWidth: Float,
    foreshorten: Float = 1f,
    seed: Int = 0,
) {
    if (length < 1f) return
    val tip = Offset(base.x + cos(angle) * length, base.y + sin(angle) * length)
    val path = leafPath(
        base = base,
        tip = tip,
        halfWidth = length * foreshorten,
        shape = species.art.leafShape,
        margin = species.art.margin,
    )
    if (path.isEmpty) return

    drawPath(path = path, color = colours.leaf)

    val alongX = cos(angle)
    val alongY = sin(angle)
    val perpX = -alongY
    val perpY = alongX
    fun at(t: Float, across: Float) = Offset(
        base.x + alongX * length * t + perpX * across,
        base.y + alongY * length * t + perpY * across,
    )

    if (detail != LeafDetail.FLAT) {
        // A flush is a wash under the margin, not a repaint of the leaf: clipping to the
        // blade and stroking its own outline from the inside is what gives the band.
        colours.flush?.let { flush ->
            clipPath(path) {
                drawPath(path = path, color = flush, style = Stroke(width = length * 0.34f))
            }
        }
        if (species.art.variegated) {
            // Gold blotches along the veins, placed from the seed so one leaf is not a
            // copy of the next, and never so regular that it reads as a pattern.
            for (blotch in 0 until 5) {
                val t = 0.22f + 0.15f * blotch
                val side = if ((seed + blotch) % 2 == 0) 1f else -1f
                // Out near the margin, where the gold actually sits on a 'Variegata'.
                val across = side * length * leafProfile(t, species.art.leafShape) * 0.86f
                drawCircle(
                    color = colours.variegation,
                    radius = length * (0.055f + 0.012f * ((seed + blotch) % 3)),
                    center = at(t, across),
                )
            }
        }
        if (species.art.surface == LeafSurface.GLOSSY) {
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = length * 0.11f,
                center = at(0.34f, length * 0.12f),
            )
        }
    }

    // The midrib is what makes a green shape read as a leaf rather than a blob; the
    // laterals are what make it read as a mint rather than a petal.
    val veinColour = colours.stem.copy(alpha = if (species.art.surface == LeafSurface.VEINED) 0.75f else 0.5f)
    val veinWidth = outlineWidth * if (species.art.surface == LeafSurface.VEINED) 1.1f else 0.8f
    drawLine(
        color = veinColour,
        start = base,
        end = at(0.94f, 0f),
        strokeWidth = veinWidth,
        cap = StrokeCap.Round,
    )

    if (detail == LeafDetail.FULL) {
        val pairs = when (species.art.leafShape) {
            LeafShape.LINEAR -> 0
            LeafShape.ROUND, LeafShape.OVAL -> 2
            else -> 3
        }
        for (pair in 1..pairs) {
            val t = 0.18f + 0.22f * (pair - 1)
            val reach = leafProfile(t + 0.1f, species.art.leafShape) * length * 0.82f
            for (side in intArrayOf(1, -1)) {
                drawLine(
                    color = veinColour.copy(alpha = veinColour.alpha * 0.8f),
                    start = at(t, 0f),
                    end = at(t + 0.16f, side * reach * foreshorten),
                    strokeWidth = veinWidth * 0.6f,
                    cap = StrokeCap.Round,
                )
            }
        }
        if (species.art.surface == LeafSurface.PUCKERED) {
            // Quilting: the blade bulges between the veins, so the shading sits in the
            // pockets rather than along the edge.
            for (pocket in 0 until 3) {
                val t = 0.26f + 0.2f * pocket
                for (side in intArrayOf(1, -1)) {
                    val reach = leafProfile(t, species.art.leafShape) * length * 0.5f
                    drawCircle(
                        color = colours.stem.copy(alpha = 0.13f),
                        radius = length * 0.10f,
                        center = at(t, side * reach * foreshorten),
                    )
                }
            }
        }
    }

    // Hairs last, over everything, because that is where they are.
    if (detail != LeafDetail.FLAT && species.art.pubescence != Pubescence.NONE) {
        val wash = when (species.art.pubescence) {
            Pubescence.WOOLLY -> 0.30f
            Pubescence.DOWNY -> 0.16f
            else -> 0f
        }
        if (wash > 0f) {
            drawPath(path = path, color = colours.hair.copy(alpha = wash))
        }
        if (detail == LeafDetail.FULL) {
            val hairs = when (species.art.pubescence) {
                Pubescence.WOOLLY -> 9
                Pubescence.DOWNY -> 6
                Pubescence.HAIRY -> 5
                Pubescence.NONE -> 0
            }
            val hairLength = length * if (species.art.pubescence == Pubescence.WOOLLY) 0.13f else 0.09f
            for (hair in 0 until hairs) {
                val t = 0.12f + 0.76f * (hair.toFloat() / hairs.coerceAtLeast(1))
                val side = if ((hair + seed) % 2 == 0) 1f else -1f
                val edge = leafProfile(t, species.art.leafShape) * length * foreshorten
                val from = at(t, side * edge)
                val lean = angle + side * 1.2f
                drawLine(
                    color = colours.hair.copy(alpha = 0.75f),
                    start = from,
                    end = Offset(
                        from.x + cos(lean) * hairLength,
                        from.y + sin(lean) * hairLength,
                    ),
                    strokeWidth = outlineWidth * 0.45f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    drawPath(
        path = path,
        color = colours.stem.copy(alpha = 0.85f),
        style = Stroke(width = outlineWidth),
    )
}

/**
 * The opposite pair every mint carries at a node.
 *
 * [edgeOn] draws the pair that is turned 90° out of the page. Mint is decussate, so every
 * second pair is, and drawing them all flat is what makes a plant stop reading as mint:
 * an edge-on pair is short, narrow and close to the stem, and the leaf pointing away is
 * drawn a touch smaller than the one coming toward the reader.
 */
fun DrawScope.drawLeafPair(
    node: Offset,
    axisAngle: Float,
    leafLength: Float,
    species: Species,
    colours: PlantColours,
    detail: LeafDetail = LeafDetail.SIMPLE,
    edgeOn: Boolean = false,
    wilted: Boolean = false,
    outlineWidth: Float = 1f,
    spread: Float = spreadFor(species),
    seed: Int = 0,
) {
    val stalk = if (species.art.attachment == LeafAttachment.STALKED) leafLength * 0.20f else 0f

    for ((index, side) in intArrayOf(1, -1).withIndex()) {
        // Away from the reader first, so the near leaf overlaps it.
        val away = edgeOn && side == 1
        // A pair turned out of the page still leaves the stem at the same angle; what
        // changes is that the blade points at the reader, so it projects short and
        // narrow rather than swinging in toward the stem.
        val angle = axisAngle + spread * side * (if (edgeOn) 0.82f else 1f) +
            if (wilted) 0.45f * side else 0f
        val length = leafLength * when {
            !edgeOn -> 1f
            away -> 0.34f
            else -> 0.46f
        } * if (wilted) 0.62f else 1f
        val foreshorten = if (edgeOn) 0.62f else 1f

        val bladeBase = Offset(
            node.x + cos(angle) * stalk,
            node.y + sin(angle) * stalk,
        )
        if (stalk > 0f) {
            drawLine(
                color = colours.stem,
                start = node,
                end = bladeBase,
                strokeWidth = outlineWidth * 1.4f,
                cap = StrokeCap.Round,
            )
        }
        drawBlade(
            base = bladeBase,
            angle = angle,
            length = length,
            species = species,
            colours = if (away) colours.copy(leaf = colours.leaf.copy(alpha = 0.82f)) else colours,
            detail = detail,
            outlineWidth = outlineWidth,
            foreshorten = foreshorten,
            seed = seed + index,
        )
    }
}

/**
 * How far a leaf stands off the stem. A rosemary-like linear leaf sweeps up close to the
 * stem; a round mat leaf lies out almost square to it.
 */
private fun spreadFor(species: Species): Float = when {
    species.art.leafShape == LeafShape.LINEAR -> 0.58f
    species.art.habit == GrowthHabit.MAT -> 1.25f
    else -> 0.92f
}

/**
 * Mint stems are square, and on the hairy species the hairs run down the angles. At any
 * real size a second line down one face is all it takes to say so.
 */
private fun DrawScope.drawStemSegment(
    start: Offset,
    end: Offset,
    width: Float,
    colour: Color,
    hairy: Boolean,
    detail: LeafDetail,
) {
    drawLine(
        color = colour,
        start = start,
        end = end,
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
    if (detail == LeafDetail.FLAT) return

    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = hypot(dx, dy)
    if (length < 1f) return
    val perpX = -dy / length
    val perpY = dx / length

    // The lit angle of a square stem.
    drawLine(
        color = Color.White.copy(alpha = 0.16f),
        start = Offset(start.x + perpX * width * 0.26f, start.y + perpY * width * 0.26f),
        end = Offset(end.x + perpX * width * 0.26f, end.y + perpY * width * 0.26f),
        strokeWidth = width * 0.3f,
        cap = StrokeCap.Round,
    )

    if (hairy && detail == LeafDetail.FULL) {
        val hairs = (length / 7f).toInt().coerceIn(2, 7)
        for (hair in 0 until hairs) {
            val t = (hair + 0.5f) / hairs
            val at = Offset(start.x + dx * t, start.y + dy * t)
            for (side in intArrayOf(1, -1)) {
                drawLine(
                    color = colour.copy(alpha = 0.55f),
                    start = at,
                    end = Offset(
                        at.x + perpX * side * width * 1.5f - dx / length * width * 0.6f,
                        at.y + perpY * side * width * 1.5f - dy / length * width * 0.6f,
                    ),
                    strokeWidth = width * 0.22f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * How a species flowers tells the mints apart faster than leaf shape does: a slender or
 * blunt spike above the tip, a round head with one whorl under it, whorls in the leaf
 * axils with the stem continuing past them, whorls spaced up a bare stem, or dense globes
 * with the stamens sticking out.
 */
private fun DrawScope.drawInflorescence(
    tip: Offset,
    nodes: List<Offset>,
    angle: Float,
    unit: Float,
    form: FlowerForm,
    colour: Color,
) {
    fun along(distance: Float) = Offset(
        tip.x + cos(angle) * distance,
        tip.y + sin(angle) * distance,
    )

    when (form) {
        FlowerForm.SLENDER_SPIKE -> for (step in 0 until 7) {
            val fraction = step / 6f
            drawCircle(colour, unit * (0.9f - 0.55f * fraction), along(unit * step * 0.8f))
        }
        FlowerForm.BLUNT_SPIKE -> for (step in 0 until 3) {
            drawCircle(colour, unit * 1.15f, along(unit * step * 1.25f))
        }
        FlowerForm.ROUND_HEAD -> {
            drawCircle(colour, unit * 1.9f, along(unit * 2.2f))
            // The single whorl below the terminal head: water mint's giveaway.
            drawCircle(colour, unit * 0.85f, along(-unit * 0.4f))
        }
        FlowerForm.GLOBE -> {
            val centre = along(unit * 1.2f)
            drawCircle(colour, unit * 1.15f, centre)
            // Protruding stamens: the reason this one does not read as a mint at all.
            for (spoke in 0 until 8) {
                val spokeAngle = spoke * 0.7854f
                drawLine(
                    color = colour,
                    start = centre,
                    end = Offset(
                        centre.x + cos(spokeAngle) * unit * 1.8f,
                        centre.y + sin(spokeAngle) * unit * 1.8f,
                    ),
                    strokeWidth = unit * 0.24f,
                )
            }
        }
        // Whorls sit at the leaf axils, and the stem carries on above them — no head.
        FlowerForm.AXILLARY_WHORLS -> nodes.forEach { node ->
            for (side in intArrayOf(1, -1)) {
                drawCircle(
                    colour,
                    unit * 0.62f,
                    Offset(node.x + cos(angle + 1.5708f * side) * unit * 0.7f, node.y + sin(angle + 1.5708f * side) * unit * 0.7f),
                )
            }
        }
        FlowerForm.SPACED_WHORLS -> nodes.forEach { drawCircle(colour, unit * 1.05f, it) }
        FlowerForm.NONE -> Unit
    }
}

/**
 * Draws a whole plant from its node structure. [unitPx] is how many pixels one structural
 * unit is worth — one stem segment is roughly one unit — so the same plant draws at any
 * size, and [origin] is where its base meets the ground.
 */
fun DrawScope.drawPlant(
    plant: Plant,
    origin: Offset,
    unitPx: Float,
    species: Species,
    colours: PlantColours,
    showFlowers: Boolean = plant.isMature,
    wilted: Boolean = false,
) {
    fun project(x: Float, y: Float) = Offset(origin.x + x * unitPx, origin.y + y * unitPx)

    // A mint leaf is about as long as the internode below it. Half that — which is
    // where this started — draws a plant with specks on it rather than foliage.
    val leafUnit = unitPx * plant.segmentUnit * 0.92f * species.art.leafScale
    val detail = detailFor(leafUnit)
    val outline = (unitPx * 0.012f).coerceIn(0.5f, 2.4f)

    for (segment in plant.segments) {
        drawStemSegment(
            start = project(segment.startX, segment.startY),
            end = project(segment.endX, segment.endY),
            width = (segment.width * unitPx).coerceAtLeast(1.2f),
            colour = colours.stem,
            hairy = species.art.stemHairy,
            detail = detail,
        )
    }

    // Edge-on pairs first: they sit behind the ones facing the reader.
    val ordered = plant.nodes.sortedBy { if (it.pairIsEdgeOn) 0 else 1 }
    for (node in ordered) {
        drawLeafPair(
            node = project(node.x, node.y),
            axisAngle = node.stemAngle,
            leafLength = leafUnit * node.scale,
            species = species,
            colours = colours,
            detail = detail,
            edgeOn = node.pairIsEdgeOn,
            wilted = wilted,
            outlineWidth = outline,
            spread = spreadFor(species),
            seed = (node.sessionId and 0xFF).toInt(),
        )
    }

    if (!showFlowers || wilted) return
    val flowerUnit = (unitPx * plant.segmentUnit * 0.15f).coerceAtLeast(1.1f)
    when (species.art.flower) {
        FlowerForm.AXILLARY_WHORLS, FlowerForm.SPACED_WHORLS -> drawInflorescence(
            tip = project(0f, 0f),
            nodes = plant.nodes.filterNot { it.pairIsEdgeOn }.map { project(it.x, it.y) },
            angle = -1.5707964f,
            unit = flowerUnit,
            form = species.art.flower,
            colour = colours.flower,
        )
        else -> plant.terminals.forEach { tip ->
            drawInflorescence(
                tip = project(tip.x, tip.y),
                nodes = emptyList(),
                angle = tip.stemAngle,
                unit = flowerUnit,
                form = species.art.flower,
                colour = colours.flower,
            )
        }
    }
}

/** A failed session: a short bare stem with two collapsed leaves and nothing else. */
fun DrawScope.drawWiltedStub(
    origin: Offset,
    unitPx: Float,
    species: Species?,
    colours: PlantColours,
) {
    val tip = Offset(origin.x + unitPx * 0.10f, origin.y - unitPx * 0.55f)
    drawLine(
        color = colours.stem,
        start = origin,
        end = tip,
        strokeWidth = (unitPx * 0.05f).coerceAtLeast(1f),
        cap = StrokeCap.Round,
    )
    if (species == null) return
    drawLeafPair(
        node = tip,
        axisAngle = -1.2f,
        leafLength = unitPx * 0.22f,
        species = species,
        colours = colours,
        detail = detailFor(unitPx * 0.22f),
        wilted = true,
        outlineWidth = (unitPx * 0.012f).coerceIn(0.5f, 2f),
    )
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
    val colours = PlantColours.of(palette, species, unlocked)
    // A catalogue illustration shows the plant in flower, because flower form is one of
    // the traits the catalogue is naming. In the Grove it waits for maturity.
    // A sprig, not a whole plant: at emblem size a mature twelve-node specimen is a
    // thicket, and the point of the catalogue is to show one leaf clearly.
    val plant = remember(species.id) {
        PlantBuilder.grow(
            speciesId = species.id,
            plantIndex = 0,
            sessionIds = (1L..5L).toList(),
            habit = species.art.habit,
            seed = species.id.hashCode().toLong(),
        )
    }
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "${species.commonName} sprig" },
    ) {
        // Leaves reach about one unit out from the stem, so the drawn plant is wider
        // and taller than its skeleton; padding the bounds by that keeps it in frame.
        val reach = 1.1f * plant.segmentUnit * species.art.leafScale
        val spanX = (plant.maxX - plant.minX) + 2f * reach
        val spanY = -plant.minY + reach + 0.6f
        val unit = minOf(this.size.width / spanX, this.size.height / spanY)
        drawPlant(
            plant = plant,
            origin = Offset(
                x = this.size.width / 2f - (plant.minX + plant.maxX) / 2f * unit,
                y = this.size.height * 0.96f,
            ),
            unitPx = unit,
            species = species,
            colours = colours,
            showFlowers = true,
        )
    }
}

package dev.nheox.mintal.domain.grove

import dev.nheox.mintal.core.model.GrowthHabit
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** How far along a plant's life one is, which is also what decides whether it flowers. */
enum class GrowthStage { SEEDLING, YOUNG, GROWING, MATURE }

/**
 * One node: a point on a stem carrying an opposite pair of leaves, and possibly the
 * origin of a branch.
 *
 * [rotation] alternates 0, 1, 0, 1 up every axis. Mint is decussate — each pair is turned
 * roughly 90° from the pair below it — so seen from the side, every second pair points at
 * the viewer rather than across the page. Drawing them all flat is the single thing that
 * makes a plant read as a generic herb instead of a mint.
 */
data class PlantNode(
    val sessionId: Long,
    val x: Float,
    val y: Float,
    val stemAngle: Float,
    val rotation: Int,
    val axisId: Int,
    val indexOnAxis: Int,
    val scale: Float,
    val terminal: Boolean,
) {
    val pairIsEdgeOn: Boolean get() = rotation % 2 == 1
}

/** A length of square stem between two nodes (or from the ground to the first node). */
data class PlantSegment(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val axisId: Int,
    val width: Float,
)

data class Plant(
    val speciesId: String,
    val plantIndex: Int,
    val nodes: List<PlantNode>,
    val segments: List<PlantSegment>,
    val stage: GrowthStage,
    /**
     * Length of this plant's first internode, in structural units. Leaf size is taken
     * from it rather than from the unit, because a leaf is only ever big or small
     * relative to the stem it sits on — which is the whole difference between Corsican
     * mint and spearmint.
     */
    val segmentUnit: Float,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
) {
    val nodeCount: Int get() = nodes.size
    val isMature: Boolean get() = stage == GrowthStage.MATURE

    /** Tips of the main stem and of each branch: where an inflorescence belongs. */
    val terminals: List<PlantNode> get() = nodes.filter { it.terminal }
}

/**
 * Turns a run of sessions into one plant's structure (spec §5.6, amended).
 *
 * One session is one node: a stem segment plus the leaf pair at its top. Not a stage
 * jump, not a leaf on its own — a mint cannot grow a single leaf, and a plant that
 * doubled in size every session outgrew the bed within a week.
 *
 * Where the node goes is fixed, not random, and chosen to keep the plant even: the main
 * stem goes up first, and after that nodes alternate strictly left, right, left, right
 * onto two branches that start from the two opposite axils of one node — which is where
 * a real branch starts, since that is where the buds are. A plant therefore never leans
 * because of the order its sessions happened to arrive in.
 *
 * At [MATURE_NODES] it is done. The next session for that species starts a new plant
 * beside it, which is why a well-used mint ends up a clump rather than a tower.
 */
object PlantBuilder {

    const val MAIN_AXIS_NODES = 6
    const val MATURE_NODES = 12

    private const val UP = -1.5707964f

    private class Tuning(
        val segment: Float,
        val baseAngle: Float,
        val wobble: Float,
        val branchSpread: Float,
        val branchLift: Float,
        val stemWidth: Float,
        val mainNodes: Int,
        val branchAxilIndex: Int,
    ) {
        /** Whatever maturity leaves over, split evenly between the two branches. */
        val branchNodes: Int get() = (MATURE_NODES - mainNodes) / 2
    }

    private fun tuningFor(habit: GrowthHabit): Tuning = when (habit) {
        GrowthHabit.UPRIGHT -> Tuning(1f, UP, 0.10f, 0.95f, 0.22f, 0.072f, MAIN_AXIS_NODES, 2)
        GrowthHabit.TALL -> Tuning(1.22f, UP, 0.06f, 0.75f, 0.26f, 0.062f, MAIN_AXIS_NODES, 2)
        // Pennyroyal: low and spreading, with the flowering shoots still going up.
        GrowthHabit.SPREADING -> Tuning(0.80f, UP + 0.10f, 0.16f, 1.30f, 0.34f, 0.080f, 4, 1)
        // Corsican mint does not stand up at all. Two runners leave the crown in opposite
        // directions and stay down, which is what a mat spilling over stone looks like —
        // and it keeps the plant inside its own patch instead of crawling into the next.
        GrowthHabit.MAT -> Tuning(0.32f, UP, 0.10f, 1.52f, 0.0f, 0.050f, 2, 0)
    }

    fun stageFor(nodeCount: Int): GrowthStage = when {
        nodeCount >= MATURE_NODES -> GrowthStage.MATURE
        nodeCount >= MATURE_NODES / 2 -> GrowthStage.GROWING
        nodeCount >= 3 -> GrowthStage.YOUNG
        else -> GrowthStage.SEEDLING
    }

    /**
     * [sessionIds] are this plant's sessions in the order they happened, at most
     * [MATURE_NODES] of them. [seed] gives the plant its own slight lean and is taken
     * from a session id, so the same history always draws the same plant.
     */
    fun grow(
        speciesId: String,
        plantIndex: Int,
        sessionIds: List<Long>,
        habit: GrowthHabit,
        seed: Long,
    ): Plant {
        val count = sessionIds.size.coerceAtMost(MATURE_NODES)
        val tuning = tuningFor(habit)
        val nodes = ArrayList<PlantNode>(count)
        val segments = ArrayList<PlantSegment>(count)

        var mainX = 0f
        var mainY = 0f
        var mainAngle = tuning.baseAngle
        val mainNodes = ArrayList<PlantNode>(MAIN_AXIS_NODES)

        val mainCount = minOf(count, tuning.mainNodes)
        for (index in 0 until mainCount) {
            // The lowest internode is the shortest on a real stem, and a seedling whose
            // first pair of leaves floats halfway up a bare stalk looks like a mistake.
            val length = tuning.segment * (if (index == 0) 0.62f else 1f) * 0.95f.pow(index)
            mainAngle += (noise(seed, index) - 0.5f) * 2f * tuning.wobble
            val endX = mainX + cos(mainAngle) * length
            val endY = mainY + sin(mainAngle) * length
            segments += PlantSegment(
                startX = mainX,
                startY = mainY,
                endX = endX,
                endY = endY,
                axisId = 0,
                width = tuning.stemWidth * (1f - 0.06f * index),
            )
            val node = PlantNode(
                sessionId = sessionIds[index],
                x = endX,
                y = endY,
                stemAngle = mainAngle,
                rotation = index % 2,
                axisId = 0,
                indexOnAxis = index,
                scale = 1f - 0.055f * index,
                terminal = false,
            )
            nodes += node
            mainNodes += node
            mainX = endX
            mainY = endY
        }

        // Both branches leave from the same node, through its two opposite axils, and
        // then turn back toward the light — which is what keeps the plant balanced
        // whichever of them a session lands on.
        val axil = mainNodes.getOrNull(tuning.branchAxilIndex)
        if (axil != null) {
            val tips = HashMap<Int, Triple<Float, Float, Float>>()
            for (position in tuning.mainNodes until count) {
                val order = position - tuning.mainNodes
                val side = if (order % 2 == 0) -1 else 1
                val onBranch = order / 2
                if (onBranch >= tuning.branchNodes) break
                val axisId = if (side < 0) 1 else 2

                val state = tips.getOrPut(axisId) {
                    Triple(axil.x, axil.y, axil.stemAngle + side * tuning.branchSpread)
                }
                val (startX, startY, incoming) = state
                val angle = incoming - side * tuning.branchLift * onBranch +
                    (noise(seed, 40 + position) - 0.5f) * tuning.wobble
                val length = tuning.segment * 0.80f * 0.92f.pow(onBranch)
                val endX = startX + cos(angle) * length
                val endY = startY + sin(angle) * length

                segments += PlantSegment(
                    startX = startX,
                    startY = startY,
                    endX = endX,
                    endY = endY,
                    axisId = axisId,
                    width = tuning.stemWidth * (0.74f - 0.06f * onBranch),
                )
                nodes += PlantNode(
                    sessionId = sessionIds[position],
                    x = endX,
                    y = endY,
                    stemAngle = angle,
                    // The branch's own count, so its pairs alternate too; starting from
                    // the axil node's own rotation would put the first pair edge-on into
                    // the stem it just left.
                    rotation = onBranch % 2,
                    axisId = axisId,
                    indexOnAxis = onBranch,
                    scale = 0.82f - 0.05f * onBranch,
                    terminal = false,
                )
                tips[axisId] = Triple(endX, endY, angle)
            }
        }

        val withTerminals = nodes.map { node ->
            val isTip = nodes.none { other -> other.axisId == node.axisId && other.indexOnAxis > node.indexOnAxis }
            node.copy(terminal = isTip)
        }

        return Plant(
            speciesId = speciesId,
            plantIndex = plantIndex,
            nodes = withTerminals,
            segments = segments,
            stage = stageFor(count),
            segmentUnit = tuning.segment,
            minX = (withTerminals.minOfOrNull { it.x } ?: 0f).coerceAtMost(0f),
            maxX = (withTerminals.maxOfOrNull { it.x } ?: 0f).coerceAtLeast(0f),
            minY = (withTerminals.minOfOrNull { it.y } ?: 0f).coerceAtMost(0f),
        )
    }

    /** Deterministic [0f, 1f) from a session id, so a plant never redraws differently. */
    fun noise(seed: Long, salt: Int): Float {
        var x = seed * -7046029254386353131L + salt * 1609587929392839161L
        x = (x xor (x ushr 30)) * -4658895280553007687L
        x = (x xor (x ushr 27)) * -7723592293110705685L
        x = x xor (x ushr 31)
        return ((x ushr 40).toInt() and 0xFFFF) / 65536f
    }
}

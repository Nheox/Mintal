package dev.nheox.mintal.domain

import dev.nheox.mintal.core.model.GrowthHabit
import dev.nheox.mintal.domain.grove.GrowthStage
import dev.nheox.mintal.domain.grove.PlantBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * The growth rules, which are botanical claims as much as geometric ones: one node per
 * session, opposite pairs turned ninety degrees from the pair below, branches out of leaf
 * axils, and a plant that stops when it is grown.
 */
class PlantBuilderTest {

    private fun grow(nodes: Int, habit: GrowthHabit = GrowthHabit.UPRIGHT) =
        PlantBuilder.grow(
            speciesId = "spicata",
            plantIndex = 0,
            sessionIds = (1L..nodes.toLong()).toList(),
            habit = habit,
            seed = 7L,
        )

    @Test
    fun `one session is one node, and never more`() {
        for (count in 1..PlantBuilder.MATURE_NODES) {
            assertEquals(count, grow(count).nodeCount, "$count sessions")
            assertEquals(count, grow(count).segments.size, "$count segments")
        }
    }

    @Test
    fun `a plant stops at maturity instead of growing forever`() {
        val overgrown = PlantBuilder.grow(
            speciesId = "spicata",
            plantIndex = 0,
            sessionIds = (1L..40L).toList(),
            habit = GrowthHabit.UPRIGHT,
            seed = 1L,
        )
        assertEquals(PlantBuilder.MATURE_NODES, overgrown.nodeCount)
        assertTrue(overgrown.isMature)
    }

    @Test
    fun `leaf pairs alternate ninety degrees up every axis`() {
        val plant = grow(PlantBuilder.MATURE_NODES)
        plant.nodes.groupBy { it.axisId }.forEach { (axis, nodes) ->
            nodes.sortedBy { it.indexOnAxis }.forEachIndexed { index, node ->
                assertEquals(
                    index % 2,
                    node.rotation,
                    "axis $axis node $index should face ${if (index % 2 == 0) "the reader" else "away"}",
                )
            }
        }
    }

    @Test
    fun `branches leave from a leaf axil, not from the middle of a stem`() {
        val plant = grow(PlantBuilder.MATURE_NODES)
        val nodePoints = plant.nodes.map { it.x to it.y }.toSet() + (0f to 0f)
        for (segment in plant.segments) {
            assertTrue(
                (segment.startX to segment.startY) in nodePoints,
                "a segment on axis ${segment.axisId} starts at no node at all",
            )
        }
        // And both branches leave the same node, as a real pair of axillary buds does.
        val axil = plant.nodes.first { it.axisId == 0 && it.indexOnAxis == 2 }
        val roots = plant.segments
            .filter { it.axisId != 0 }
            .groupBy { it.axisId }
            .map { (_, segments) -> segments.first() }
        assertEquals(2, roots.size, "a mature plant should carry two branches")
        roots.forEach { root ->
            assertEquals(
                axil.x to axil.y,
                root.startX to root.startY,
                "a branch must start at the axil node, not partway up the stem",
            )
        }
    }

    @Test
    fun `growth stays balanced however many sessions arrive`() {
        for (count in PlantBuilder.MAIN_AXIS_NODES..PlantBuilder.MATURE_NODES) {
            val plant = grow(count)
            val left = plant.nodes.count { it.axisId == 1 }
            val right = plant.nodes.count { it.axisId == 2 }
            assertTrue(
                abs(left - right) <= 1,
                "$count sessions piled $left nodes left and $right right",
            )
        }
    }

    @Test
    fun `a mature plant is roughly symmetric about its stem`() {
        val plant = grow(PlantBuilder.MATURE_NODES)
        val lean = (plant.minX + plant.maxX) / 2f
        assertTrue(abs(lean) < 0.35f, "the plant leans off centre by $lean")
        assertTrue(plant.minY < -2f, "a mature plant should stand up: top at ${plant.minY}")
    }

    @Test
    fun `stages run seedling, young, growing, mature`() {
        assertEquals(GrowthStage.SEEDLING, PlantBuilder.stageFor(1))
        assertEquals(GrowthStage.SEEDLING, PlantBuilder.stageFor(2))
        assertEquals(GrowthStage.YOUNG, PlantBuilder.stageFor(3))
        assertEquals(GrowthStage.GROWING, PlantBuilder.stageFor(PlantBuilder.MAIN_AXIS_NODES))
        assertEquals(GrowthStage.MATURE, PlantBuilder.stageFor(PlantBuilder.MATURE_NODES))
    }

    @Test
    fun `the same sessions always draw the same plant`() {
        val first = grow(9)
        val second = grow(9)
        assertEquals(first.nodes, second.nodes)
        assertEquals(first.segments, second.segments)
    }

    @Test
    fun `growing a plant never moves the nodes already on it`() {
        // A session added today must not rearrange yesterday's growth.
        // Everything but the terminal flag, which by definition moves to the new tip.
        val before = grow(5).nodes.map { it.copy(terminal = false) }
        val after = grow(6).nodes.take(5).map { it.copy(terminal = false) }
        assertEquals(before, after)
    }

    @Test
    fun `a creeping mat stays low and a tall mint stands up`() {
        val mat = grow(PlantBuilder.MATURE_NODES, GrowthHabit.MAT)
        val tall = grow(PlantBuilder.MATURE_NODES, GrowthHabit.TALL)

        // Habit is a shape, not a size: what separates Corsican mint from horse mint is
        // that one is wider than it is high and the other is the reverse.
        val matAspect = (mat.maxX - mat.minX) / -mat.minY
        val tallAspect = (tall.maxX - tall.minX) / -tall.minY
        assertTrue(matAspect > 2f, "a mat should sprawl, aspect was $matAspect")
        assertTrue(tallAspect < 1f, "a tall mint should stand, aspect was $tallAspect")
        assertTrue(tall.minY < mat.minY, "horse mint should out-top Corsican mint")
    }
}

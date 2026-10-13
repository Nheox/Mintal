package dev.nheox.mintal.domain

import dev.nheox.mintal.core.model.GrowthHabit
import dev.nheox.mintal.core.model.LeafHue
import dev.nheox.mintal.core.model.LeafMargin
import dev.nheox.mintal.core.model.LeafShape
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.model.SpeciesArt
import dev.nheox.mintal.core.model.StemHue
import dev.nheox.mintal.domain.milestone.MilestoneEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MilestoneEngineTest {

    private fun species(id: String, unlockMinutes: Int) = Species(
        id = id,
        binomial = "Mentha $id",
        commonName = id,
        unlockMinutes = unlockMinutes,
        hybrid = false,
        note = "",
        art = SpeciesArt(
            leafShape = LeafShape.OVATE,
            margin = LeafMargin.SERRATE,
            habit = GrowthHabit.UPRIGHT,
            leafScale = 1f,
            leafHue = LeafHue.LEAF_MID,
            stemHue = StemHue.STEM_GREEN,
        ),
    )

    private val catalogue = listOf(
        species("spicata", 0),
        species("piperita", 120),
        species("suaveolens", 300),
    )

    @Test
    fun `the starter mint is unlocked with no focus at all`() {
        assertTrue(MilestoneEngine.isUnlocked(catalogue[0], 0))
        assertFalse(MilestoneEngine.isUnlocked(catalogue[1], 0))
    }

    @Test
    fun `a threshold is crossed exactly at its second, not a second before`() {
        assertFalse(MilestoneEngine.isUnlocked(catalogue[1], 120 * 60 - 1))
        assertTrue(MilestoneEngine.isUnlocked(catalogue[1], 120 * 60))
    }

    @Test
    fun `newly unlocked only reports what has not been stamped`() {
        val newly = MilestoneEngine.newlyUnlocked(
            catalog = catalogue,
            alreadyStamped = setOf("spicata"),
            totalSuccessfulSeconds = 120 * 60,
        )
        assertEquals(listOf("piperita"), newly.map { it.id })
    }

    @Test
    fun `progress runs between the previous threshold and the next one`() {
        assertEquals(0f, MilestoneEngine.progressToNext(catalogue, 0))
        assertEquals(0.5f, MilestoneEngine.progressToNext(catalogue, 60 * 60))
        assertEquals(1f, MilestoneEngine.progressToNext(catalogue, 10_000 * 60))
    }

    @Test
    fun `next milestone is the cheapest one still locked`() {
        assertEquals("piperita", MilestoneEngine.nextMilestone(catalogue, 0)?.id)
        assertEquals("suaveolens", MilestoneEngine.nextMilestone(catalogue, 120 * 60)?.id)
    }
}

package dev.nheox.mintal.domain

import dev.nheox.mintal.core.model.FlowerForm
import dev.nheox.mintal.core.model.GrowthHabit
import dev.nheox.mintal.core.model.LeafAttachment
import dev.nheox.mintal.core.model.LeafFlush
import dev.nheox.mintal.core.model.LeafHue
import dev.nheox.mintal.core.model.LeafMargin
import dev.nheox.mintal.core.model.LeafShape
import dev.nheox.mintal.core.model.LeafSurface
import dev.nheox.mintal.core.model.Pubescence
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.model.SpeciesArt
import dev.nheox.mintal.core.model.StemHue
import dev.nheox.mintal.domain.milestone.MilestoneEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
            surface = LeafSurface.SMOOTH,
            pubescence = Pubescence.NONE,
            flush = LeafFlush.NONE,
            habit = GrowthHabit.UPRIGHT,
            leafScale = 1f,
            leafHue = LeafHue.LEAF_MID,
            stemHue = StemHue.STEM_GREEN,
            attachment = LeafAttachment.SESSILE,
            stemHairy = false,
            variegated = false,
            flower = FlowerForm.SLENDER_SPIKE,
        ),
    )

    private val catalogue = listOf(
        species("spicata", 0),
        species("piperita", 120),
        species("suaveolens", 300),
    )

    @Test
    fun `the first slot is open before any focus at all`() {
        assertEquals(1, MilestoneEngine.earnedSlots(catalogue, 0))
        assertEquals(1, MilestoneEngine.pendingChoices(catalogue, 0, chosenCount = 0))
    }

    @Test
    fun `a threshold is crossed exactly at its second, not a second before`() {
        assertEquals(1, MilestoneEngine.earnedSlots(catalogue, 120 * 60 - 1))
        assertEquals(2, MilestoneEngine.earnedSlots(catalogue, 120 * 60))
    }

    @Test
    fun `choosing spends a slot, and the slot count is what the time earned`() {
        val total = 300 * 60
        assertEquals(3, MilestoneEngine.earnedSlots(catalogue, total))
        assertEquals(3, MilestoneEngine.pendingChoices(catalogue, total, chosenCount = 0))
        assertEquals(1, MilestoneEngine.pendingChoices(catalogue, total, chosenCount = 2))
        assertEquals(0, MilestoneEngine.pendingChoices(catalogue, total, chosenCount = 3))
    }

    @Test
    fun `nothing is ever taken back when history shrinks`() {
        // A restored backup, or a deleted session, can leave more mints chosen than the
        // remaining time earns. Revoking one would punish the player for a repair.
        assertEquals(0, MilestoneEngine.pendingChoices(catalogue, 0, chosenCount = 3))
    }

    @Test
    fun `the catalogue cannot be over-spent`() {
        val enormous = 10_000 * 60
        assertEquals(
            0,
            MilestoneEngine.pendingChoices(catalogue, enormous, chosenCount = catalogue.size),
        )
    }

    @Test
    fun `the next milestone is the cheapest threshold not yet reached`() {
        assertEquals(120, MilestoneEngine.nextThresholdMinutes(catalogue, 0))
        assertEquals(300, MilestoneEngine.nextThresholdMinutes(catalogue, 120 * 60))
        assertNull(MilestoneEngine.nextThresholdMinutes(catalogue, 300 * 60))
    }

    @Test
    fun `progress runs between the previous threshold and the next one`() {
        assertEquals(0f, MilestoneEngine.progressToNext(catalogue, 0))
        assertEquals(0.5f, MilestoneEngine.progressToNext(catalogue, 60 * 60))
        assertEquals(1f, MilestoneEngine.progressToNext(catalogue, 10_000 * 60))
    }

    @Test
    fun `thresholds are the catalogue's, in order, with no duplicates`() {
        assertEquals(listOf(0, 120, 300), MilestoneEngine.thresholdsMinutes(catalogue.reversed()))
    }
}

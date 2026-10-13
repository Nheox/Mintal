package dev.nheox.mintal.domain.milestone

import dev.nheox.mintal.core.model.Species

/**
 * Unlocks are derived from one number — cumulative successful seconds — and never from a
 * stored counter (spec §7). That is what makes a restored backup recompute correctly and
 * keeps a deleted session from stranding an unlock.
 */
object MilestoneEngine {

    fun isUnlocked(species: Species, totalSuccessfulSeconds: Int): Boolean =
        totalSuccessfulSeconds >= species.unlockMinutes * 60

    fun unlockedIds(catalog: List<Species>, totalSuccessfulSeconds: Int): Set<String> =
        catalog.filter { isUnlocked(it, totalSuccessfulSeconds) }.map { it.id }.toSet()

    /** Species whose threshold is met but which have not been stamped yet. */
    fun newlyUnlocked(
        catalog: List<Species>,
        alreadyStamped: Set<String>,
        totalSuccessfulSeconds: Int,
    ): List<Species> = catalog
        .filter { isUnlocked(it, totalSuccessfulSeconds) && it.id !in alreadyStamped }
        .sortedBy { it.unlockMinutes }

    fun nextMilestone(catalog: List<Species>, totalSuccessfulSeconds: Int): Species? = catalog
        .filterNot { isUnlocked(it, totalSuccessfulSeconds) }
        .minByOrNull { it.unlockMinutes }

    /** 0f..1f progress toward the next milestone, or 1f once the catalogue is complete. */
    fun progressToNext(catalog: List<Species>, totalSuccessfulSeconds: Int): Float {
        val next = nextMilestone(catalog, totalSuccessfulSeconds) ?: return 1f
        val previousThreshold = catalog
            .filter { isUnlocked(it, totalSuccessfulSeconds) }
            .maxOfOrNull { it.unlockMinutes * 60 } ?: 0
        val span = next.unlockMinutes * 60 - previousThreshold
        if (span <= 0) return 1f
        return ((totalSuccessfulSeconds - previousThreshold).toFloat() / span).coerceIn(0f, 1f)
    }
}

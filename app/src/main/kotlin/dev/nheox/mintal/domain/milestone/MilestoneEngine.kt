package dev.nheox.mintal.domain.milestone

import dev.nheox.mintal.core.model.Species

/**
 * Milestones earn a *slot*, not a particular mint (spec §6.1, amended).
 *
 * The thresholds are still the catalogue's — twelve of them, from zero to three hundred
 * hours — and still derived from one number, cumulative successful seconds, so a restored
 * backup recomputes correctly and a deleted session cannot strand an unlock. What changed
 * is the other half: crossing a threshold entitles the player to one more mint, and which
 * mint that is, is theirs to pick. Twelve candidates at the first milestone, eleven at the
 * second, one at the last.
 *
 * So two sources of truth sit side by side, and the distinction is the thing to keep hold
 * of: *how many* mints are unlocked is derived here and never stored; *which* ones is a
 * decision only the player can make, and is the one thing the species table holds.
 */
object MilestoneEngine {

    /** Every milestone threshold in the catalogue, in minutes, ascending. */
    fun thresholdsMinutes(catalog: List<Species>): List<Int> =
        catalog.map { it.unlockMinutes }.distinct().sorted()

    /** How many mints this much successful focus entitles the player to have chosen. */
    fun earnedSlots(catalog: List<Species>, totalSuccessfulSeconds: Int): Int =
        thresholdsMinutes(catalog).count { totalSuccessfulSeconds >= it * 60 }

    /**
     * Choices waiting to be made. Never negative — a backup restored onto a shorter
     * history, or a deleted session, leaves more mints chosen than earned, and taking one
     * away again would be a punishment the player cannot act on.
     */
    fun pendingChoices(
        catalog: List<Species>,
        totalSuccessfulSeconds: Int,
        chosenCount: Int,
    ): Int = (earnedSlots(catalog, totalSuccessfulSeconds) - chosenCount)
        .coerceIn(0, (catalog.size - chosenCount).coerceAtLeast(0))

    /** Minutes at the next threshold the player has not reached, or null when done. */
    fun nextThresholdMinutes(catalog: List<Species>, totalSuccessfulSeconds: Int): Int? =
        thresholdsMinutes(catalog).firstOrNull { totalSuccessfulSeconds < it * 60 }

    /** 0f..1f toward the next milestone, or 1f once every threshold is behind the player. */
    fun progressToNext(catalog: List<Species>, totalSuccessfulSeconds: Int): Float {
        val next = nextThresholdMinutes(catalog, totalSuccessfulSeconds) ?: return 1f
        val previous = thresholdsMinutes(catalog)
            .filter { totalSuccessfulSeconds >= it * 60 }
            .maxOrNull() ?: 0
        val span = (next - previous) * 60
        if (span <= 0) return 1f
        return ((totalSuccessfulSeconds - previous * 60).toFloat() / span).coerceIn(0f, 1f)
    }
}

package dev.nheox.mintal.domain.stats

/**
 * Axis ticks for the statistics charts (spec §5.7).
 *
 * The first version labelled the peak, half the peak and zero, formatted with the same
 * human-duration helper the summary lines use. On real data that reads as nonsense: a
 * day with one fifteen-second test session gave "15 s / 7 s / 0", and a rating breakdown
 * whose tallest bar is one gave "1 / 0 / 0" — a repeated label on a scale that is not
 * linear in anything the reader can see.
 *
 * So ticks are chosen here instead, from a ladder of round steps, and the axis top is the
 * first multiple of that step at or above the data. Two properties matter and both are
 * tested: every label on an axis is distinct, and every label on a duration axis is in
 * the same unit, so "60 min" and "1 h" can never appear on the same scale.
 */
object AxisTicks {

    data class Tick(val value: Int, val label: String)

    data class Axis(val top: Int, val ticks: List<Tick>) {
        /** 0f at the baseline, 1f at the axis top — where a value or a gridline sits. */
        fun fractionOf(value: Int): Float =
            if (top <= 0) 0f else (value.toFloat() / top).coerceIn(0f, 1f)
    }

    private val COUNT_STEPS = intArrayOf(1, 2, 5, 10, 20, 25, 50, 100, 250, 500, 1_000)

    // Round amounts of time rather than round numbers of seconds: a 20-minute gridline is
    // readable, a 1024-second one is not. Every step at or above an hour is a whole number
    // of hours, and every step below it a whole number of minutes or seconds, which is
    // what keeps one unit per axis honest.
    private val DURATION_STEPS = intArrayOf(
        5, 10, 15, 30,
        60, 120, 300, 600, 900, 1_800,
        3_600, 7_200, 10_800, 21_600, 43_200, 86_400,
    )

    /** A whole-number axis: session counts, rating tallies. */
    fun counts(maxValue: Int, maxTicks: Int = 4): Axis {
        val step = chooseStep(maxValue, maxTicks, COUNT_STEPS)
        return axis(maxValue, step) { it.toString() }
    }

    /** A duration axis, labelled in one unit chosen from the step. */
    fun durations(maxSeconds: Int, maxTicks: Int = 4): Axis {
        val step = chooseStep(maxSeconds, maxTicks, DURATION_STEPS)
        val unit = unitFor(step)
        return axis(maxSeconds, step) { seconds -> format(seconds, unit) }
    }

    private enum class Unit { SECONDS, MINUTES, HOURS }

    private fun unitFor(step: Int): Unit = when {
        step % 3_600 == 0 -> Unit.HOURS
        step % 60 == 0 -> Unit.MINUTES
        else -> Unit.SECONDS
    }

    private fun format(seconds: Int, unit: Unit): String {
        if (seconds == 0) return "0"
        return when (unit) {
            Unit.SECONDS -> "$seconds s"
            Unit.MINUTES -> "${seconds / 60} min"
            Unit.HOURS -> "${seconds / 3_600} h"
        }
    }

    /**
     * The smallest ladder step that fits the data into [maxTicks] intervals. Falling off
     * the end of the ladder means growing the largest step instead of giving up on it, so
     * an absurd total still produces a sane number of gridlines.
     */
    private fun chooseStep(maxValue: Int, maxTicks: Int, ladder: IntArray): Int {
        val intervals = maxTicks.coerceAtLeast(1)
        val target = maxValue.coerceAtLeast(1)
        ladder.firstOrNull { step -> ceilDiv(target, step) <= intervals }?.let { return it }
        val largest = ladder.last()
        return largest * ceilDiv(ceilDiv(target, intervals), largest)
    }

    private fun axis(maxValue: Int, step: Int, label: (Int) -> String): Axis {
        val top = (step * ceilDiv(maxValue.coerceAtLeast(1), step)).coerceAtLeast(step)
        val ticks = (0..top step step).map { Tick(it, label(it)) }
        return Axis(top = top, ticks = ticks)
    }

    private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor
}

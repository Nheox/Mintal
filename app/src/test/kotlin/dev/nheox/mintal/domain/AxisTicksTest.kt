package dev.nheox.mintal.domain

import dev.nheox.mintal.domain.stats.AxisTicks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The two device screenshots that prompted this: a focus axis reading "15 s / 7 s / 0"
 * and a rating axis reading "1 / 0 / 0". Both are label collisions, so that is what these
 * assert — on every plausible axis top, not just the two that were reported.
 */
class AxisTicksTest {

    @Test
    fun `a one-session rating tally still gets two distinct labels`() {
        val axis = AxisTicks.counts(1)
        assertEquals(listOf("0", "1"), axis.ticks.map { it.label })
    }

    @Test
    fun `an empty chart keeps a scale rather than collapsing to zero`() {
        val axis = AxisTicks.counts(0)
        assertTrue(axis.top > 0, "an axis needs a top above zero to divide by")
        assertEquals(axis.ticks.map { it.label }.distinct(), axis.ticks.map { it.label })
    }

    @Test
    fun `fifteen seconds of focus is labelled in seconds`() {
        val axis = AxisTicks.durations(15)
        assertEquals(15, axis.top)
        assertEquals(listOf("0", "5 s", "10 s", "15 s"), axis.ticks.map { it.label })
    }

    @Test
    fun `half an hour is labelled in minutes, not seconds`() {
        val axis = AxisTicks.durations(25 * 60)
        assertTrue(axis.top >= 25 * 60, "the axis must reach the data")
        assertTrue(
            axis.ticks.drop(1).all { it.label.endsWith(" min") },
            "expected minutes, got ${axis.ticks.map { it.label }}",
        )
    }

    @Test
    fun `a long day is labelled in hours, not thousands of minutes`() {
        val axis = AxisTicks.durations(6 * 3_600)
        assertTrue(
            axis.ticks.drop(1).all { it.label.endsWith(" h") },
            "expected hours, got ${axis.ticks.map { it.label }}",
        )
    }

    @Test
    fun `no duration axis ever repeats a label or mixes units`() {
        val probes = buildList {
            addAll(0..200)
            addAll((0..48).map { it * 900 })
            addAll((0..30).map { it * 3_600 })
            add(86_399)
            add(500_000)
        }
        for (maxSeconds in probes) {
            val axis = AxisTicks.durations(maxSeconds)
            val labels = axis.ticks.map { it.label }
            assertEquals(labels.distinct(), labels, "$maxSeconds s repeated a label: $labels")
            assertTrue(axis.top >= maxSeconds, "$maxSeconds s fell short of the data")
            assertTrue(axis.ticks.size in 2..6, "$maxSeconds s produced $labels")
            val units = labels.drop(1).map { it.substringAfter(' ') }.distinct()
            assertEquals(1, units.size, "$maxSeconds s mixed units: $labels")
        }
    }

    @Test
    fun `no count axis ever repeats a label`() {
        for (maxValue in 0..500) {
            val axis = AxisTicks.counts(maxValue)
            val labels = axis.ticks.map { it.label }
            assertEquals(labels.distinct(), labels, "$maxValue repeated a label: $labels")
            assertTrue(axis.top >= maxValue, "$maxValue fell short of the data")
            assertTrue(axis.ticks.size in 2..6, "$maxValue produced $labels")
        }
    }

    @Test
    fun `a bar is drawn against the axis top, not the tallest bar`() {
        val axis = AxisTicks.durations(25 * 60)
        assertEquals(0f, axis.fractionOf(0))
        assertEquals(1f, axis.fractionOf(axis.top))
        assertTrue(
            axis.fractionOf(25 * 60) <= 1f,
            "the tallest bar should stop below a top the axis rounded up to",
        )
    }
}

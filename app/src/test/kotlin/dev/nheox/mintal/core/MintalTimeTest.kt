package dev.nheox.mintal.core

import dev.nheox.mintal.core.time.MintalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId

class MintalTimeTest {

    private val utc = ZoneId.of("UTC")

    @Test
    fun `clock formats minutes and seconds, and hours only when there are hours`() {
        assertEquals("00:30", MintalTime.formatClock(30))
        assertEquals("25:00", MintalTime.formatClock(25 * 60))
        assertEquals("1:05:00", MintalTime.formatClock(3900))
        assertEquals("00:00", MintalTime.formatClock(-5))
    }

    @Test
    fun `durations read as a human would say them`() {
        assertEquals("40 s", MintalTime.formatDuration(40))
        assertEquals("25 min", MintalTime.formatDuration(25 * 60))
        assertEquals("1 h", MintalTime.formatDuration(3600))
        assertEquals("1 h 25 min", MintalTime.formatDuration(5100))
    }

    @Test
    fun `a late night session belongs to the previous day when the day starts at three`() {
        // 1970-01-02T02:00Z, with a 03:00 day start, is still day 0.
        val twoInTheMorning = 26L * 3600L * 1000L
        assertEquals(1L, MintalTime.epochDayOf(twoInTheMorning, utc, dayStartMinutes = 0))
        assertEquals(0L, MintalTime.epochDayOf(twoInTheMorning, utc, dayStartMinutes = 180))
    }

    @Test
    fun `start of day round-trips with the day start offset`() {
        val dayStart = 180
        val start = MintalTime.startOfDayMillis(10L, utc, dayStart)
        assertEquals(10L, MintalTime.epochDayOf(start, utc, dayStart))
        assertEquals(9L, MintalTime.epochDayOf(start - 1, utc, dayStart))
    }
}

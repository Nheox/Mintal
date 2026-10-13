package dev.nheox.mintal.domain

import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.domain.stats.StatsCalculator
import dev.nheox.mintal.domain.stats.StatsRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId

class StatsCalculatorTest {

    private val utc = ZoneId.of("UTC")
    private val day = 20_000L

    private fun session(
        hour: Int,
        seconds: Int,
        outcome: SessionOutcome = SessionOutcome.SUCCESS,
        rating: Int? = null,
        tagId: Long? = null,
        epochDay: Long = day,
    ) = SessionEntity(
        id = (epochDay * 100 + hour),
        startedAt = MintalTime.startOfDayMillis(epochDay, utc, 0) + hour * 3_600_000L,
        endedAt = MintalTime.startOfDayMillis(epochDay, utc, 0) + hour * 3_600_000L + seconds * 1000L,
        plannedSeconds = seconds,
        actualSeconds = seconds,
        mode = SessionMode.COUNTDOWN,
        outcome = outcome,
        speciesId = "spicata",
        tagId = tagId,
        rating = rating,
        note = null,
        timezoneId = "UTC",
    )

    @Test
    fun `an empty range reports itself as empty rather than as zeroes`() {
        val result = StatsCalculator.compute(emptyList(), StatsRange.DAY, day, 0, 1, utc)
        assertTrue(result.isEmpty)
        assertNull(result.averageRating)
    }

    @Test
    fun `totals, success rate and longest session come from the sessions in range`() {
        val sessions = listOf(
            session(hour = 9, seconds = 1500, rating = 4),
            session(hour = 11, seconds = 3000, rating = 2),
            session(hour = 13, seconds = 600, outcome = SessionOutcome.GAVE_UP),
            // Out of range: the day before.
            session(hour = 9, seconds = 9000, epochDay = day - 1),
        )
        val result = StatsCalculator.compute(sessions, StatsRange.DAY, day, 0, 1, utc)

        assertEquals(3, result.sessionCount)
        assertEquals(5100, result.totalFocusSeconds)
        assertEquals(2, result.successCount)
        assertEquals(3000, result.longestSessionSeconds)
        assertEquals(1700, result.averageSessionSeconds)
        assertEquals(3f, result.averageRating)
        assertEquals(11, result.bestHour)
    }

    @Test
    fun `a failed session still counts toward statistics`() {
        val sessions = listOf(session(hour = 8, seconds = 300, outcome = SessionOutcome.LEFT_APP))
        val result = StatsCalculator.compute(sessions, StatsRange.DAY, day, 0, 1, utc)
        assertEquals(300, result.totalFocusSeconds)
        assertEquals(0, result.successCount)
        assertEquals(0f, result.successRate)
    }

    @Test
    fun `an unrated session is absent from the distribution, never counted as zero`() {
        val sessions = listOf(
            session(hour = 9, seconds = 600, rating = 5),
            session(hour = 10, seconds = 600, rating = null),
        )
        val result = StatsCalculator.compute(sessions, StatsRange.DAY, day, 0, 1, utc)
        assertEquals(listOf(0, 0, 0, 0, 1), result.ratingDistribution)
        assertEquals(1, result.ratingTrend.size)
        assertEquals(5f, result.averageRating)
    }

    @Test
    fun `a day range buckets by hour and a week range by day`() {
        val dayResult = StatsCalculator.compute(
            listOf(session(hour = 7, seconds = 1200)),
            StatsRange.DAY,
            day,
            0,
            1,
            utc,
        )
        assertEquals(24, dayResult.buckets.size)
        assertEquals(1200, dayResult.buckets[7].seconds)

        val weekResult = StatsCalculator.compute(
            listOf(session(hour = 7, seconds = 1200)),
            StatsRange.WEEK,
            day,
            0,
            1,
            utc,
        )
        assertEquals(7, weekResult.buckets.size)
        assertEquals(1200, weekResult.buckets.sumOf { it.seconds })
    }

    @Test
    fun `tag breakdown averages ratings per tag`() {
        val sessions = listOf(
            session(hour = 9, seconds = 600, rating = 4, tagId = 1L),
            session(hour = 10, seconds = 1200, rating = 2, tagId = 1L),
            session(hour = 11, seconds = 300, tagId = null),
        )
        val result = StatsCalculator.compute(sessions, StatsRange.DAY, day, 0, 1, utc)
        val tagged = result.tagRows.first { it.tagId == 1L }
        assertEquals(1800, tagged.seconds)
        assertEquals(3f, tagged.averageRating)
        assertNull(result.tagRows.first { it.tagId == null }.averageRating)
    }
}

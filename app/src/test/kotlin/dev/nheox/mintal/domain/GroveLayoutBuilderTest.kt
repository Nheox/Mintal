package dev.nheox.mintal.domain

import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.domain.grove.GroveLayoutBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId

class GroveLayoutBuilderTest {

    private val utc = ZoneId.of("UTC")

    private fun session(
        id: Long,
        epochDay: Long,
        seconds: Int = 1500,
        outcome: SessionOutcome = SessionOutcome.SUCCESS,
    ) = SessionEntity(
        id = id,
        startedAt = MintalTime.startOfDayMillis(epochDay, utc, 0) + id * 60_000L,
        endedAt = MintalTime.startOfDayMillis(epochDay, utc, 0) + id * 60_000L + seconds * 1000L,
        plannedSeconds = seconds,
        actualSeconds = seconds,
        mode = SessionMode.COUNTDOWN,
        outcome = outcome,
        speciesId = "spicata",
        tagId = null,
        rating = null,
        note = null,
        timezoneId = "UTC",
    )

    @Test
    fun `an empty history is an empty grove, not a crash`() {
        val layout = GroveLayoutBuilder.build(emptyList(), 0, utc)
        assertTrue(layout.isEmpty)
        assertTrue(layout.anchors.isEmpty())
    }

    @Test
    fun `every session becomes one segment and every day one anchor`() {
        val sessions = listOf(
            session(1, 100),
            session(2, 100),
            session(3, 101),
        )
        val layout = GroveLayoutBuilder.build(sessions, 0, utc)
        assertEquals(3, layout.segments.size)
        assertEquals(2, layout.anchors.size)
        assertEquals(listOf(100L, 101L), layout.anchors.map { it.epochDay })
        assertEquals(3000, layout.anchors.first { it.epochDay == 100L }.seconds)
    }

    @Test
    fun `the same history always lays out identically`() {
        val sessions = (1L..12L).map { session(it, 100 + it / 4) }
        val first = GroveLayoutBuilder.build(sessions, 0, utc)
        val second = GroveLayoutBuilder.build(sessions.reversed(), 0, utc)
        assertEquals(first.segments.map { it.sessionId }, second.segments.map { it.sessionId })
        assertEquals(first.segments.map { it.endX }, second.segments.map { it.endX })
        assertEquals(first.segments.map { it.endY }, second.segments.map { it.endY })
    }

    @Test
    fun `growth is continuous - each segment starts where an earlier one ended`() {
        val sessions = (1L..8L).map { session(it, 100) }
        val layout = GroveLayoutBuilder.build(sessions, 0, utc)
        val tips = HashSet<Pair<Float, Float>>()
        tips += 0f to 0f
        for (segment in layout.segments) {
            assertTrue(
                (segment.startX to segment.startY) in tips,
                "segment ${segment.sessionId} is detached from the network",
            )
            tips += segment.endX to segment.endY
        }
    }

    @Test
    fun `a failed session is marked wilted and a longer session grows further`() {
        val layout = GroveLayoutBuilder.build(
            listOf(
                session(1, 100, seconds = 300),
                session(2, 100, seconds = 7200, outcome = SessionOutcome.GAVE_UP),
            ),
            0,
            utc,
        )
        assertTrue(layout.segments[1].wilted)
        val shortLength = length(layout.segments[0])
        val longLength = length(layout.segments[1])
        assertTrue(longLength > shortLength, "a longer session should lay down more runner")
    }

    private fun length(segment: dev.nheox.mintal.domain.grove.GroveSegment): Float {
        val dx = segment.endX - segment.startX
        val dy = segment.endY - segment.startY
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

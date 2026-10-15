package dev.nheox.mintal.domain

import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.domain.grove.FailureDisplay
import dev.nheox.mintal.domain.grove.GroveLayoutBuilder
import dev.nheox.mintal.domain.grove.PlantBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
        speciesId: String = "spicata",
    ) = SessionEntity(
        id = id,
        startedAt = MintalTime.startOfDayMillis(epochDay, utc, 0) + id * 60_000L,
        endedAt = MintalTime.startOfDayMillis(epochDay, utc, 0) + id * 60_000L + seconds * 1000L,
        plannedSeconds = seconds,
        actualSeconds = seconds,
        mode = SessionMode.COUNTDOWN,
        outcome = outcome,
        speciesId = speciesId,
        tagId = null,
        rating = null,
        note = null,
        timezoneId = "UTC",
    )

    @Test
    fun `an empty history is an empty grove, not a crash`() {
        val layout = GroveLayoutBuilder.build(emptyList(), emptyMap(), 0, utc)
        assertTrue(layout.isEmpty)
        assertTrue(layout.anchors.isEmpty())
    }

    @Test
    fun `sessions of one species pile onto one plant until it is grown`() {
        val sessions = (1L..PlantBuilder.MATURE_NODES.toLong()).map { session(it, 100) }
        val layout = GroveLayoutBuilder.build(sessions, emptyMap(), 0, utc)

        assertEquals(1, layout.plantCount)
        assertEquals(PlantBuilder.MATURE_NODES, layout.plantings.single().plant.nodeCount)
        assertTrue(layout.plantings.single().plant.isMature)
    }

    @Test
    fun `the session after maturity starts a second plant rather than a taller one`() {
        val sessions = (1L..(PlantBuilder.MATURE_NODES + 1).toLong()).map { session(it, 100) }
        val layout = GroveLayoutBuilder.build(sessions, emptyMap(), 0, utc)

        assertEquals(2, layout.plantCount)
        assertEquals(
            listOf(PlantBuilder.MATURE_NODES, 1),
            layout.plantings.map { it.plant.nodeCount },
        )
        assertTrue(
            layout.plantings[1].baseX > layout.plantings[0].baseX,
            "the new plant should stand beside the old one, not inside it",
        )
    }

    @Test
    fun `each species gets its own plants`() {
        val layout = GroveLayoutBuilder.build(
            listOf(
                session(1, 100, speciesId = "spicata"),
                session(2, 100, speciesId = "piperita"),
                session(3, 100, speciesId = "spicata"),
            ),
            emptyMap(),
            0,
            utc,
        )
        assertEquals(2, layout.plantCount)
        assertEquals(
            mapOf("spicata" to 2, "piperita" to 1),
            layout.plantings.associate { it.speciesId to it.plant.nodeCount },
        )
    }

    @Test
    fun `a plant never moves sideways as it grows`() {
        val early = GroveLayoutBuilder.build((1L..3L).map { session(it, 100) }, emptyMap(), 0, utc)
        val later = GroveLayoutBuilder.build((1L..9L).map { session(it, 100) }, emptyMap(), 0, utc)
        assertEquals(early.plantings.single().baseX, later.plantings.single().baseX)
    }

    @Test
    fun `the same history always lays out identically`() {
        val sessions = (1L..14L).map { session(it, 100 + it / 4) }
        val first = GroveLayoutBuilder.build(sessions, emptyMap(), 0, utc)
        val second = GroveLayoutBuilder.build(sessions.reversed(), emptyMap(), 0, utc)
        assertEquals(
            first.plantings.map { it.baseX to it.plant.nodes },
            second.plantings.map { it.baseX to it.plant.nodes },
        )
    }

    @Test
    fun `a failed session leaves a wilted stub and grows nothing, when stubs are shown`() {
        val layout = GroveLayoutBuilder.build(
            listOf(
                session(1, 100),
                session(2, 100, outcome = SessionOutcome.GAVE_UP),
                session(3, 100, outcome = SessionOutcome.LEFT_APP),
            ),
            emptyMap(),
            0,
            utc,
            FailureDisplay.Fading(todayEpochDay = 100),
        )
        assertEquals(1, layout.plantings.single().plant.nodeCount)
        assertEquals(listOf(2L, 3L), layout.stubs.map { it.sessionId })
        assertTrue(layout.stubs.all { it.fade == 1f }, "a stub from today is at full strength")
    }

    @Test
    fun `failures are not drawn by default, and take no room in the bed`() {
        val sessions = listOf(
            session(1, 100),
            session(2, 100, outcome = SessionOutcome.GAVE_UP),
            session(3, 100),
        )
        val hidden = GroveLayoutBuilder.build(sessions, emptyMap(), 0, utc)
        val successesOnly = GroveLayoutBuilder.build(
            sessions.filter { it.outcome == SessionOutcome.SUCCESS },
            emptyMap(),
            0,
            utc,
        )

        assertTrue(hidden.stubs.isEmpty())
        // The failure must not leave a gap where its stub would have stood: the bed has
        // to be identical to one that never contained it.
        assertEquals(
            successesOnly.plantings.map { it.baseX },
            hidden.plantings.map { it.baseX },
        )
    }

    @Test
    fun `a hidden failure still leaves its day reachable`() {
        val layout = GroveLayoutBuilder.build(
            listOf(session(1, 100), session(2, 101, outcome = SessionOutcome.LEFT_APP)),
            emptyMap(),
            0,
            utc,
        )
        assertTrue(layout.stubs.isEmpty())
        // A day on which nothing grew is still a day you can open from the Grove.
        assertEquals(listOf(100L, 101L), layout.anchors.map { it.epochDay })
    }

    @Test
    fun `a stub fades with age and is gone once the window passes`() {
        val days = FailureDisplay.DEFAULT_FADE_DAYS
        fun stubFadeAfter(age: Int): Float? = GroveLayoutBuilder.build(
            listOf(session(1, 100, outcome = SessionOutcome.GAVE_UP)),
            emptyMap(),
            0,
            utc,
            FailureDisplay.Fading(todayEpochDay = 100L + age),
        ).stubs.singleOrNull()?.fade

        assertEquals(1f, stubFadeAfter(0), "the day it happened, a stub is at full strength")
        assertTrue(
            stubFadeAfter(days / 2)!! < 0.6f,
            "half way through the window it should be most of the way gone",
        )
        assertNull(stubFadeAfter(days), "past the window there is no stub at all")
        assertNull(stubFadeAfter(days * 3))
    }

    @Test
    fun `every day gets one anchor, counting failures too`() {
        val layout = GroveLayoutBuilder.build(
            listOf(
                session(1, 100),
                session(2, 100, outcome = SessionOutcome.GAVE_UP),
                session(3, 101),
            ),
            emptyMap(),
            0,
            utc,
        )
        assertEquals(listOf(100L, 101L), layout.anchors.map { it.epochDay })
        assertEquals(2, layout.anchors.first { it.epochDay == 100L }.sessionCount)
        assertEquals(3000, layout.anchors.first { it.epochDay == 100L }.seconds)
    }

    @Test
    fun `the bed has a floor to stand on`() {
        val layout = GroveLayoutBuilder.build((1L..6L).map { session(it, 100) }, emptyMap(), 0, utc)
        assertTrue(layout.bounds.maxY >= 0f, "the ground line must be inside the bounds")
        assertTrue(layout.bounds.minY < 0f, "plants grow upward from it")
    }
}

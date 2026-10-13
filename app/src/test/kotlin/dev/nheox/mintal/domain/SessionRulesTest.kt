package dev.nheox.mintal.domain

import dev.nheox.mintal.domain.session.SessionRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionRulesTest {

    @Test
    fun `durations snap to five minute steps inside the allowed range`() {
        assertEquals(25, SessionRules.clampMinutes(25))
        assertEquals(25, SessionRules.clampMinutes(27))
        assertEquals(5, SessionRules.clampMinutes(1))
        assertEquals(5, SessionRules.clampMinutes(-10))
        assertEquals(180, SessionRules.clampMinutes(240))
        assertEquals(30, SessionRules.clampMinutes(28))
    }

    @Test
    fun `the choices run from five minutes to three hours`() {
        assertEquals(5, SessionRules.durationChoices.first())
        assertEquals(180, SessionRules.durationChoices.last())
        assertTrue(SessionRules.durationChoices.all { it % 5 == 0 })
    }

    @Test
    fun `the documented defaults are the ones in the spec`() {
        assertEquals(25, SessionRules.DEFAULT_MINUTES)
        assertEquals(15, SessionRules.GRACE_SECONDS)
        assertEquals(60, SessionRules.COUNT_UP_MIN_SECONDS)
        assertEquals(200, SessionRules.NOTE_MAX_LENGTH)
        assertEquals(1..5, SessionRules.RATING_RANGE)
    }
}

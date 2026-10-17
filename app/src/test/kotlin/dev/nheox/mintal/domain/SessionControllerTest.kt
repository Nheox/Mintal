package dev.nheox.mintal.domain

import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.domain.session.SessionController
import dev.nheox.mintal.domain.session.SessionResult
import dev.nheox.mintal.domain.session.SessionRules
import dev.nheox.mintal.domain.session.SessionStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The state machine a mistake would cost real focus time in: a session failed a second
 * early, or a finished block never written down. Time is injected, so these run instantly
 * and deterministically.
 */
class SessionControllerTest {

    private class FakeClock : MintalClock {
        var wall = 1_760_000_000_000L
        var elapsed = 5_000L

        override fun nowMillis(): Long = wall

        override fun elapsedRealtimeMillis(): Long = elapsed

        fun advanceSeconds(seconds: Long) {
            elapsed += seconds * 1000L
            wall += seconds * 1000L
        }
    }

    private class FakeStore : SessionStore {
        val recorded = mutableListOf<SessionEntity>()
        var unlockQueries = 0
        var pendingChoices = 0
        var onRecord: (() -> Unit)? = null

        override suspend fun record(session: SessionEntity): Long {
            onRecord?.invoke()
            recorded += session
            return recorded.size.toLong()
        }

        override suspend fun pendingUnlockChoices(): Int {
            unlockQueries++
            return pendingChoices
        }
    }

    private val clock = FakeClock()
    private val store = FakeStore()
    private val controller = SessionController(store, clock)

    @Test
    fun `a countdown completes at its planned time, not a tick before`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)

        clock.advanceSeconds(25 * 60 - 1L)
        controller.tick()
        assertNotNull(controller.running.value, "one second short should still be running")
        assertTrue(store.recorded.isEmpty())

        clock.advanceSeconds(1)
        controller.tick()
        assertNull(controller.running.value)

        val row = store.recorded.single()
        assertEquals(SessionOutcome.SUCCESS, row.outcome)
        assertEquals(25 * 60, row.actualSeconds)
        assertEquals(25 * 60, row.plannedSeconds)
        assertEquals(1, store.unlockQueries)
    }

    @Test
    fun `leaving the app fails the session only once the grace period runs out`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        clock.advanceSeconds(300)
        controller.setDistracted(true)
        controller.tick()
        assertEquals(SessionRules.GRACE_SECONDS, controller.running.value?.graceRemainingSeconds)

        clock.advanceSeconds(SessionRules.GRACE_SECONDS - 1L)
        controller.tick()
        assertNotNull(controller.running.value, "still inside the grace period")
        assertEquals(1, controller.running.value?.graceRemainingSeconds)

        clock.advanceSeconds(1)
        controller.tick()
        assertNull(controller.running.value)

        val row = store.recorded.single()
        assertEquals(SessionOutcome.LEFT_APP, row.outcome)
        assertEquals(315, row.actualSeconds, "partial time is kept")
        assertEquals(
            0,
            (controller.result.value as? SessionResult.Recorded)?.pendingUnlockChoices,
            "a failed session adds no successful time, so it opens no slot",
        )
    }

    @Test
    fun `coming back in time keeps the session alive`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        clock.advanceSeconds(60)
        controller.setDistracted(true)
        clock.advanceSeconds(10)
        controller.tick()
        assertEquals(5, controller.running.value?.graceRemainingSeconds)

        controller.setDistracted(false)
        clock.advanceSeconds(600)
        controller.tick()

        val running = controller.running.value
        assertNotNull(running)
        assertNull(running?.graceRemainingSeconds, "the warning is cleared on return")
        assertTrue(store.recorded.isEmpty())
    }

    @Test
    fun `the grace period restarts rather than resuming on a second departure`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        controller.setDistracted(true)
        clock.advanceSeconds(10)
        controller.tick()
        controller.setDistracted(false)
        controller.setDistracted(true)
        clock.advanceSeconds(10)
        controller.tick()

        assertNotNull(controller.running.value, "the first ten seconds must not carry over")
        assertEquals(5, controller.running.value?.graceRemainingSeconds)
    }

    @Test
    fun `a count-up session under a minute is discarded entirely`() = runTest {
        controller.start(SessionMode.COUNT_UP, 0, "spicata", null)
        clock.advanceSeconds(45)
        controller.stopCountUp()

        assertNull(controller.running.value)
        assertTrue(store.recorded.isEmpty(), "nothing under the minimum is written down")
        assertEquals(SessionResult.Discarded, controller.result.value)
    }

    @Test
    fun `stopping a count-up past the minimum records what was earned`() = runTest {
        controller.start(SessionMode.COUNT_UP, 0, "spicata", 7L)
        clock.advanceSeconds(90)
        controller.stopCountUp()

        val row = store.recorded.single()
        assertEquals(SessionOutcome.SUCCESS, row.outcome)
        assertEquals(90, row.actualSeconds)
        assertEquals(0, row.plannedSeconds, "a count-up session has nothing planned")
        assertEquals(7L, row.tagId)
        assertEquals(1, store.unlockQueries)
    }

    @Test
    fun `giving up keeps the partial time and earns no species`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "piperita", null)
        clock.advanceSeconds(420)
        controller.giveUp()

        val row = store.recorded.single()
        assertEquals(SessionOutcome.GAVE_UP, row.outcome)
        assertEquals(420, row.actualSeconds)
        assertEquals("piperita", row.speciesId, "intent is kept even though the Grove wilts it")
        assertEquals(
            0,
            (controller.result.value as? SessionResult.Recorded)?.pendingUnlockChoices,
        )
    }

    @Test
    fun `a milestone is reported as a choice, never taken on the player's behalf`() = runTest {
        store.pendingChoices = 1
        controller.start(SessionMode.COUNTDOWN, 5 * 60, "spicata", null)
        clock.advanceSeconds(5 * 60L)
        controller.tick()

        val result = controller.result.value as? SessionResult.Recorded
        assertEquals(1, result?.pendingUnlockChoices)
        assertEquals(
            1,
            store.unlockQueries,
            "asked once, after the row is written, so this session's time counts",
        )
    }

    @Test
    fun `the result carries the row id the result screen then edits`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 5 * 60, "spicata", null)
        clock.advanceSeconds(5 * 60L)
        controller.tick()

        val result = controller.result.value as? SessionResult.Recorded
        assertNotNull(result)
        assertEquals(1L, result?.sessionId)
        assertEquals(SessionOutcome.SUCCESS, result?.outcome)
        assertEquals(5 * 60, result?.actualSeconds)

        controller.clearResult()
        assertNull(controller.result.value)
    }

    @Test
    fun `the running session outlives the write, so the UI never sees neither`() = runTest {
        // Clearing the running session before the row is written leaves a window where the
        // screen sees no session and no result, reads it as abandoned, and skips the
        // rating prompt entirely. This is that bug, pinned down.
        var sawSessionDuringWrite = false
        store.onRecord = { sawSessionDuringWrite = controller.running.value != null }

        controller.start(SessionMode.COUNTDOWN, 5 * 60, "spicata", null)
        clock.advanceSeconds(5 * 60L)
        controller.tick()

        assertTrue(sawSessionDuringWrite, "the session was cleared before the result existed")
        assertNull(controller.running.value)
        assertTrue(controller.result.value is SessionResult.Recorded)
    }

    @Test
    fun `starting again while a session runs changes nothing`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        controller.start(SessionMode.COUNTDOWN, 5 * 60, "piperita", 3L)

        val running = controller.running.value
        assertEquals(25 * 60, running?.plannedSeconds)
        assertEquals("spicata", running?.speciesId)
        assertNull(running?.tagId)
    }

    @Test
    fun `a dark screen is not the controller's business - no distraction, no grace`() = runTest {
        // The service decides what counts as away; the controller only acts on the verdict.
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        clock.advanceSeconds(3600)
        controller.setDistracted(false)
        controller.tick()

        assertNull(controller.running.value, "an hour past a 25 minute countdown is complete")
        assertEquals(SessionOutcome.SUCCESS, store.recorded.single().outcome)
        assertEquals(25 * 60, store.recorded.single().actualSeconds)
    }

    @Test
    fun `a pause stops the clock, so the countdown ends later rather than early`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        clock.advanceSeconds(300)
        controller.pause()

        clock.advanceSeconds(SessionRules.PAUSE_SECONDS.toLong())
        controller.tick()
        assertNotNull(controller.running.value, "the pause ending must not end the session")
        assertEquals(300, controller.running.value?.elapsedSeconds, "paused time is not focus time")

        // The two paused minutes are owed back: the session still needs its full 25.
        clock.advanceSeconds(25 * 60 - 300L)
        controller.tick()
        assertNull(controller.running.value)
        assertEquals(25 * 60, store.recorded.single().actualSeconds)
    }

    @Test
    fun `leaving the app while paused never starts the grace period`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        clock.advanceSeconds(300)
        controller.pause()

        controller.setDistracted(true)
        clock.advanceSeconds(60)
        controller.tick()

        assertNotNull(controller.running.value, "a pause is for leaving the app")
        assertNull(controller.running.value?.graceRemainingSeconds)
        assertTrue(store.recorded.isEmpty())
    }

    @Test
    fun `the pause counts down and ends by itself`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        controller.pause()
        assertEquals(SessionRules.PAUSE_SECONDS, controller.running.value?.pauseRemainingSeconds)

        clock.advanceSeconds(SessionRules.PAUSE_SECONDS - 1L)
        controller.tick()
        assertEquals(1, controller.running.value?.pauseRemainingSeconds)

        clock.advanceSeconds(1)
        controller.tick()
        assertNull(controller.running.value?.pauseRemainingSeconds, "the pause is over")
        assertEquals(false, controller.running.value?.isPaused)
    }

    @Test
    fun `resuming early only gives back the time actually paused`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        clock.advanceSeconds(300)
        controller.pause()
        clock.advanceSeconds(30)
        controller.resume()
        controller.tick()

        assertEquals(300, controller.running.value?.elapsedSeconds)
        assertNull(controller.running.value?.pauseRemainingSeconds)
    }

    @Test
    fun `the next pause waits thirty seconds after the last one ended`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        controller.pause()
        clock.advanceSeconds(20)
        controller.resume()
        assertEquals(SessionRules.PAUSE_COOLDOWN_SECONDS, controller.running.value?.pauseCooldownSeconds)
        assertEquals(false, controller.running.value?.canPause)

        controller.pause()
        assertEquals(false, controller.running.value?.isPaused, "refused during the cooldown")

        clock.advanceSeconds(SessionRules.PAUSE_COOLDOWN_SECONDS - 1L)
        controller.tick()
        assertEquals(1, controller.running.value?.pauseCooldownSeconds)

        clock.advanceSeconds(1)
        controller.tick()
        assertNull(controller.running.value?.pauseCooldownSeconds)
        assertEquals(true, controller.running.value?.canPause)

        controller.pause()
        assertEquals(true, controller.running.value?.isPaused, "available again")
    }

    @Test
    fun `a pause that runs out by itself starts the cooldown too`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        controller.pause()
        clock.advanceSeconds(SessionRules.PAUSE_SECONDS.toLong())
        controller.tick()

        assertEquals(false, controller.running.value?.isPaused)
        assertEquals(SessionRules.PAUSE_COOLDOWN_SECONDS, controller.running.value?.pauseCooldownSeconds)
    }

    @Test
    fun `a fresh session can pause straight away`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        controller.pause()
        controller.resume()
        controller.giveUp()

        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
        assertEquals(true, controller.running.value?.canPause, "a cooldown does not outlive its session")
    }

    @Test
    fun `a pause expiring while still away gives a full grace period, not an instant failure`() =
        runTest {
            controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", null)
            clock.advanceSeconds(300)
            controller.pause()
            controller.setDistracted(true)

            clock.advanceSeconds(SessionRules.PAUSE_SECONDS.toLong())
            controller.tick()
            assertNotNull(controller.running.value, "the pause running out must not fail it")

            // The service reports the user is still away on the very next tick.
            controller.setDistracted(true)
            controller.tick()
            assertEquals(SessionRules.GRACE_SECONDS, controller.running.value?.graceRemainingSeconds)

            clock.advanceSeconds(SessionRules.GRACE_SECONDS.toLong())
            controller.tick()
            assertNull(controller.running.value)
            assertEquals(SessionOutcome.LEFT_APP, store.recorded.single().outcome)
        }

    @Test
    fun `the tag can change mid-session, and the row carries the new one`() = runTest {
        controller.start(SessionMode.COUNTDOWN, 25 * 60, "spicata", tagId = 1L)
        clock.advanceSeconds(600)
        controller.tick()
        controller.setTag(2L)
        controller.tick()

        val running = controller.running.value
        assertEquals(2L, running?.tagId)
        assertEquals(600, running?.elapsedSeconds, "a label moves no clock")
        assertEquals(25 * 60, running?.plannedSeconds)
        assertEquals("spicata", running?.speciesId)

        clock.advanceSeconds(25 * 60 - 600L)
        controller.tick()
        assertEquals(2L, store.recorded.single().tagId)
    }

    @Test
    fun `a session started tagged can end untagged`() = runTest {
        controller.start(SessionMode.COUNT_UP, 0, "spicata", tagId = 1L)
        clock.advanceSeconds(120)
        controller.setTag(null)
        controller.stopCountUp()

        assertNull(store.recorded.single().tagId)
    }

    @Test
    fun `giving up during a pause records only the focused time`() = runTest {
        controller.start(SessionMode.COUNT_UP, 0, "spicata", null)
        clock.advanceSeconds(600)
        controller.pause()
        clock.advanceSeconds(90)
        controller.giveUp()

        assertEquals(600, store.recorded.single().actualSeconds, "the 90 paused seconds are not focus")
    }
}

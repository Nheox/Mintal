package dev.nheox.mintal.domain.session

import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.data.repository.SessionRepository
import dev.nheox.mintal.data.repository.SpeciesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** The live session, as the UI and the notification need to see it. */
data class RunningSession(
    val mode: SessionMode,
    val plannedSeconds: Int,
    val speciesId: String,
    val tagId: Long?,
    val startedAtMillis: Long,
    val elapsedSeconds: Int,
    val graceRemainingSeconds: Int?,
) {
    val remainingSeconds: Int
        get() = (plannedSeconds - elapsedSeconds).coerceAtLeast(0)

    /** 0f..1f for the session bar; a count-up session has no end to progress toward. */
    val progress: Float
        get() = if (mode == SessionMode.COUNTDOWN && plannedSeconds > 0) {
            (elapsedSeconds.toFloat() / plannedSeconds).coerceIn(0f, 1f)
        } else {
            0f
        }

    val inGracePeriod: Boolean get() = graceRemainingSeconds != null
}

/** What happened when a session ended, so the result screen can say it once. */
sealed interface SessionResult {
    data class Recorded(
        val sessionId: Long,
        val outcome: SessionOutcome,
        val actualSeconds: Int,
        val plannedSeconds: Int,
        val speciesId: String,
        val newlyUnlocked: List<Species>,
    ) : SessionResult

    /** A count-up session under the minimum: not recorded at all (spec §5.4). */
    data object Discarded : SessionResult
}

/**
 * Owns the one live session. The foreground service drives [tick] and reports whether the
 * user is distracted; the UI reads [running] and [result]. Nothing here touches Android
 * framework classes beyond the clock, so the whole state machine is unit-testable.
 */
@Singleton
class SessionController @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val speciesRepository: SpeciesRepository,
    private val clock: MintalClock,
) {
    private val finishLock = Mutex()

    private var startedAtElapsed = 0L
    private var graceStartedElapsed: Long? = null
    private var distracted = false

    private val _running = MutableStateFlow<RunningSession?>(null)
    val running: StateFlow<RunningSession?> = _running.asStateFlow()

    private val _result = MutableStateFlow<SessionResult?>(null)
    val result: StateFlow<SessionResult?> = _result.asStateFlow()

    val isRunning: Boolean get() = _running.value != null

    fun start(mode: SessionMode, plannedSeconds: Int, speciesId: String, tagId: Long?) {
        if (_running.value != null) return
        startedAtElapsed = clock.elapsedRealtimeMillis()
        graceStartedElapsed = null
        distracted = false
        _result.value = null
        _running.value = RunningSession(
            mode = mode,
            plannedSeconds = if (mode == SessionMode.COUNTDOWN) plannedSeconds else 0,
            speciesId = speciesId,
            tagId = tagId,
            startedAtMillis = clock.nowMillis(),
            elapsedSeconds = 0,
            graceRemainingSeconds = null,
        )
    }

    /**
     * Called by the service each tick. True starts the grace countdown; false clears it,
     * which is also how an allowlisted app, a phone call or a dark screen stays harmless
     * (spec §5.3).
     */
    fun setDistracted(value: Boolean) {
        if (_running.value == null) {
            distracted = false
            graceStartedElapsed = null
            return
        }
        if (value && !distracted) {
            graceStartedElapsed = clock.elapsedRealtimeMillis()
        } else if (!value) {
            graceStartedElapsed = null
        }
        distracted = value
    }

    suspend fun tick() {
        val current = _running.value ?: return
        val now = clock.elapsedRealtimeMillis()
        val elapsed = ((now - startedAtElapsed) / 1000L).toInt().coerceAtLeast(0)

        val graceStart = graceStartedElapsed
        if (graceStart != null) {
            val graceElapsed = ((now - graceStart) / 1000L).toInt()
            if (graceElapsed >= SessionRules.GRACE_SECONDS) {
                finish(SessionOutcome.LEFT_APP, elapsed)
                return
            }
        }

        if (current.mode == SessionMode.COUNTDOWN && elapsed >= current.plannedSeconds) {
            finish(SessionOutcome.SUCCESS, current.plannedSeconds)
            return
        }
        if (current.mode == SessionMode.COUNT_UP && elapsed >= SessionRules.MAX_SECONDS) {
            finish(SessionOutcome.SUCCESS, SessionRules.MAX_SECONDS)
            return
        }

        _running.value = current.copy(
            elapsedSeconds = elapsed,
            graceRemainingSeconds = graceStart?.let {
                (SessionRules.GRACE_SECONDS - ((now - it) / 1000L).toInt()).coerceAtLeast(0)
            },
        )
    }

    /** The user confirmed Give Up (spec §5.2). */
    suspend fun giveUp() {
        if (_running.value == null) return
        finish(SessionOutcome.GAVE_UP, elapsedSecondsNow())
    }

    /** Stopping a count-up session is a success if it reached the minimum. */
    suspend fun stopCountUp() {
        val current = _running.value ?: return
        if (current.mode != SessionMode.COUNT_UP) return
        finish(SessionOutcome.SUCCESS, elapsedSecondsNow())
    }

    fun clearResult() {
        _result.value = null
    }

    private fun elapsedSecondsNow(): Int =
        ((clock.elapsedRealtimeMillis() - startedAtElapsed) / 1000L).toInt().coerceAtLeast(0)

    private suspend fun finish(outcome: SessionOutcome, actualSeconds: Int) {
        finishLock.withLock {
            val current = _running.value ?: return
            _running.value = null
            distracted = false
            graceStartedElapsed = null

            if (current.mode == SessionMode.COUNT_UP &&
                actualSeconds < SessionRules.COUNT_UP_MIN_SECONDS
            ) {
                _result.value = SessionResult.Discarded
                return
            }

            val endedAt = clock.nowMillis()
            val entity = SessionEntity(
                startedAt = current.startedAtMillis,
                endedAt = endedAt,
                plannedSeconds = current.plannedSeconds,
                actualSeconds = actualSeconds,
                mode = current.mode,
                outcome = outcome,
                speciesId = current.speciesId,
                tagId = current.tagId,
                rating = null,
                note = null,
                timezoneId = MintalTime.systemZone().id,
            )
            val id = sessionRepository.record(entity)
            val unlocked = if (outcome.isSuccess) {
                speciesRepository.refreshUnlocks(endedAt)
            } else {
                emptyList()
            }
            _result.value = SessionResult.Recorded(
                sessionId = id,
                outcome = outcome,
                actualSeconds = actualSeconds,
                plannedSeconds = current.plannedSeconds,
                speciesId = current.speciesId,
                newlyUnlocked = unlocked,
            )
        }
    }
}

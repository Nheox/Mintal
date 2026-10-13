package dev.nheox.mintal.domain.session

import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.data.db.entity.SessionEntity

/**
 * Everything [SessionController] needs from storage, and nothing more.
 *
 * The controller is the one piece of logic where a mistake costs a user real focus time —
 * a session failed a second too early, or a completed block never written down. Keeping
 * its dependency this small is what lets the whole state machine be tested without Room,
 * an emulator, or a clock that actually ticks.
 */
interface SessionStore {
    suspend fun record(session: SessionEntity): Long

    suspend fun refreshUnlocks(nowMillis: Long): List<Species>
}

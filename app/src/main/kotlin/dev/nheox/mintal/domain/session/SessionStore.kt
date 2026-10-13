package dev.nheox.mintal.domain.session

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

    /**
     * How many mints the player has earned and not yet picked, counting the session just
     * recorded. The controller never picks one itself — that is the player's call at a
     * milestone (spec §6.1, amended) — it only reports that one is waiting.
     */
    suspend fun pendingUnlockChoices(): Int
}

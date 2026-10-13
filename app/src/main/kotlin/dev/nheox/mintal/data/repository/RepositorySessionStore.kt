package dev.nheox.mintal.data.repository

import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.domain.session.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

/** The real [SessionStore]: Room for the row, the derived slot count for the rest. */
@Singleton
class RepositorySessionStore @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val speciesRepository: SpeciesRepository,
) : SessionStore {

    override suspend fun record(session: SessionEntity): Long = sessionRepository.record(session)

    override suspend fun pendingUnlockChoices(): Int = speciesRepository.pendingChoices()
}

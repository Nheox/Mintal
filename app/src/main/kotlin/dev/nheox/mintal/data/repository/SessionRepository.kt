package dev.nheox.mintal.data.repository

import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.dao.SessionDao
import dev.nheox.mintal.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
) {
    fun observeAll(): Flow<List<SessionEntity>> = sessionDao.observeAll()

    fun observeById(id: Long): Flow<SessionEntity?> =
        sessionDao.observeById(id).map { it.firstOrNull() }

    fun observeDay(epochDay: Long, dayStartMinutes: Int): Flow<List<SessionEntity>> {
        val from = MintalTime.startOfDayMillis(epochDay, dayStartMinutes = dayStartMinutes)
        val to = MintalTime.startOfDayMillis(epochDay + 1, dayStartMinutes = dayStartMinutes)
        return sessionDao.observeBetween(from, to)
    }

    fun observeBetween(fromMillis: Long, toMillis: Long): Flow<List<SessionEntity>> =
        sessionDao.observeBetween(fromMillis, toMillis)

    fun observeSuccessfulSecondsTotal(): Flow<Int> = sessionDao.observeSuccessfulSecondsTotal()

    suspend fun successfulSecondsTotal(): Int = sessionDao.successfulSecondsTotal()

    suspend fun record(session: SessionEntity): Long = sessionDao.insert(session)

    suspend fun update(session: SessionEntity) = sessionDao.update(session)

    suspend fun delete(session: SessionEntity) = sessionDao.delete(session)

    suspend fun getById(id: Long): SessionEntity? = sessionDao.getById(id)

    suspend fun getAll(): List<SessionEntity> = sessionDao.getAll()

    suspend fun replaceAll(sessions: List<SessionEntity>) {
        sessionDao.deleteAll()
        if (sessions.isNotEmpty()) sessionDao.insertAll(sessions)
    }

    suspend fun deleteAll() = sessionDao.deleteAll()
}

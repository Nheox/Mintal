package dev.nheox.mintal.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.nheox.mintal.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startedAt ASC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startedAt ASC")
    suspend fun getAll(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE startedAt >= :fromMillis AND startedAt < :toMillis ORDER BY startedAt ASC")
    fun observeBetween(fromMillis: Long, toMillis: Long): Flow<List<SessionEntity>>

    /** Cumulative successful seconds — the single SUM unlock state is derived from (spec §7). */
    @Query("SELECT COALESCE(SUM(actualSeconds), 0) FROM sessions WHERE outcome = 'SUCCESS'")
    suspend fun successfulSecondsTotal(): Int

    @Query("SELECT COALESCE(SUM(actualSeconds), 0) FROM sessions WHERE outcome = 'SUCCESS'")
    fun observeSuccessfulSecondsTotal(): Flow<Int>

    @Query("UPDATE sessions SET tagId = NULL WHERE tagId = :tagId")
    suspend fun clearTag(tagId: Long)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

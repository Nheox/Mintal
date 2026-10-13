package dev.nheox.mintal.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.nheox.mintal.data.db.entity.AllowedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AllowedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AllowedAppEntity>)

    @Query("DELETE FROM allowed_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM allowed_apps ORDER BY packageName ASC")
    fun observeAll(): Flow<List<AllowedAppEntity>>

    @Query("SELECT * FROM allowed_apps ORDER BY packageName ASC")
    suspend fun getAll(): List<AllowedAppEntity>

    @Query("DELETE FROM allowed_apps")
    suspend fun deleteAll()
}

package dev.nheox.mintal.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.nheox.mintal.data.db.entity.SpeciesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeciesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(species: SpeciesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(species: List<SpeciesEntity>)

    @Query("SELECT * FROM species")
    fun observeAll(): Flow<List<SpeciesEntity>>

    @Query("SELECT * FROM species")
    suspend fun getAll(): List<SpeciesEntity>

    @Query("DELETE FROM species")
    suspend fun deleteAll()
}

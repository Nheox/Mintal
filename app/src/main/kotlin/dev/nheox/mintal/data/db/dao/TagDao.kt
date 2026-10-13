package dev.nheox.mintal.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.nheox.mintal.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun count(): Int

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}

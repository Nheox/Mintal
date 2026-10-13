package dev.nheox.mintal.data.repository

import dev.nheox.mintal.data.db.dao.SessionDao
import dev.nheox.mintal.data.db.dao.TagDao
import dev.nheox.mintal.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
    private val sessionDao: SessionDao,
) {
    fun observeAll(): Flow<List<TagEntity>> = tagDao.observeAll()

    suspend fun getAll(): List<TagEntity> = tagDao.getAll()

    suspend fun getById(id: Long): TagEntity? = tagDao.getById(id)

    /** Names are unique case-insensitively, max 30 characters (spec §5.8). */
    suspend fun create(name: String, colorIndex: Int, nowMillis: Long): Long? {
        val trimmed = name.trim().take(MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return null
        if (tagDao.findByName(trimmed) != null) return null
        val sortOrder = tagDao.count()
        return tagDao.insert(
            TagEntity(
                name = trimmed,
                colorIndex = colorIndex,
                createdAt = nowMillis,
                sortOrder = sortOrder,
            ),
        )
    }

    suspend fun rename(tag: TagEntity, name: String): Boolean {
        val trimmed = name.trim().take(MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return false
        val clash = tagDao.findByName(trimmed)
        if (clash != null && clash.id != tag.id) return false
        tagDao.update(tag.copy(name = trimmed))
        return true
    }

    suspend fun recolour(tag: TagEntity, colorIndex: Int) {
        tagDao.update(tag.copy(colorIndex = colorIndex))
    }

    /** Deleting a tag untags its sessions rather than deleting them (spec §5.8). */
    suspend fun delete(tag: TagEntity) {
        sessionDao.clearTag(tag.id)
        tagDao.delete(tag)
    }

    suspend fun seedStarterTagsIfEmpty(nowMillis: Long) {
        if (tagDao.count() > 0) return
        val starters = listOf("Study", "Work", "Reading", "Exercise", "Rest")
        tagDao.insertAll(
            starters.mapIndexed { index, name ->
                TagEntity(
                    name = name,
                    colorIndex = index % TAG_PALETTE_SIZE,
                    createdAt = nowMillis,
                    sortOrder = index,
                )
            },
        )
    }

    suspend fun replaceAll(tags: List<TagEntity>) {
        tagDao.deleteAll()
        if (tags.isNotEmpty()) tagDao.insertAll(tags)
    }

    companion object {
        const val MAX_NAME_LENGTH = 30
        const val TAG_PALETTE_SIZE = 12
    }
}

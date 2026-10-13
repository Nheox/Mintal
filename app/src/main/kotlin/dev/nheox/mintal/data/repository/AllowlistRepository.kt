package dev.nheox.mintal.data.repository

import dev.nheox.mintal.data.db.dao.AllowedAppDao
import dev.nheox.mintal.data.db.entity.AllowedAppEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The allowlist starts empty and is only consulted by detection layer B (spec §5.9). */
@Singleton
class AllowlistRepository @Inject constructor(
    private val allowedAppDao: AllowedAppDao,
) {
    fun observePackages(): Flow<Set<String>> =
        allowedAppDao.observeAll().map { rows -> rows.map { it.packageName }.toSet() }

    suspend fun allow(packageName: String, nowMillis: Long) =
        allowedAppDao.insert(AllowedAppEntity(packageName = packageName, addedAt = nowMillis))

    suspend fun disallow(packageName: String) = allowedAppDao.delete(packageName)

    suspend fun getAll(): List<AllowedAppEntity> = allowedAppDao.getAll()

    suspend fun replaceAll(rows: List<AllowedAppEntity>) {
        allowedAppDao.deleteAll()
        if (rows.isNotEmpty()) allowedAppDao.insertAll(rows)
    }
}

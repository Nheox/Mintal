package dev.nheox.mintal.data.repository

import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.model.SpeciesStatus
import dev.nheox.mintal.data.db.dao.SpeciesDao
import dev.nheox.mintal.data.db.entity.SpeciesEntity
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.domain.milestone.MilestoneEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeciesRepository @Inject constructor(
    private val speciesDao: SpeciesDao,
    private val sessionRepository: SessionRepository,
    private val catalog: SpeciesCatalog,
) {
    suspend fun catalogue(): List<Species> = catalog.all()

    suspend fun byId(id: String): Species? = catalog.byId(id)

    fun observeStatuses(): Flow<List<SpeciesStatus>> = speciesDao.observeAll().map { rows ->
        val stamps = rows.associate { it.id to it.unlockedAt }
        catalog.all().map { species -> SpeciesStatus(species, stamps[species.id]) }
    }

    /**
     * Recomputes cumulative successful time, stamps anything whose threshold was crossed,
     * and returns what was newly earned so the result screen can announce it once.
     */
    suspend fun refreshUnlocks(nowMillis: Long): List<Species> {
        val total = sessionRepository.successfulSecondsTotal()
        val stamped = speciesDao.getAll().filter { it.unlockedAt != null }.map { it.id }.toSet()
        val newly = MilestoneEngine.newlyUnlocked(catalog.all(), stamped, total)
        if (newly.isNotEmpty()) {
            speciesDao.insertAll(newly.map { SpeciesEntity(id = it.id, unlockedAt = nowMillis) })
        }
        return newly
    }

    suspend fun replaceAll(rows: List<SpeciesEntity>) {
        speciesDao.deleteAll()
        if (rows.isNotEmpty()) speciesDao.insertAll(rows)
    }

    suspend fun allUnlockRows(): List<SpeciesEntity> = speciesDao.getAll()
}

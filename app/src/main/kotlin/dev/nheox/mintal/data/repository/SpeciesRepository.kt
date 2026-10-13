package dev.nheox.mintal.data.repository

import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.model.SpeciesStatus
import dev.nheox.mintal.data.db.dao.SpeciesDao
import dev.nheox.mintal.data.db.entity.SpeciesEntity
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.domain.milestone.MilestoneEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    /** How many mints the player has earned and not yet picked (spec §6.1, amended). */
    fun observePendingChoices(): Flow<Int> = combine(
        sessionRepository.observeSuccessfulSecondsTotal(),
        speciesDao.observeAll(),
    ) { total, rows ->
        MilestoneEngine.pendingChoices(
            catalog = catalog.all(),
            totalSuccessfulSeconds = total,
            chosenCount = rows.count { it.unlockedAt != null },
        )
    }

    suspend fun pendingChoices(): Int = MilestoneEngine.pendingChoices(
        catalog = catalog.all(),
        totalSuccessfulSeconds = sessionRepository.successfulSecondsTotal(),
        chosenCount = speciesDao.getAll().count { it.unlockedAt != null },
    )

    /**
     * Records the player's pick for one earned slot. Everything is re-read here rather
     * than trusted from the caller: the screen that offers the choice is driven by a
     * flow, and two taps arriving together must not spend one slot twice.
     *
     * Returns false if there was no slot to spend, or the mint was already unlocked.
     */
    suspend fun choose(speciesId: String, nowMillis: Long): Boolean {
        val catalogue = catalog.all()
        if (catalogue.none { it.id == speciesId }) return false
        val rows = speciesDao.getAll()
        if (rows.any { it.id == speciesId && it.unlockedAt != null }) return false
        val pending = MilestoneEngine.pendingChoices(
            catalog = catalogue,
            totalSuccessfulSeconds = sessionRepository.successfulSecondsTotal(),
            chosenCount = rows.count { it.unlockedAt != null },
        )
        if (pending <= 0) return false
        speciesDao.upsert(SpeciesEntity(id = speciesId, unlockedAt = nowMillis))
        return true
    }

    suspend fun unlockedIds(): Set<String> =
        speciesDao.getAll().filter { it.unlockedAt != null }.map { it.id }.toSet()

    suspend fun replaceAll(rows: List<SpeciesEntity>) {
        speciesDao.deleteAll()
        if (rows.isNotEmpty()) speciesDao.insertAll(rows)
    }

    suspend fun allUnlockRows(): List<SpeciesEntity> = speciesDao.getAll()
}

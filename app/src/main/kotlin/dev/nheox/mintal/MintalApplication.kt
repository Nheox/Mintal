package dev.nheox.mintal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.data.repository.SpeciesRepository
import dev.nheox.mintal.data.repository.TagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MintalApplication : Application() {

    @Inject lateinit var tagRepository: TagRepository

    @Inject lateinit var speciesRepository: SpeciesRepository

    @Inject lateinit var clock: MintalClock

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val now = clock.nowMillis()
            // Starter tags on first launch (spec §5.8), and the one mint that is available
            // from the start — unlock state is always recomputed, never assumed (spec §7).
            tagRepository.seedStarterTagsIfEmpty(now)
            speciesRepository.refreshUnlocks(now)
        }
    }
}

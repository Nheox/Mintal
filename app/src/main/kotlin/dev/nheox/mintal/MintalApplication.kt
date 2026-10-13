package dev.nheox.mintal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.repository.SpeciesRepository
import dev.nheox.mintal.data.repository.TagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MintalApplication : Application() {

    @Inject lateinit var tagRepository: TagRepository

    @Inject lateinit var speciesRepository: SpeciesRepository

    @Inject lateinit var clock: MintalClock

    @Inject lateinit var preferences: MintalPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val now = clock.nowMillis()
            // Starter tags on first launch only (spec §5.8): they are all deletable, so
            // deleting every one of them must not bring them back on the next launch.
            if (!preferences.settings.first().starterTagsSeeded) {
                tagRepository.seedStarterTagsIfEmpty(now)
                preferences.setStarterTagsSeeded(true)
            }
            // Unlock state is always recomputed, never assumed (spec §7).
            speciesRepository.refreshUnlocks(now)
        }
    }
}

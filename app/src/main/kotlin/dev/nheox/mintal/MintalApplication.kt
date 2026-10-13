package dev.nheox.mintal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.data.prefs.MintalPreferences
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
            // Nothing to do for unlocks here any more: how many mints are earned is
            // derived from the session total whenever it is asked for, and which ones
            // they are is the player's choice, offered at the milestone (spec §6.1).
        }
    }
}

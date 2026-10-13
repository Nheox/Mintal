package dev.nheox.mintal.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.prefs.MintalSettings
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.data.repository.AllowlistRepository
import dev.nheox.mintal.data.transfer.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: MintalPreferences,
    private val backupManager: BackupManager,
) : ViewModel() {

    val settings: StateFlow<MintalSettings> = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MintalSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() {
        _message.value = null
    }

    fun setKeepScreenOn(value: Boolean) = viewModelScope.launch {
        preferences.setKeepScreenOn(value)
    }

    fun setVibrate(value: Boolean) = viewModelScope.launch {
        preferences.setVibrateOnFinish(value)
    }

    fun setDetectLeavingApp(value: Boolean) = viewModelScope.launch {
        preferences.setDetectLeavingApp(value)
    }

    fun setDetectForegroundApp(value: Boolean) = viewModelScope.launch {
        preferences.setDetectForegroundApp(value)
    }

    fun setTheme(choice: ThemeChoice) = viewModelScope.launch { preferences.setTheme(choice) }

    fun setDayStartHour(hour: Int) = viewModelScope.launch {
        preferences.setDayStartMinutes(hour.coerceIn(0, 23) * 60)
    }

    fun setMode(mode: SessionMode) = viewModelScope.launch { preferences.setMode(mode) }

    /** 0 follows the device locale; otherwise 1 = Monday .. 7 = Sunday, as in java.time. */
    fun setFirstWeekday(day: Int) = viewModelScope.launch {
        preferences.setFirstWeekdayOverride(day)
    }

    fun csvFileName(): String = backupManager.suggestedCsvName()

    fun backupFileName(): String = backupManager.suggestedBackupName()

    fun exportCsv(target: Uri) = viewModelScope.launch {
        backupManager.exportCsv(target)
            .onSuccess { _message.value = "Exported $it sessions to CSV." }
            .onFailure { _message.value = "Export failed: ${it.message}" }
    }

    fun exportBackup(target: Uri) = viewModelScope.launch {
        backupManager.exportBackup(target)
            .onSuccess { _message.value = "Backed up $it sessions." }
            .onFailure { _message.value = "Backup failed: ${it.message}" }
    }

    fun restoreBackup(source: Uri) = viewModelScope.launch {
        backupManager.importBackup(source)
            .onSuccess { _message.value = "Restored $it sessions." }
            .onFailure { _message.value = "Restore failed: ${it.message}" }
    }

    fun clearAllData() = viewModelScope.launch {
        backupManager.clearAllData()
        _message.value = "All sessions deleted."
    }

    /** User-initiated only, and never a precondition for starting a session (spec §9). */
    fun batteryOptimisationIntent(): Intent = Intent(
        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}"),
    )
}

data class AllowlistRow(
    val packageName: String,
    val label: String,
    val system: Boolean,
    val allowed: Boolean,
)

data class AllowlistUiState(
    val loading: Boolean = true,
    val userApps: List<AllowlistRow> = emptyList(),
    val systemApps: List<AllowlistRow> = emptyList(),
    val layerEnabled: Boolean = false,
)

@HiltViewModel
class AllowlistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val allowlistRepository: AllowlistRepository,
    private val clock: MintalClock,
    preferences: MintalPreferences,
) : ViewModel() {

    private val installed = MutableStateFlow<List<Triple<String, String, Boolean>>>(emptyList())

    val state: StateFlow<AllowlistUiState> = combine(
        installed,
        allowlistRepository.observePackages(),
        preferences.settings,
    ) { apps, allowed, settings ->
        val rows = apps.map { (packageName, label, system) ->
            AllowlistRow(
                packageName = packageName,
                label = label,
                system = system,
                allowed = packageName in allowed,
            )
        }.sortedBy { it.label.lowercase() }
        AllowlistUiState(
            loading = apps.isEmpty(),
            userApps = rows.filterNot { it.system },
            systemApps = rows.filter { it.system },
            layerEnabled = settings.detectForegroundApp,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AllowlistUiState())

    init {
        viewModelScope.launch { installed.value = loadLaunchableApps() }
    }

    fun setAllowed(packageName: String, allowed: Boolean) = viewModelScope.launch {
        if (allowed) {
            allowlistRepository.allow(packageName, clock.nowMillis())
        } else {
            allowlistRepository.disallow(packageName)
        }
    }

    /** Uses the manifest's <queries> block; QUERY_ALL_PACKAGES is not needed (spec §9). */
    private suspend fun loadLaunchableApps(): List<Triple<String, String, Boolean>> =
        withContext(Dispatchers.IO) {
            val manager = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = runCatching {
                manager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }.getOrElse { emptyList() }
            resolved
                .mapNotNull { info ->
                    val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                    if (appInfo.packageName == context.packageName) return@mapNotNull null
                    Triple(
                        appInfo.packageName,
                        runCatching { manager.getApplicationLabel(appInfo).toString() }
                            .getOrDefault(appInfo.packageName),
                        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
                }
                .distinctBy { it.first }
        }
}

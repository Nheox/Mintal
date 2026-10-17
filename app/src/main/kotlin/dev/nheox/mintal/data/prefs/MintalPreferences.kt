package dev.nheox.mintal.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nheox.mintal.core.model.SessionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mintalDataStore: DataStore<Preferences> by preferencesDataStore(name = "mintal_settings")

/** Which theme the token layer should resolve to (spec §3.2, §8.2). */
enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/**
 * How the species catalogue is laid out: the scrolling list, or one mint at a time
 * (spec §5.1).
 *
 * Here rather than in `ui/species/` because it is persisted, the way [ThemeChoice] is.
 * Which layout someone reads the catalogue in is a preference about the app, not state
 * belonging to one visit to the screen, and it was previously forgotten the moment the
 * screen left the back stack.
 */
enum class SpeciesViewMode { LIST, CARDS }

/**
 * Everything that is a preference rather than history (spec §7). Session rows live in
 * Room; this is the small mutable state around them.
 */
data class MintalSettings(
    val selectedSpeciesId: String = "spicata",
    val selectedTagId: Long? = null,
    val lastDurationMinutes: Int = 25,
    val mode: SessionMode = SessionMode.COUNTDOWN,
    val dayStartMinutes: Int = 0,
    val firstWeekdayOverride: Int = 0,
    val keepScreenOn: Boolean = false,
    val vibrateOnFinish: Boolean = true,
    val detectLeavingApp: Boolean = true,
    val detectForegroundApp: Boolean = false,
    val showFailuresInGrove: Boolean = false,
    val onboardingComplete: Boolean = false,
    val starterTagsSeeded: Boolean = false,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val speciesViewMode: SpeciesViewMode = SpeciesViewMode.LIST,
)

@Singleton
class MintalPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val SELECTED_SPECIES = stringPreferencesKey("selected_species")
        val SELECTED_TAG = longPreferencesKey("selected_tag")
        val LAST_DURATION = intPreferencesKey("last_duration_minutes")
        val MODE = stringPreferencesKey("mode")
        val DAY_START = intPreferencesKey("day_start_minutes")
        val FIRST_WEEKDAY = intPreferencesKey("first_weekday")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val VIBRATE = booleanPreferencesKey("vibrate_on_finish")
        val DETECT_LEAVING = booleanPreferencesKey("detect_leaving_app")
        val DETECT_FOREGROUND = booleanPreferencesKey("detect_foreground_app")
        val SHOW_FAILURES = booleanPreferencesKey("show_failures_in_grove")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_complete")
        val STARTER_TAGS = booleanPreferencesKey("starter_tags_seeded")
        val THEME = stringPreferencesKey("theme")
        val SPECIES_VIEW_MODE = stringPreferencesKey("species_view_mode")
    }

    val settings: Flow<MintalSettings> = context.mintalDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { prefs ->
            val storedTag = prefs[Keys.SELECTED_TAG]
            MintalSettings(
                selectedSpeciesId = prefs[Keys.SELECTED_SPECIES] ?: "spicata",
                selectedTagId = if (storedTag == null || storedTag < 0L) null else storedTag,
                lastDurationMinutes = prefs[Keys.LAST_DURATION] ?: 25,
                mode = runCatching { SessionMode.valueOf(prefs[Keys.MODE] ?: "") }
                    .getOrDefault(SessionMode.COUNTDOWN),
                dayStartMinutes = prefs[Keys.DAY_START] ?: 0,
                firstWeekdayOverride = prefs[Keys.FIRST_WEEKDAY] ?: 0,
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
                vibrateOnFinish = prefs[Keys.VIBRATE] ?: true,
                detectLeavingApp = prefs[Keys.DETECT_LEAVING] ?: true,
                detectForegroundApp = prefs[Keys.DETECT_FOREGROUND] ?: false,
                showFailuresInGrove = prefs[Keys.SHOW_FAILURES] ?: false,
                onboardingComplete = prefs[Keys.ONBOARDING_DONE] ?: false,
                starterTagsSeeded = prefs[Keys.STARTER_TAGS] ?: false,
                theme = runCatching { ThemeChoice.valueOf(prefs[Keys.THEME] ?: "") }
                    .getOrDefault(ThemeChoice.SYSTEM),
                speciesViewMode = runCatching {
                    SpeciesViewMode.valueOf(prefs[Keys.SPECIES_VIEW_MODE] ?: "")
                }.getOrDefault(SpeciesViewMode.LIST),
            )
        }

    suspend fun setSelectedSpecies(id: String) = edit { it[Keys.SELECTED_SPECIES] = id }

    suspend fun setSelectedTag(tagId: Long?) = edit { it[Keys.SELECTED_TAG] = tagId ?: -1L }

    suspend fun setLastDuration(minutes: Int) = edit { it[Keys.LAST_DURATION] = minutes }

    suspend fun setMode(mode: SessionMode) = edit { it[Keys.MODE] = mode.name }

    suspend fun setDayStartMinutes(minutes: Int) = edit { it[Keys.DAY_START] = minutes }

    suspend fun setFirstWeekdayOverride(day: Int) = edit { it[Keys.FIRST_WEEKDAY] = day }

    suspend fun setKeepScreenOn(enabled: Boolean) = edit { it[Keys.KEEP_SCREEN_ON] = enabled }

    suspend fun setVibrateOnFinish(enabled: Boolean) = edit { it[Keys.VIBRATE] = enabled }

    suspend fun setDetectLeavingApp(enabled: Boolean) = edit { it[Keys.DETECT_LEAVING] = enabled }

    suspend fun setDetectForegroundApp(enabled: Boolean) = edit { it[Keys.DETECT_FOREGROUND] = enabled }

    suspend fun setShowFailuresInGrove(enabled: Boolean) =
        edit { it[Keys.SHOW_FAILURES] = enabled }

    suspend fun setOnboardingComplete(complete: Boolean) = edit { it[Keys.ONBOARDING_DONE] = complete }

    suspend fun setStarterTagsSeeded(seeded: Boolean) = edit { it[Keys.STARTER_TAGS] = seeded }

    suspend fun setTheme(theme: ThemeChoice) = edit { it[Keys.THEME] = theme.name }

    suspend fun setSpeciesViewMode(mode: SpeciesViewMode) =
        edit { it[Keys.SPECIES_VIEW_MODE] = mode.name }

    /** Used by backup restore, which replaces preferences wholesale (spec §7.1). */
    suspend fun replaceAll(settings: MintalSettings) = edit { prefs ->
        prefs[Keys.SELECTED_SPECIES] = settings.selectedSpeciesId
        prefs[Keys.SELECTED_TAG] = settings.selectedTagId ?: -1L
        prefs[Keys.LAST_DURATION] = settings.lastDurationMinutes
        prefs[Keys.MODE] = settings.mode.name
        prefs[Keys.DAY_START] = settings.dayStartMinutes
        prefs[Keys.FIRST_WEEKDAY] = settings.firstWeekdayOverride
        prefs[Keys.KEEP_SCREEN_ON] = settings.keepScreenOn
        prefs[Keys.VIBRATE] = settings.vibrateOnFinish
        prefs[Keys.DETECT_LEAVING] = settings.detectLeavingApp
        prefs[Keys.DETECT_FOREGROUND] = settings.detectForegroundApp
        prefs[Keys.SHOW_FAILURES] = settings.showFailuresInGrove
        prefs[Keys.ONBOARDING_DONE] = settings.onboardingComplete
        prefs[Keys.STARTER_TAGS] = settings.starterTagsSeeded
        prefs[Keys.THEME] = settings.theme.name
        prefs[Keys.SPECIES_VIEW_MODE] = settings.speciesViewMode.name
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.mintalDataStore.edit { prefs -> block(prefs) }
    }
}

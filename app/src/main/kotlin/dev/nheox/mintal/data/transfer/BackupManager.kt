package dev.nheox.mintal.data.transfer

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.data.db.entity.AllowedAppEntity
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.data.db.entity.SpeciesEntity
import dev.nheox.mintal.data.db.entity.TagEntity
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.prefs.MintalSettings
import dev.nheox.mintal.data.prefs.SpeciesViewMode
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.data.repository.AllowlistRepository
import dev.nheox.mintal.data.repository.SessionRepository
import dev.nheox.mintal.data.repository.SpeciesRepository
import dev.nheox.mintal.data.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CSV export and the full JSON backup (spec §7.1). Both write through a Uri the user
 * picked with the system document picker, so Mintal needs no storage permission — and it
 * is the same code path v2's opt-in cloud backup will reuse (spec §6.2).
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val tagRepository: TagRepository,
    private val speciesRepository: SpeciesRepository,
    private val allowlistRepository: AllowlistRepository,
    private val preferences: MintalPreferences,
) {
    fun suggestedCsvName(): String = "mintal_sessions_${LocalDate.now()}.csv"

    fun suggestedBackupName(): String = "mintal_backup_${LocalDate.now()}.json"

    suspend fun exportCsv(target: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val sessions = sessionRepository.getAll()
            val tagNames = tagRepository.getAll().associate { it.id to it.name }
            write(target, CsvExporter.toCsv(sessions, tagNames))
            sessions.size
        }
    }

    suspend fun exportBackup(target: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val sessions = sessionRepository.getAll()
            val root = JSONObject()
            root.put("app", "Mintal")
            root.put("schemaVersion", SCHEMA_VERSION)
            root.put("exportedAt", System.currentTimeMillis())
            root.put("sessions", JSONArray().apply { sessions.forEach { put(it.toJson()) } })
            root.put(
                "tags",
                JSONArray().apply { tagRepository.getAll().forEach { put(it.toJson()) } },
            )
            root.put(
                "species",
                JSONArray().apply { speciesRepository.allUnlockRows().forEach { put(it.toJson()) } },
            )
            root.put(
                "allowedApps",
                JSONArray().apply { allowlistRepository.getAll().forEach { put(it.toJson()) } },
            )
            root.put("preferences", preferences.settings.first().toJson())
            write(target, root.toString(2))
            sessions.size
        }
    }

    /** Restore replaces the database wholesale, after the caller has confirmed (spec §7.1). */
    suspend fun importBackup(source: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = read(source)
            val root = JSONObject(raw)
            val version = root.optInt("schemaVersion", 1)
            require(version <= SCHEMA_VERSION) {
                "This backup was written by a newer version of Mintal."
            }

            val sessions = root.optJSONArray("sessions").mapObjects { it.toSessionEntity() }
            val tags = root.optJSONArray("tags").mapObjects { it.toTagEntity() }
            val species = root.optJSONArray("species").mapObjects { it.toSpeciesEntity() }
            val allowed = root.optJSONArray("allowedApps").mapObjects { it.toAllowedAppEntity() }

            sessionRepository.replaceAll(sessions)
            tagRepository.replaceAll(tags)
            speciesRepository.replaceAll(species)
            allowlistRepository.replaceAll(allowed)
            root.optJSONObject("preferences")?.let { preferences.replaceAll(it.toSettings()) }

            // The stamps in the file are kept as they are: which mints were chosen is
            // the player's own history and cannot be recomputed from anything (spec
            // §6.1, amended). How many they are entitled to still follows from the
            // restored sessions, so a backup from a longer history simply leaves a
            // choice waiting.

            sessions.size
        }
    }

    suspend fun clearAllData() {
        sessionRepository.deleteAll()
        // Back to the starting state, which is not "spearmint again" but "pick one":
        // with no successful time, exactly one slot is earned and nothing fills it.
        speciesRepository.replaceAll(emptyList())
    }

    private fun write(target: Uri, text: String) {
        val stream = context.contentResolver.openOutputStream(target, "wt")
            ?: error("Could not open the chosen file for writing.")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private fun read(source: Uri): String {
        val stream = context.contentResolver.openInputStream(source)
            ?: error("Could not open the chosen file for reading.")
        return stream.use { input -> input.bufferedReader(Charsets.UTF_8).readText() }
    }

    private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val result = ArrayList<T>(length())
        for (index in 0 until length()) {
            result += transform(getJSONObject(index))
        }
        return result
    }

    private fun SessionEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("startedAt", startedAt)
        .put("endedAt", endedAt)
        .put("plannedSeconds", plannedSeconds)
        .put("actualSeconds", actualSeconds)
        .put("mode", mode.name)
        .put("outcome", outcome.name)
        .put("speciesId", speciesId)
        .put("tagId", tagId ?: JSONObject.NULL)
        .put("rating", rating ?: JSONObject.NULL)
        .put("note", note ?: JSONObject.NULL)
        .put("timezoneId", timezoneId)

    private fun JSONObject.toSessionEntity(): SessionEntity = SessionEntity(
        id = optLong("id", 0L),
        startedAt = getLong("startedAt"),
        endedAt = getLong("endedAt"),
        plannedSeconds = optInt("plannedSeconds", 0),
        actualSeconds = optInt("actualSeconds", 0),
        mode = runCatching { SessionMode.valueOf(optString("mode")) }
            .getOrDefault(SessionMode.COUNTDOWN),
        outcome = runCatching { SessionOutcome.valueOf(optString("outcome")) }
            .getOrDefault(SessionOutcome.SUCCESS),
        speciesId = optString("speciesId", "spicata"),
        tagId = if (isNull("tagId")) null else optLong("tagId"),
        rating = if (isNull("rating")) null else optInt("rating"),
        note = if (isNull("note")) null else optString("note"),
        timezoneId = optString("timezoneId", "UTC"),
    )

    private fun TagEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("colorIndex", colorIndex)
        .put("createdAt", createdAt)
        .put("sortOrder", sortOrder)

    private fun JSONObject.toTagEntity(): TagEntity = TagEntity(
        id = optLong("id", 0L),
        name = optString("name"),
        colorIndex = optInt("colorIndex", 0),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        sortOrder = optInt("sortOrder", 0),
    )

    private fun SpeciesEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("unlockedAt", unlockedAt ?: JSONObject.NULL)

    private fun JSONObject.toSpeciesEntity(): SpeciesEntity = SpeciesEntity(
        id = optString("id"),
        unlockedAt = if (isNull("unlockedAt")) null else optLong("unlockedAt"),
    )

    private fun AllowedAppEntity.toJson(): JSONObject = JSONObject()
        .put("packageName", packageName)
        .put("addedAt", addedAt)

    private fun JSONObject.toAllowedAppEntity(): AllowedAppEntity = AllowedAppEntity(
        packageName = optString("packageName"),
        addedAt = optLong("addedAt", System.currentTimeMillis()),
    )

    private fun MintalSettings.toJson(): JSONObject = JSONObject()
        .put("selectedSpeciesId", selectedSpeciesId)
        .put("selectedTagId", selectedTagId ?: JSONObject.NULL)
        .put("lastDurationMinutes", lastDurationMinutes)
        .put("mode", mode.name)
        .put("dayStartMinutes", dayStartMinutes)
        .put("firstWeekdayOverride", firstWeekdayOverride)
        .put("keepScreenOn", keepScreenOn)
        .put("vibrateOnFinish", vibrateOnFinish)
        .put("detectLeavingApp", detectLeavingApp)
        .put("detectForegroundApp", detectForegroundApp)
        .put("showFailuresInGrove", showFailuresInGrove)
        .put("onboardingComplete", onboardingComplete)
        .put("starterTagsSeeded", starterTagsSeeded)
        .put("theme", theme.name)
        .put("speciesViewMode", speciesViewMode.name)

    private fun JSONObject.toSettings(): MintalSettings {
        val defaults = MintalSettings()
        return MintalSettings(
            selectedSpeciesId = optString("selectedSpeciesId", defaults.selectedSpeciesId),
            selectedTagId = if (isNull("selectedTagId")) null else optLong("selectedTagId"),
            lastDurationMinutes = optInt("lastDurationMinutes", defaults.lastDurationMinutes),
            mode = runCatching { SessionMode.valueOf(optString("mode")) }
                .getOrDefault(defaults.mode),
            dayStartMinutes = optInt("dayStartMinutes", defaults.dayStartMinutes),
            firstWeekdayOverride = optInt("firstWeekdayOverride", defaults.firstWeekdayOverride),
            keepScreenOn = optBoolean("keepScreenOn", defaults.keepScreenOn),
            vibrateOnFinish = optBoolean("vibrateOnFinish", defaults.vibrateOnFinish),
            detectLeavingApp = optBoolean("detectLeavingApp", defaults.detectLeavingApp),
            detectForegroundApp = optBoolean("detectForegroundApp", defaults.detectForegroundApp),
            showFailuresInGrove = optBoolean("showFailuresInGrove", defaults.showFailuresInGrove),
            onboardingComplete = optBoolean("onboardingComplete", defaults.onboardingComplete),
            starterTagsSeeded = optBoolean("starterTagsSeeded", defaults.starterTagsSeeded),
            theme = runCatching { ThemeChoice.valueOf(optString("theme")) }
                .getOrDefault(defaults.theme),
            // Absent from backups written before the catalogue layout was remembered, so
            // an older file restores to the default rather than failing.
            speciesViewMode = runCatching { SpeciesViewMode.valueOf(optString("speciesViewMode")) }
                .getOrDefault(defaults.speciesViewMode),
        )
    }

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}

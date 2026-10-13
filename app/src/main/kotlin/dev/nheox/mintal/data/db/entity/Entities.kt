package dev.nheox.mintal.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome

/** Spec §7. One row per session, successful or not. */
@Entity(
    tableName = "sessions",
    indices = [Index("startedAt"), Index("tagId")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val mode: SessionMode,
    val outcome: SessionOutcome,
    val speciesId: String,
    val tagId: Long?,
    val rating: Int?,
    val note: String?,
    val timezoneId: String,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int,
    val createdAt: Long,
    val sortOrder: Int,
)

/**
 * Unlock state only. The catalogue itself is a bundled asset, so it can grow across app
 * versions without a migration (spec §7).
 */
@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long?,
)

@Entity(tableName = "allowed_apps")
data class AllowedAppEntity(
    @PrimaryKey val packageName: String,
    val addedAt: Long,
)

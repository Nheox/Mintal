package dev.nheox.mintal.data.db

import androidx.room.TypeConverter
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome

/** Enums are stored as their names so a dumped database stays readable. */
class Converters {

    @TypeConverter
    fun modeToString(value: SessionMode): String = value.name

    @TypeConverter
    fun stringToMode(value: String): SessionMode =
        runCatching { SessionMode.valueOf(value) }.getOrDefault(SessionMode.COUNTDOWN)

    @TypeConverter
    fun outcomeToString(value: SessionOutcome): String = value.name

    @TypeConverter
    fun stringToOutcome(value: String): SessionOutcome =
        runCatching { SessionOutcome.valueOf(value) }.getOrDefault(SessionOutcome.SUCCESS)
}

package dev.nheox.mintal.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.nheox.mintal.data.db.dao.AllowedAppDao
import dev.nheox.mintal.data.db.dao.SessionDao
import dev.nheox.mintal.data.db.dao.SpeciesDao
import dev.nheox.mintal.data.db.dao.TagDao
import dev.nheox.mintal.data.db.entity.AllowedAppEntity
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.data.db.entity.SpeciesEntity
import dev.nheox.mintal.data.db.entity.TagEntity

@Database(
    entities = [
        SessionEntity::class,
        TagEntity::class,
        SpeciesEntity::class,
        AllowedAppEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MintalDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    abstract fun tagDao(): TagDao

    abstract fun speciesDao(): SpeciesDao

    abstract fun allowedAppDao(): AllowedAppDao

    companion object {
        const val NAME = "mintal.db"
    }
}

package dev.nheox.mintal.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.core.time.SystemMintalClock
import dev.nheox.mintal.data.db.MintalDatabase
import dev.nheox.mintal.data.db.dao.AllowedAppDao
import dev.nheox.mintal.data.db.dao.SessionDao
import dev.nheox.mintal.data.db.dao.SpeciesDao
import dev.nheox.mintal.data.db.dao.TagDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MintalDatabase =
        Room.databaseBuilder(context, MintalDatabase::class.java, MintalDatabase.NAME).build()

    @Provides
    fun provideSessionDao(database: MintalDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideTagDao(database: MintalDatabase): TagDao = database.tagDao()

    @Provides
    fun provideSpeciesDao(database: MintalDatabase): SpeciesDao = database.speciesDao()

    @Provides
    fun provideAllowedAppDao(database: MintalDatabase): AllowedAppDao = database.allowedAppDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ClockModule {

    @Binds
    abstract fun bindClock(impl: SystemMintalClock): MintalClock
}

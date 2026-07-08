package com.construccionia.app.di

import android.content.Context
import com.construccionia.app.data.local.AppDatabase
import com.construccionia.app.data.local.InfographicDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que provee la base de datos Room y sus DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.create(context)
    }

    @Provides
    @Singleton
    fun provideInfographicDao(database: AppDatabase): InfographicDao {
        return database.infographicDao()
    }
}

package com.construccionia.app.di

import android.content.Context
import com.construccionia.app.data.local.InfographicDao
import com.construccionia.app.data.repository.ImageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que provee los repositorios.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideImageRepository(
        @ApplicationContext context: Context,
        infographicDao: InfographicDao
    ): ImageRepository {
        return ImageRepository(context, infographicDao)
    }
}

package com.construccionia.feature.generation.data.di

import com.construccionia.feature.generation.data.mapper.GeminiMapper
import com.construccionia.feature.generation.data.remote.GeminiApiService
import com.construccionia.feature.generation.data.repository.GeminiRepositoryImpl
import com.construccionia.feature.generation.domain.repository.GeminiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GenerationModule {

    @Provides
    @Singleton
    fun provideGeminiMapper(): GeminiMapper {
        return GeminiMapper()
    }

    @Provides
    @Singleton
    fun provideGeminiRepository(
        geminiApiService: GeminiApiService,
        geminiMapper: GeminiMapper
    ): GeminiRepository {
        return GeminiRepositoryImpl(geminiApiService, geminiMapper)
    }
}

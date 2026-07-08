package com.construccionia.feature.ocr.data.di

import com.construccionia.feature.ocr.data.mlkit.MlKitTextRecognizer
import com.construccionia.feature.ocr.data.repository.OcrRepositoryImpl
import com.construccionia.feature.ocr.domain.repository.OcrRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideMlKitTextRecognizer(): MlKitTextRecognizer {
        return MlKitTextRecognizer()
    }

    @Provides
    @Singleton
    fun provideOcrRepository(
        textRecognizer: MlKitTextRecognizer
    ): OcrRepository {
        return OcrRepositoryImpl(textRecognizer)
    }
}

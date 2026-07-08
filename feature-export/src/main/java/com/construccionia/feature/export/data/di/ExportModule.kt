package com.construccionia.feature.export.data.di

import com.construccionia.feature.export.data.apachepoi.ApachePoiPptxGenerator
import com.construccionia.feature.export.data.apachepoi.PptxGenerator
import com.construccionia.feature.export.data.repository.ExportRepositoryImpl
import android.content.Context
import com.construccionia.feature.export.domain.repository.ExportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExportModule {

    @Provides
    @Singleton
    fun providePptxGenerator(
        generator: ApachePoiPptxGenerator
    ): PptxGenerator = generator

    @Provides
    @Singleton
    fun provideExportRepository(
        pptxGenerator: PptxGenerator,
        @ApplicationContext context: Context
    ): ExportRepository {
        return ExportRepositoryImpl(pptxGenerator, context)
    }
}

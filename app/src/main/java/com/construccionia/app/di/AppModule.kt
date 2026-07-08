package com.construccionia.app.di

import android.content.Context
import com.construccionia.app.export.PdfExportHelper
import com.construccionia.app.export.PptExportHelper
import com.construccionia.app.ocr.OcrHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que provee dependencias de aplicación.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOcrHelper(): OcrHelper {
        return OcrHelper()
    }

    @Provides
    @Singleton
    fun providePptExportHelper(@ApplicationContext context: Context): PptExportHelper {
        return PptExportHelper(context)
    }

    @Provides
    @Singleton
    fun providePdfExportHelper(@ApplicationContext context: Context): PdfExportHelper {
        return PdfExportHelper(context)
    }
}

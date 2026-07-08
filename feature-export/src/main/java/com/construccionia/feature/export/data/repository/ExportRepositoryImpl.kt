package com.construccionia.feature.export.data.repository

import android.content.Context
import com.construccionia.core.common.AppResult
import com.construccionia.core.common.FileOutput
import com.construccionia.feature.export.data.apachepoi.PptxGenerator
import com.construccionia.feature.export.domain.model.ExportConfig
import com.construccionia.feature.export.domain.model.SlideContent
import com.construccionia.feature.export.domain.repository.ExportRepository
import java.io.File

class ExportRepositoryImpl(
    private val pptxGenerator: PptxGenerator,
    private val context: Context
) : ExportRepository {

    override suspend fun exportToPptx(
        slides: List<SlideContent>,
        config: ExportConfig
    ): AppResult<FileOutput> {
        return try {
            val outputDir = File(context.cacheDir, "exports")
            outputDir.mkdirs()
            val outputPath = File(outputDir, "${config.fileName}.pptx").absolutePath

            val result = pptxGenerator.generate(slides, config, outputPath)
            AppResult.Success(result)
        } catch (e: Exception) {
            AppResult.Error(
                com.construccionia.core.common.ProcessingException(
                    "Error al exportar a PPTX: ${e.message}", e
                )
            )
        }
    }

    override suspend fun exportToPdf(
        slides: List<SlideContent>,
        config: ExportConfig
    ): AppResult<FileOutput> {
        // TODO: Implementar exportación a PDF
        return AppResult.Error(
            com.construccionia.core.common.ProcessingException(
                "Exportación a PDF no implementada aún"
            )
        )
    }
}

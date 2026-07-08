package com.construccionia.core.testing

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.FileOutput
import com.construccionia.feature.export.domain.model.ExportConfig
import com.construccionia.feature.export.domain.model.SlideContent
import com.construccionia.feature.export.domain.repository.ExportRepository

/**
 * Fake de [ExportRepository] para pruebas.
 * Simula la exportación sin Apache POI real.
 */
class FakeExportRepository : ExportRepository {

    private var shouldFail = false
    private var exportCount = 0

    fun setShouldFail(fail: Boolean) {
        shouldFail = fail
    }

    fun getExportCount(): Int = exportCount

    override suspend fun exportToPptx(
        slides: List<SlideContent>,
        config: ExportConfig
    ): AppResult<FileOutput> {
        exportCount++
        if (shouldFail) {
            return AppResult.Error(
                com.construccionia.core.common.ProcessingException("Error simulado de exportación PPTX")
            )
        }

        return AppResult.Success(
            FileOutput(
                path = "/tmp/exports/${config.fileName}.pptx",
                mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                fileName = "${config.fileName}.pptx"
            )
        )
    }

    override suspend fun exportToPdf(
        slides: List<SlideContent>,
        config: ExportConfig
    ): AppResult<FileOutput> {
        exportCount++
        if (shouldFail) {
            return AppResult.Error(
                com.construccionia.core.common.ProcessingException("Error simulado de exportación PDF")
            )
        }

        return AppResult.Success(
            FileOutput(
                path = "/tmp/exports/${config.fileName}.pdf",
                mimeType = "application/pdf",
                fileName = "${config.fileName}.pdf"
            )
        )
    }
}

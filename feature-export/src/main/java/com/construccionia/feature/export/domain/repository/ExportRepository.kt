package com.construccionia.feature.export.domain.repository

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.FileOutput
import com.construccionia.feature.export.domain.model.ExportConfig
import com.construccionia.feature.export.domain.model.SlideContent

/**
 * Repositorio para exportar presentaciones.
 */
interface ExportRepository {
    /**
     * Exporta una lista de diapositivas a un archivo PowerPoint.
     */
    suspend fun exportToPptx(
        slides: List<SlideContent>,
        config: ExportConfig
    ): AppResult<FileOutput>

    /**
     * Exporta una lista de diapositivas a PDF.
     */
    suspend fun exportToPdf(
        slides: List<SlideContent>,
        config: ExportConfig
    ): AppResult<FileOutput>
}

package com.construccionia.feature.export.data.apachepoi

import com.construccionia.core.common.FileOutput
import com.construccionia.feature.export.domain.model.ExportConfig
import com.construccionia.feature.export.domain.model.SlideContent

/**
 * Interfaz wrapper para la generación de archivos PowerPoint.
 * Abstrae la dependencia de Apache POI.
 */
interface PptxGenerator {
    /**
     * Genera un archivo PowerPoint a partir de diapositivas.
     */
    suspend fun generate(
        slides: List<SlideContent>,
        config: ExportConfig,
        outputPath: String
    ): FileOutput
}

package com.construccionia.feature.export.domain.model

/**
 * Configuración para la exportación a PowerPoint.
 */
data class ExportConfig(
    val fileName: String = "presentacion_construccion",
    val title: String = "Proyecto de Construcción",
    val author: String = "Construcción IA",
    val includeSlideNumbers: Boolean = true,
    val themeColor: String = "#1A5276",
    val outputFormat: OutputFormat = OutputFormat.PPTX
)

enum class OutputFormat {
    PPTX,
    PDF
}

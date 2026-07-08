package com.construccionia.core.common

/**
 * Representa la salida de un archivo generado por la aplicación.
 */
data class FileOutput(
    val path: String,
    val mimeType: String,
    val fileName: String
)

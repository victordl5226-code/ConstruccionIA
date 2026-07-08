package com.construccionia.feature.generation.domain.model

import com.construccionia.core.common.ImageBytes

/**
 * Representa una imagen generada por el modelo.
 */
data class GeneratedImage(
    val id: String,
    val imageBytes: ImageBytes,
    val mimeType: String,
    val promptUsed: String,
    val timestamp: Long = System.currentTimeMillis()
)

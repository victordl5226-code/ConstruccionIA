package com.construccionia.feature.generation.data.mapper

import android.graphics.Bitmap
import com.construccionia.core.common.ImageBytes
import com.construccionia.core.common.extension.toImageBytes
import com.construccionia.feature.generation.domain.model.GeneratedImage
import com.construccionia.feature.generation.domain.model.GenerationPrompt
import java.util.UUID
import javax.inject.Inject

/**
 * Mapea las respuestas de la API de Gemini a modelos de dominio.
 */
class GeminiMapper @Inject constructor() {

    /**
     * Convierte un Bitmap generado por la API en un [GeneratedImage].
     */
    fun mapToGeneratedImage(
        bitmap: Bitmap,
        prompt: GenerationPrompt,
        mimeType: String = "image/png"
    ): GeneratedImage {
        val imageBytes: ImageBytes = bitmap.toImageBytes(mimeType)
        return GeneratedImage(
            id = UUID.randomUUID().toString(),
            imageBytes = imageBytes,
            mimeType = mimeType,
            promptUsed = prompt.toFullPrompt(),
            timestamp = System.currentTimeMillis()
        )
    }
}

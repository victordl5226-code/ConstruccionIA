package com.construccionia.feature.generation.domain.repository

import com.construccionia.core.common.AppResult
import com.construccionia.feature.generation.domain.model.GeneratedImage
import com.construccionia.feature.generation.domain.model.GenerationPrompt

/**
 * Repositorio para interactuar con la API de Gemini.
 */
interface GeminiRepository {
    /**
     * Genera una imagen a partir de un prompt.
     */
    suspend fun generateImage(prompt: GenerationPrompt): AppResult<GeneratedImage>

    /**
     * Verifica si la API de Gemini está disponible.
     */
    suspend fun isApiAvailable(): AppResult<Boolean>
}

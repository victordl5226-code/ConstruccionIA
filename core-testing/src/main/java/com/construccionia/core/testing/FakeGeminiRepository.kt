package com.construccionia.core.testing

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ImageBytes
import com.construccionia.feature.generation.domain.model.AspectRatio
import com.construccionia.feature.generation.domain.model.GeneratedImage
import com.construccionia.feature.generation.domain.model.GenerationPrompt
import com.construccionia.feature.generation.domain.model.PromptStyle
import com.construccionia.feature.generation.domain.repository.GeminiRepository

/**
 * Fake de [GeminiRepository] para pruebas.
 * Simula la generación de imágenes sin llamar a la API real.
 */
class FakeGeminiRepository : GeminiRepository {

    private var shouldFail = false
    private var failCount = 0
    private var generateCount = 0

    /** Configura el fake para fallar en la próxima llamada. */
    fun setShouldFail(fail: Boolean) {
        shouldFail = fail
    }

    fun getGenerateCount(): Int = generateCount

    override suspend fun generateImage(prompt: GenerationPrompt): AppResult<GeneratedImage> {
        generateCount++
        if (shouldFail) {
            failCount++
            return AppResult.Error(
                com.construccionia.core.common.ServerException(
                    500,
                    "Error simulado del servidor Gemini"
                )
            )
        }

        val fakeImage = GeneratedImage(
            id = "fake-image-${generateCount}",
            imageBytes = ImageBytes(
                bytes = ByteArray(100) { it.toByte() },
                width = 512,
                height = 512
            ),
            mimeType = "image/png",
            promptUsed = prompt.toFullPrompt(),
            timestamp = System.currentTimeMillis()
        )
        return AppResult.Success(fakeImage)
    }

    override suspend fun isApiAvailable(): AppResult<Boolean> {
        return if (shouldFail) {
            AppResult.Error(
                com.construccionia.core.common.NetworkException("API no disponible simulada")
            )
        } else {
            AppResult.Success(true)
        }
    }
}

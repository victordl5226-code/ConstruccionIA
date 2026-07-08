package com.construccionia.feature.generation.domain.usecase

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.InvalidInputException
import com.construccionia.feature.generation.domain.model.GeneratedImage
import com.construccionia.feature.generation.domain.model.GenerationPrompt
import com.construccionia.feature.generation.domain.repository.GeminiRepository
import javax.inject.Inject

/**
 * Caso de uso para generar una imagen a partir de un prompt.
 */
class GenerateImageUseCase @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val validatePromptUseCase: ValidatePromptUseCase
) {
    /**
     * Valida el prompt y, si es válido, genera la imagen.
     */
    suspend operator fun invoke(prompt: GenerationPrompt): AppResult<GeneratedImage> {
        val validation = validatePromptUseCase(prompt.text)
        if (!validation.isValid) {
            return AppResult.Error(
                InvalidInputException(validation.errorMessage ?: "Prompt inválido")
            )
        }
        return geminiRepository.generateImage(prompt)
    }
}

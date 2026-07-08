package com.construccionia.feature.generation.domain.usecase

import com.construccionia.core.common.AppConstants
import javax.inject.Inject

/**
 * Resultado de la validación de un prompt.
 */
data class PromptValidation(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Caso de uso para validar que un prompt sea adecuado antes de enviarlo a la API.
 */
class ValidatePromptUseCase @Inject constructor() {

    /**
     * Valida el texto del prompt.
     */
    operator fun invoke(promptText: String): PromptValidation {
        if (promptText.isBlank()) {
            return PromptValidation(false, "El prompt no puede estar vacío")
        }
        if (promptText.length > AppConstants.MAX_PROMPT_LENGTH) {
            return PromptValidation(
                false,
                "El prompt es demasiado largo (máximo ${AppConstants.MAX_PROMPT_LENGTH} caracteres)"
            )
        }
        if (promptText.length < 10) {
            return PromptValidation(
                false,
                "El prompt debe tener al menos 10 caracteres para obtener buenos resultados"
            )
        }
        return PromptValidation(true)
    }
}

package com.construccionia.feature.generation.presentation

import com.construccionia.core.common.AppException
import com.construccionia.feature.generation.domain.model.AspectRatio
import com.construccionia.feature.generation.domain.model.GeneratedImage
import com.construccionia.feature.generation.domain.model.PromptStyle

/**
 * Estados posibles de la pantalla de generación.
 */
sealed class GenerationUiState {
    /** Estado inicial, esperando interacción del usuario. */
    data object Idle : GenerationUiState()

    /** El usuario está escribiendo el prompt. */
    data class Editing(
        val prompt: String = "",
        val style: PromptStyle = PromptStyle.FOTO_REALISTA,
        val aspectRatio: AspectRatio = AspectRatio.SQUARE_1_1
    ) : GenerationUiState()

    /** Generando la imagen. */
    data object Generating : GenerationUiState()

    /** Imagen generada exitosamente. */
    data class Success(val generatedImage: GeneratedImage) : GenerationUiState()

    /** Error durante la generación. */
    data class Error(val exception: AppException) : GenerationUiState()
}

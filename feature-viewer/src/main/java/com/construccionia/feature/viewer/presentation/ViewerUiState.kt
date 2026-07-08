package com.construccionia.feature.viewer.presentation

import com.construccionia.core.common.AppException
import com.construccionia.core.common.ImageBytes

/**
 * Estados de la pantalla del visualizador.
 */
sealed class ViewerUiState {
    data object Idle : ViewerUiState()
    data object Loading : ViewerUiState()
    data class Loaded(val imageBytes: ImageBytes, val imageUrl: String? = null) : ViewerUiState()
    data class Error(val exception: AppException) : ViewerUiState()
}

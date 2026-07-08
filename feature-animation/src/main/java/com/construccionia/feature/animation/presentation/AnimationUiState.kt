package com.construccionia.feature.animation.presentation

import com.construccionia.feature.animation.domain.model.AnimationConfig
import com.construccionia.feature.animation.domain.model.AnimationType

/**
 * Estados de la pantalla de animación.
 */
sealed class AnimationUiState {
    data object Idle : AnimationUiState()
    data class Playing(val config: AnimationConfig) : AnimationUiState()
    data object Paused : AnimationUiState()
    data class Error(val message: String) : AnimationUiState()
}

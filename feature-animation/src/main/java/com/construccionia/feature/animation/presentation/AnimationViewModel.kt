package com.construccionia.feature.animation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccionia.core.common.AppResult
import com.construccionia.feature.animation.domain.model.AnimationType
import com.construccionia.feature.animation.domain.repository.AnimationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimationViewModel @Inject constructor(
    private val animationRepository: AnimationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnimationUiState>(AnimationUiState.Idle)
    val uiState: StateFlow<AnimationUiState> = _uiState.asStateFlow()

    private val _selectedType = MutableStateFlow(AnimationType.CONSTRUCTION_SITE)
    val selectedType: StateFlow<AnimationType> = _selectedType.asStateFlow()

    private val _availableAnimations = MutableStateFlow<List<AnimationType>>(emptyList())
    val availableAnimations: StateFlow<List<AnimationType>> = _availableAnimations.asStateFlow()

    init {
        loadAvailableAnimations()
    }

    private fun loadAvailableAnimations() {
        viewModelScope.launch {
            when (val result = animationRepository.getAvailableAnimations()) {
                is AppResult.Success -> {
                    _availableAnimations.value = result.data
                }
                is AppResult.Error -> {
                    _uiState.value = AnimationUiState.Error(
                        "No se pudieron cargar las animaciones: ${result.exception.message}"
                    )
                }
            }
        }
    }

    /**
     * Selecciona un tipo de animación y la reproduce.
     * Verifica disponibilidad antes de intentar reproducir.
     */
    fun selectAnimation(type: AnimationType) {
        _selectedType.value = type
        viewModelScope.launch {
            // ANIMATION-02 FIX: Verificar disponibilidad antes de reproducir
            val isAvailable = animationRepository.isAnimationAvailable(type)
            if (!isAvailable) {
                _uiState.value = AnimationUiState.Error(
                    "La animación '${type.displayName}' no está disponible"
                )
                return@launch
            }

            // ANIMATION-01 FIX: Manejar AppResult con estado Error
            when (val result = animationRepository.getAnimationConfig(type)) {
                is AppResult.Success -> {
                    _uiState.value = AnimationUiState.Playing(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = AnimationUiState.Error(
                        "Error al cargar animación: ${result.exception.message}"
                    )
                }
            }
        }
    }

    /**
     * Pausa la animación actual.
     */
    fun pause() {
        _uiState.value = AnimationUiState.Paused
    }

    /**
     * Reanuda la animación.
     */
    fun resume() {
        val current = _uiState.value
        if (current is AnimationUiState.Paused) {
            viewModelScope.launch {
                val type = _selectedType.value
                val isAvailable = animationRepository.isAnimationAvailable(type)
                if (!isAvailable) {
                    _uiState.value = AnimationUiState.Error(
                        "La animación '${type.displayName}' ya no está disponible"
                    )
                    return@launch
                }

                when (val result = animationRepository.getAnimationConfig(type)) {
                    is AppResult.Success -> {
                        _uiState.value = AnimationUiState.Playing(result.data)
                    }
                    is AppResult.Error -> {
                        _uiState.value = AnimationUiState.Error(
                            "Error al reanudar animación: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Reinicia el estado.
     */
    fun reset() {
        _uiState.value = AnimationUiState.Idle
        _selectedType.value = AnimationType.CONSTRUCTION_SITE
    }
}

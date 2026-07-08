package com.construccionia.feature.generation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccionia.core.common.AppResult
import com.construccionia.feature.generation.domain.model.AspectRatio
import com.construccionia.feature.generation.domain.model.GeneratedImage
import com.construccionia.feature.generation.domain.model.GenerationPrompt
import com.construccionia.feature.generation.domain.model.PromptStyle
import com.construccionia.feature.generation.domain.usecase.GenerateImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenerationViewModel @Inject constructor(
    private val generateImageUseCase: GenerateImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val uiState: StateFlow<GenerationUiState> = _uiState.asStateFlow()

    private val _promptText = MutableStateFlow("")
    val promptText: StateFlow<String> = _promptText.asStateFlow()

    private val _selectedStyle = MutableStateFlow(PromptStyle.FOTO_REALISTA)
    val selectedStyle: StateFlow<PromptStyle> = _selectedStyle.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow(AspectRatio.SQUARE_1_1)
    val selectedAspectRatio: StateFlow<AspectRatio> = _selectedAspectRatio.asStateFlow()

    /**
     * Actualiza el texto del prompt.
     */
    fun updatePrompt(text: String) {
        _promptText.value = text
        _uiState.value = GenerationUiState.Editing(
            prompt = text,
            style = _selectedStyle.value,
            aspectRatio = _selectedAspectRatio.value
        )
    }

    /**
     * Actualiza el estilo seleccionado.
     */
    fun updateStyle(style: PromptStyle) {
        _selectedStyle.value = style
        _uiState.value = GenerationUiState.Editing(
            prompt = _promptText.value,
            style = style,
            aspectRatio = _selectedAspectRatio.value
        )
    }

    /**
     * Actualiza la proporción de aspecto seleccionada.
     */
    fun updateAspectRatio(aspectRatio: AspectRatio) {
        _selectedAspectRatio.value = aspectRatio
        _uiState.value = GenerationUiState.Editing(
            prompt = _promptText.value,
            style = _selectedStyle.value,
            aspectRatio = aspectRatio
        )
    }

    /**
     * Ejecuta la generación de la imagen.
     */
    fun generate() {
        val prompt = _promptText.value.trim()
        if (prompt.isBlank()) return

        _uiState.value = GenerationUiState.Generating

        viewModelScope.launch {
            val generationPrompt = GenerationPrompt(
                text = prompt,
                style = _selectedStyle.value,
                aspectRatio = _selectedAspectRatio.value
            )

            when (val result = generateImageUseCase(generationPrompt)) {
                is AppResult.Success -> {
                    _uiState.value = GenerationUiState.Success(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = GenerationUiState.Error(result.exception)
                }
            }
        }
    }

    /**
     * Reinicia el estado a idle para permitir una nueva generación.
     */
    fun reset() {
        _uiState.value = GenerationUiState.Idle
        _promptText.value = ""
    }

    /**
     * Limpia el estado de error y vuelve a editing.
     */
    fun clearError() {
        _uiState.value = GenerationUiState.Editing(
            prompt = _promptText.value,
            style = _selectedStyle.value,
            aspectRatio = _selectedAspectRatio.value
        )
    }
}

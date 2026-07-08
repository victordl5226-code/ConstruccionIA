package com.construccionia.feature.ocr.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ImageBytes
import com.construccionia.feature.ocr.domain.repository.OcrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val ocrRepository: OcrRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private val _editedText = MutableStateFlow("")
    val editedText: StateFlow<String> = _editedText.asStateFlow()

    /**
     * Reconce texto en una imagen.
     */
    fun recognizeText(image: ImageBytes) {
        _uiState.value = OcrUiState.Loading
        viewModelScope.launch {
            when (val result = ocrRepository.recognizeText(image)) {
                is AppResult.Success -> {
                    _uiState.value = OcrUiState.TextDetected(result.data)
                    _editedText.value = result.data.text
                }
                is AppResult.Error -> {
                    _uiState.value = OcrUiState.Error(result.exception)
                }
            }
        }
    }

    /**
     * Actualiza el texto editado manualmente.
     */
    fun updateEditedText(text: String) {
        _editedText.value = text
    }

    /**
     * Reinicia el estado.
     */
    fun reset() {
        _uiState.value = OcrUiState.Idle
        _editedText.value = ""
    }
}

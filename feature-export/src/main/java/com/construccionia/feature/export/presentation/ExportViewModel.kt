package com.construccionia.feature.export.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccionia.core.common.AppResult
import com.construccionia.feature.export.domain.model.ExportConfig
import com.construccionia.feature.export.domain.model.SlideContent
import com.construccionia.feature.export.domain.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepository: ExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _slides = MutableStateFlow<List<SlideContent>>(emptyList())
    val slides: StateFlow<List<SlideContent>> = _slides.asStateFlow()

    private val _config = MutableStateFlow(ExportConfig())
    val config: StateFlow<ExportConfig> = _config.asStateFlow()

    /**
     * Añade una diapositiva a la lista.
     */
    fun addSlide(slide: SlideContent) {
        _slides.value = _slides.value + slide
    }

    /**
     * Elimina una diapositiva por índice.
     */
    fun removeSlide(index: Int) {
        _slides.value = _slides.value.toMutableList().also { it.removeAt(index) }
    }

    /**
     * Actualiza la configuración de exportación.
     */
    fun updateConfig(newConfig: ExportConfig) {
        _config.value = newConfig
    }

    /**
     * Exporta las diapositivas a PowerPoint.
     */
    fun exportToPptx() {
        if (_slides.value.isEmpty()) return

        _uiState.value = ExportUiState.Exporting
        viewModelScope.launch {
            when (val result = exportRepository.exportToPptx(_slides.value, _config.value)) {
                is AppResult.Success -> {
                    _uiState.value = ExportUiState.Success(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = ExportUiState.Error(result.exception)
                }
            }
        }
    }

    /**
     * Reinicia el estado.
     */
    fun reset() {
        _uiState.value = ExportUiState.Idle
        _slides.value = emptyList()
    }
}

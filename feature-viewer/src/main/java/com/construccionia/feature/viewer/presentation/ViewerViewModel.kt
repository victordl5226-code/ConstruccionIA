package com.construccionia.feature.viewer.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccionia.core.common.AppResult
import com.construccionia.feature.viewer.domain.repository.ImageViewerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val imageViewerRepository: ImageViewerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Idle)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    /**
     * Carga una imagen por su ID.
     */
    fun loadImage(id: String) {
        _uiState.value = ViewerUiState.Loading
        viewModelScope.launch {
            when (val result = imageViewerRepository.getImage(id)) {
                is AppResult.Success -> {
                    val fileUri = saveBytesToTempFile(result.data.bytes, result.data.mimeType)
                    _uiState.value = ViewerUiState.Loaded(
                        imageBytes = result.data,
                        imageUrl = fileUri
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = ViewerUiState.Error(result.exception)
                }
            }
        }
    }

    /**
     * Carga la imagen de demostración desde recursos.
     */
    fun loadDemoImage() {
        _uiState.value = ViewerUiState.Loading
        viewModelScope.launch {
            when (val result = imageViewerRepository.loadDemoImage()) {
                is AppResult.Success -> {
                    val fileUri = saveBytesToTempFile(result.data.bytes, result.data.mimeType)
                    _uiState.value = ViewerUiState.Loaded(
                        imageBytes = result.data,
                        imageUrl = fileUri
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = ViewerUiState.Error(result.exception)
                }
            }
        }
    }

    /**
     * Reinicia el estado.
     */
    fun reset() {
        _uiState.value = ViewerUiState.Idle
    }

    /**
     * Guarda bytes de imagen en un archivo temporal y devuelve la URI.
     */
    private suspend fun saveBytesToTempFile(bytes: ByteArray, mimeType: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val extension = when {
                    mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                    mimeType.contains("webp") -> "webp"
                    else -> "png"
                }
                val tempDir = File(context.cacheDir, "viewer_cache")
                tempDir.mkdirs()
                val tempFile = File(tempDir, "viewer_image_${System.currentTimeMillis()}.$extension")
                tempFile.writeBytes(bytes)
                "file://${tempFile.absolutePath}"
            } catch (e: Exception) {
                null
            }
        }
    }
}

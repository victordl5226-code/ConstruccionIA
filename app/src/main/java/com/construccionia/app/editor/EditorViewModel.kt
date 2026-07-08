package com.construccionia.app.editor

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel del editor de vista explosionada.
 * Gestiona capas, anotaciones, zoom y modo explosión.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private var nextLayerId = 1L
    private var nextAnnotationId = 1L

    // ── Carga de imagen base ──

    /** Carga una imagen desde URI como base del editor. */
    fun loadImage(uri: String) {
        viewModelScope.launch {
            _editorState.value = _editorState.value.copy(isLoading = true)
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    // Intentar cargar desde URI
                    val inputStream = getApplication<Application>()
                        .contentResolver
                        .openInputStream(Uri.parse(uri))
                    inputStream?.use { BitmapFactory.decodeStream(it) }
                }

                if (bitmap != null) {
                    _editorState.value = _editorState.value.copy(
                        baseImageUri = uri,
                        baseBitmap = bitmap,
                        isLoading = false,
                        // Crear una capa inicial con la imagen completa
                        layers = listOf(
                            EditorLayer(
                                id = nextLayerId++,
                                name = "Capa base",
                                bitmap = bitmap,
                                imageSource = uri,
                                tintColor = androidx.compose.ui.graphics.Color(0xFF1565C0)
                            )
                        )
                    )
                } else {
                    _editorState.value = _editorState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo cargar la imagen"
                    )
                }
            } catch (e: Exception) {
                _editorState.value = _editorState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.localizedMessage}"
                )
            }
        }
    }

    // ── Gestión de capas ──

    /** Añade una nueva capa vacía. */
    fun addLayer(name: String, bitmapPath: String? = null) {
        val current = _editorState.value
        val newLayer = EditorLayer(
            id = nextLayerId++,
            name = name,
            imageSource = bitmapPath ?: "",
            tintColor = layerColors[current.layers.size % layerColors.size]
        )
        _editorState.value = current.copy(layers = current.layers + newLayer)
    }

    /** Elimina una capa. */
    fun removeLayer(layerId: Long) {
        val current = _editorState.value
        _editorState.value = current.copy(
            layers = current.layers.filter { it.id != layerId },
            selectedLayerId = if (current.selectedLayerId == layerId) null else current.selectedLayerId
        )
    }

    /** Selecciona una capa. */
    fun selectLayer(layerId: Long?) {
        _editorState.value = _editorState.value.copy(selectedLayerId = layerId)
    }

    /** Cambia la opacidad de una capa. */
    fun setLayerOpacity(layerId: Long, opacity: Float) {
        val current = _editorState.value
        _editorState.value = current.copy(
            layers = current.layers.map {
                if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0f, 1f)) else it
            }
        )
    }

    /** Muestra/oculta una capa. */
    fun toggleLayerVisibility(layerId: Long) {
        val current = _editorState.value
        _editorState.value = current.copy(
            layers = current.layers.map {
                if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
            }
        )
    }

    /** Cambia el nombre de una capa. */
    fun renameLayer(layerId: Long, newName: String) {
        val current = _editorState.value
        _editorState.value = current.copy(
            layers = current.layers.map {
                if (it.id == layerId) it.copy(name = newName) else it
            }
        )
    }

    /** Cambia el offset de explosión de una capa individual. */
    fun setLayerExplodeOffset(layerId: Long, offset: Float) {
        val current = _editorState.value
        _editorState.value = current.copy(
            layers = current.layers.map {
                if (it.id == layerId) it.copy(explodeOffset = offset) else it
            }
        )
    }

    // ── Control de explosión ──

    /** Cambia el nivel de explosión general (0..1). */
    fun setExplodeLevel(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        val current = _editorState.value
        // Distribuir el offset entre capas según el nivel
        val layerCount = current.layers.size
        _editorState.value = current.copy(
            explodeLevel = clamped,
            layers = current.layers.mapIndexed { index, layer ->
                // Cada capa se separa proporcionalmente a su índice
                val layerOffset = if (layerCount > 1) {
                    (index.toFloat() / (layerCount - 1).toFloat()) * clamped * 200f
                } else 0f
                layer.copy(explodeOffset = layerOffset)
            }
        )
    }

    // ── Zoom y paneo ──

    fun setZoom(zoom: Float) {
        _editorState.value = _editorState.value.copy(
            zoom = zoom.coerceIn(0.5f, 5f)
        )
    }

    fun setPanOffset(offset: androidx.compose.ui.geometry.Offset) {
        _editorState.value = _editorState.value.copy(panOffset = offset)
    }

    // ── Anotaciones ──

    /** Cambia el modo del editor. */
    fun setEditorMode(mode: EditorMode) {
        _editorState.value = _editorState.value.copy(editorMode = mode)
    }

    /** Añade una anotación de texto. */
    fun addTextAnnotation(text: String, position: androidx.compose.ui.geometry.Offset) {
        val annotation = LayerAnnotation.TextLabel(
            id = nextAnnotationId++,
            text = text,
            position = position
        )
        val current = _editorState.value
        _editorState.value = current.copy(
            annotations = current.annotations + annotation
        )
    }

    /** Añade una flecha. */
    fun addArrow(start: androidx.compose.ui.geometry.Offset, end: androidx.compose.ui.geometry.Offset) {
        val annotation = LayerAnnotation.Arrow(
            id = nextAnnotationId++,
            start = start,
            end = end
        )
        val current = _editorState.value
        _editorState.value = current.copy(
            annotations = current.annotations + annotation
        )
    }

    /** Añade un callout. */
    fun addCallout(text: String, target: androidx.compose.ui.geometry.Offset, label: androidx.compose.ui.geometry.Offset) {
        val annotation = LayerAnnotation.Callout(
            id = nextAnnotationId++,
            text = text,
            targetPoint = target,
            labelPosition = label
        )
        val current = _editorState.value
        _editorState.value = current.copy(
            annotations = current.annotations + annotation
        )
    }

    /** Elimina una anotación. */
    fun removeAnnotation(annotationId: Long) {
        val current = _editorState.value
        _editorState.value = current.copy(
            annotations = current.annotations.filter {
                when (it) {
                    is LayerAnnotation.TextLabel -> it.id != annotationId
                    is LayerAnnotation.Arrow -> it.id != annotationId
                    is LayerAnnotation.Callout -> it.id != annotationId
                }
            }
        )
    }

    /** Limpia el error. */
    fun clearError() {
        _editorState.value = _editorState.value.copy(errorMessage = null)
    }

    companion object {
        private val layerColors = listOf(
            androidx.compose.ui.graphics.Color(0xFF1565C0),
            androidx.compose.ui.graphics.Color(0xFFE53935),
            androidx.compose.ui.graphics.Color(0xFF43A047),
            androidx.compose.ui.graphics.Color(0xFFFB8C00),
            androidx.compose.ui.graphics.Color(0xFF8E24AA),
            androidx.compose.ui.graphics.Color(0xFF00ACC1),
            androidx.compose.ui.graphics.Color(0xFFD81B60),
            androidx.compose.ui.graphics.Color(0xFF6D4C41)
        )
    }
}

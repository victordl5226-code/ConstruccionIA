package com.construccionia.app.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.construccionia.app.data.models.Infographic
import com.construccionia.app.data.repository.ImageRepository
import com.construccionia.app.export.PdfExportHelper
import com.construccionia.app.export.PptExportHelper
import com.construccionia.app.ocr.OcrHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de Galería.
 * Gestiona la lista de infografías, OCR y exportación a PPT.
 *
 * Usa inyección de Hilt para obtener dependencias.
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    application: Application,
    private val repository: ImageRepository,
    private val ocrHelper: OcrHelper,
    private val pptHelper: PptExportHelper,
    private val pdfHelper: PdfExportHelper
) : AndroidViewModel(application) {

    // ── Estado de la galería ──
    private val _galleryState = MutableStateFlow(GalleryUiState())
    val galleryState: StateFlow<GalleryUiState> = _galleryState.asStateFlow()

    // ── Estado del OCR ──
    private val _ocrState = MutableStateFlow(OcrUiState())
    val ocrState: StateFlow<OcrUiState> = _ocrState.asStateFlow()

    // ── Estado de exportación ──
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // ── Evento de compartir ──
    private val _shareUriEvent = Channel<Uri>(Channel.BUFFERED)
    val shareUriEvent: Flow<Uri> = _shareUriEvent.receiveAsFlow()

    // ── Estado para renombrar ──
    var renameTarget: Infographic? by mutableStateOf(null)

    // ── Estado de refresco (pull-to-refresh) ──
    var isRefreshing by mutableStateOf(false)

    // ── Estado de búsqueda ──
    var searchQuery by mutableStateOf("")

    /** Lista de infografías filtrada por el término de búsqueda. */
    val filteredInfographics: List<Infographic>
        get() {
            val all = _galleryState.value.infographics
            val query = searchQuery
            return if (query.isBlank()) all
            else all.filter { it.name.contains(query, ignoreCase = true) }
        }

    init {
        // Observar cambios reactivos desde Room
        viewModelScope.launch {
            repository.getAllInfographicsFlow().collect { infographics ->
                _galleryState.value = _galleryState.value.copy(
                    isLoading = false,
                    infographics = infographics
                )
            }
        }
        // Carga inicial desde almacenamiento externo
        loadInfographics()
    }

    /** Carga la lista de infografías (sincroniza con MediaStore/filesystem). */
    fun loadInfographics() {
        viewModelScope.launch {
            _galleryState.value = _galleryState.value.copy(isLoading = true)
            repository.getAllInfographics() // esto sincroniza y Room emite el Flow
        }
    }

    /** Selecciona/deselecciona una infografía. */
    fun toggleSelection(infographic: Infographic) {
        val current = _galleryState.value.selectedIds
        val updated = if (infographic.id in current) {
            current - infographic.id
        } else {
            current + infographic.id
        }
        _galleryState.value = _galleryState.value.copy(selectedIds = updated)
    }

    /** Limpia la selección. */
    fun clearSelection() {
        _galleryState.value = _galleryState.value.copy(selectedIds = emptySet())
    }

    /** Elimina una infografía. */
    fun deleteInfographic(infographic: Infographic) {
        viewModelScope.launch {
            repository.deleteByPath(infographic.filePath)
        }
    }

    /** Comparte una infografía mediante Intent con el URI obtenido del repositorio. */
    fun shareInfographic(infographic: Infographic) {
        viewModelScope.launch {
            val uri = repository.getShareUri(infographic.filePath)
            _shareUriEvent.send(uri)
        }
    }

    /** Renombra una infografía. */
    fun renameInfographic(infographic: Infographic, newName: String) {
        viewModelScope.launch {
            repository.updateInfographic(infographic.copy(name = newName))
            renameTarget = null
        }
    }

    /** Recarga la galería desde el almacenamiento (pull-to-refresh). */
    suspend fun refreshGallery() {
        isRefreshing = true
        repository.getAllInfographics()
        isRefreshing = false
    }

    /** Actualiza el término de búsqueda. */
    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    // ── Conectividad ──

    /**
     * Verifica si hay conexión a internet.
     * Útil para prevenir operaciones que requieren red.
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ── OCR ──

    /**
     * Ejecuta OCR sobre la imagen seleccionada.
     * @param filePath Ruta absoluta de la imagen.
     */
    fun performOcr(filePath: String) {
        viewModelScope.launch {
            _ocrState.value = OcrUiState(isProcessing = true)

            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(filePath, options)

                val scaleOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(options, 2048, 2048)
                }
                val bitmap = BitmapFactory.decodeFile(filePath, scaleOptions)
                    ?: run {
                        _ocrState.value = OcrUiState(error = "No se pudo decodificar la imagen")
                        return@launch
                    }

                val result = ocrHelper.recognizeText(bitmap)
                bitmap.recycle()

                result.fold(
                    onSuccess = { text ->
                        _ocrState.value = OcrUiState(isProcessing = false, recognizedText = text)
                    },
                    onFailure = { error ->
                        _ocrState.value = OcrUiState(
                            isProcessing = false,
                            error = error.message ?: "Error en OCR"
                        )
                    }
                )
            } catch (e: Exception) {
                _ocrState.value = OcrUiState(isProcessing = false, error = "Error: ${e.localizedMessage}")
            }
        }
    }

    fun clearOcr() {
        _ocrState.value = OcrUiState()
    }

    fun updateEditedText(text: String) {
        _ocrState.value = _ocrState.value.copy(recognizedText = text)
    }

    // ── Exportación PPT ──

    fun exportSelectedToPpt() {
        val selectedFiles = _galleryState.value.selectedIds.mapNotNull { id ->
            _galleryState.value.infographics.find { it.id == id }?.filePath
        }

        if (selectedFiles.isEmpty()) {
            _exportState.value = ExportState.Error("Selecciona al menos una imagen")
            return
        }

        if (!isNetworkAvailable()) {
            _exportState.value = ExportState.Error(
                "No hay conexión a internet. La exportación a PPT requiere conectividad para procesar imágenes."
            )
            return
        }

        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            val uri = pptHelper.exportToPpt(selectedFiles)
            _exportState.value = if (uri != null) {
                ExportState.Success(uri)
            } else {
                ExportState.Error("Error al exportar la presentación")
            }
        }
    }

    fun exportSingleToPpt(filePath: String) {
        if (!isNetworkAvailable()) {
            _exportState.value = ExportState.Error(
                "No hay conexión a internet. La exportación a PPT requiere conectividad para procesar imágenes."
            )
            return
        }

        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            val uri = pptHelper.exportToPpt(listOf(filePath))
            _exportState.value = if (uri != null) {
                ExportState.Success(uri)
            } else {
                ExportState.Error("Error al exportar la presentación")
            }
        }
    }

    // ── Exportación PDF ──

    fun exportSelectedToPdf() {
        val selectedFiles = _galleryState.value.selectedIds.mapNotNull { id ->
            _galleryState.value.infographics.find { it.id == id }?.filePath
        }

        if (selectedFiles.isEmpty()) {
            _exportState.value = ExportState.Error("Selecciona al menos una imagen")
            return
        }

        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            val uri = pdfHelper.exportToPdf(selectedFiles)
            _exportState.value = if (uri != null) {
                ExportState.Success(uri)
            } else {
                ExportState.Error("Error al exportar PDF")
            }
        }
    }

    fun exportSingleToPdf(filePath: String) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            val uri = pdfHelper.exportSingleToPdf(filePath)
            _exportState.value = if (uri != null) {
                ExportState.Success(uri)
            } else {
                ExportState.Error("Error al exportar PDF")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        ocrHelper.close()
    }
}

// ── Estados de UI ──

data class GalleryUiState(
    val isLoading: Boolean = true,
    val infographics: List<Infographic> = emptyList(),
    val selectedIds: Set<Long> = emptySet()
)

data class OcrUiState(
    val isProcessing: Boolean = false,
    val recognizedText: String? = null,
    val error: String? = null
)

sealed class ExportState {
    data object Idle : ExportState()
    data object Exporting : ExportState()
    data class Success(val uri: Uri) : ExportState()
    data class Error(val message: String) : ExportState()
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    maxWidth: Int,
    maxHeight: Int
): Int {
    val rawHeight = options.outHeight
    val rawWidth = options.outWidth
    var inSampleSize = 1

    if (rawHeight > maxHeight || rawWidth > maxWidth) {
        val halfHeight = rawHeight / 2
        val halfWidth = rawWidth / 2
        while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

package com.construccionia.app.viewmodel

import android.app.Application
import timber.log.Timber
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.construccionia.app.webview.ImageEvent
import com.construccionia.app.webview.ImageJavaScriptInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel que gestiona el estado del WebView de Gemini y las descargas.
 * Usa inyección de Hilt.
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        const val GEMINI_URL = "https://gemini.google.com"
    }

    // ── Scope propio para el JSInterface ──
    private val jsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── JavaScriptInterface ──
    val jsInterface = ImageJavaScriptInterface(application, scope = jsScope)

    // ── Estados ──
    private val _webViewState = MutableStateFlow(WebViewUiState())
    val webViewState: StateFlow<WebViewUiState> = _webViewState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _detectedImages = MutableStateFlow<List<String>>(emptyList())
    val detectedImages: StateFlow<List<String>> = _detectedImages.asStateFlow()

    init {
        viewModelScope.launch {
            jsInterface.imageEvents.collect { event ->
                when (event) {
                    is ImageEvent.Detected -> {
                        Timber.d("Imagen detectada: ${event.imageUrl.take(80)}...")
                        _detectedImages.value = _detectedImages.value + event.imageUrl
                        _downloadState.value = DownloadState.Ready(event.imageUrl)
                    }
                    is ImageEvent.Downloaded -> {
                        _downloadState.value = if (event.success) {
                            DownloadState.Success(
                                message = event.message,
                                filePath = event.filePath ?: ""
                            )
                        } else {
                            DownloadState.Error(event.message)
                        }
                    }
                }
            }
        }
    }

    // ── Acciones ──

    fun onPageStarted(url: String) {
        _webViewState.value = _webViewState.value.copy(isLoading = true, currentUrl = url)
    }

    fun onPageFinished(url: String) {
        _webViewState.value = _webViewState.value.copy(isLoading = false, currentUrl = url)
    }

    fun onProgressChanged(progress: Int) {
        _webViewState.value = _webViewState.value.copy(loadingProgress = progress)
    }

    fun onWebViewError(errorCode: Int, description: String) {
        _webViewState.value = _webViewState.value.copy(
            isLoading = false,
            errorMessage = "Error $errorCode: $description"
        )
    }

    fun clearError() {
        _webViewState.value = _webViewState.value.copy(errorMessage = null)
    }

    fun downloadImage(imageUrl: String) {
        _downloadState.value = DownloadState.Downloading
        jsInterface.downloadImage(imageUrl)
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    fun reloadWebView() {
        _webViewState.value = _webViewState.value.copy(errorMessage = null, isLoading = true)
    }

    override fun onCleared() {
        super.onCleared()
        jsScope.cancel()
        Timber.d("WebViewViewModel cleared")
    }
}

// ── Estados de UI ──

data class WebViewUiState(
    val isLoading: Boolean = true,
    val loadingProgress: Int = 0,
    val currentUrl: String = "",
    val errorMessage: String? = null
)

sealed class DownloadState {
    data object Idle : DownloadState()
    data object Downloading : DownloadState()
    data class Ready(val imageUrl: String) : DownloadState()
    data class Success(val message: String, val filePath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

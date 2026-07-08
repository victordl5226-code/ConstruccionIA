package com.construccionia.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.construccionia.app.viewmodel.DownloadState
import com.construccionia.app.viewmodel.WebViewViewModel

/**
 * Pantalla principal que aloja el WebView con Google Gemini.
 * Muestra un FAB para descargar imágenes detectadas.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    viewModel: WebViewViewModel
) {
    val webViewState by viewModel.webViewState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estado para saber si hay una imagen lista para descargar
    val isImageReady = downloadState is DownloadState.Ready

    // Referencia al WebView para inyectar JS
    var webView: WebView? by remember { mutableStateOf(null) }

    // Mostrar Snackbar según estado de descarga
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetDownloadState()
            }
            is DownloadState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.resetDownloadState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isImageReady,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        val state = downloadState
                        if (state is DownloadState.Ready) {
                            viewModel.downloadImage(state.imageUrl)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Descargar imagen",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                            javaScriptCanOpenWindowsAutomatically = false
                            mediaPlaybackRequiresUserGesture = true
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            userAgentString = settings.userAgentString + " ConstruccionIA/1.0"
                        }

                        // WebViewClient personalizado
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                viewModel.onPageStarted(url ?: "")
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                viewModel.onPageFinished(url ?: "")
                                // Inyectar JS después de cada carga
                                view?.evaluateJavascript(
                                    viewModel.jsInterface.getInjectionScript(),
                                    null
                                )
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                viewModel.onWebViewError(
                                    error?.errorCode ?: -1,
                                    error?.description?.toString() ?: "Error"
                                )
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                viewModel.onProgressChanged(newProgress)
                            }
                        }

                        // Añadir JavaScriptInterface
                        addJavascriptInterface(
                            viewModel.jsInterface,
                            com.construccionia.app.webview.ImageJavaScriptInterface.JS_INTERFACE_NAME
                        )

                        // Cargar Gemini
                        loadUrl(WebViewViewModel.GEMINI_URL)

                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Indicador de carga superior
            if (webViewState.isLoading && webViewState.loadingProgress < 100) {
                LinearProgressIndicator(
                    progress = { webViewState.loadingProgress / 100f },
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Indicador de descarga
            if (downloadState is DownloadState.Downloading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Mensaje de error con botón de recarga
            if (webViewState.errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error al cargar",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = webViewState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        androidx.compose.material3.FilledTonalButton(
                            onClick = {
                                viewModel.clearError()
                                webView?.reload()
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("  Reintentar", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

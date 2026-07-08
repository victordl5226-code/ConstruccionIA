package com.construccionia.app.webview

import android.graphics.Bitmap
import android.net.http.SslError
import timber.log.Timber
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebViewClient personalizado para cargar Google Gemini.
 * Gestiona SSL, errores de red y notifica progreso.
 */
class GeminiWebViewClient(
    private val onPageStarted: (url: String) -> Unit = {},
    private val onPageFinished: (url: String) -> Unit = {},
    private val onProgressChanged: (progress: Int) -> Unit = {},
    private val onError: (errorCode: Int, description: String) -> Unit = { _, _ -> }
) : WebViewClient() {



    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let {
            Timber.d("Cargando: $it")
            onPageStarted(it)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let {
            Timber.d("Página cargada: $it")
            onPageFinished(it)
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        val errorCode = error?.errorCode ?: -1
        val description = error?.description?.toString() ?: "Error desconocido"
        Timber.e("Error WebView: $errorCode - $description")
        onError(errorCode, description)
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        // No permitir errores SSL en producción
        Timber.w("Error SSL: ${error?.toString()}")
        handler?.cancel()
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // Dejar que el WebView maneje la navegación normal
        return false
    }
}

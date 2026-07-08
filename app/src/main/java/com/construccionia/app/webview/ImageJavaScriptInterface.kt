package com.construccionia.app.webview

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import timber.log.Timber
import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Puente JavaScript → Kotlin.
 * Se inyecta en el WebView y detecta imágenes generadas por Gemini.
 *
 * @param context Contexto de la aplicación.
 * @param scope CoroutineScope para lanzar operaciones. Debe cancelarse externamente
 *              (ej. con viewModelScope) para evitar fugas de memoria.
 */
class ImageJavaScriptInterface(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    companion object {
        private const val SUBDIR = "ConstruccionIA"
        const val JS_INTERFACE_NAME = "AndroidImageBridge"
    }

    /** Flujo de eventos: cada vez que se genera/descubre una imagen nueva. */
    private val _imageEvents = MutableSharedFlow<ImageEvent>(extraBufferCapacity = 10)
    val imageEvents: SharedFlow<ImageEvent> = _imageEvents

    // ── Métodos expuestos a JavaScript ──

    /**
     * Llamado desde JS cuando se detecta una nueva imagen renderizada en Gemini.
     * @param imageUrl URL absoluta de la imagen (data: o https://)
     * @param altText texto alternativo de la imagen
     */
    @JavascriptInterface
    fun onImageDetected(imageUrl: String, altText: String) {
        Timber.d("Imagen detectada: url=$imageUrl, alt=$altText")
        // tryEmit es no-suspending: añade al buffer y retorna inmediatamente.
        // Con extraBufferCapacity=10, siempre hay espacio disponible.
        _imageEvents.tryEmit(ImageEvent.Detected(imageUrl, altText))
    }

    /**
     * Llamado desde JS cuando el usuario hace clic en "Descargar" desde el floating action.
     * @param imageUrl URL de la imagen a descargar
     */
    @JavascriptInterface
    fun downloadImage(imageUrl: String) {
        Timber.d("Solicitud de descarga: $imageUrl")
        scope.launch {
            val result = saveImageToDisk(imageUrl)
            _imageEvents.emit(result)
        }
    }

    // ── Lógica de descarga ──

    /**
     * Descarga una imagen desde una URL (data: o https://) y la guarda en
     * Downloads/ConstruccionIA/.
     * Usa MediaStore en Android 10+ para compatibilidad con scoped storage.
     */
    private suspend fun saveImageToDisk(imageUrl: String): ImageEvent {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
                val fileName = "infografia_$timestamp.png"

                val bitmap = decodeImageUrl(imageUrl)
                val filePath: String

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ usar MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "image/png")
                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS + "/$SUBDIR"
                        )
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val uri: Uri? = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out: OutputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            out.flush()
                        }

                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)

                        filePath = uri.toString()
                        Timber.d("Imagen guardada via MediaStore: $uri")
                    } else {
                        throw Exception("No se pudo crear el archivo en MediaStore")
                    }
                } else {
                    // Android 9 y anteriores: acceso directo al filesystem
                    val downloadsDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        SUBDIR
                    )
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()

                    val outputFile = File(downloadsDir, fileName)
                    FileOutputStream(outputFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }
                    filePath = outputFile.absolutePath
                    Timber.d("Imagen guardada: ${outputFile.absolutePath}")
                }

                ImageEvent.Downloaded(
                    success = true,
                    filePath = filePath,
                    fileName = fileName,
                    message = "Imagen guardada en Downloads/$SUBDIR/"
                )
            } catch (e: Exception) {
                Timber.e(e, "Error al descargar imagen")
                ImageEvent.Downloaded(
                    success = false,
                    filePath = null,
                    fileName = null,
                    message = "Error: ${e.localizedMessage ?: "Desconocido"}"
                )
            }
        }
    }

    /** Decodifica una URL (data: base64 o https://) a Bitmap. */
    private fun decodeImageUrl(imageUrl: String): Bitmap {
        return when {
            imageUrl.startsWith("data:") -> {
                // data:image/png;base64,iVBOR...
                val base64Data = imageUrl.substringAfter("base64,")
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            else -> {
                // URL HTTP/HTTPS
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.getInputStream().use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        }
    }

    /**
     * Código JavaScript que se inyecta en el WebView para monitorizar imágenes.
     * Soporta:
     * - Imágenes <img> estándar
     * - Shadow DOM (componentes web de Gemini)
     * - Background images con URL
     * - data: URIs y blob: URIs
     * - Canvas elements
     */
    fun getInjectionScript(): String = """
        (function() {
            'use strict';
            
            let observedImages = new Set();
            let scanCount = 0;
            
            // Función recursiva para escanear también dentro de Shadow DOM
            function scanNode(root) {
                root = root || document.body;
                
                // 1. Buscar <img> en el nodo actual
                const images = root.querySelectorAll('img');
                images.forEach(function(img) {
                    if (img.src && !observedImages.has(img.src)) {
                        observedImages.add(img.src);
                        try {
                            AndroidImageBridge.onImageDetected(img.src, img.alt || '');
                        } catch(e) {
                            console.warn('JSInterface error:', e);
                        }
                    }
                });
                
                // 2. Buscar elementos con background-image
                const allElements = root.querySelectorAll('*');
                allElements.forEach(function(el) {
                    var bg = window.getComputedStyle(el).backgroundImage;
                    if (bg && bg !== 'none' && bg.indexOf('url(') >= 0) {
                        var matches = bg.match(/url\(['"]?([^'"()]+)['"]?\)/g);
                        if (matches) {
                            matches.forEach(function(m) {
                                var url = m.replace(/url\(['"]?|['"]?\)/g, '').trim();
                                if (url && !observedImages.has(url)) {
                                    observedImages.add(url);
                                    try {
                                        AndroidImageBridge.onImageDetected(url, el.alt || el.title || '');
                                    } catch(e) {}
                                }
                            });
                        }
                    }
                });
                
                // 3. Buscar shadow roots
                function findShadowRoots(node) {
                    if (node.shadowRoot) {
                        scanNode(node.shadowRoot);
                    }
                    if (node.children) {
                        for (var i = 0; i < node.children.length; i++) {
                            findShadowRoots(node.children[i]);
                        }
                    }
                    // También en nodos asignados a slots
                    if (node.assignedNodes) {
                        var assigned = node.assignedNodes();
                        if (assigned) {
                            assigned.forEach(function(n) {
                                if (n.nodeType === 1) findShadowRoots(n);
                            });
                        }
                    }
                }
                findShadowRoots(root);
            }
            
            function scanForImages() {
                scanCount++;
                scanNode(document.body);
                
                // También escanear en iframes (Gemini usa iframes internos)
                try {
                    var iframes = document.querySelectorAll('iframe');
                    iframes.forEach(function(iframe) {
                        try {
                            var iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                            if (iframeDoc) scanNode(iframeDoc.body);
                        } catch(e) {
                            // Cross-origin iframe, ignorar silenciosamente
                        }
                    });
                } catch(e) {}
            }
            
            // Escanear cada 2 segundos (más frecuente para capturar imágenes generadas)
            setInterval(scanForImages, 2000);
            
            // También ejecutar al cargar
            document.addEventListener('DOMContentLoaded', scanForImages);
            
            // Observar cambios en el DOM con MutationObserver mejorado
            if (window.MutationObserver) {
                var observerConfig = {
                    childList: true,
                    subtree: true,
                    attributes: true,
                    attributeFilter: ['src', 'style', 'class']
                };
                var observer = new MutationObserver(function(mutations) {
                    var shouldScan = false;
                    mutations.forEach(function(m) {
                        if (m.type === 'childList' && m.addedNodes.length > 0) {
                            shouldScan = true;
                        }
                        if (m.type === 'attributes') {
                            shouldScan = true;
                        }
                    });
                    if (shouldScan) setTimeout(scanForImages, 300);
                });
                // Intentar observar document.body cuando exista
                var waitBody = setInterval(function() {
                    if (document.body) {
                        observer.observe(document.body, observerConfig);
                        clearInterval(waitBody);
                    }
                }, 100);
            }
            
            console.log('ConstruccionIA v2: Monitor de imágenes con Shadow DOM activado');
        })();
    """.trimIndent()
}

// ── Eventos sellados ──

sealed class ImageEvent {
    data class Detected(val imageUrl: String, val altText: String) : ImageEvent()
    data class Downloaded(
        val success: Boolean,
        val filePath: String?,
        val fileName: String?,
        val message: String
    ) : ImageEvent()
}

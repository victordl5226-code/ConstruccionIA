package com.construccionia.app.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper para exportar infografías a PDF.
 * Usa Android Canvas PdfDocument para generar PDF nativo sin dependencias externas.
 * Soporta Android 10+ (MediaStore) y legacy (File).
 */
@Singleton
class PdfExportHelper @Inject constructor(
    private val context: Context
) {
    /**
     * Exporta una lista de rutas de imágenes a un archivo PDF.
     * Cada imagen ocupa una página completa.
     * @param imagePaths Lista de rutas absolutas de imágenes
     * @return Uri del PDF generado, o null si falló
     */
    suspend fun exportToPdf(imagePaths: List<String>): Uri? = withContext(Dispatchers.IO) {
        try {
            if (imagePaths.isEmpty()) {
                Timber.w("No hay imágenes para exportar a PDF")
                return@withContext null
            }

            val document = PdfDocument()
            val pageWidth = 595  // A4 en puntos (72 dpi): 210mm
            val pageHeight = 842 // A4 en puntos (72 dpi): 297mm

            for ((index, path) in imagePaths.withIndex()) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap == null) {
                    Timber.w("No se pudo decodificar imagen: $path")
                    continue
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index).create()
                val page = document.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Escalar bitmap para que quepa en la página manteniendo proporción
                val scaleX = pageWidth.toFloat() / bitmap.width
                val scaleY = pageHeight.toFloat() / bitmap.height
                val scale = minOf(scaleX, scaleY)

                val scaledWidth = (bitmap.width * scale).toInt()
                val scaledHeight = (bitmap.height * scale).toInt()
                val offsetX = (pageWidth - scaledWidth) / 2
                val offsetY = (pageHeight - scaledHeight) / 2

                canvas.drawBitmap(bitmap, null, android.graphics.Rect(
                    offsetX, offsetY, offsetX + scaledWidth, offsetY + scaledHeight
                ), null)

                document.finishPage(page)
                bitmap.recycle()
            }

            // Guardar el documento
            val uri = saveDocument(document)
            document.close()
            uri
        } catch (e: Exception) {
            Timber.e(e, "Error al exportar PDF")
            null
        }
    }

    /**
     * Exporta una sola imagen a PDF.
     */
    suspend fun exportSingleToPdf(imagePath: String): Uri? {
        return exportToPdf(listOf(imagePath))
    }

    /**
     * Guarda el PdfDocument en el almacenamiento.
     * Usa MediaStore en Android 10+, File legacy en versiones anteriores.
     */
    private fun saveDocument(document: PdfDocument): Uri? {
        return try {
            val fileName = "ConstruccionIA_${System.currentTimeMillis()}.pdf"
            val mimeType = "application/pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Usar MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                resolver.openOutputStream(uri)?.use { outputStream ->
                    document.writeTo(outputStream)
                }

                // Marcar como no pendiente
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                uri
            } else {
                // Android 9 y anteriores - Guardar en archivo
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val appDir = File(downloadsDir, "ConstruccionIA")
                if (!appDir.exists()) appDir.mkdirs()

                val file = File(appDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    document.writeTo(outputStream)
                }

                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error al guardar PDF")
            null
        }
    }
}

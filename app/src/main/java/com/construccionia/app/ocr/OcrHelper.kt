package com.construccionia.app.ocr

import android.graphics.Bitmap
import timber.log.Timber
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Helper que envuelve Google ML Kit Text Recognition para extraer texto
 * de imágenes de infografías técnicas.
 */
class OcrHelper {



    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extrae texto de un Bitmap de forma síncrona (corrutina).
     * @return Result con el texto reconocido o un error.
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = Tasks.await(recognizer.process(image), 30, TimeUnit.SECONDS)
            val fullText = result.text

            if (fullText.isBlank()) {
                Result.failure(OcrException("No se detectó texto en la imagen"))
            } else {
                Timber.d("Texto reconocido (${fullText.length} caracteres)")
                Result.success(fullText)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error en OCR")
            Result.failure(OcrException("Error al procesar OCR: ${e.localizedMessage}", e))
        }
    }

    /** Libera los recursos del recognizer. */
    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            Timber.w(e, "Error al cerrar recognizer")
        }
    }
}

class OcrException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

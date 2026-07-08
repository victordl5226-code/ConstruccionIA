package com.construccionia.feature.ocr.data.mlkit

import android.graphics.Bitmap
import com.construccionia.core.common.ProcessingException
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper alrededor del TextRecognizer de ML Kit.
 * Proporciona una interfaz limpia para el reconocimiento de texto
 * usando corrutinas en lugar de callbacks.
 */
@Singleton
class MlKitTextRecognizer @Inject constructor() {

    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    /**
     * Reconce texto en un bitmap usando ML Kit.
     * Convierte la Task asíncrona de ML Kit en una función suspend.
     *
     * @param bitmap Imagen a procesar
     * @return Resultado del reconocimiento con el texto extraído
     */
    suspend fun recognize(bitmap: Bitmap): Text = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val task = recognizer.process(inputImage)
            Tasks.await(task)
        } catch (e: Exception) {
            throw ProcessingException("Error al reconocer texto: ${e.message}", e)
        }
    }

    /**
     * Libera los recursos del recognizer.
     */
    fun close() {
        recognizer.close()
    }
}

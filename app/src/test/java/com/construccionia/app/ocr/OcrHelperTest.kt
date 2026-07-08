package com.construccionia.app.ocr

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests para [OcrHelper].
 *
 * OcrHelper utiliza Google ML Kit Text Recognition, que es una dependencia
 * de Android y no está disponible en tests unitarios puros. Por lo tanto,
 * estos tests verifican el manejo de errores y el comportamiento cuando
 * ML Kit no está disponible (lo que resulta en Result.failure).
 *
 * Para pruebas completas de OCR se requiere un test instrumental en emulador.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OcrHelperTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var ocrHelper: OcrHelper

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ocrHelper = OcrHelper()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ocrHelper.close()
    }

    // ── Manejo de bitmap nulo (simulado) ──

    @Test
    fun `recognizeText with empty bitmap returns failure`() = runTest {
        // Crear un bitmap válido pero vacío (1x1)
        // ML Kit no está disponible en tests unitarios, por lo que
        // recognizer.process() lanzará una excepción que se captura
        // y retorna como Result.failure
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)

        val result = ocrHelper.recognizeText(bitmap)

        assertTrue(
            "recognizeText debe fallar porque ML Kit no está disponible en tests unitarios",
            result.isFailure
        )

        // Verificar que la excepción es del tipo OcrException
        val exception = result.exceptionOrNull()
        assertNotNull("Debe haber una excepción", exception)
        assertTrue(
            "La excepción debe ser OcrException o una subclase",
            exception is OcrException || exception?.message?.contains("OCR") == true ||
                    exception?.message?.contains("Error") == true
        )

        bitmap.recycle()
    }

    @Test
    fun `recognizeText with large bitmap handles error gracefully`() = runTest {
        // Crear un bitmap de tamaño considerable
        val bitmap = Bitmap.createBitmap(1024, 768, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLACK)

        val result = ocrHelper.recognizeText(bitmap)

        assertTrue(
            "Debe fallar porque ML Kit no está disponible en test unitario",
            result.isFailure
        )

        bitmap.recycle()
    }

    // ── Manejo de error ──

    @Test
    fun `recognizeText returns OcrException on failure`() = runTest {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val result = ocrHelper.recognizeText(bitmap)

        assertTrue("Result debe ser failure", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(
            "Error debe ser OcrException",
            error is OcrException
        )
        assertTrue(
            "El mensaje debe indicar error en OCR",
            error?.message?.contains("OCR", ignoreCase = true) == true ||
                    error?.message?.contains("Error", ignoreCase = true) == true
        )

        bitmap.recycle()
    }

    @Test
    fun `recognizeText with photo-like bitmap returns failure gracefully`() = runTest {
        // Simular un bitmap más realista con muchos píxeles
        val width = 640
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Dibujar algunos patrones simples para simular una imagen
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 20f
        }
        canvas.drawText("Test", 10f, 30f, paint)

        val result = ocrHelper.recognizeText(bitmap)

        // Sin ML Kit real, debe fallar
        assertTrue("Debe fallar sin ML Kit", result.isFailure)
        assertTrue("Excepción debe ser OcrException", result.exceptionOrNull() is OcrException)

        bitmap.recycle()
    }

    // ── Comportamiento de close ──

    @Test
    fun `close multiple times does not throw`() = runTest {
        // Llamar a close varias veces debe ser seguro
        ocrHelper.close()
        ocrHelper.close() // Segunda llamada no debe lanzar excepción
    }

    @Test
    fun `recognizeText after close handles error`() = runTest {
        // Cerrar el helper
        ocrHelper.close()

        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val result = ocrHelper.recognizeText(bitmap)

        // Después de close() el recognizer puede estar cerrado
        // El resultado debe ser failure (no una excepción no capturada)
        assertTrue("Debe ser failure incluso después de close", result.isFailure)

        bitmap.recycle()
    }

    // ── Result type ──

    @Test
    fun `recognizeText returns Result type`() = runTest {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        val result = ocrHelper.recognizeText(bitmap)

        // Verificar que el tipo de retorno es Result<String>
        assertTrue("Result debe ser de tipo Result<String>", result is Result<String>)
        assertTrue(result.isFailure)

        bitmap.recycle()
    }

    @Test
    fun `recognizeText failure contains OcrException with message`() = runTest {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        val result = ocrHelper.recognizeText(bitmap)
        val exception = result.exceptionOrNull() as? OcrException

        assertNotNull("Debe ser OcrException", exception)
        assertNotNull("El mensaje no debe ser nulo", exception?.message)
        assertTrue(
            "El mensaje debe contener información del error",
            exception?.message?.isNotEmpty() == true
        )

        bitmap.recycle()
    }

    // ── Creación de OcrHelper ──

    @Test
    fun `ocrHelper is created successfully`() = runTest {
        val helper = OcrHelper()
        assertNotNull("OcrHelper debe crearse sin errores", helper)
        helper.close()
    }
}

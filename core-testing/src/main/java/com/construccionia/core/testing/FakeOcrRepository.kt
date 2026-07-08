package com.construccionia.core.testing

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ImageBytes
import com.construccionia.feature.ocr.domain.model.OcrResult
import com.construccionia.feature.ocr.domain.model.TextBlock
import com.construccionia.feature.ocr.domain.repository.OcrRepository

/**
 * Fake de [OcrRepository] para pruebas.
 * Simula el reconocimiento de texto sin ML Kit real.
 */
class FakeOcrRepository : OcrRepository {

    private var shouldFail = false

    fun setShouldFail(fail: Boolean) {
        shouldFail = fail
    }

    override suspend fun recognizeText(image: ImageBytes): AppResult<OcrResult> {
        if (shouldFail) {
            return AppResult.Error(
                com.construccionia.core.common.ProcessingException("Error simulado de OCR")
            )
        }

        val fakeResult = OcrResult(
            text = "Texto simulado de prueba para la imagen de construcción.",
            textBlocks = listOf(
                TextBlock(
                    text = "Texto simulado de prueba",
                    boundingBox = android.graphics.Rect(10, 10, 200, 50),
                    confidence = 0.95f,
                    lines = listOf("Texto simulado", "de prueba")
                )
            ),
            confidence = 0.95f,
            processingTimeMs = 150L
        )
        return AppResult.Success(fakeResult)
    }

    override suspend fun isOcrAvailable(): Boolean = true
}

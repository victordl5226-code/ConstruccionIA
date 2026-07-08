package com.construccionia.feature.ocr.domain.repository

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ImageBytes
import com.construccionia.feature.ocr.domain.model.OcrResult

/**
 * Repositorio para el reconocimiento óptico de caracteres (OCR).
 */
interface OcrRepository {
    /**
     * Reconce texto en una imagen.
     */
    suspend fun recognizeText(image: ImageBytes): AppResult<OcrResult>

    /**
     * Verifica si el dispositivo soporta OCR.
     */
    suspend fun isOcrAvailable(): Boolean
}

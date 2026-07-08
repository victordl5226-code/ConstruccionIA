package com.construccionia.feature.ocr.domain.model

/**
 * Resultado completo del reconocimiento OCR.
 */
data class OcrResult(
    val text: String,
    val textBlocks: List<TextBlock>,
    val confidence: Float,
    val processingTimeMs: Long
)

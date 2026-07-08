package com.construccionia.feature.ocr.domain.model

/**
 * Bloque de texto individual detectado por OCR.
 */
data class TextBlock(
    val text: String,
    val boundingBox: BoundingBox,
    val confidence: Float,
    val lines: List<String>
)

/**
 * Representa un rectángulo delimitador sin depender de android.graphics.Rect.
 * Úsalo en la capa domain para mantenerla libre de dependencias Android.
 */
data class BoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

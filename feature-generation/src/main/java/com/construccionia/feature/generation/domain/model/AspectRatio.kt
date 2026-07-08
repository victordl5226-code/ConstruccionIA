package com.construccionia.feature.generation.domain.model

/**
 * Proporciones de aspecto para las imágenes generadas.
 */
enum class AspectRatio(val displayName: String, val width: Int, val height: Int) {
    SQUARE_1_1("1:1", 1024, 1024),
    LANDSCAPE_4_3("4:3", 1024, 768),
    LANDSCAPE_16_9("16:9", 1280, 720),
    PORTRAIT_3_4("3:4", 768, 1024),
    PORTRAIT_9_16("9:16", 720, 1280)
}

package com.construccionia.core.network

/**
 * Constantes de API para servicios remotos.
 */
object ApiConstants {
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"
    const val GEMINI_MODEL_ID = "gemini-1.5-pro"
    const val GEMINI_API_KEY_ENV = "GEMINI_API_KEY"

    // Timeouts en segundos
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 120L
    const val WRITE_TIMEOUT = 120L

    // Límites
    const val MAX_PROMPT_LENGTH = 5000
    const val MAX_IMAGE_SIZE_MB = 20
}

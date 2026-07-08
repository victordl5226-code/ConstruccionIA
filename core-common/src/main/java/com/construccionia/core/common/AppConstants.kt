package com.construccionia.core.common

/**
 * Constantes globales de la aplicación compartidas entre capas.
 * Colocar aquí constantes que necesiten ser accedidas desde domain sin
 * depender de módulos de infraestructura (como core-network).
 */
object AppConstants {
    /** Longitud máxima del prompt para la API de generación */
    const val MAX_PROMPT_LENGTH = 5000
}

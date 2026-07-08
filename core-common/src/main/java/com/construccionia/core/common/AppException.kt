package com.construccionia.core.common

/**
 * Jerarquía de errores de la aplicación.
 */
sealed class AppException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/** Error de red (sin conexión, timeout, etc.) */
class NetworkException(
    message: String = "Error de conexión de red",
    cause: Throwable? = null
) : AppException(message, cause)

/** Error de autenticación (API key inválida, etc.) */
class AuthenticationException(
    message: String = "Error de autenticación",
    cause: Throwable? = null
) : AppException(message, cause)

/** Error del servidor remoto (500, 503, etc.) */
class ServerException(
    val code: Int,
    message: String = "Error del servidor (HTTP $code)",
    cause: Throwable? = null
) : AppException(message, cause)

/** Error de entrada inválida */
class InvalidInputException(
    message: String = "Entrada inválida",
    cause: Throwable? = null
) : AppException(message, cause)

/** Error al procesar datos localmente */
class ProcessingException(
    message: String = "Error al procesar datos",
    cause: Throwable? = null
) : AppException(message, cause)

/** Error de almacenamiento */
class StorageException(
    message: String = "Error de almacenamiento",
    cause: Throwable? = null
) : AppException(message, cause)

/** Error desconocido */
class UnknownException(
    message: String = "Error desconocido",
    cause: Throwable? = null
) : AppException(message, cause)

/**
 * Convierte una excepción de aplicación a un mensaje amigable para el usuario.
 * Úsalo en las pantallas en lugar de mostrar el mensaje técnico raw.
 */
fun AppException.toUserMessage(): String = when (this) {
    is NetworkException -> "No hay conexión a internet. Verifica tu red e intenta de nuevo."
    is AuthenticationException -> "Error de autenticación. La API key no es válida o ha expirado."
    is ServerException -> when {
        code == 429 -> "Has alcanzado el límite de solicitudes. Espera un momento e intenta de nuevo."
        code in 500..599 -> "El servidor está experimentando problemas. Intenta más tarde."
        else -> "Error del servidor (código $code). Intenta de nuevo."
    }
    is InvalidInputException -> "El texto ingresado no es válido. Revísalo e intenta de nuevo."
    is ProcessingException -> "No se pudo procesar la solicitud. Intenta con una imagen o texto diferente."
    is StorageException -> "No se pudo guardar el archivo. Verifica el espacio disponible."
    is UnknownException -> "Ocurrió un error inesperado. Intenta de nuevo."
    else -> "Ocurrió un error. Intenta de nuevo."
}

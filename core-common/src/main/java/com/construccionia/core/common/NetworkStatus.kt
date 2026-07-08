package com.construccionia.core.common

/**
 * Representa el estado de conectividad de red del dispositivo.
 */
sealed class NetworkStatus {
    data object Available : NetworkStatus()
    data object Unavailable : NetworkStatus()
    data class Limited(val reason: String = "Conexión restringida") : NetworkStatus()
}

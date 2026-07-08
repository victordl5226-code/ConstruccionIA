package com.construccionia.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Estado de conectividad de red.
 */
enum class ConnectivityStatus {
    AVAILABLE,    // Hay internet
    UNAVAILABLE,  // No hay internet
    METERED       // Datos móviles / conexión medida
}

/**
 * Observable que emite cambios en la conectividad de red.
 * Usa ConnectivityManager.NetworkCallback para recibir cambios en tiempo real.
 */
class ConnectivityObserver(private val context: Context) {

    val observe: Flow<ConnectivityStatus> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(ConnectivityStatus.AVAILABLE)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(ConnectivityStatus.UNAVAILABLE)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val isMetered = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                ).not()

                when {
                    !hasInternet -> trySend(ConnectivityStatus.UNAVAILABLE)
                    isMetered -> trySend(ConnectivityStatus.METERED)
                    else -> trySend(ConnectivityStatus.AVAILABLE)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        // Emitir estado actual inmediatamente
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        val currentStatus = when {
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true -> {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
                    ConnectivityStatus.AVAILABLE
                else
                    ConnectivityStatus.METERED
            }
            else -> ConnectivityStatus.UNAVAILABLE
        }
        trySend(currentStatus)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}

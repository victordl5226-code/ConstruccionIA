package com.construccionia.core.testing

import com.construccionia.core.common.NetworkStatus

/**
 * Fake para observar la conectividad en pruebas.
 * Permite simular estados de red.
 */
class FakeConnectivityObserver {

    private var currentStatus: NetworkStatus = NetworkStatus.Available

    fun setStatus(status: NetworkStatus) {
        currentStatus = status
    }

    fun getCurrentStatus(): NetworkStatus = currentStatus
}

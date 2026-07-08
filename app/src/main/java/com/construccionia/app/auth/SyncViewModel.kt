package com.construccionia.app.auth

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import timber.log.Timber
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para controlar la sincronización con Firebase.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    application: Application,
    private val syncService: SyncService,
    private val storageService: StorageService
) : AndroidViewModel(application) {



    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    private val _storageUsage = MutableStateFlow<String>("—")
    val storageUsage: StateFlow<String> = _storageUsage.asStateFlow()

    init {
        loadLastSync()
        loadStorageUsage()
    }

    /**
     * Verifica si hay conexión a internet antes de sincronizar.
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Inicia sincronización completa (metadatos + imágenes). */
    fun syncNow() {
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.Error(
                "No hay conexión a internet. Conéctate a una red para sincronizar."
            )
            return
        }

        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            Timber.d("Iniciando sincronización...")
            val result = syncService.syncAll(uploadImages = true)
            _syncState.value = result

            if (result is SyncState.Success) {
                loadLastSync()
                loadStorageUsage()
            }
        }
    }

    /** Solo descarga metadatos desde la nube. */
    fun downloadFromCloud() {
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.Error(
                "No hay conexión a internet. Conéctate a una red para descargar."
            )
            return
        }

        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = syncService.downloadMetadata()
            _syncState.value = result
        }
    }

    /** Carga la fecha del último sincronización. */
    private fun loadLastSync() {
        viewModelScope.launch {
            val time = syncService.getLastSyncTime()
            _lastSyncTime.value = if (time != null) {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(time))
            } else {
                null
            }
        }
    }

    /** Carga el uso de almacenamiento. */
    private fun loadStorageUsage() {
        viewModelScope.launch {
            val result = storageService.getStorageUsage()
            result.onSuccess { bytes ->
                _storageUsage.value = if (bytes < 1024 * 1024) {
                    "${bytes / 1024} KB"
                } else {
                    "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
                }
            }
        }
    }

    /** Limpia el estado de sincronización. */
    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    fun isLoggedIn(): Boolean = syncService.isLoggedIn()
}

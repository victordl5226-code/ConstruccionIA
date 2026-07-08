package com.construccionia.app.auth

import android.app.Application
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
 * ViewModel de autenticación.
 * Gestiona login con Google, email, y estado de sesión.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val app: Application,
    private val authRepository: AuthRepository
) : AndroidViewModel(app) {


    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        // Escuchar cambios de autenticación
        authRepository.onAuthStateChanged { state ->
            _authState.value = state
        }
    }

    /** Inicia sesión con Google (token del ID). */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result = authRepository.signInWithGoogle(idToken)
                result.fold(
                    onSuccess = { user ->
                        Timber.d("Login Google exitoso: ${user.displayName}")
                        SyncWorker.schedule(app)
                    },
                    onFailure = { error ->
                        _authState.value = AuthState.Error(error.message ?: "Error al iniciar sesión")
                    }
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** Inicia sesión con email y contraseña. */
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result = authRepository.signInWithEmail(email, password)
                result.fold(
                    onSuccess = {
                        Timber.d("Login email exitoso")
                        SyncWorker.schedule(app)
                    },
                    onFailure = { error ->
                        _authState.value = AuthState.Error(error.message ?: "Error al iniciar sesión")
                    }
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** Registra un nuevo usuario. */
    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result = authRepository.registerWithEmail(email, password)
                result.fold(
                    onSuccess = {
                        Timber.d("Registro exitoso")
                        SyncWorker.schedule(app)
                    },
                    onFailure = { error ->
                        _authState.value = AuthState.Error(error.message ?: "Error al registrarse")
                    }
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** Cierra sesión. */
    fun signOut() {
        SyncWorker.cancel(app)
        authRepository.signOut()
    }

    /** Envía email de restablecimiento de contraseña. */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result = authRepository.sendPasswordReset(email)
                result.fold(
                    onSuccess = { _authState.value = AuthState.Unauthenticated },
                    onFailure = { error ->
                        _authState.value = AuthState.Error(error.message ?: "Error")
                    }
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** Limpia el estado de error. */
    fun clearError() {
        _authState.value = when (val current = _authState.value) {
            is AuthState.Error -> AuthState.Unauthenticated
            else -> current
        }
    }
}

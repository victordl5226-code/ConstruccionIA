package com.construccionia.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Pantalla de cuenta: inicio de sesión o panel de sincronización.
 * Muestra login cuando no autenticado, dashboard de sync cuando sí.
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    syncViewModel: SyncViewModel,
    onAuthenticated: () -> Unit,
    onNavigateToDashboard: () -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val isProcessing by authViewModel.isProcessing.collectAsState()
    val syncState by syncViewModel.syncState.collectAsState()
    val lastSync by syncViewModel.lastSyncTime.collectAsState()
    val storageUsage by syncViewModel.storageUsage.collectAsState()

    // ── Autenticado: mostrar panel de sincronización ──
    if (authState is AuthState.Authenticated) {
        val user = (authState as AuthState.Authenticated).user

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Avatar / nombre
            Text(
                text = user.displayName ?: "Usuario",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            user.email?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Tarjeta de sincronización ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Sincronización en la nube",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Último sync
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (lastSync != null) "Última sincronización: $lastSync"
                            else "Nunca sincronizado",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Almacenamiento
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null, modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Almacenamiento usado: $storageUsage",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Barra de progreso durante sync
                    if (syncState is SyncState.Syncing) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Sincronizando…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Resultado
                    if (syncState is SyncState.Success) {
                        val result = syncState as SyncState.Success
                        Text(
                            text = "✅ ${result.itemsUploaded} items sincronizados, " +
                                    "${result.imagesUploaded} imágenes subidas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (syncState is SyncState.Error) {
                        Text(
                            text = "❌ ${(syncState as SyncState.Error).message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Botones de acción
                    Button(
                        onClick = { syncViewModel.syncNow() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = syncState !is SyncState.Syncing
                    ) {
                        Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sincronizar ahora")
                    }

                    OutlinedButton(
                        onClick = { syncViewModel.downloadFromCloud() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = syncState !is SyncState.Syncing
                    ) {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Descargar desde la nube")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Historial de sincronización ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Historial de sincronización",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Última sincronización
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (lastSync != null) "Última sincronización: $lastSync"
                            else "Nunca sincronizado",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Estado actual
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null, modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        val statusText = when (syncState) {
                            is SyncState.Idle -> "En espera"
                            is SyncState.Syncing -> "Sincronizando…"
                            is SyncState.Success -> "Completado"
                            is SyncState.Error -> "Error"
                        }
                        Text(
                            text = "Estado: $statusText",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Botón Ver Dashboard
                    Button(
                        onClick = onNavigateToDashboard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ver Dashboard")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Cerrar sesión
            OutlinedButton(
                onClick = { authViewModel.signOut() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
        return
    }

    // ── No autenticado: mostrar formulario de login ──
    AuthLoginForm(
        authViewModel = authViewModel,
        authState = authState,
        isProcessing = isProcessing
    )
}

/**
 * Formulario de inicio de sesión / registro.
 */
@Composable
private fun AuthLoginForm(
    authViewModel: AuthViewModel,
    authState: AuthState,
    isProcessing: Boolean
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("ConstruccionIA", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (isRegistering) "Crea tu cuenta para sincronizar proyectos"
                    else "Inicia sesión para sincronizar tus proyectos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (isRegistering) authViewModel.registerWithEmail(email, password)
                        else authViewModel.signInWithEmail(email, password)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = email.isNotBlank() && password.isNotBlank() && !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isRegistering) "Crear cuenta" else "Iniciar sesión")
                    }
                }

                TextButton(onClick = { isRegistering = !isRegistering }) {
                    Text(if (isRegistering) "¿Ya tienes cuenta? Inicia sesión"
                    else "¿No tienes cuenta? Regístrate")
                }
                TextButton(onClick = { showForgotPassword = true; forgotEmail = email }) {
                    Text("¿Olvidaste tu contraseña?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (showForgotPassword) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showForgotPassword = false },
                        title = { Text("Restablecer contraseña") },
                        text = {
                            OutlinedTextField(
                                value = forgotEmail, onValueChange = { forgotEmail = it },
                                label = { Text("Correo") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (forgotEmail.isNotBlank()) {
                                    authViewModel.sendPasswordReset(forgotEmail)
                                    showForgotPassword = false
                                }
                            }) { Text("Enviar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showForgotPassword = false }) { Text("Cancelar") }
                        }
                    )
                }

                if (authState is AuthState.Error) {
                    Text((authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { authViewModel.clearError() }) { Text("Reintentar") }
                }
            }
        }
    }
}

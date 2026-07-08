package com.construccionia.app.auth

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Datos del usuario autenticado.
 */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: Uri?
)

/**
 * Estado de autenticación.
 */
sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Repositorio de autenticación con Firebase Auth.
 * Soporta login con Google y Email/Password.
 */
@Singleton
class AuthRepository @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /** Escucha cambios de estado de autenticación. */
    fun onAuthStateChanged(callback: (AuthState) -> Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            callback(if (user != null) {
                AuthState.Authenticated(user.toAuthUser())
            } else {
                AuthState.Unauthenticated
            })
        }
    }

    /** Obtiene el usuario actual (si existe). */
    fun getCurrentUser(): AuthUser? = auth.currentUser?.toAuthUser()

    /** ¿Hay un usuario autenticado? */
    fun isLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Inicia sesión con Google mediante token de ID.
     * @param idToken Token de ID de Google Sign-In.
     */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Usuario nulo tras autenticación")

            // Guardar usuario en Firestore (primera vez)
            saveUserToFirestore(firebaseUser)

            Result.success(firebaseUser.toAuthUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inicia sesión con email y contraseña.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Usuario nulo")
            Result.success(firebaseUser.toAuthUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registra un nuevo usuario con email y contraseña.
     */
    suspend fun registerWithEmail(email: String, password: String): Result<AuthUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Usuario nulo")

            // Guardar en Firestore
            saveUserToFirestore(firebaseUser)

            Result.success(firebaseUser.toAuthUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Cierra sesión. */
    fun signOut() {
        auth.signOut()
    }

    /** Envía email de restablecimiento de contraseña. */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Elimina la cuenta del usuario actual. */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No hay usuario autenticado")
            // Eliminar datos de Firestore
            firestore.collection("users").document(user.uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Privado ──

    /** Guarda/actualiza el perfil del usuario en Firestore. */
    private suspend fun saveUserToFirestore(firebaseUser: FirebaseUser) {
        val userData = hashMapOf(
            "uid" to firebaseUser.uid,
            "displayName" to (firebaseUser.displayName ?: ""),
            "email" to (firebaseUser.email ?: ""),
            "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
            "lastLogin" to System.currentTimeMillis()
        )
        firestore.collection("users")
            .document(firebaseUser.uid)
            .set(userData)
            .await()
    }

    /** Convierte FirebaseUser a AuthUser. */
    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = this.uid,
        displayName = this.displayName,
        email = this.email,
        photoUrl = this.photoUrl
    )
}

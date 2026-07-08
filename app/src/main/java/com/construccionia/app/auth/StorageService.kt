package com.construccionia.app.auth

import android.content.Context
import android.net.Uri
import timber.log.Timber
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resultado de una operación de almacenamiento.
 */
sealed class StorageResult {
    data class Success(val downloadUrl: String) : StorageResult()
    data class Progress(val bytes: Long, val totalBytes: Long) : StorageResult()
    data class Error(val message: String) : StorageResult()
}

/**
 * Servicio de almacenamiento en Firebase Storage.
 * Sube y descarga las imágenes de infografías al bucket del proyecto.
 *
 * Estructura en Storage:
 *   users/{uid}/infographics/{filename}
 */
@Singleton
class StorageService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PATH_USERS = "users"
        private const val PATH_INFOGRAPHICS = "infographics"
    }

    private val auth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    /** Reference base para el usuario autenticado. */
    private val userRef: StorageReference?
        get() {
            val uid = auth.currentUser?.uid ?: return null
            return storage.reference.child(PATH_USERS).child(uid).child(PATH_INFOGRAPHICS)
        }

    /**
     * Sube una imagen desde un URI local a Firebase Storage.
     * @param imageUri URI de la imagen en el dispositivo.
     * @param fileName Nombre con el que se guardará en Storage.
     * @return URL de descarga pública o error.
     */
    suspend fun uploadImage(imageUri: Uri, fileName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val ref = userRef
                    ?: return@withContext Result.failure(Exception("Usuario no autenticado"))

                val imageRef = ref.child(fileName)

                // Subir archivo
                val uploadTask = imageRef.putFile(imageUri)
                uploadTask.await()

                // Obtener URL de descarga
                val downloadUrl = imageRef.downloadUrl.await()

                Timber.d("Imagen subida: $downloadUrl")
                Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                Timber.e(e, "Error al subir imagen")
                Result.failure(e)
            }
        }
    }

    /**
     * Sube una imagen desde un archivo local.
     */
    suspend fun uploadFromFile(file: File, fileName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val ref = userRef
                    ?: return@withContext Result.failure(Exception("Usuario no autenticado"))

                val imageRef = ref.child(fileName)
                val uploadTask = imageRef.putFile(Uri.fromFile(file))
                uploadTask.await()

                val downloadUrl = imageRef.downloadUrl.await()
                Timber.d("Imagen subida desde archivo: $downloadUrl")
                Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                Timber.e(e, "Error al subir archivo")
                Result.failure(e)
            }
        }
    }

    /**
     * Descarga una imagen desde Firebase Storage a un archivo local temporal.
     * @param downloadUrl URL completa de descarga.
     * @return Archivo temporal con la imagen descargada.
     */
    suspend fun downloadImage(downloadUrl: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File.createTempFile(
                    "cloud_",
                    ".png",
                    context.cacheDir
                )

                val ref = storage.getReferenceFromUrl(downloadUrl)
                ref.getFile(file).await()

                Timber.d("Imagen descargada: ${file.absolutePath}")
                Result.success(file)
            } catch (e: Exception) {
                Timber.e(e, "Error al descargar imagen")
                Result.failure(e)
            }
        }
    }

    /**
     * Lista todas las imágenes del usuario en Storage.
     * @return Lista de nombres de archivo con sus URLs de descarga.
     */
    suspend fun listUserImages(): Result<List<Pair<String, String>>> {
        return withContext(Dispatchers.IO) {
            try {
                val ref = userRef
                    ?: return@withContext Result.failure(Exception("Usuario no autenticado"))

                val items = ref.listAll().await()
                val result = mutableListOf<Pair<String, String>>()

                for (item in items.items) {
                    val url = item.downloadUrl.await()
                    result.add(item.name to url.toString())
                }

                Result.success(result)
            } catch (e: Exception) {
                Timber.e(e, "Error al listar imágenes")
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina una imagen de Storage por su nombre de archivo.
     */
    suspend fun deleteImage(fileName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val ref = userRef
                    ?: return@withContext Result.failure(Exception("Usuario no autenticado"))

                ref.child(fileName).delete().await()
                Timber.d("Imagen eliminada: $fileName")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error al eliminar imagen")
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene el espacio utilizado por el usuario (aproximado).
     */
    suspend fun getStorageUsage(): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val ref = userRef
                    ?: return@withContext Result.failure(Exception("Usuario no autenticado"))

                val items = ref.listAll().await()
                var totalBytes = 0L

                for (item in items.items) {
                    val metadata = item.metadata.await()
                    totalBytes += metadata.sizeBytes
                }

                Result.success(totalBytes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** Verifica si el usuario está autenticado para usar Storage. */
    fun isAuthenticated(): Boolean = auth.currentUser != null
}

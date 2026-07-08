package com.construccionia.app.auth

import android.content.Context
import android.net.Uri
import timber.log.Timber
import com.construccionia.app.data.local.InfographicDao
import com.construccionia.app.data.models.Infographic
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado de una operación de sincronización.
 */
sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val itemsUploaded: Int, val imagesUploaded: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Servicio de sincronización con Firebase.
 * Sube metadatos a Firestore e imágenes a Firebase Storage.
 */
@Singleton
class SyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val infographicDao: InfographicDao,
    private val storageService: StorageService
) {

    companion object {
        private const val COLLECTION_INFOS = "infographics"
        private const val COLLECTION_SYNC = "sync_metadata"
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val userId: String? get() = auth.currentUser?.uid

    /**
     * Sincronización completa: sube metadatos a Firestore e imágenes a Storage.
     * @param uploadImages Si es true, también sube las imágenes a Storage.
     */
    suspend fun syncAll(uploadImages: Boolean = true): SyncState = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext SyncState.Error("Usuario no autenticado")

        try {
            val infographics = infographicDao.getAllInfographics().first()
            val batch = firestore.batch()
            var imagesUploaded = 0

            for (infographic in infographics) {
                // 1. Subir imagen a Storage
                if (uploadImages && infographic.filePath.isNotBlank()) {
                    val fileName = "${infographic.id}_${infographic.name}.png"
                    val imageResult = try {
                        storageService.uploadFromFile(File(infographic.filePath), fileName)
                    } catch (e: Exception) {
                        // Si el archivo no existe localmente, intentar con URI
                        try {
                            val uri = Uri.parse(infographic.uriString)
                            storageService.uploadImage(uri, fileName)
                        } catch (e2: Exception) {
                            Timber.w(e2, "No se pudo subir imagen ${infographic.name}")
                            null
                        }
                    }

                    if (imageResult?.isSuccess == true) {
                        imagesUploaded++
                    }
                }

                // 2. Subir metadatos a Firestore
                val docRef = firestore
                    .collection(COLLECTION_INFOS)
                    .document(uid)
                    .collection("items")
                    .document(infographic.id.toString())

                val data = hashMapOf(
                    "name" to infographic.name,
                    "uriString" to infographic.uriString,
                    "filePath" to infographic.filePath,
                    "isDemo" to infographic.isDemo,
                    "createdAt" to infographic.createdAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            // 3. Actualizar metadatos de sync
            val syncRef = firestore.collection(COLLECTION_SYNC).document(uid)
            batch.set(syncRef, hashMapOf(
                "lastSync" to FieldValue.serverTimestamp(),
                "itemCount" to infographics.size,
                "imagesCount" to imagesUploaded
            ), SetOptions.merge())

            batch.commit().await()

            Timber.d("Sync completo: ${infographics.size} items, $imagesUploaded imágenes")
            SyncState.Success(
                itemsUploaded = infographics.size,
                imagesUploaded = imagesUploaded
            )
        } catch (e: Exception) {
            Timber.e(e, "Error en sync")
            SyncState.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Descarga metadatos desde Firestore y los fusiona con Room.
     */
    suspend fun downloadMetadata(): SyncState = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext SyncState.Error("Usuario no autenticado")
        try {
            val snapshot = firestore
                .collection(COLLECTION_INFOS)
                .document(uid)
                .collection("items")
                .get()
                .await()

            var count = 0
            for (document in snapshot.documents) {
                val name = document.getString("name") ?: continue
                val uriString = document.getString("uriString") ?: continue
                val filePath = document.getString("filePath") ?: continue
                val isDemo = document.getBoolean("isDemo") ?: false
                val createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()

                val existing = infographicDao.getByFileName(filePath)
                if (existing == null) {
                    infographicDao.insert(Infographic(
                        name = name,
                        uriString = uriString,
                        filePath = filePath,
                        isDemo = isDemo,
                        createdAt = createdAt
                    ))
                    count++
                }
            }
            Timber.d("Descarga: $count nuevos items")
            SyncState.Success(itemsUploaded = 0, imagesUploaded = count)
        } catch (e: Exception) {
            Timber.e(e, "Error en descarga")
            SyncState.Error(e.message ?: "Error desconocido")
        }
    }

    /** Exporta URI a archivo temporal. */
    suspend fun exportToFile(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val tempFile = File(context.cacheDir, "sync_export_${System.currentTimeMillis()}.png")
            FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Timber.e(e, "Error exportar archivo")
            null
        }
    }

    /** Fecha del último sync. */
    suspend fun getLastSyncTime(): Long? = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext null
        try {
            val doc = firestore.collection(COLLECTION_SYNC).document(uid).get().await()
            doc.getTimestamp("lastSync")?.toDate()?.time
        } catch (e: Exception) { null }
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null
}

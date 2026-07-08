package com.construccionia.app.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.construccionia.app.ConstruccionIAApp
import com.construccionia.app.data.local.InfographicDao
import com.construccionia.app.data.models.Infographic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repositorio encargado de gestionar las imágenes almacenadas.
 *
 * Combina dos fuentes:
 * 1. Room ([InfographicDao]) para metadatos y búsqueda offline
 * 2. MediaStore / filesystem para acceso a los archivos reales
 *
 * Usa MediaStore en Android 10+ para compatibilidad con scoped storage.
 * En Android 9- usa acceso directo al filesystem.
 */
class ImageRepository(
    private val context: Context,
    private val infographicDao: InfographicDao
) {

    companion object {
        private const val SUBDIR = ConstruccionIAApp.DOWNLOADS_SUBDIR
    }

    // ── Flow de Room (reactivo) ──

    /** Flow de todas las infografías desde Room. */
    fun getAllInfographicsFlow(): Flow<List<Infographic>> {
        return infographicDao.getAllInfographics()
    }

    /** Buscar infografías por nombre (Flow reactivo). */
    fun searchByNameFlow(query: String): Flow<List<Infographic>> {
        return infographicDao.searchByName(query)
    }

    /** Contar infografías (Flow reactivo). */
    fun countFlow(): Flow<Int> {
        return infographicDao.count()
    }

    // ── Operaciones suspend (one-shot) ──

    /** Obtiene todas las infografías (una vez). Sincroniza con almacenamiento externo. */
    suspend fun getAllInfographics(): List<Infographic> = withContext(Dispatchers.IO) {
        val external = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryMediaStoreImages()
        } else {
            readLegacyFiles()
        }
        // Sincronizar con Room: insertar las que no existen
        for (infographic in external) {
            val existing = infographicDao.getByFileName(infographic.filePath)
            if (existing == null) {
                infographicDao.insert(infographic)
            }
        }
        // Obtener lista one-shot desde el Flow
        infographicDao.getAllInfographics().first()
    }

    /** Obtiene una infografía por ID. */
    suspend fun getById(id: Long): Infographic? = withContext(Dispatchers.IO) {
        infographicDao.getById(id)
    }

    /** Obtiene una infografía por nombre de archivo. */
    suspend fun getByName(fileName: String): Infographic? = withContext(Dispatchers.IO) {
        infographicDao.getByFileName(fileName)
    }

    /** Insertar una infografía en Room. */
    suspend fun insert(infographic: Infographic): Long = withContext(Dispatchers.IO) {
        infographicDao.insert(infographic)
    }

    /** Elimina una infografía por su ruta. Elimina de Room y del almacenamiento externo. */
    suspend fun deleteByPath(filePath: String): Boolean = withContext(Dispatchers.IO) {
        // Eliminar de Room
        infographicDao.deleteByPath(filePath)

        // Eliminar del almacenamiento externo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val uri = Uri.parse(filePath)
                context.contentResolver.delete(uri, null, null) > 0
            } catch (e: Exception) {
                false
            }
        } else {
            File(filePath).delete()
        }
    }

    /** Elimina una infografía por su nombre de archivo. */
    suspend fun deleteByName(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val info = getByName(fileName)
        if (info != null) deleteByPath(info.filePath) else false
    }

    /** Obtiene la URI para compartir via FileProvider o directamente. */
    fun getShareUri(filePath: String): Uri {
        if (filePath.startsWith("content://")) {
            return Uri.parse(filePath)
        }
        val file = File(filePath)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /** Actualiza una infografía existente en Room. */
    suspend fun updateInfographic(infographic: Infographic) = withContext(Dispatchers.IO) {
        infographicDao.update(infographic)
    }

    /** Comprueba si el almacenamiento externo está montado. */
    fun isStorageAccessible(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    // ── Métodos privados de consulta externa ──

    private fun getLegacyImagesDir(): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            SUBDIR
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun queryMediaStoreImages(): List<Infographic> {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.DATE_MODIFIED
        )
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND " +
                "(${MediaStore.Downloads.MIME_TYPE} = ? OR ${MediaStore.Downloads.MIME_TYPE} = ?)"
        val selectionArgs = arrayOf(
            "%$SUBDIR%",
            "image/png",
            "image/jpeg"
        )
        val sortOrder = "${MediaStore.Downloads.DATE_MODIFIED} DESC"

        val resolver = context.contentResolver
        val infographics = mutableListOf<Infographic>()

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex)
                val dateModified = cursor.getLong(dateIndex) * 1000L

                val contentUri = Uri.withAppendedPath(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                infographics.add(
                    Infographic(
                        name = name.substringBeforeLast("."),
                        uriString = contentUri.toString(),
                        filePath = contentUri.toString(),
                        createdAt = dateModified
                    )
                )
            }
        }
        return infographics
    }

    private fun readLegacyFiles(): List<Infographic> {
        val imagesDir = getLegacyImagesDir()
        val files = imagesDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".png", ignoreCase = true)
                    || file.name.endsWith(".jpg", ignoreCase = true)
                    || file.name.endsWith(".jpeg", ignoreCase = true))
        } ?: emptyArray()

        return files.map { file ->
            Infographic(
                name = file.nameWithoutExtension,
                uriString = Uri.fromFile(file).toString(),
                filePath = file.absolutePath,
                createdAt = file.lastModified()
            )
        }.sortedByDescending { it.createdAt }
    }
}

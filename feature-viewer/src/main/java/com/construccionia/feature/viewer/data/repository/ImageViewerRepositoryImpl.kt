package com.construccionia.feature.viewer.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ImageBytes
import com.construccionia.core.common.ProcessingException
import com.construccionia.core.common.StorageException
import com.construccionia.core.common.extension.toImageBytes
import com.construccionia.feature.viewer.domain.repository.ImageViewerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageViewerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageViewerRepository {

    override suspend fun getImage(id: String): AppResult<ImageBytes> {
        return try {
            val file = File(context.cacheDir, "images/$id.png")
            if (!file.exists()) {
                return AppResult.Error(
                    StorageException("Imagen no encontrada: $id")
                )
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: return AppResult.Error(ProcessingException("No se pudo decodificar la imagen"))
            AppResult.Success(bitmap.toImageBytes())
        } catch (e: Exception) {
            AppResult.Error(ProcessingException("Error al cargar imagen: ${e.message}", e))
        }
    }

    override suspend fun loadDemoImage(): AppResult<ImageBytes> {
        return try {
            // Cargar imagen demo desde recursos drawable del módulo
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                com.construccionia.feature.viewer.R.drawable.demo_construction
            )
                ?: return AppResult.Error(ProcessingException("No se pudo decodificar la imagen demo"))
            AppResult.Success(bitmap.toImageBytes())
        } catch (e: Exception) {
            AppResult.Error(ProcessingException("Error al cargar imagen demo: ${e.message}", e))
        }
    }

    override suspend fun getRecentImages(): AppResult<List<ImageBytes>> {
        return try {
            val imagesDir = File(context.cacheDir, "images")
            if (!imagesDir.exists()) {
                return AppResult.Success(emptyList())
            }
            val files = imagesDir.listFiles()?.sortedByDescending { it.lastModified() }?.take(20)
                ?: emptyList()
            val images = files.mapNotNull { file ->
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    bitmap?.toImageBytes()
                } catch (e: Exception) {
                    null
                }
            }
            AppResult.Success(images)
        } catch (e: Exception) {
            AppResult.Error(ProcessingException("Error al listar imágenes recientes: ${e.message}", e))
        }
    }
}

package com.construccionia.feature.viewer.domain.repository

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ImageBytes

/**
 * Repositorio para acceder a imágenes del visualizador.
 */
interface ImageViewerRepository {
    /**
     * Obtiene una imagen por su ID.
     */
    suspend fun getImage(id: String): AppResult<ImageBytes>

    /**
     * Carga una imagen de demostración desde recursos.
     */
    suspend fun loadDemoImage(): AppResult<ImageBytes>

    /**
     * Obtiene la lista de imágenes recientes.
     */
    suspend fun getRecentImages(): AppResult<List<ImageBytes>>
}

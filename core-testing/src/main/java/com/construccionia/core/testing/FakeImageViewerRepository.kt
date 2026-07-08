package com.construccionia.core.testing

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ImageBytes
import com.construccionia.core.common.ProcessingException
import com.construccionia.feature.viewer.domain.repository.ImageViewerRepository

/**
 * Fake de [ImageViewerRepository] para pruebas.
 * Simula la carga de imágenes sin almacenamiento real.
 *
 * Úsalo en tests de [com.construccionia.feature.viewer.presentation.ViewerViewModel]
 * junto con [MainDispatcherRule].
 */
class FakeImageViewerRepository : ImageViewerRepository {

    private var shouldFail = false
    private var getImageCount = 0
    private var demoLoadCount = 0

    /** Configura el fake para fallar en la próxima llamada. */
    fun setShouldFail(fail: Boolean) {
        shouldFail = fail
    }

    /** Retorna cuántas veces se llamó a [getImage]. */
    fun getGetImageCount(): Int = getImageCount

    /** Retorna cuántas veces se llamó a [loadDemoImage]. */
    fun getDemoLoadCount(): Int = demoLoadCount

    override suspend fun getImage(id: String): AppResult<ImageBytes> {
        getImageCount++
        if (shouldFail) {
            return AppResult.Error(
                ProcessingException("Error simulado al cargar imagen: $id")
            )
        }
        return AppResult.Success(
            ImageBytes(
                bytes = ByteArray(100) { it.toByte() },
                width = 800,
                height = 600,
                mimeType = "image/png"
            )
        )
    }

    override suspend fun loadDemoImage(): AppResult<ImageBytes> {
        demoLoadCount++
        if (shouldFail) {
            return AppResult.Error(
                ProcessingException("Error simulado al cargar imagen demo")
            )
        }
        return AppResult.Success(
            ImageBytes(
                bytes = ByteArray(50) { (it + 1).toByte() },
                width = 400,
                height = 300,
                mimeType = "image/png"
            )
        )
    }

    override suspend fun getRecentImages(): AppResult<List<ImageBytes>> {
        return AppResult.Success(emptyList())
    }
}

package com.construccionia.feature.viewer.presentation

import android.content.Context
import app.cash.turbine.test
import com.construccionia.core.common.ImageBytes
import com.construccionia.core.testing.FakeImageViewerRepository
import com.construccionia.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Pruebas unitarias para [ViewerViewModel].
 *
 * - Verifica la transición correcta de estados [ViewerUiState].
 * - Verifica que [loadImage] y [loadDemoImage] actualicen el estado
 *   tanto en éxito como en error.
 * - Usa [FakeImageViewerRepository] para simular el repositorio y
 *   MockK para mockear [Context] necesario para la caché de archivos temporales.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    // SUT
    private lateinit var viewModel: ViewerViewModel

    // Dependencias
    private val fakeRepository = FakeImageViewerRepository()
    private val tempCacheDir: File = File(System.getProperty("java.io.tmpdir"), "viewer_test_cache")

    private val context: Context = mockk {
        every { cacheDir } returns tempCacheDir
    }

    @Before
    fun setUp() {
        tempCacheDir.mkdirs()
        fakeRepository.setShouldFail(false)
        viewModel = ViewerViewModel(fakeRepository, context)
    }

    @After
    fun tearDown() {
        // Limpiar archivos temporales creados durante los tests
        tempCacheDir.deleteRecursively()
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de estado inicial
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `initialState_isIdle()`() = runTest {
        // When: el ViewModel se acaba de crear
        // Then: el estado debe ser Idle
        viewModel.uiState.test {
            assertEquals(
                "El estado inicial debe ser Idle",
                ViewerUiState.Idle,
                awaitItem()
            )
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de loadImage
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `loadImage_cuandoImagenValida_cambiaALoaded()`() = runTest {
        // When: cargamos una imagen válida
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.loadImage("test-image-1")

            // Then: debe pasar por Loading y luego Loaded
            val loading = awaitItem()
            assertTrue(
                "Después de loadImage debe emitir Loading",
                loading is ViewerUiState.Loading
            )

            val loaded = awaitItem()
            assertTrue(
                "Luego debe emitir Loaded",
                loaded is ViewerUiState.Loaded
            )

            val loadedState = loaded as ViewerUiState.Loaded
            assertNotNull(
                "Loaded debe contener los bytes de la imagen",
                loadedState.imageBytes
            )
            assertNotNull(
                "Loaded debe contener una URL de archivo",
                loadedState.imageUrl
            )
            assertTrue(
                "La URL debe empezar con file://",
                loadedState.imageUrl!!.startsWith("file://")
            )
            cancel()
        }
    }

    @Test
    fun `loadImage_cuandoFalla_cambiaAError()`() = runTest {
        // Given: el repositorio falla
        fakeRepository.setShouldFail(true)

        // When
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.loadImage("test-image-fail")

            // Then: debe pasar por Loading y luego Error
            awaitItem() // Loading
            val error = awaitItem()

            assertTrue(
                "Debe emitir Error cuando el repositorio falla",
                error is ViewerUiState.Error
            )
            val errorState = error as ViewerUiState.Error
            assertEquals(
                "El mensaje de error debe ser informativo",
                "Error simulado al cargar imagen: test-image-fail",
                errorState.exception.message
            )
            cancel()
        }
    }

    @Test
    fun `loadImage_cuandoLoading_emiteEstadoLoading()`() = runTest {
        // When: iniciamos loadImage
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.loadImage("test-image-loading")

            // Then: el primer estado después de Idle debe ser Loading
            val state = awaitItem()
            assertTrue(
                "Debe emitir Loading inmediatamente después de llamar a loadImage",
                state is ViewerUiState.Loading
            )
            cancel()
        }
    }

    @Test
    fun `loadImage_conImagenValida_incluyeBytesEnLoaded()`() = runTest {
        // When
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.loadImage("test-image-bytes")
            awaitItem() // Loading
            val loaded = awaitItem() as ViewerUiState.Loaded

            // Then: los bytes deben ser los que retornó el fake
            assertTrue(
                "Los bytes de la imagen deben ser los proporcionados por el repositorio",
                loaded.imageBytes.bytes.isNotEmpty()
            )
            assertEquals(
                "El width debe coincidir con el fake",
                800,
                loaded.imageBytes.width
            )
            assertEquals(
                "El height debe coincidir con el fake",
                600,
                loaded.imageBytes.height
            )
            cancel()
        }
    }

    @Test
    fun `loadImage_conIdVacio_igualIntentaCargar()`() = runTest {
        // Given: ID vacío
        // When
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.loadImage("")

            // Then: debe intentar cargar igual (el repositorio decide si falla)
            awaitItem() // Loading
            val loaded = awaitItem()
            assertTrue(
                "Debe intentar cargar incluso con ID vacío",
                loaded is ViewerUiState.Loaded
            )
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de loadDemoImage
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `loadDemoImage_cargaImagenDemo()`() = runTest {
        // When: cargamos la imagen demo
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.loadDemoImage()

            // Then: debe pasar por Loading y luego Loaded
            awaitItem() // Loading
            val loaded = awaitItem()

            assertTrue(
                "loadDemoImage debe terminar en Loaded",
                loaded is ViewerUiState.Loaded
            )
            val loadedState = loaded as ViewerUiState.Loaded
            assertNotNull(
                "La imagen demo debe tener bytes",
                loadedState.imageBytes
            )
            assertNotNull(
                "La imagen demo debe tener URL",
                loadedState.imageUrl
            )
            cancel()
        }
    }

    @Test
    fun `loadDemoImage_cuandoFalla_muestraError()`() = runTest {
        // Given: el repositorio falla para demo
        fakeRepository.setShouldFail(true)

        // When
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.loadDemoImage()

            // Then: debe pasar por Loading y luego Error
            awaitItem() // Loading
            val error = awaitItem()

            assertTrue(
                "loadDemoImage debe emitir Error cuando falla",
                error is ViewerUiState.Error
            )
            val errorState = error as ViewerUiState.Error
            assertEquals(
                "Debe contener el mensaje de error simulado",
                "Error simulado al cargar imagen demo",
                errorState.exception.message
            )
            cancel()
        }
    }

    @Test
    fun `loadDemoImage_cargaUnaVez_incrementaContador()`() = runTest {
        // Given
        assertEquals("El contador demo debe empezar en 0", 0, fakeRepository.getDemoLoadCount())

        // When
        viewModel.loadDemoImage()

        // Then: debe haberse llamado una vez (usamos runTest y turbina para esperar)
        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Loading
            awaitItem() // Loaded
            assertEquals("El repositorio debe haber recibido 1 llamada", 1, fakeRepository.getDemoLoadCount())
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de reset
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `reset_vuelveAIdle()`() = runTest {
        // Given: cargamos una imagen exitosamente
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.loadImage("test-reset")
            awaitItem() // Loading
            awaitItem() // Loaded

            // When: reseteamos
            viewModel.reset()

            // Then: debe volver a Idle
            val resetState = awaitItem()
            assertEquals(
                "Después de reset debe ser Idle",
                ViewerUiState.Idle,
                resetState
            )
            cancel()
        }
    }

    @Test
    fun `reset_desdeError_vuelveAIdle()`() = runTest {
        // Given: simulamos un error
        fakeRepository.setShouldFail(true)

        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.loadImage("test-reset-error")
            awaitItem() // Loading
            awaitItem() // Error

            // When: reseteamos
            viewModel.reset()

            // Then: debe volver a Idle
            val resetState = awaitItem()
            assertEquals(
                "Después de reset desde Error debe ser Idle",
                ViewerUiState.Idle,
                resetState
            )
            cancel()
        }
    }

    @Test
    fun `reset_variasVeces_mantieneIdle()`() = runTest {
        // Given: estamos en Idle
        viewModel.uiState.test {
            awaitItem() // Idle

            // When: reseteamos varias veces
            viewModel.reset()
            viewModel.reset()
            viewModel.reset()

            // Then: debe seguir en Idle
            val state = awaitItem()
            assertEquals(
                "Múltiples reset deben mantener Idle",
                ViewerUiState.Idle,
                state
            )
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de flujo completo
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `loadImage_y_reset_flujoCompleto()`() = runTest {
        // Given: cargamos y luego reseteamos
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.loadImage("test-flujo")
            awaitItem() // Loading
            val loaded = awaitItem() as ViewerUiState.Loaded
            assertTrue("Debe estar en Loaded", loaded.imageBytes.bytes.isNotEmpty())

            viewModel.reset()
            assertEquals("Reset debe volver a Idle", ViewerUiState.Idle, awaitItem())

            // Luego podemos cargar otra vez
            viewModel.loadImage("test-flujo-2")
            awaitItem() // Loading
            val loaded2 = awaitItem()
            assertTrue("Segunda carga debe ser Loaded también", loaded2 is ViewerUiState.Loaded)
            cancel()
        }
    }
}

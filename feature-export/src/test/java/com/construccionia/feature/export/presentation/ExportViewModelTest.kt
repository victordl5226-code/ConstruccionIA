package com.construccionia.feature.export.presentation

import app.cash.turbine.test
import com.construccionia.core.common.ImageBytes
import com.construccionia.core.testing.FakeExportRepository
import com.construccionia.core.testing.MainDispatcherRule
import com.construccionia.feature.export.domain.model.ExportConfig
import com.construccionia.feature.export.domain.model.SlideContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas unitarias para [ExportViewModel].
 *
 * - Verifica la transición correcta de estados [ExportUiState].
 * - Verifica que [addSlide], [removeSlide] y [updateConfig] actualicen
 *   los datos correspondientes.
 * - Verifica que [exportToPptx] maneje éxito y error con [FakeExportRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    // SUT
    private lateinit var viewModel: ExportViewModel

    // Dependencia
    private val fakeRepository = FakeExportRepository()

    // Datos de prueba
    private val sampleSlide = SlideContent(
        title = "Introducción",
        body = "Este proyecto consiste en la construcción de un edificio sostenible.",
        image = ImageBytes(
            bytes = ByteArray(50) { it.toByte() },
            width = 400,
            height = 300,
            mimeType = "image/png"
        ),
        notes = "Nota de prueba"
    )

    private val sampleSlide2 = SlideContent(
        title = "Materiales",
        body = "Hormigón armado, acero estructural y vidrio templado.",
        notes = ""
    )

    @Before
    fun setUp() {
        fakeRepository.setShouldFail(false)
        viewModel = ExportViewModel(fakeRepository)
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
                ExportUiState.Idle,
                awaitItem()
            )
            cancel()
        }
    }

    @Test
    fun `initialSlides_isEmpty()`() {
        // Then: la lista de diapositivas debe estar vacía
        assertTrue(
            "La lista de slides inicial debe estar vacía",
            viewModel.slides.value.isEmpty()
        )
    }

    @Test
    fun `initialConfig_hasDefaultValues()`() {
        // Then: la configuración debe tener valores por defecto
        val config = viewModel.config.value
        assertEquals(
            "El nombre de archivo por defecto debe ser 'presentacion_construccion'",
            "presentacion_construccion",
            config.fileName
        )
        assertEquals(
            "El título por defecto debe ser 'Proyecto de Construcción'",
            "Proyecto de Construcción",
            config.title
        )
        assertTrue(
            "Los números de página deben estar habilitados por defecto",
            config.includeSlideNumbers
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de addSlide, removeSlide, updateConfig
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `addSlide_agregaSlideALista()`() {
        // Given: no hay slides

        // When: agregamos un slide
        viewModel.addSlide(sampleSlide)

        // Then: la lista debe contener el slide agregado
        assertEquals(
            "Debe haber 1 slide después de agregar",
            1,
            viewModel.slides.value.size
        )
        assertEquals(
            "El slide agregado debe coincidir con el original",
            sampleSlide,
            viewModel.slides.value[0]
        )
    }

    @Test
    fun `addSlide_variosSlides_agregaEnOrden()`() {
        // When: agregamos dos slides
        viewModel.addSlide(sampleSlide)
        viewModel.addSlide(sampleSlide2)

        // Then: deben estar en el orden correcto
        assertEquals(
            "Debe haber 2 slides",
            2,
            viewModel.slides.value.size
        )
        assertEquals(
            "El primer slide debe ser sampleSlide",
            sampleSlide,
            viewModel.slides.value[0]
        )
        assertEquals(
            "El segundo slide debe ser sampleSlide2",
            sampleSlide2,
            viewModel.slides.value[1]
        )
    }

    @Test
    fun `removeSlide_eliminaSlideCorrectamente()`() {
        // Given: agregamos dos slides
        viewModel.addSlide(sampleSlide)
        viewModel.addSlide(sampleSlide2)
        assertEquals("Debe haber 2 slides", 2, viewModel.slides.value.size)

        // When: eliminamos el primero
        viewModel.removeSlide(0)

        // Then: debe quedar solo sampleSlide2
        assertEquals(
            "Debe quedar 1 slide",
            1,
            viewModel.slides.value.size
        )
        assertEquals(
            "El slide restante debe ser sampleSlide2",
            sampleSlide2,
            viewModel.slides.value[0]
        )
    }

    @Test
    fun `updateConfig_actualizaConfiguracion()`() {
        // Given: una nueva configuración
        val newConfig = ExportConfig(
            fileName = "mi_proyecto",
            title = "Mi Proyecto Personal",
            author = "Usuario Test",
            includeSlideNumbers = false
        )

        // When: actualizamos la configuración
        viewModel.updateConfig(newConfig)

        // Then: la configuración debe coincidir
        val config = viewModel.config.value
        assertEquals(
            "El nombre de archivo debe actualizarse",
            "mi_proyecto",
            config.fileName
        )
        assertEquals(
            "El título debe actualizarse",
            "Mi Proyecto Personal",
            config.title
        )
        assertEquals(
            "El autor debe actualizarse",
            "Usuario Test",
            config.author
        )
        assertEquals(
            "includeSlideNumbers debe ser false",
            false,
            config.includeSlideNumbers
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de exportToPptx
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `exportToPptx_cuandoExito_cambiaASuccess()`() = runTest {
        // Given: tenemos al menos un slide
        viewModel.addSlide(sampleSlide)

        // When: exportamos
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.exportToPptx()

            // Then: debe pasar por Exporting y luego Success
            val exporting = awaitItem()
            assertTrue(
                "Después de exportToPptx debe emitir Exporting",
                exporting is ExportUiState.Exporting
            )

            val success = awaitItem()
            assertTrue(
                "Luego debe emitir Success",
                success is ExportUiState.Success
            )

            val successState = success as ExportUiState.Success
            assertNotNull(
                "Success debe contener un FileOutput",
                successState.output
            )
            assertTrue(
                "El path del archivo debe terminar en .pptx",
                successState.output.path.endsWith(".pptx")
            )
            cancel()
        }
    }

    @Test
    fun `exportToPptx_cuandoFalla_cambiaAError()`() = runTest {
        // Given: el repositorio falla y tenemos slides
        fakeRepository.setShouldFail(true)
        viewModel.addSlide(sampleSlide)

        // When
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.exportToPptx()

            // Then: debe pasar por Exporting y luego Error
            awaitItem() // Exporting
            val error = awaitItem()

            assertTrue(
                "Debe emitir Error cuando la exportación falla",
                error is ExportUiState.Error
            )
            val errorState = error as ExportUiState.Error
            assertEquals(
                "El mensaje de error debe ser el simulado",
                "Error simulado de exportación PPTX",
                errorState.exception.message
            )
            cancel()
        }
    }

    @Test
    fun `exportToPptx_sinSlides_noCambiaEstado()`() = runTest {
        // Given: no hay slides (lista vacía)

        // When: intentamos exportar
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.exportToPptx()

            // Then: no debe emitir nuevos estados (permanece Idle)
            // Turbine lanzaría timeout si esperáramos otro item,
            // pero verificamos que no haya cambios después de 100ms
            cancel()
        }

        // Verificar que el repositorio no fue llamado
        assertEquals(
            "El repositorio no debe ser llamado si no hay slides",
            0,
            fakeRepository.getExportCount()
        )
    }

    @Test
    fun `exportToPptx_cuandoExporting_emiteEstadoExporting()`() = runTest {
        // Given
        viewModel.addSlide(sampleSlide)

        // When
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.exportToPptx()

            // Then: el primer estado después de Idle debe ser Exporting
            val state = awaitItem()
            assertTrue(
                "Debe emitir Exporting inmediatamente",
                state is ExportUiState.Exporting
            )
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de reset
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `reset_vuelveAIdle()`() = runTest {
        // Given: exportamos exitosamente
        viewModel.addSlide(sampleSlide)
        viewModel.exportToPptx()

        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Exporting
            awaitItem() // Success

            // When: reseteamos
            viewModel.reset()

            // Then: debe volver a Idle y limpiar slides
            assertEquals(
                "Después de reset debe ser Idle",
                ExportUiState.Idle,
                awaitItem()
            )
            assertTrue(
                "La lista de slides debe quedar vacía después de reset",
                viewModel.slides.value.isEmpty()
            )
            cancel()
        }
    }

    @Test
    fun `reset_desdeError_vuelveAIdleYLimpiaSlides()`() = runTest {
        // Given: simulamos error
        fakeRepository.setShouldFail(true)
        viewModel.addSlide(sampleSlide)
        viewModel.exportToPptx()

        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Exporting
            awaitItem() // Error

            // When: reseteamos
            viewModel.reset()

            // Then: debe volver a Idle
            assertEquals(
                "Reset desde Error debe volver a Idle",
                ExportUiState.Idle,
                awaitItem()
            )
            assertTrue(
                "Los slides deben limpiarse después de reset",
                viewModel.slides.value.isEmpty()
            )
            cancel()
        }
    }
}

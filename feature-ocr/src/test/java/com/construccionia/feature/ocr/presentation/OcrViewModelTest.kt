package com.construccionia.feature.ocr.presentation

import app.cash.turbine.test
import com.construccionia.core.common.ImageBytes
import com.construccionia.core.testing.FakeOcrRepository
import com.construccionia.core.testing.MainDispatcherRule
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
 * Pruebas unitarias para [OcrViewModel].
 *
 * - Verifica la transición correcta de estados [OcrUiState].
 * - Verifica que [recognizeText] y [updateEditedText] actualicen
 *   el texto editado correctamente.
 * - Usa [FakeOcrRepository] para simular el OCR sin ML Kit real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OcrViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    // SUT
    private lateinit var viewModel: OcrViewModel

    // Dependencia
    private val fakeRepository = FakeOcrRepository()

    // Datos de prueba
    private val testImageBytes = ImageBytes(
        bytes = ByteArray(200) { it.toByte() },
        width = 640,
        height = 480,
        mimeType = "image/jpeg"
    )

    @Before
    fun setUp() {
        fakeRepository.setShouldFail(false)
        viewModel = OcrViewModel(fakeRepository)
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
                OcrUiState.Idle,
                awaitItem()
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `initialEditedText_isEmpty()`() {
        // Then: el texto editado inicial debe estar vacío
        assertEquals(
            "El texto editado inicial debe estar vacío",
            "",
            viewModel.editedText.value
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de recognizeText
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `recognizeText_conImagenValida_cambiaATextDetected()`() = runTest {
        // When: reconocemos texto en una imagen válida
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.recognizeText(testImageBytes)

            // Then: debe pasar por Loading y luego TextDetected
            val loading = awaitItem()
            assertTrue(
                "Después de recognizeText debe emitir Loading",
                loading is OcrUiState.Loading
            )

            val detected = awaitItem()
            assertTrue(
                "Luego debe emitir TextDetected",
                detected is OcrUiState.TextDetected
            )

            val detectedState = detected as OcrUiState.TextDetected
            assertNotNull(
                "TextDetected debe contener un resultado OCR",
                detectedState.result
            )
            assertTrue(
                "El texto detectado no debe estar vacío",
                detectedState.result.text.isNotEmpty()
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `recognizeText_actualizaEditedText()`() = runTest {
        // When: reconocemos texto
        viewModel.recognizeText(testImageBytes)

        // Then: editedText debe contener el texto reconocido
        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Loading
            val detected = awaitItem() as OcrUiState.TextDetected

            // Esperamos a que se propague el cambio en editedText
            assertEquals(
                "editedText debe coincidir con el texto reconocido",
                detected.result.text,
                viewModel.editedText.value
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `recognizeText_cuandoFalla_cambiaAError()`() = runTest {
        // Given: el repositorio falla
        fakeRepository.setShouldFail(true)

        // When
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.recognizeText(testImageBytes)

            // Then: debe pasar por Loading y luego Error
            awaitItem() // Loading
            val error = awaitItem()

            assertTrue(
                "Debe emitir Error cuando el OCR falla",
                error is OcrUiState.Error
            )
            val errorState = error as OcrUiState.Error
            assertEquals(
                "El mensaje de error debe ser informativo",
                "Error simulado de OCR",
                errorState.exception.message
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `recognizeText_cuandoFalla_noActualizaEditedText()`() = runTest {
        // Given: el repositorio falla
        fakeRepository.setShouldFail(true)

        // When
        viewModel.recognizeText(testImageBytes)

        // Then: editedText debe permanecer vacío
        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Loading
            awaitItem() // Error

            assertEquals(
                "Si hay error, editedText debe estar vacío",
                "",
                viewModel.editedText.value
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `recognizeText_conImagenValida_incluyeResultadoConConfianza()`() = runTest {
        // When
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.recognizeText(testImageBytes)
            awaitItem() // Loading
            val detected = awaitItem() as OcrUiState.TextDetected

            // Then: el resultado debe tener confianza > 0
            assertTrue(
                "La confianza del OCR debe ser positiva",
                detected.result.confidence > 0f
            )
            assertTrue(
                "El tiempo de procesamiento debe ser positivo",
                detected.result.processingTimeMs > 0
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `recognizeText_textoDetectado_coincideConResultado()`() = runTest {
        // When
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.recognizeText(testImageBytes)
            awaitItem() // Loading
            val detected = awaitItem() as OcrUiState.TextDetected

            // Then: editedText debe coincidir exactamente con el texto del resultado
            val expectedText = detected.result.text
            assertEquals(
                "editedText debe ser el texto extraído del resultado OCR",
                expectedText,
                viewModel.editedText.value
            )
            expectNoEvents()
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de updateEditedText
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `updateEditedText_actualizaTexto()`() = runTest {
        // Given: texto de prueba
        val nuevoTexto = "Texto editado manualmente por el usuario"

        // When: actualizamos el texto
        viewModel.updateEditedText(nuevoTexto)

        // Then: editedText debe contener el nuevo texto
        assertEquals(
            "editedText debe reflejar el texto ingresado",
            nuevoTexto,
            viewModel.editedText.value
        )
    }

    @Test
    fun `updateEditedText_variasVeces_actualizaCorrectamente()`() {
        // Given
        val textos = listOf("Primera edición", "Segunda edición", "Tercera edición")

        // When
        for (texto in textos) {
            viewModel.updateEditedText(texto)
        }

        // Then: debe quedar el último texto
        assertEquals(
            "editedText debe contener el último texto ingresado",
            textos.last(),
            viewModel.editedText.value
        )
    }

    @Test
    fun `updateEditedText_conTextoVacio_limpiaTexto()`() {
        // Given: primero establecemos un texto
        viewModel.updateEditedText("Texto previo")

        // When: limpiamos con texto vacío
        viewModel.updateEditedText("")

        // Then: debe quedar vacío
        assertEquals(
            "editedText debe quedar vacío después de actualizar con string vacío",
            "",
            viewModel.editedText.value
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de reset
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `reset_vuelveAIdleYLimpiaTexto()`() = runTest {
        // Given: reconocemos texto y luego editamos
        viewModel.recognizeText(testImageBytes)

        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Loading
            awaitItem() // TextDetected

            viewModel.updateEditedText("Edición manual")

            // When: reseteamos
            viewModel.reset()

            // Then: debe volver a Idle
            assertEquals(
                "Después de reset debe ser Idle",
                OcrUiState.Idle,
                awaitItem()
            )
            assertEquals(
                "editedText debe quedar vacío después de reset",
                "",
                viewModel.editedText.value
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `reset_desdeError_vuelveAIdleYLimpiaTexto()`() = runTest {
        // Given: simulamos error
        fakeRepository.setShouldFail(true)
        viewModel.recognizeText(testImageBytes)

        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Loading
            awaitItem() // Error

            // When: reseteamos
            viewModel.reset()

            // Then: debe volver a Idle
            assertEquals(
                "Reset desde Error debe volver a Idle",
                OcrUiState.Idle,
                awaitItem()
            )
            assertEquals(
                "editedText debe estar vacío",
                "",
                viewModel.editedText.value
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `reset_desdeIdle_mantieneIdleYVacio()`() = runTest {
        // Given: estamos en Idle con texto vacío
        viewModel.uiState.test {
            assertEquals(OcrUiState.Idle, awaitItem())

            // When: reseteamos desde Idle
            viewModel.reset()

            // Then: debe seguir en Idle
            assertEquals(
                "Reset desde Idle debe mantener Idle",
                OcrUiState.Idle,
                awaitItem()
            )
            assertEquals(
                "editedText debe seguir vacío",
                "",
                viewModel.editedText.value
            )
            expectNoEvents()
            cancel()
        }
    }
}

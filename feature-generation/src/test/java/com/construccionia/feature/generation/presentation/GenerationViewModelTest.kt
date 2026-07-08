package com.construccionia.feature.generation.presentation

import app.cash.turbine.test
import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ImageBytes
import com.construccionia.core.common.InvalidInputException
import com.construccionia.core.common.NetworkException
import com.construccionia.core.testing.MainDispatcherRule
import com.construccionia.feature.generation.domain.model.AspectRatio
import com.construccionia.feature.generation.domain.model.GeneratedImage
import com.construccionia.feature.generation.domain.model.GenerationPrompt
import com.construccionia.feature.generation.domain.model.PromptStyle
import com.construccionia.feature.generation.domain.usecase.GenerateImageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
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
 * Pruebas unitarias para [GenerationViewModel].
 *
 * - Verifica la transición correcta de estados [GenerationUiState].
 * - Verifica que las actualizaciones de prompt, estilo y aspect ratio
 *   se reflejen correctamente en el estado.
 * - Verifica el flujo completo de generación (Idle ? Editing ? Generating ? Success/Error).
 * - Usa Turbine para testear StateFlow y MockK para mockear el caso de uso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GenerationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    // SUT
    private lateinit var viewModel: GenerationViewModel

    // Dependencia mockeada
    private val generateImageUseCase: GenerateImageUseCase = mockk()

    // Datos de prueba
    private val validPromptText = "Casa moderna con jardín vertical y paneles solares"
    private val validGenerationPrompt = GenerationPrompt(
        text = validPromptText,
        style = PromptStyle.FOTO_REALISTA,
        aspectRatio = AspectRatio.SQUARE_1_1
    )

    private val fakeGeneratedImage = GeneratedImage(
        id = "test-image-1",
        imageBytes = ImageBytes(
            bytes = ByteArray(100) { it.toByte() },
            width = 1024,
            height = 1024
        ),
        mimeType = "image/png",
        promptUsed = validPromptText,
        timestamp = 123456789L
    )

    @Before
    fun setUp() {
        // Configuramos el mock por defecto para que retorne éxito
        coEvery { generateImageUseCase(any()) } returns AppResult.Success(fakeGeneratedImage)

        viewModel = GenerationViewModel(generateImageUseCase)
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
                GenerationUiState.Idle,
                awaitItem()
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `initialPromptText_isEmpty()`() {
        // Then
        assertEquals(
            "El prompt inicial debe estar vacío",
            "",
            viewModel.promptText.value
        )
    }

    @Test
    fun `initialStyle_isFotoRealista()`() {
        // Then
        assertEquals(
            "El estilo inicial debe ser FOTO_REALISTA",
            PromptStyle.FOTO_REALISTA,
            viewModel.selectedStyle.value
        )
    }

    @Test
    fun `initialAspectRatio_isSquare()`() {
        // Then
        assertEquals(
            "El aspect ratio inicial debe ser SQUARE_1_1",
            AspectRatio.SQUARE_1_1,
            viewModel.selectedAspectRatio.value
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de actualización de prompt
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `updatePrompt_cambiaEstadoAEditing()`() = runTest {
        // Given
        val testPrompt = "Nuevo prompt de prueba"

        // When
        viewModel.updatePrompt(testPrompt)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("El estado debe ser Editing", state is GenerationUiState.Editing)
            val editingState = state as GenerationUiState.Editing
            assertEquals("El prompt debe coincidir", testPrompt, editingState.prompt)
            assertEquals("El estilo debe mantenerse", PromptStyle.FOTO_REALISTA, editingState.style)
            assertEquals(
                "El aspect ratio debe mantenerse",
                AspectRatio.SQUARE_1_1,
                editingState.aspectRatio
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `updatePrompt_variasVeces_actualizaCorrectamente()`() = runTest {
        // Given
        val prompts = listOf("Primero", "Segundo", "Tercero")

        // When
        viewModel.uiState.test {
            // Saltamos el estado Idle inicial
            awaitItem()

            for (prompt in prompts) {
                viewModel.updatePrompt(prompt)
                val state = awaitItem()
                assertTrue("Estado debe ser Editing", state is GenerationUiState.Editing)
                assertEquals(
                    "Prompt debe ser '$prompt'",
                    prompt,
                    (state as GenerationUiState.Editing).prompt
                )
            }
            expectNoEvents()
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de generación
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `generate_conPromptVacio_noCambiaEstado()`() = runTest {
        // Given: el prompt está vacío (por defecto)
        // When
        viewModel.generate()

        // Then: el estado debe seguir siendo Idle porque no se inició generación
        viewModel.uiState.test {
            assertEquals(
                "El estado debe seguir siendo Idle",
                GenerationUiState.Idle,
                awaitItem()
            )
            expectNoEvents()
            cancel()
        }
        // Verificar que el caso de uso NO fue llamado
        coVerify(exactly = 0) { generateImageUseCase(any()) }
    }

    @Test
    fun `generate_conPromptSoloEspacios_noCambiaEstado()`() = runTest {
        // Given
        viewModel.updatePrompt("   ")

        // When
        viewModel.generate()

        // Then
        coVerify(exactly = 0) { generateImageUseCase(any()) }
    }

    @Test
    fun `generate_conPromptValido_cambiaA_Generating_luego_Success()`() = runTest {
        // Given
        viewModel.updatePrompt(validPromptText)

        // When
        viewModel.uiState.test {
            // Saltamos Idle y Editing
            awaitItem() // Idle
            awaitItem() // Editing

            viewModel.generate()

            // Deberíamos recibir Generating y luego Success
            val state1 = awaitItem()
            assertTrue(
                "Después de llamar a generate debe ir a Generating",
                state1 is GenerationUiState.Generating
            )

            val state2 = awaitItem()
            assertTrue(
                "Luego debe ir a Success",
                state2 is GenerationUiState.Success
            )
            val successState = state2 as GenerationUiState.Success
            assertEquals(
                "La imagen generada debe coincidir",
                fakeGeneratedImage,
                successState.generatedImage
            )
            expectNoEvents()
            cancel()
        }

        // Verificar que el caso de uso fue llamado exactamente una vez
        coVerify(exactly = 1) { generateImageUseCase(any()) }
    }

    @Test
    fun `generate_conPromptValido_usaLosParametrosSeleccionados()`() = runTest {
        // Given
        val testStyle = PromptStyle.ESQUEMA_TECNICO
        val testRatio = AspectRatio.LANDSCAPE_16_9
        viewModel.updatePrompt(validPromptText)
        viewModel.updateStyle(testStyle)
        viewModel.updateAspectRatio(testRatio)

        // When
        viewModel.generate()

        // Then
        coVerify(exactly = 1) {
            generateImageUseCase(
                withArg { prompt ->
                    assertEquals("Prompt text debe coincidir", validPromptText, prompt.text)
                    assertEquals("Style debe coincidir", testStyle, prompt.style)
                    assertEquals("AspectRatio debe coincidir", testRatio, prompt.aspectRatio)
                }
            )
        }
    }

    @Test
    fun `generate_cuandoFalla_cambiaA_Error()`() = runTest {
        // Given
        val expectedException = NetworkException("Error de red simulado")
        coEvery { generateImageUseCase(any()) } returns AppResult.Error(expectedException)
        viewModel.updatePrompt(validPromptText)

        // When
        viewModel.uiState.test {
            // Saltamos Idle y Editing
            awaitItem() // Idle
            awaitItem() // Editing

            viewModel.generate()

            // Deberíamos recibir Generating y luego Error
            awaitItem() // Generating
            val errorState = awaitItem()

            assertTrue(
                "Debe ser estado Error",
                errorState is GenerationUiState.Error
            )
            val error = errorState as GenerationUiState.Error
            assertTrue(
                "La excepción debe ser NetworkException",
                error.exception is NetworkException
            )
            assertEquals(
                "El mensaje debe coincidir",
                "Error de red simulado",
                error.exception.message
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `generate_cuandoFallaPorInputInvalido_cambiaA_Error()`() = runTest {
        // Given
        val expectedException = InvalidInputException("Prompt demasiado corto")
        coEvery { generateImageUseCase(any()) } returns AppResult.Error(expectedException)
        viewModel.updatePrompt(validPromptText)

        // When
        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Editing

            viewModel.generate()

            awaitItem() // Generating
            val errorState = awaitItem()

            assertTrue(
                "Debe ser estado Error",
                errorState is GenerationUiState.Error
            )
            val error = errorState as GenerationUiState.Error
            assertTrue(
                "La excepción debe ser InvalidInputException",
                error.exception is InvalidInputException
            )
            expectNoEvents()
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de reset
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `reset_vuelveA_Idle()`() = runTest {
        // Given: primero generamos una imagen exitosamente
        viewModel.updatePrompt(validPromptText)
        viewModel.generate()

        // Then: ahora el estado debería ser Success
        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Editing
            awaitItem() // Generating
            awaitItem() // Success

            // When: reseteamos
            viewModel.reset()

            // Then: debe volver a Idle
            val resetState = awaitItem()
            assertEquals(
                "Después de reset debe ser Idle",
                GenerationUiState.Idle,
                resetState
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `reset_limpiaElPromptText()`() {
        // Given
        viewModel.updatePrompt(validPromptText)
        assertNotNull("El prompt no debe estar vacío", viewModel.promptText.value)

        // When
        viewModel.reset()

        // Then
        assertEquals(
            "El prompt debe quedar vacío después de reset",
            "",
            viewModel.promptText.value
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de clearError
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `clearError_vuelveA_Editing()`() = runTest {
        // Given: simulamos un error
        coEvery { generateImageUseCase(any()) } returns AppResult.Error(
            NetworkException("Error")
        )
        viewModel.updatePrompt(validPromptText)
        viewModel.generate()

        // Then: estamos en Error
        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Editing
            awaitItem() // Generating
            val errorState = awaitItem()
            assertTrue("Debe estar en Error", errorState is GenerationUiState.Error)

            // When: limpiamos el error
            viewModel.clearError()

            // Then: debe volver a Editing conservando el prompt
            val editingState = awaitItem()
            assertTrue(
                "Después de clearError debe ser Editing",
                editingState is GenerationUiState.Editing
            )
            assertEquals(
                "Debe conservar el prompt",
                validPromptText,
                (editingState as GenerationUiState.Editing).prompt
            )
            expectNoEvents()
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de actualización de estilo y aspect ratio
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `updateStyle_cambiaEstadoAEditing_conNuevoEstilo()`() = runTest {
        // Given
        val nuevoEstilo = PromptStyle.RENDER_3D

        // When
        viewModel.updateStyle(nuevoEstilo)

        // Then
        viewModel.uiState.test {
            awaitItem() // Idle

            val editingState = awaitItem()
            assertTrue("Estado debe ser Editing", editingState is GenerationUiState.Editing)
            assertEquals(
                "El estilo debe ser el nuevo",
                nuevoEstilo,
                (editingState as GenerationUiState.Editing).style
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `updateAspectRatio_cambiaEstadoAEditing_conNuevoRatio()`() = runTest {
        // Given
        val nuevoRatio = AspectRatio.LANDSCAPE_16_9

        // When
        viewModel.updateAspectRatio(nuevoRatio)

        // Then
        viewModel.uiState.test {
            awaitItem() // Idle

            val editingState = awaitItem()
            assertTrue("Estado debe ser Editing", editingState is GenerationUiState.Editing)
            assertEquals(
                "El aspect ratio debe ser el nuevo",
                nuevoRatio,
                (editingState as GenerationUiState.Editing).aspectRatio
            )
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `multiplosUpdate_mantienenCoherencia()`() = runTest {
        // Given
        viewModel.updatePrompt("Prompt inicial")
        viewModel.updateStyle(PromptStyle.ESQUEMA_TECNICO)
        viewModel.updateAspectRatio(AspectRatio.PORTRAIT_3_4)

        // Then
        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Editing (prompt)
            val promptUpdate = awaitItem() // Editing (prompt + value)
            awaitItem() // Editing (style)

            val finalState = awaitItem() // Editing (aspectRatio)
            assertTrue("Estado debe ser Editing", finalState is GenerationUiState.Editing)
            val editing = finalState as GenerationUiState.Editing
            assertEquals("Prompt debe ser el último", "Prompt inicial", editing.prompt)
            assertEquals("Style debe ser ESQUEMA_TECNICO", PromptStyle.ESQUEMA_TECNICO, editing.style)
            assertEquals(
                "Ratio debe ser PORTRAIT_3_4",
                AspectRatio.PORTRAIT_3_4,
                editing.aspectRatio
            )
            expectNoEvents()
            cancel()
        }
    }
}

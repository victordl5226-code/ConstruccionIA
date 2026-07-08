package com.construccionia.feature.generation.domain.usecase

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.InvalidInputException
import com.construccionia.core.common.NetworkException
import com.construccionia.core.common.ServerException
import com.construccionia.core.testing.FakeGeminiRepository
import com.construccionia.feature.generation.domain.model.AspectRatio
import com.construccionia.feature.generation.domain.model.GeneratedImage
import com.construccionia.feature.generation.domain.model.GenerationPrompt
import com.construccionia.feature.generation.domain.model.PromptStyle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para [GenerateImageUseCase].
 *
 * - Verifica la validación del prompt antes de delegar al repositorio.
 * - Verifica que el repositorio sea invocado correctamente.
 * - Verifica el manejo de errores del repositorio.
 */
class GenerateImageUseCaseTest {

    // SUT (System Under Test)
    private lateinit var generateImageUseCase: GenerateImageUseCase

    // Dependencias reales
    private val validatePromptUseCase = ValidatePromptUseCase()

    // Dependencias fake
    private lateinit var fakeGeminiRepository: FakeGeminiRepository

    // Dependencias mock (para escenarios específicos)
    private val mockGeminiRepository: GeminiRepository = mockk()

    // Datos de prueba
    private val validPrompt = GenerationPrompt(
        text = "Casa moderna con jardín vertical y paneles solares",
        style = PromptStyle.FOTO_REALISTA,
        aspectRatio = AspectRatio.SQUARE_1_1
    )

    private val emptyPrompt = GenerationPrompt(
        text = "",
        style = PromptStyle.FOTO_REALISTA,
        aspectRatio = AspectRatio.SQUARE_1_1
    )

    private val shortPrompt = GenerationPrompt(
        text = "Corto",
        style = PromptStyle.FOTO_REALISTA,
        aspectRatio = AspectRatio.SQUARE_1_1
    )

    @Before
    fun setUp() {
        fakeGeminiRepository = FakeGeminiRepository()
        // Por defecto, el fake no falla
        fakeGeminiRepository.setShouldFail(false)
    }

    // ──────────────────────────────────────────────────────────────
    // Tests con FakeGeminiRepository
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `generateImage_conPromptValido_retornaSuccess()`() = runTest {
        // Given
        generateImageUseCase = GenerateImageUseCase(
            geminiRepository = fakeGeminiRepository,
            validatePromptUseCase = validatePromptUseCase
        )

        // When
        val result = generateImageUseCase(validPrompt)

        // Then
        assertTrue("El resultado debe ser Success", result is AppResult.Success)
        val successResult = result as AppResult.Success
        assertNotNull("La imagen generada no debe ser nula", successResult.data)
        assertEquals(
            "El prompt usado debe coincidir",
            validPrompt.toFullPrompt(),
            successResult.data.promptUsed
        )
    }

    @Test
    fun `generateImage_conPromptVacio_retornaError()`() = runTest {
        // Given
        generateImageUseCase = GenerateImageUseCase(
            geminiRepository = fakeGeminiRepository,
            validatePromptUseCase = validatePromptUseCase
        )

        // When
        val result = generateImageUseCase(emptyPrompt)

        // Then
        assertTrue("El resultado debe ser Error", result is AppResult.Error)
        val errorResult = result as AppResult.Error
        assertTrue(
            "La excepción debe ser InvalidInputException",
            errorResult.exception is InvalidInputException
        )
        assertEquals(
            "El mensaje de error debe indicar prompt vacío",
            "El prompt no puede estar vacío",
            errorResult.exception.message
        )
    }

    @Test
    fun `generateImage_conPromptCorto_retornaError()`() = runTest {
        // Given
        generateImageUseCase = GenerateImageUseCase(
            geminiRepository = fakeGeminiRepository,
            validatePromptUseCase = validatePromptUseCase
        )

        // When
        val result = generateImageUseCase(shortPrompt)

        // Then
        assertTrue("El resultado debe ser Error", result is AppResult.Error)
        val errorResult = result as AppResult.Error
        assertTrue(
            "La excepción debe ser InvalidInputException",
            errorResult.exception is InvalidInputException
        )
        assertTrue(
            "El mensaje debe indicar longitud mínima",
            errorResult.exception.message?.contains("10 caracteres") == true
        )
    }

    @Test
    fun `generateImage_cuandoApiFalla_retornaError()`() = runTest {
        // Given
        fakeGeminiRepository.setShouldFail(true)
        generateImageUseCase = GenerateImageUseCase(
            geminiRepository = fakeGeminiRepository,
            validatePromptUseCase = validatePromptUseCase
        )

        // When
        val result = generateImageUseCase(validPrompt)

        // Then
        assertTrue("El resultado debe ser Error", result is AppResult.Error)
        val errorResult = result as AppResult.Error
        assertTrue(
            "La excepción debe ser ServerException",
            errorResult.exception is ServerException
        )
        assertEquals(
            "El código de error HTTP debe ser 500",
            500,
            (errorResult.exception as ServerException).code
        )
    }

    @Test
    fun `generateImage_cuandoRepoSinRed_retornaNetworkError()`() = runTest {
        // Given
        val networkException = NetworkException("Sin conexión a Internet")
        coEvery { mockGeminiRepository.generateImage(any()) } returns AppResult.Error(networkException)
        generateImageUseCase = GenerateImageUseCase(
            geminiRepository = mockGeminiRepository,
            validatePromptUseCase = validatePromptUseCase
        )

        // When
        val result = generateImageUseCase(validPrompt)

        // Then
        assertTrue("El resultado debe ser Error", result is AppResult.Error)
        val errorResult = result as AppResult.Error
        assertTrue(
            "La excepción debe ser NetworkException",
            errorResult.exception is NetworkException
        )
        assertEquals(
            "El mensaje debe indicar sin conexión",
            "Sin conexión a Internet",
            errorResult.exception.message
        )
        coVerify(exactly = 1) { mockGeminiRepository.generateImage(any()) }
    }

    @Test
    fun `generateImage_verificaQueLlamaAlRepositorio()`() = runTest {
        // Given
        generateImageUseCase = GenerateImageUseCase(
            geminiRepository = fakeGeminiRepository,
            validatePromptUseCase = validatePromptUseCase
        )
        val initialCount = fakeGeminiRepository.getGenerateCount()

        // When
        generateImageUseCase(validPrompt)

        // Then
        val finalCount = fakeGeminiRepository.getGenerateCount()
        assertEquals(
            "El repositorio debe haberse llamado una vez",
            initialCount + 1,
            finalCount
        )
    }

    @Test
    fun `generateImage_conPromptVacio_noLlamaAlRepositorio()`() = runTest {
        // Given
        generateImageUseCase = GenerateImageUseCase(
            geminiRepository = fakeGeminiRepository,
            validatePromptUseCase = validatePromptUseCase
        )
        val initialCount = fakeGeminiRepository.getGenerateCount()

        // When
        generateImageUseCase(emptyPrompt)

        // Then
        val finalCount = fakeGeminiRepository.getGenerateCount()
        assertEquals(
            "El repositorio NO debe ser llamado cuando el prompt es inválido",
            initialCount,
            finalCount
        )
    }

    @Test
    fun `generateImage_conDiferentesEstilos_mantieneFuncionalidad()`() = runTest {
        // Given
        generateImageUseCase = GenerateImageUseCase(
            geminiRepository = fakeGeminiRepository,
            validatePromptUseCase = validatePromptUseCase
        )
        val prompts = listOf(
            PromptStyle.FOTO_REALISTA,
            PromptStyle.ESQUEMA_TECNICO,
            PromptStyle.RENDER_3D,
            PromptStyle.BOCETO
        )

        for (style in prompts) {
            val prompt = GenerationPrompt(
                text = "Prueba de estilo arquitectónico",
                style = style,
                aspectRatio = AspectRatio.SQUARE_1_1
            )

            // When
            val result = generateImageUseCase(prompt)

            // Then
            assertTrue(
                "Debe ser Success para estilo ${style.displayName}",
                result is AppResult.Success
            )
        }
    }
}

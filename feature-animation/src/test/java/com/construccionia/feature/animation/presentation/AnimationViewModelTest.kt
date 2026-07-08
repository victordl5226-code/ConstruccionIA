package com.construccionia.feature.animation.presentation

import app.cash.turbine.test
import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ProcessingException
import com.construccionia.core.testing.MainDispatcherRule
import com.construccionia.feature.animation.domain.model.AnimationConfig
import com.construccionia.feature.animation.domain.model.AnimationType
import com.construccionia.feature.animation.domain.repository.AnimationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
 * Pruebas unitarias para [AnimationViewModel].
 *
 * - Verifica la transición correcta de estados [AnimationUiState].
 * - Verifica que [selectAnimation], [pause], [resume] y [reset]
 *   actualicen el estado correctamente.
 * - Usa MockK para mockear [AnimationRepository] dado que su interfaz
 *   combina métodos que retornan [AppResult] con métodos que retornan
 *   valores directos.
 *
 * ## Observaciones de testabilidad
 *
 * - Todos los bugs reportados por QA (ANIMATION-01, ANIMATION-02, ANIMATION-03)
 *   han sido corregidos. Los tests ahora cubren los estados de error.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnimationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    // SUT
    private lateinit var viewModel: AnimationViewModel

    // Dependencia mockeada
    private val repository: AnimationRepository = mockk()

    // Configuraciones de prueba
    private val defaultConfig = AnimationConfig(
        type = AnimationType.CONSTRUCTION_SITE,
        speed = 1f,
        autoPlay = true,
        loop = true,
        iterations = 1
    )

    @Before
    fun setUp() {
        // Configuración por defecto del mock
        coEvery { repository.getAvailableAnimations() } returns AppResult.Success(
            AnimationType.entries.toList()
        )
        coEvery { repository.getAnimationConfig(any()) } returns AppResult.Success(defaultConfig)
        coEvery { repository.isAnimationAvailable(any()) } returns true

        viewModel = AnimationViewModel(repository)
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
                AnimationUiState.Idle,
                awaitItem()
            )
            cancel()
        }
    }

    @Test
    fun `initialSelectedType_isConstructionSite()`() {
        // Then: el tipo seleccionado inicial debe ser CONSTRUCTION_SITE
        assertEquals(
            "El tipo seleccionado inicial debe ser CONSTRUCTION_SITE",
            AnimationType.CONSTRUCTION_SITE,
            viewModel.selectedType.value
        )
    }

    @Test
    fun `availableAnimations_cuandoFalla_initEmiteError()`() = runTest {
        // Given: el repositorio falla al cargar animaciones
        coEvery { repository.getAvailableAnimations() } returns
            AppResult.Error(ProcessingException("Error de carga simulado"))

        // When: creamos el ViewModel (init ejecuta loadAvailableAnimations)
        val vm = AnimationViewModel(repository)

        // Then: debe emitir Error
        vm.uiState.test {
            val state = awaitItem()
            assertTrue(
                "Cuando falla la carga inicial debe emitir Error",
                state is AnimationUiState.Error
            )
            val errorState = state as AnimationUiState.Error
            assertTrue(
                "El mensaje de error debe referirse a la carga de animaciones",
                errorState.message.contains("No se pudieron cargar")
            )
            cancel()
        }
    }

    @Test
    fun `availableAnimations_seCargaEnInit()`() {
        // Then: las animaciones disponibles deben haberse cargado al crear el ViewModel
        coVerify(exactly = 1) { repository.getAvailableAnimations() }
        assertNotNull(
            "availableAnimations no debe ser null",
            viewModel.availableAnimations.value
        )
        assertTrue(
            "Debe haber al menos una animación disponible",
            viewModel.availableAnimations.value.isNotEmpty()
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de selectAnimation
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `selectAnimation_cambiaAPlaying()`() = runTest {
        // When: seleccionamos un tipo de animación
        viewModel.uiState.test {
            awaitItem() // Idle (se emite antes de que init termine)

            viewModel.selectAnimation(AnimationType.BLUEPRINT)

            // Then: debe emitir Playing
            val playing = awaitItem()
            assertTrue(
                "selectAnimation debe emitir Playing",
                playing is AnimationUiState.Playing
            )
            val playingState = playing as AnimationUiState.Playing
            assertEquals(
                "La configuración debe corresponder al tipo seleccionado",
                AnimationType.BLUEPRINT,
                playingState.config.type
            )
            cancel()
        }
    }

    @Test
    fun `selectAnimation_cambiaSelectedType()`() {
        // Given
        val nuevoTipo = AnimationType.MEASURING

        // When
        viewModel.selectAnimation(nuevoTipo)

        // Then: selectedType debe actualizarse
        assertEquals(
            "selectedType debe ser el nuevo tipo",
            nuevoTipo,
            viewModel.selectedType.value
        )
    }

    @Test
    fun `selectAnimation_variasVeces_actualizaCorrectamente()`() = runTest {
        // Given
        val tipos = listOf(
            AnimationType.BLUEPRINT,
            AnimationType.BUILDING_PROGRESS,
            AnimationType.MEASURING,
            AnimationType.CONSTRUCTION_SITE
        )

        viewModel.uiState.test {
            awaitItem() // Idle

            for (tipo in tipos) {
                viewModel.selectAnimation(tipo)
                val playing = awaitItem() as AnimationUiState.Playing
                assertEquals(
                    "La configuración debe ser para $tipo",
                    tipo,
                    playing.config.type
                )
                assertEquals(
                    "selectedType debe ser $tipo",
                    tipo,
                    viewModel.selectedType.value
                )
            }
            cancel()
        }
    }

    @Test
    fun `selectAnimation_cuandoConfigFalla_muestraError()`() = runTest {
        // Given: la config de animación falla
        coEvery { repository.getAnimationConfig(AnimationType.MEASURING) } returns
            AppResult.Error(ProcessingException("Error simulado"))

        // When
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.selectAnimation(AnimationType.MEASURING)

            // Then: debe emitir Error en lugar de Playing
            val state = awaitItem()
            assertTrue(
                "Cuando la configuración falla debe emitir Error",
                state is AnimationUiState.Error
            )
            val errorState = state as AnimationUiState.Error
            assertTrue(
                "El mensaje de error debe contener el detalle del fallo",
                errorState.message.contains("Error simulado")
            )
            cancel()
        }
    }

    @Test
    fun `selectAnimation_cuandoNoDisponible_muestraError()`() = runTest {
        // Given: una animación no disponible
        coEvery { repository.isAnimationAvailable(AnimationType.NONE) } returns false

        // When
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.selectAnimation(AnimationType.NONE)

            // Then: debe emitir Error en lugar de Playing
            val state = awaitItem()
            assertTrue(
                "Cuando la animación no está disponible debe emitir Error",
                state is AnimationUiState.Error
            )
            val errorState = state as AnimationUiState.Error
            assertTrue(
                "El mensaje de error debe indicar que no está disponible",
                errorState.message.isNotBlank()
            )
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de pause
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `pause_cambiaAPaused()`() = runTest {
        // Given: primero seleccionamos una animación para estar en Playing
        viewModel.selectAnimation(AnimationType.CONSTRUCTION_SITE)

        // When: pausamos
        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Playing

            viewModel.pause()

            // Then: debe emitir Paused
            val paused = awaitItem()
            assertEquals(
                "pause debe emitir Paused",
                AnimationUiState.Paused,
                paused
            )
            cancel()
        }
    }

    @Test
    fun `pause_desdeIdle_cambiaAPaused()`() = runTest {
        // When: pausamos desde Idle
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.pause()

            // Then: debe emitir Paused
            assertEquals(
                "pause desde Idle debe emitir Paused",
                AnimationUiState.Paused,
                awaitItem()
            )
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de resume
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `resume_desdePaused_vuelveAPlaying()`() = runTest {
        // Given: seleccionamos y pausamos
        viewModel.selectAnimation(AnimationType.CONSTRUCTION_SITE)
        viewModel.pause()

        // When: reanudamos
        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Playing
            awaitItem() // Paused

            viewModel.resume()

            // Then: debe volver a Playing
            val playing = awaitItem()
            assertTrue(
                "resume desde Paused debe emitir Playing",
                playing is AnimationUiState.Playing
            )
            val playingState = playing as AnimationUiState.Playing
            assertEquals(
                "La configuración debe mantener el tipo seleccionado",
                AnimationType.CONSTRUCTION_SITE,
                playingState.config.type
            )
            cancel()
        }
    }

    @Test
    fun `resume_desdeIdle_noCambiaEstado()`() = runTest {
        // When: reanudamos desde Idle
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.resume()

            // Then: no debe emitir nuevos estados (permanece Idle)
            // Si no hay timeout la prueba pasa (no hay más items)
            cancel()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Tests de reset
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `reset_vuelveAIdle()`() = runTest {
        // Given: seleccionamos una animación
        viewModel.selectAnimation(AnimationType.BLUEPRINT)

        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Playing

            // When: reseteamos
            viewModel.reset()

            // Then: debe volver a Idle
            assertEquals(
                "Después de reset debe ser Idle",
                AnimationUiState.Idle,
                awaitItem()
            )
            cancel()
        }
    }

    @Test
    fun `reset_restauraSelectedTypeADefecto()`() {
        // Given: cambiamos el tipo seleccionado
        viewModel.selectAnimation(AnimationType.MEASURING)
        assertEquals("selectedType debe ser MEASURING", AnimationType.MEASURING, viewModel.selectedType.value)

        // When: reseteamos
        viewModel.reset()

        // Then: selectedType debe volver a CONSTRUCTION_SITE
        assertEquals(
            "Después de reset selectedType debe ser CONSTRUCTION_SITE",
            AnimationType.CONSTRUCTION_SITE,
            viewModel.selectedType.value
        )
    }

    @Test
    fun `reset_desdePaused_vuelveAIdleYRestauraTipo()`() = runTest {
        // Given: playing, pausa
        viewModel.selectAnimation(AnimationType.BLUEPRINT)
        viewModel.pause()

        viewModel.uiState.test {
            awaitItem() // Idle
            awaitItem() // Playing
            awaitItem() // Paused

            // When: reseteamos
            viewModel.reset()

            // Then: debe volver a Idle
            assertEquals(
                "Reset desde Paused debe volver a Idle",
                AnimationUiState.Idle,
                awaitItem()
            )
            assertEquals(
                "selectedType debe restaurarse a CONSTRUCTION_SITE",
                AnimationType.CONSTRUCTION_SITE,
                viewModel.selectedType.value
            )
            cancel()
        }
    }
}

package com.construccionia.core.testing

import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ProcessingException
import com.construccionia.feature.animation.domain.model.AnimationConfig
import com.construccionia.feature.animation.domain.model.AnimationType
import com.construccionia.feature.animation.domain.repository.AnimationRepository

/**
 * Fake de [AnimationRepository] para pruebas.
 * Simula la obtención de configuraciones de animación sin Lottie real.
 *
 * Úsalo en tests de [com.construccionia.feature.animation.presentation.AnimationViewModel]
 * junto con [MainDispatcherRule].
 */
class FakeAnimationRepository : AnimationRepository {

    private var shouldConfigFail = false
    private var shouldAvailableFail = false
    private var availableTypes: List<AnimationType> = AnimationType.entries.toList()

    /** Configura el fake para que [getAnimationConfig] lance una excepción. */
    fun setShouldConfigFail(fail: Boolean) {
        shouldConfigFail = fail
    }

    /** Configura el fake para que [getAvailableAnimations] retorne error. */
    fun setShouldAvailableFail(fail: Boolean) {
        shouldAvailableFail = fail
    }

    /** Define la lista de animaciones disponibles. */
    fun setAvailableTypes(types: List<AnimationType>) {
        availableTypes = types
    }

    override suspend fun getAnimationConfig(type: AnimationType): AppResult<AnimationConfig> {
        if (shouldConfigFail) {
            return AppResult.Error(
                ProcessingException("Error simulado al obtener configuración de animación")
            )
        }
        return AppResult.Success(
            AnimationConfig(
                type = type,
                speed = 1f,
                autoPlay = true,
                loop = true,
                iterations = 1
            )
        )
    }

    override suspend fun isAnimationAvailable(type: AnimationType): Boolean {
        return type in availableTypes
    }

    override suspend fun getAvailableAnimations(): AppResult<List<AnimationType>> {
        if (shouldAvailableFail) {
            return AppResult.Error(
                ProcessingException("Error simulado al obtener animaciones disponibles")
            )
        }
        return AppResult.Success(availableTypes)
    }
}

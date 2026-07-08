package com.construccionia.feature.animation.domain.repository

import com.construccionia.core.common.AppResult
import com.construccionia.feature.animation.domain.model.AnimationConfig
import com.construccionia.feature.animation.domain.model.AnimationType

/**
 * Repositorio para gestionar animaciones Lottie.
 */
interface AnimationRepository {
    /**
     * Obtiene la configuración de animación para un tipo específico.
     * Retorna AppResult para permitir simular errores en tests.
     */
    suspend fun getAnimationConfig(type: AnimationType): AppResult<AnimationConfig>

    /**
     * Verifica si un asset de animación está disponible.
     */
    suspend fun isAnimationAvailable(type: AnimationType): Boolean

    /**
     * Obtiene la lista de animaciones disponibles.
     */
    suspend fun getAvailableAnimations(): AppResult<List<AnimationType>>
}

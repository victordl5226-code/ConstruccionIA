package com.construccionia.feature.animation.data.repository

import com.construccionia.core.common.AppResult
import com.construccionia.feature.animation.data.lottie.LottieAnimationProvider
import com.construccionia.feature.animation.domain.model.AnimationConfig
import com.construccionia.feature.animation.domain.model.AnimationType
import com.construccionia.feature.animation.domain.repository.AnimationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimationRepositoryImpl @Inject constructor(
    private val animationProvider: LottieAnimationProvider
) : AnimationRepository {

    override suspend fun getAnimationConfig(type: AnimationType): AppResult<AnimationConfig> {
        return try {
            val config = animationProvider.getConfig(type)
            AppResult.Success(config)
        } catch (e: Exception) {
            AppResult.Error(
                com.construccionia.core.common.ProcessingException(
                    "Error al obtener configuración de animación: ${e.message}", e
                )
            )
        }
    }

    override suspend fun isAnimationAvailable(type: AnimationType): Boolean {
        return try {
            animationProvider.isAssetAvailable(type)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAvailableAnimations(): AppResult<List<AnimationType>> {
        return try {
            val available = AnimationType.entries.filter { type ->
                type == AnimationType.NONE || animationProvider.isAssetAvailable(type)
            }
            AppResult.Success(available)
        } catch (e: Exception) {
            AppResult.Error(
                com.construccionia.core.common.ProcessingException(
                    "Error al obtener animaciones disponibles: ${e.message}", e
                )
            )
        }
    }
}

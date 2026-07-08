package com.construccionia.feature.animation.data.lottie

import android.content.Context
import com.construccionia.feature.animation.domain.model.AnimationConfig
import com.construccionia.feature.animation.domain.model.AnimationType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Proveedor de animaciones Lottie.
 * Gestiona la carga y disponibilidad de assets de animación.
 */
@Singleton
class LottieAnimationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Obtiene la configuración de animación por defecto para un tipo.
     */
    fun getConfig(type: AnimationType): AnimationConfig {
        return when (type) {
            AnimationType.CONSTRUCTION_SITE -> AnimationConfig(
                type = type,
                speed = 1f,
                autoPlay = true,
                loop = true
            )
            AnimationType.BLUEPRINT -> AnimationConfig(
                type = type,
                speed = 0.8f,
                autoPlay = true,
                loop = true
            )
            AnimationType.BUILDING_PROGRESS -> AnimationConfig(
                type = type,
                speed = 1.2f,
                autoPlay = true,
                loop = false,
                iterations = 3
            )
            AnimationType.MEASURING -> AnimationConfig(
                type = type,
                speed = 1f,
                autoPlay = true,
                loop = true
            )
            AnimationType.NONE -> AnimationConfig(
                type = type,
                autoPlay = false,
                loop = false
            )
        }
    }

    /**
     * Verifica si el asset Lottie para un tipo de animación existe.
     */
    fun isAssetAvailable(type: AnimationType): Boolean {
        val assetName = type.lottieAssetName ?: return false
        return try {
            context.assets.open(assetName).use { true }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtiene la ruta del asset para un tipo de animación.
     */
    fun getAssetPath(type: AnimationType): String? {
        return type.lottieAssetName
    }

    companion object {
        private const val TAG = "LottieAnimationProvider"
    }
}

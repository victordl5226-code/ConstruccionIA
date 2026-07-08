package com.construccionia.feature.animation.domain.model

/**
 * Configuración para la reproducción de animaciones.
 */
data class AnimationConfig(
    val type: AnimationType = AnimationType.NONE,
    val speed: Float = 1f,
    val autoPlay: Boolean = true,
    val loop: Boolean = true,
    val iterations: Int = 1,
    val backgroundColor: String = "#FFFFFF"
)

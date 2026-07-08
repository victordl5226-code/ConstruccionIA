package com.construccionia.feature.animation.domain.model

/**
 * Tipos de animaciones disponibles para la visualización de proyectos.
 */
enum class AnimationType(
    val displayName: String,
    val lottieAssetName: String? = null
) {
    CONSTRUCTION_SITE("Obra en construcción", "construction_site.json"),
    BLUEPRINT("Plano animado", "blueprint.json"),
    BUILDING_PROGRESS("Progreso de obra", "building_progress.json"),
    MEASURING("Mediciones", "measuring.json"),
    NONE("Sin animación", null)
}

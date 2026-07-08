package com.construccionia.feature.export.domain.model

import com.construccionia.core.common.ImageBytes

/**
 * Contenido de una diapositiva para exportar a PowerPoint.
 */
data class SlideContent(
    val title: String,
    val body: String,
    val image: ImageBytes? = null,
    val notes: String = ""
)

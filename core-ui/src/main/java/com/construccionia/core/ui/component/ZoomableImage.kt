package com.construccionia.core.ui.component

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Componente de imagen con soporte para zoom (pinch) y pan (arrastre).
 * Utiliza [Modifier.graphicsLayer] y [detectTransformGestures] para
 * manejar gestos táctiles de zoom y desplazamiento.
 *
 * @param imageModel Modelo de imagen para Coil (URL, ByteArray, URI, Resource, etc.)
 * @param contentDescription Descripción para accesibilidad
 * @param modifier Modifier para personalizar el layout
 * @param maxZoomFactor Factor de zoom máximo permitido (default 5f)
 * @param minZoomFactor Factor de zoom mínimo permitido (default 1f)
 */
@Composable
fun ZoomableImage(
    imageModel: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxZoomFactor: Float = 5f,
    minZoomFactor: Float = 1f
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // Aplicar zoom con límites
                    val newScale = (scale * zoom).coerceIn(minZoomFactor, maxZoomFactor)

                    // Ajustar el offset para que el zoom se centre en el punto de pinza
                    val scaleChange = newScale / scale
                    val newOffset = (offset + centroid) * scaleChange - centroid + pan

                    scale = newScale
                    offset = newOffset
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

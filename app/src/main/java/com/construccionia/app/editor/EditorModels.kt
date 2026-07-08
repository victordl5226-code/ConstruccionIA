package com.construccionia.app.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Representa una capa visual dentro de la vista explosionada.
 * Cada capa puede ser una imagen independiente o una región de la imagen original.
 */
data class EditorLayer(
    val id: Long,
    val name: String,
    /** Bitmap de la capa (puede ser un recorte de la imagen original). */
    val bitmap: Bitmap? = null,
    /** Ruta o URI de la imagen original. */
    val imageSource: String = "",
    /** Opacidad de la capa 0.0 a 1.0. */
    val opacity: Float = 1.0f,
    /** Separación vertical (en dp/píxeles) en modo explosionado. */
    val explodeOffset: Float = 0f,
    /** Visible o no. */
    val isVisible: Boolean = true,
    /** Color distintivo de la capa (para bordes o selector). */
    val tintColor: Color = Color.Gray
)

/**
 * Anotación sobre una capa: texto, flecha o callout.
 */
sealed class LayerAnnotation {
    /** Texto libre posicionado sobre la capa. */
    data class TextLabel(
        val id: Long,
        val text: String,
        val position: Offset,
        val color: Color = Color.Red,
        val fontSize: Float = 14f
    ) : LayerAnnotation()

    /** Flecha direccional entre dos puntos. */
    data class Arrow(
        val id: Long,
        val start: Offset,
        val end: Offset,
        val color: Color = Color.Red,
        val strokeWidth: Float = 3f
    ) : LayerAnnotation()

    /** Callout: rectángulo con texto y flecha apuntando a un punto. */
    data class Callout(
        val id: Long,
        val text: String,
        val targetPoint: Offset,
        val labelPosition: Offset,
        val color: Color = Color.Blue
    ) : LayerAnnotation()
}

/**
 * Estado completo del editor de vista explosionada.
 */
data class EditorState(
    /** Imagen base cargada. */
    val baseImageUri: String = "",
    /** Bitmap de la imagen base (escalada para visualización). */
    val baseBitmap: Bitmap? = null,
    /** Lista de capas definidas. */
    val layers: List<EditorLayer> = emptyList(),
    /** Anotaciones globales (no ligadas a una capa). */
    val annotations: List<LayerAnnotation> = emptyList(),
    /** Nivel de explosión: 0..1 donde 1 es completamente separado. */
    val explodeLevel: Float = 0f,
    /** Factor de zoom. */
    val zoom: Float = 1f,
    /** Desplazamiento panorámico. */
    val panOffset: Offset = Offset.Zero,
    /** Capa actualmente seleccionada. */
    val selectedLayerId: Long? = null,
    /** Modo del editor. */
    val editorMode: EditorMode = EditorMode.VIEW,
    /** Indicador de carga. */
    val isLoading: Boolean = false,
    /** Mensaje de error. */
    val errorMessage: String? = null
)

/** Modos de edición. */
enum class EditorMode {
    /** Solo visualización, sin edición. */
    VIEW,
    /** Seleccionar y mover capas. */
    SELECT,
    /** Añadir anotaciones de texto. */
    TEXT,
    /** Dibujar flechas. */
    ARROW,
    /** Añadir callouts. */
    CALLOUT
}

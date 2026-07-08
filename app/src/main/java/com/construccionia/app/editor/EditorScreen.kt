package com.construccionia.app.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla del editor de vista explosionada.
 * Muestra la imagen en un Canvas con soporte para capas, anotaciones,
 * zoom/pan y control de explosión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    imageUri: String,
    onBack: () -> Unit
) {
    val state by viewModel.editorState.collectAsState()
    val context = LocalContext.current

    // Cargar imagen al iniciar
    androidx.compose.runtime.LaunchedEffect(imageUri) {
        if (imageUri.isNotBlank() && state.baseBitmap == null) {
            viewModel.loadImage(imageUri)
        }
    }

    // Diálogo para añadir texto
    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var textTapPosition by remember { mutableStateOf(Offset.Zero) }

    // Diálogo para añadir callout
    var showCalloutDialog by remember { mutableStateOf(false) }
    var calloutText by remember { mutableStateOf("") }

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Añadir texto") },
            text = {
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Texto de la anotación") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.addTextAnnotation(textInput, textTapPosition)
                        textInput = ""
                        showTextDialog = false
                    }
                }) { Text("Añadir") }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showCalloutDialog) {
        AlertDialog(
            onDismissRequest = { showCalloutDialog = false },
            title = { Text("Añadir callout") },
            text = {
                TextField(
                    value = calloutText,
                    onValueChange = { calloutText = it },
                    label = { Text("Texto del callout") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (calloutText.isNotBlank()) {
                        viewModel.addCallout(calloutText, Offset.Zero, Offset.Zero)
                        calloutText = ""
                        showCalloutDialog = false
                    }
                }) { Text("Añadir") }
            },
            dismissButton = {
                TextButton(onClick = { showCalloutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    BottomSheetScaffold(
        sheetContent = {
            EditorBottomPanel(
                state = state,
                onExplodeChange = { viewModel.setExplodeLevel(it) },
                onLayerSelect = { viewModel.selectLayer(it) },
                onLayerToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                onLayerRemove = { viewModel.removeLayer(it) },
                onLayerOpacityChange = { id, opacity -> viewModel.setLayerOpacity(id, opacity) }
            )
        },
        sheetPeekHeight = 140.dp
    ) { paddingValues ->
        Scaffold(
            modifier = Modifier.padding(paddingValues),
            topBar = {
                TopAppBar(
                    title = { Text("Editor Explosionado") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        // Selector de modo
                        EditorModeChips(
                            currentMode = state.editorMode,
                            onModeChange = { viewModel.setEditorMode(it) }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            floatingActionButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botón de zoom +
                    SmallFAB(icon = Icons.Default.ZoomIn) {
                        viewModel.setZoom(state.zoom * 1.2f)
                    }
                    // Botón de zoom -
                    SmallFAB(icon = Icons.Default.ZoomOut) {
                        viewModel.setZoom(state.zoom / 1.2f)
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                } else if (state.errorMessage != null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(state.errorMessage ?: "")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.clearError() }) {
                            Text("Reintentar")
                        }
                    }
                } else if (state.baseBitmap != null) {
                    // Canvas principal con zoom/pan
                    var currentZoom by remember { mutableFloatStateOf(state.zoom) }
                    var currentPan by remember { mutableStateOf(state.panOffset) }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    currentZoom = (currentZoom * zoom).coerceIn(0.5f, 5f)
                                    currentPan = currentPan + pan
                                    viewModel.setZoom(currentZoom)
                                    viewModel.setPanOffset(currentPan)
                                }
                            }
                            .pointerInput(state.editorMode) {
                                detectTapGestures { tapOffset ->
                                    when (state.editorMode) {
                                        EditorMode.TEXT -> {
                                            textTapPosition = tapOffset
                                            showTextDialog = true
                                        }
                                        EditorMode.CALLOUT -> {
                                            showCalloutDialog = true
                                        }
                                        EditorMode.SELECT -> {
                                            // Seleccionar capa por proximidad al tap
                                            // (implementación simplificada)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                    ) {
                        // Aplicar transformaciones
                        withTransform({
                            translate(currentPan.x, currentPan.y)
                            scale(currentZoom, currentZoom, Offset(size.width / 2, size.height / 2))
                        }) {
                            val imageBitmap = state.baseBitmap!!.asImageBitmap()

                            // Dibujar capas
                            for (layer in state.layers) {
                                if (!layer.isVisible) continue

                                val explodeY = layer.explodeOffset * state.explodeLevel * size.height / 4
                                val layerBitmap = layer.bitmap?.asImageBitmap() ?: imageBitmap

                                translate(top = explodeY) {
                                    // Dibujar la imagen de la capa centrada con opacidad
                                    val imgWidth = layerBitmap.width
                                    val imgHeight = layerBitmap.height
                                    val scale = minOf(
                                        size.width / imgWidth,
                                        size.height / imgHeight
                                    ).coerceAtMost(1f)
                                    val scaledW = (imgWidth * scale).toInt()
                                    val scaledH = (imgHeight * scale).toInt()
                                    drawImage(
                                        image = layerBitmap,
                                        dstOffset = androidx.compose.ui.unit.IntOffset(
                                            ((size.width - scaledW) / 2).toInt(),
                                            ((size.height - scaledH) / 2).toInt()
                                        ),
                                        dstSize = androidx.compose.ui.unit.IntSize(scaledW, scaledH),
                                        alpha = layer.opacity
                                    )

                                    // Si está seleccionada, dibujar borde
                                    if (layer.id == state.selectedLayerId) {
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset.Zero,
                                            size = size,
                                            style = Stroke(width = 3f)
                                        )
                                        drawRect(
                                            color = layer.tintColor,
                                            topLeft = Offset.Zero,
                                            size = size,
                                            style = Stroke(width = 1f)
                                        )
                                    }
                                }
                            }

                            // Dibujar anotaciones
                            for (annotation in state.annotations) {
                                drawAnnotation(annotation)
                            }
                        }
                    }

                    // Indicador de modo activo
                    if (state.editorMode != EditorMode.VIEW && state.editorMode != EditorMode.SELECT) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Modo: ${state.editorMode.name} — Toca la imagen",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dibuja una anotación en el Canvas.
 */
private fun DrawScope.drawAnnotation(annotation: LayerAnnotation) {
    when (annotation) {
        is LayerAnnotation.TextLabel -> {
            drawContext.canvas.nativeCanvas.drawText(
                annotation.text,
                annotation.position.x,
                annotation.position.y,
                android.graphics.Paint().apply {
                    color = annotation.color.hashCode()
                    textSize = annotation.fontSize * 3 // Aprox dp a px
                    isAntiAlias = true
                }
            )
        }
        is LayerAnnotation.Arrow -> {
            drawLine(
                color = annotation.color,
                start = annotation.start,
                end = annotation.end,
                strokeWidth = annotation.strokeWidth
            )
            // Punta de flecha simple
            val direction = (annotation.end - annotation.start)
            val length = direction.getDistance()
            if (length > 0) {
                val unit = direction / length
                val perpendicular = Offset(-unit.y, unit.x)
                val arrowSize = 15f
                drawLine(
                    color = annotation.color,
                    start = annotation.end,
                    end = annotation.end - unit * arrowSize + perpendicular * arrowSize * 0.4f,
                    strokeWidth = annotation.strokeWidth
                )
                drawLine(
                    color = annotation.color,
                    start = annotation.end,
                    end = annotation.end - unit * arrowSize - perpendicular * arrowSize * 0.4f,
                    strokeWidth = annotation.strokeWidth
                )
            }
        }
        is LayerAnnotation.Callout -> {
            // Círculo en el punto objetivo
            drawCircle(
                color = annotation.color,
                radius = 8f,
                center = annotation.targetPoint
            )
            // Línea hacia la etiqueta
            drawLine(
                color = annotation.color,
                start = annotation.targetPoint,
                end = annotation.labelPosition,
                strokeWidth = 2f
            )
            // Texto de la etiqueta
            drawContext.canvas.nativeCanvas.drawText(
                annotation.text,
                annotation.labelPosition.x,
                annotation.labelPosition.y - 10f,
                android.graphics.Paint().apply {
                    color = annotation.color.hashCode()
                    textSize = 36f
                    isAntiAlias = true
                }
            )
        }
    }
}

/**
 * Panel inferior con controles de capas y explosión.
 */
@Composable
private fun EditorBottomPanel(
    state: EditorState,
    onExplodeChange: (Float) -> Unit,
    onLayerSelect: (Long?) -> Unit,
    onLayerToggleVisibility: (Long) -> Unit,
    onLayerRemove: (Long) -> Unit,
    onLayerOpacityChange: (Long, Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Control de explosión", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        // Slider de explosión
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("0%", fontSize = 12.sp)
            Slider(
                value = state.explodeLevel,
                onValueChange = onExplodeChange,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text("100%", fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))
        Text("Capas (${state.layers.size})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        // Lista de capas
        LazyColumn(
            modifier = Modifier.height(180.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.layers, key = { it.id }) { layer ->
                LayerItem(
                    layer = layer,
                    isSelected = layer.id == state.selectedLayerId,
                    onSelect = { onLayerSelect(layer.id) },
                    onToggleVisibility = { onLayerToggleVisibility(layer.id) },
                    onRemove = { onLayerRemove(layer.id) },
                    onOpacityChange = { opacity -> onLayerOpacityChange(layer.id, opacity) }
                )
            }
        }
    }
}

/**
 * Elemento individual de capa en la lista.
 */
@Composable
private fun LayerItem(
    layer: EditorLayer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onRemove: () -> Unit,
    onOpacityChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de color
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(layer.tintColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = layer.name,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp
                )
                // Visibilidad
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (layer.isVisible) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (layer.isVisible) "Visible" else "Oculto",
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Eliminar
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Eliminar capa", modifier = Modifier.size(16.dp))
                }
            }

            // Control de opacidad
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Opacidad", fontSize = 11.sp)
                    Slider(
                        value = layer.opacity,
                        onValueChange = onOpacityChange,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        valueRange = 0f..1f
                    )
                    Text("${(layer.opacity * 100).toInt()}%", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * Chips para seleccionar el modo del editor.
 */
@Composable
private fun EditorModeChips(
    currentMode: EditorMode,
    onModeChange: (EditorMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
            selected = currentMode == EditorMode.VIEW,
            onClick = { onModeChange(EditorMode.VIEW) },
            label = { Text("Ver", fontSize = 11.sp) },
            leadingIcon = {
                Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(14.dp))
            }
        )
        FilterChip(
            selected = currentMode == EditorMode.TEXT,
            onClick = { onModeChange(EditorMode.TEXT) },
            label = { Text("Texto", fontSize = 11.sp) },
            leadingIcon = {
                Icon(Icons.Default.TextFields, null, modifier = Modifier.size(14.dp))
            }
        )
        FilterChip(
            selected = currentMode == EditorMode.ARROW,
            onClick = { onModeChange(EditorMode.ARROW) },
            label = { Text("Flecha", fontSize = 11.sp) },
            leadingIcon = {
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(14.dp))
            }
        )
    }
}

@Composable
private fun SmallFAB(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
    }
}

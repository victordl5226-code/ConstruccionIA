package com.construccionia.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.construccionia.app.data.models.Infographic
import com.construccionia.app.viewmodel.ExportState
import com.construccionia.app.viewmodel.GalleryViewModel

/**
 * Pantalla de galería que muestra las infografías descargadas.
 * Soporta selección múltiple, zoom, OCR y exportación a PPT.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    navController: NavController
) {
    val galleryState by viewModel.galleryState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val context = LocalContext.current

    // Diálogo de zoom para imagen ampliada
    var zoomImagePath by remember { mutableStateOf<String?>(null) }

    // Colectar eventos de compartir y lanzar Intent
    LaunchedEffect(Unit) {
        viewModel.shareUriEvent.collect { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir infografía"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galería de Infografías") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Siempre visible: botón de cámara para OCR
                    IconButton(onClick = {
                        navController.navigate(
                            com.construccionia.app.ui.navigation.Screen.CameraOcr.route
                        )
                    }) {
                        Icon(Icons.Default.CameraAlt,
                            contentDescription = "Cámara OCR",
                            modifier = Modifier.size(20.dp))
                    }

                    if (galleryState.selectedIds.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { viewModel.exportSelectedToPdf() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  PDF (${galleryState.selectedIds.size})")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.exportSelectedToPpt() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  PPT (${galleryState.selectedIds.size})")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Barra de búsqueda ──
            if (!galleryState.isLoading && galleryState.infographics.isNotEmpty()) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Buscar infografías…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    galleryState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    viewModel.filteredInfographics.isEmpty() -> {
                        // Estado vacío
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = if (viewModel.searchQuery.isNotEmpty())
                                        "No se encontraron resultados para \"${viewModel.searchQuery}\""
                                    else "No hay imágenes aún.\n¡Genera tu primera infografía!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        // Grid de infografías con pull-to-refresh
                        val pullRefreshState = rememberPullToRefreshState()

                        Box(Modifier.nestedScroll(pullRefreshState.nestedScrollConnection)) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = viewModel.filteredInfographics,
                                    key = { it.id }
                                ) { infographic ->
                                    InfographicCard(
                                        infographic = infographic,
                                        isSelected = infographic.id in galleryState.selectedIds,
                                        onToggleSelection = { viewModel.toggleSelection(infographic) },
                                        onZoom = { zoomImagePath = infographic.filePath },
                                        onOcr = {
                                            navController.navigate(
                                                com.construccionia.app.ui.navigation.Screen.OcrStandalone
                                                    .createRoute(java.net.URLEncoder.encode(infographic.filePath, "UTF-8"))
                                            )
                                        },
                                        onInfo = {
                                            navController.navigate(
                                                com.construccionia.app.ui.navigation.Screen.Detail
                                                    .createRoute(infographic.id)
                                            )
                                        },
                                        onDelete = { viewModel.deleteInfographic(infographic) },
                                        onExportPpt = { viewModel.exportSingleToPpt(infographic.filePath) },
                                        onExportPdf = { viewModel.exportSingleToPdf(infographic.filePath) },
                                        onEdit = {
                                            navController.navigate(
                                                com.construccionia.app.ui.navigation.Screen.Editor
                                                    .createRoute(java.net.URLEncoder.encode(infographic.filePath, "UTF-8"))
                                            )
                                        },
                                        onView3D = {
                                            navController.navigate(
                                                com.construccionia.app.ui.navigation.Screen.ModelViewer.route
                                            )
                                        },
                                        onShare = { viewModel.shareInfographic(infographic) },
                                        onRename = { viewModel.renameTarget = infographic }
                                    )
                                }
                            }

                            // Disparar refresco cuando el pull se complete
                            if (pullRefreshState.isRefreshing) {
                                LaunchedEffect(Unit) {
                                    viewModel.refreshGallery()
                                }
                            }

                            // Finalizar indicador cuando el ViewModel termine
                            LaunchedEffect(viewModel.isRefreshing) {
                                if (!viewModel.isRefreshing) {
                                    pullRefreshState.endRefresh()
                                }
                            }

                            // Indicador visual de pull-to-refresh
                            PullToRefreshContainer(
                                state = pullRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }
                }

                // Zoom dialog
                zoomImagePath?.let { path ->
                    ZoomableImageDialog(
                        imagePath = path,
                        onDismiss = { zoomImagePath = null }
                    )
                }

                // Export progress
                if (exportState is ExportState.Exporting) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                "Exportando…",
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }

                // Rename dialog
                val renameTarget = viewModel.renameTarget
                if (renameTarget != null) {
                    var renameText by remember(renameTarget) { mutableStateOf(renameTarget.name) }
                    AlertDialog(
                        onDismissRequest = { viewModel.renameTarget = null },
                        title = { Text("Renombrar infografía") },
                        text = {
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                label = { Text("Nuevo nombre") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.renameInfographic(renameTarget, renameText)
                                }
                            ) {
                                Text("Renombrar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.renameTarget = null }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                // Export success/error handled via snackbar in MainActivity
            }
        }
    }
}

@Composable
private fun InfographicCard(
    infographic: Infographic,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onZoom: () -> Unit,
    onOcr: () -> Unit,
    onInfo: () -> Unit = {},
    onDelete: () -> Unit,
    onExportPpt: () -> Unit,
    onExportPdf: () -> Unit = {},
    onEdit: () -> Unit,
    onView3D: () -> Unit,
    onShare: () -> Unit = {},
    onRename: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            // Imagen
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(infographic.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = infographic.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp)),
                contentScale = ContentScale.Crop
            )

            // Checkbox de selección
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }

        // Nombre e iconos de acción
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = infographic.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row {
                    IconButton(onClick = onInfo, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Detalle", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onView3D, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Explore, contentDescription = "Ver 3D", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editor", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onZoom, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onOcr, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.TextSnippet, contentDescription = "OCR", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onExportPdf, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onExportPpt, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Exportar PPT", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Renombrar", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ── Componente ZoomableImage ──

@Composable
fun ZoomableImage(
    imageUri: Uri,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(true)
                .build(),
            contentDescription = "Imagen ampliada",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun ZoomableImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val uri = Uri.parse("file://$imagePath")
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vista ampliada") },
        text = {
            ZoomableImage(
                imageUri = uri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

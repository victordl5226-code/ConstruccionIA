package com.construccionia.feature.viewer.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.construccionia.core.common.toUserMessage
import com.construccionia.core.ui.component.ErrorMessage
import com.construccionia.core.ui.component.LoadingIndicator
import com.construccionia.core.ui.component.ZoomableImage
import com.construccionia.feature.viewer.presentation.ViewerUiState
import com.construccionia.feature.viewer.presentation.ViewerViewModel

/**
 * Pantalla visualizadora de imágenes con soporte para zoom y pan.
 * Permite cargar una imagen demo y navegar a OCR o Exportación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    imageId: String? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToOcr: (String) -> Unit = {},
    onNavigateToExport: (String) -> Unit = {},
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Cargar imagen si se proporciona un ID
    if (imageId != null && uiState is ViewerUiState.Idle) {
        viewModel.loadImage(imageId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visor de Imagen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Área de visualización de imagen
            when (val state = uiState) {
                is ViewerUiState.Idle -> {
                    // Estado inicial: mostrar opciones
                    IdleContent(
                        onLoadDemo = { viewModel.loadDemoImage() }
                    )
                }

                is ViewerUiState.Loading -> {
                    LoadingIndicator(message = "Cargando imagen...")
                }

                is ViewerUiState.Loaded -> {
                    // Mostrar imagen con zoom
                    // Usamos el imageUrl si está disponible, o los bytes directamente
                    val imageModel: Any = state.imageUrl ?: state.imageBytes.bytes
                    ZoomableImage(
                        imageModel = imageModel,
                        contentDescription = "Imagen cargada",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToOcr(imageId ?: "demo") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Escanear texto con OCR",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OCR")
                        }
                        Button(
                            onClick = { onNavigateToExport(imageId ?: "demo") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Exportar a PowerPoint",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar")
                        }
                    }
                }

                is ViewerUiState.Error -> {
                    ErrorMessage(
                        message = state.exception.toUserMessage(),
                        onRetry = {
                            if (imageId != null) viewModel.loadImage(imageId)
                            else viewModel.loadDemoImage()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Contenido del estado inicial: opciones para cargar imagen.
 */
@Composable
private fun IdleContent(
    onLoadDemo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Visualizador de Imágenes",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Usa el zoom y el arrastre para explorar la imagen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLoadDemo,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cargar Proyecto Demo")
        }
    }
}

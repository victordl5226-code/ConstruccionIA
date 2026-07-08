package com.construccionia.feature.generation.presentation.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.construccionia.core.common.toUserMessage
import com.construccionia.core.ui.component.ErrorMessage
import com.construccionia.core.ui.component.LoadingIndicator
import com.construccionia.feature.generation.domain.model.AspectRatio
import com.construccionia.feature.generation.domain.model.PromptStyle
import com.construccionia.feature.generation.presentation.GenerationUiState
import com.construccionia.feature.generation.presentation.GenerationViewModel

/**
 * Pantalla principal de generación de imágenes con IA.
 * Permite al usuario ingresar un prompt, seleccionar estilo y proporción,
 * y visualizar la imagen generada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationScreen(
    onNavigateToViewer: (String) -> Unit = {},
    viewModel: GenerationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val promptText by viewModel.promptText.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val selectedAspectRatio by viewModel.selectedAspectRatio.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Generar Imagen")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo de texto para el prompt
            OutlinedTextField(
                value = promptText,
                onValueChange = { viewModel.updatePrompt(it) },
                label = { Text("Describe la imagen que deseas generar") },
                placeholder = { Text("Ej: Una casa moderna con paneles solares...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                singleLine = false
            )

            // Selector de estilo visual
            Text(
                text = "Estilo visual",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StyleSelector(
                selectedStyle = selectedStyle,
                onStyleSelected = { viewModel.updateStyle(it) }
            )

            // Selector de proporción de aspecto
            Text(
                text = "Proporción de aspecto",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AspectRatioSelector(
                selectedRatio = selectedAspectRatio,
                onRatioSelected = { viewModel.updateAspectRatio(it) }
            )

            // Botón de generación
            Button(
                onClick = { viewModel.generate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = promptText.isNotBlank() && uiState !is GenerationUiState.Generating
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generar imagen",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido dinámico según el estado
            when (val state = uiState) {
                is GenerationUiState.Idle -> {
                    EmptyState(modifier = Modifier.fillMaxWidth())
                }

                is GenerationUiState.Editing -> {
                    // No mostrar nada extra, el usuario sigue editando
                }

                is GenerationUiState.Generating -> {
                    LoadingIndicator(message = "Generando imagen con IA...")
                }

                is GenerationUiState.Success -> {
                    val imageId = state.generatedImage.id
                    GenerationResultContent(
                        imageBytes = state.generatedImage.imageBytes.bytes,
                        mimeType = state.generatedImage.mimeType,
                        promptUsed = state.generatedImage.promptUsed,
                        onViewImage = {
                            onNavigateToViewer(imageId)
                        },
                        onGenerateNew = { viewModel.reset() }
                    )
                }

                is GenerationUiState.Error -> {
                    ErrorMessage(
                        message = state.exception.toUserMessage(),
                        onRetry = { viewModel.generate() }
                    )
                }
            }
        }
    }
}

/**
 * Selector de estilo visual con chips.
 */
@Composable
private fun StyleSelector(
    selectedStyle: PromptStyle,
    onStyleSelected: (PromptStyle) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PromptStyle.entries.forEach { style ->
            FilterChip(
                selected = style == selectedStyle,
                onClick = { onStyleSelected(style) },
                label = { Text(style.displayName, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Selector de proporción de aspecto con chips.
 */
@Composable
private fun AspectRatioSelector(
    selectedRatio: AspectRatio,
    onRatioSelected: (AspectRatio) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AspectRatio.entries.forEach { ratio ->
            FilterChip(
                selected = ratio == selectedRatio,
                onClick = { onRatioSelected(ratio) },
                label = { Text(ratio.displayName, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PhotoSizeSelectLarge,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Estado vacío cuando no se ha generado ninguna imagen.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Describe la imagen que necesitas",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Escribe un prompt detallado y selecciona el estilo.\nLa IA generará una imagen basada en tu descripción.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Muestra el resultado de la generación: la imagen generada.
 */
@Composable
private fun GenerationResultContent(
    imageBytes: ByteArray,
    mimeType: String,
    promptUsed: String,
    onViewImage: () -> Unit,
    onGenerateNew: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Imagen generada",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Vista previa de la imagen generada
            // Usamos Coil para cargar la imagen desde bytes
            AsyncImage(
                model = imageBytes,
                contentDescription = "Imagen generada: $promptUsed",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGenerateNew,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Generar otra")
                }
                Button(
                    onClick = onViewImage,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver imagen")
                }
            }
        }
    }
}

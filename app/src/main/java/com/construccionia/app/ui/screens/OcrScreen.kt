package com.construccionia.app.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.construccionia.app.viewmodel.GalleryViewModel
import java.net.URLDecoder

/**
 * Pantalla que muestra el resultado del OCR sobre una imagen seleccionada.
 * Permite editar y copiar el texto reconocido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    viewModel: GalleryViewModel,
    imagePath: String,
    navController: NavController
) {
    val ocrState by viewModel.ocrState.collectAsState()
    val context = LocalContext.current

    // Decodificar la ruta URL
    val decodedPath = remember {
        URLDecoder.decode(imagePath, "UTF-8")
    }

    var isEditing by remember { mutableStateOf(false) }
    var editableText by remember { mutableStateOf("") }

    // Iniciar OCR al cargar la pantalla
    LaunchedEffect(decodedPath) {
        viewModel.performOcr(decodedPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR Técnico") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    if (ocrState.recognizedText != null) {
                        IconButton(onClick = {
                            val text = if (isEditing) editableText else ocrState.recognizedText ?: ""
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("OCR Text", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Texto copiado", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Imagen pequeña de referencia
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(decodedPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagen analizada",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estado de procesamiento
                when {
                    ocrState.isProcessing -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Text(
                                    "Procesando OCR…",
                                    modifier = Modifier.padding(top = 16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    ocrState.error != null -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ocrState.error ?: "Error desconocido",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.performOcr(decodedPath) }) {
                            Text("Reintentar")
                        }
                    }
                    ocrState.recognizedText != null -> {
                        // Botón para modo edición
                        Button(
                            onClick = {
                                if (!isEditing) {
                                    editableText = ocrState.recognizedText ?: ""
                                }
                                isEditing = !isEditing
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (isEditing) Icons.Default.Visibility else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(if (isEditing) "Ver resultado" else "Editar texto")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Texto reconocido (editable o no)
                        if (isEditing) {
                            OutlinedTextField(
                                value = editableText,
                                onValueChange = { editableText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                shape = RoundedCornerShape(8.dp),
                                label = { Text("Editar texto técnico") }
                            )
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = ocrState.recognizedText ?: "",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón copiar
                        OutlinedButton(
                            onClick = {
                                val text = if (isEditing) editableText else ocrState.recognizedText ?: ""
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                        as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("OCR Text", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Texto copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Text("  Copiar texto")
                        }
                    }
                }
            }
        }
    }
}

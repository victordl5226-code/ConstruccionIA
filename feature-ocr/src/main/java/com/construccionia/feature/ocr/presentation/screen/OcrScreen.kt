package com.construccionia.feature.ocr.presentation.screen

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.construccionia.core.common.extension.loadImageBytes
import com.construccionia.core.common.toUserMessage
import com.construccionia.core.ui.component.ErrorMessage
import com.construccionia.core.ui.component.LoadingIndicator
import com.construccionia.feature.ocr.presentation.OcrUiState
import com.construccionia.feature.ocr.presentation.OcrViewModel
import java.io.File

/**
 * Pantalla de reconocimiento OCR.
 * Permite seleccionar o capturar una imagen y extraer su texto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToResult: () -> Unit = {},
    viewModel: OcrViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // URI para la foto capturada con la cámara
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Lanzador para solicitar permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido: crear archivo y lanzar cámara
            val photoFile = File(
                context.cacheDir,
                "ocr/ocr_${System.currentTimeMillis()}.jpg"
            )
            photoFile.parentFile?.mkdirs()
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraImageUri = photoUri
            cameraLauncher.launch(photoUri)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Permiso de cámara denegado. Usa la galería para seleccionar imágenes."
                )
            }
        }
    }

    // Lanzador para seleccionar imagen de la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.loadImageBytes(it)?.let { image ->
                viewModel.recognizeText(image)
            }
        }
    }

    // Lanzador para capturar foto con la cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri?.let { uri ->
                context.contentResolver.loadImageBytes(uri)?.let { image ->
                    viewModel.recognizeText(image)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Reconocimiento OCR") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Navegar cuando se detecta texto (usando LaunchedEffect para evitar loops)
            if (uiState is OcrUiState.TextDetected) {
                LaunchedEffect(Unit) {
                    onNavigateToResult()
                }
            }

            when (val state = uiState) {
                is OcrUiState.Idle -> {
                    IdleOcrContent(
                        onSelectFromGallery = {
                            galleryLauncher.launch("image/*")
                        },
                        onCaptureFromCamera = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }

                is OcrUiState.Loading -> {
                    LoadingIndicator(message = "Analizando imagen...")
                }

                is OcrUiState.TextDetected -> {
                    // La navegación se maneja arriba con LaunchedEffect
                    LoadingIndicator(message = "Texto detectado, redirigiendo...")
                }

                is OcrUiState.Error -> {
                    ErrorMessage(
                        message = state.exception.toUserMessage(),
                        onRetry = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleOcrContent(
    onSelectFromGallery: () -> Unit,
    onCaptureFromCamera: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.DocumentScanner,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Reconocimiento de Texto",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selecciona una imagen o toma una foto\npara extraer el texto con OCR",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSelectFromGallery,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Seleccionar de galería")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCaptureFromCamera,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tomar foto")
        }
    }
}

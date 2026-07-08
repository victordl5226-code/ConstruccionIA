package com.construccionia.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.construccionia.app.R
import com.construccionia.app.export.PptExportHelper
import com.construccionia.app.viewmodel.GalleryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Pantalla de proyectos demo.
 * Muestra una infografía técnica precargada en res/drawable.
 * Soporta tanto PNG como VectorDrawable XML.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(
    viewModel: GalleryViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val hasDemoImage = remember {
        val resId = context.resources.getIdentifier(
            "demo_house_infographic",
            "drawable",
            context.packageName
        )
        resId != 0
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { Text("Proyectos Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        navController.navigate(
                            com.construccionia.app.ui.navigation.Screen.CameraOcr.route
                        )
                    }) {
                        Icon(Icons.Default.CameraAlt,
                            contentDescription = "Cámara OCR",
                            modifier = Modifier.size(20.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título
            Text(
                text = "Infografía Técnica: Casa Habitación",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Vista explosionada de estructura de casa de 2 pisos con instalaciones y acabados.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Imagen demo
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (hasDemoImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(R.drawable.demo_house_infographic)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Infografía demo de casa",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Placeholder visual cuando no hay imagen precargada
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.padding(bottom = 8.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Imagen demo no disponible\nColoca 'demo_house_infographic.png' en res/drawable/",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Esta infografía técnica muestra los planos estructurales, " +
                        "instalaciones eléctricas, hidrosanitarias y acabados de una " +
                        "vivienda de dos niveles con vista explosionada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de acción
            Button(
                onClick = {
                    scope.launch {
                        if (hasDemoImage) {
                            snackbarHostState.showSnackbar("Exportando a PowerPoint...")
                            exportDemoToPpt(context, snackbarHostState)
                        } else {
                            snackbarHostState.showSnackbar(
                                "Coloca 'demo_house_infographic.png' en res/drawable/ para usar esta función"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Text("  Exportar a PowerPoint")
            }

            Button(
                onClick = {
                    scope.launch {
                        if (hasDemoImage) {
                            openDemoOcr(context, navController, snackbarHostState)
                        } else {
                            snackbarHostState.showSnackbar(
                                "Coloca 'demo_house_infographic.png' en res/drawable/ para usar esta función"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Text("  Analizar con OCR")
            }

            // Botón Visor 3D
            Button(
                onClick = {
                    navController.navigate(
                        com.construccionia.app.ui.navigation.Screen.ModelViewer.route
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Explore, contentDescription = null)
                Text("  Ver modelo 3D de la casa")
            }
        }
    }
}

/**
 * Convierte un recurso drawable (incluyendo VectorDrawable XML) a Bitmap.
 * Usa AppCompatResources.getDrawable() para soportar vectores.
 */
private fun drawableToBitmap(context: Context, drawableResId: Int): Bitmap? {
    return try {
        val drawable: Drawable? = AppCompatResources.getDrawable(context, drawableResId)
        if (drawable == null) return null

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.takeIf { it > 0 } ?: 800,
            drawable.intrinsicHeight.takeIf { it > 0 } ?: 600,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

/**
 * Exporta la imagen demo a PowerPoint.
 */
private suspend fun exportDemoToPpt(
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    try {
        val uri = withContext(Dispatchers.IO) {
            val resId = context.resources.getIdentifier(
                "demo_house_infographic",
                "drawable",
                context.packageName
            )

            val bitmap = drawableToBitmap(context, resId)
                ?: throw Exception("No se pudo decodificar la imagen demo")

            // Guardar temporalmente y exportar
            val tempDir = File(context.cacheDir, "demo_exports")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, "demo_infographic.png")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val pptHelper = PptExportHelper(context)
            val resultUri = pptHelper.exportToPpt(listOf(tempFile.absolutePath))
            // Limpiar temp
            tempFile.delete()
            resultUri
        }

        if (uri != null) {
            snackbarHostState.showSnackbar("PPT exportado exitosamente")
        } else {
            snackbarHostState.showSnackbar("Error al exportar PPT")
        }
    } catch (e: Exception) {
        snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
    }
}

/**
 * Abre el OCR con la imagen demo.
 */
private suspend fun openDemoOcr(
    context: Context,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    try {
        val bitmap = withContext(Dispatchers.IO) {
            val resId = context.resources.getIdentifier(
                "demo_house_infographic",
                "drawable",
                context.packageName
            )
            val bmp = drawableToBitmap(context, resId)
                ?: throw Exception("No se pudo decodificar la imagen demo")

            val tempDir = File(context.cacheDir, "demo_ocr")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, "demo_infographic.png")
            FileOutputStream(tempFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            tempFile.absolutePath
        }

        navController.navigate(
            com.construccionia.app.ui.navigation.Screen.OcrStandalone
                .createRoute(java.net.URLEncoder.encode(bitmap, "UTF-8"))
        )
    } catch (e: Exception) {
        snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
    }
}

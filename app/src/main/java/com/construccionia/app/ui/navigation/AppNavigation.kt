package com.construccionia.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rutas de navegación de la aplicación.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object WebView : Screen(
        route = "webview",
        title = "Generador IA",
        icon = Icons.Default.WebAsset
    )

    data object Gallery : Screen(
        route = "gallery",
        title = "Galería",
        icon = Icons.Default.PhotoLibrary
    )

    data object Ocr : Screen(
        route = "ocr/{imagePath}",
        title = "OCR Técnico",
        icon = Icons.Default.TextSnippet
    ) {
        fun createRoute(imagePath: String) = "ocr/$imagePath"
    }

    data object OcrStandalone : Screen(
        route = "ocr_standalone/{imagePath}",
        title = "OCR Técnico",
        icon = Icons.Default.TextSnippet
    ) {
        fun createRoute(imagePath: String) = "ocr_standalone/$imagePath"
    }

    data object Demo : Screen(
        route = "demo",
        title = "Proyectos Demo",
        icon = Icons.Default.Image
    )

    data object Editor : Screen(
        route = "editor/{imageUri}",
        title = "Editor Explosionado",
        icon = Icons.Default.Image
    ) {
        fun createRoute(imageUri: String) = "editor/$imageUri"
    }

    data object Auth : Screen(
        route = "auth",
        title = "Iniciar Sesión",
        icon = Icons.Default.AccountCircle
    )

    data object ModelViewer : Screen(
        route = "model_viewer",
        title = "Visor 3D",
        icon = Icons.Default.Explore
    )

    data object CameraOcr : Screen(
        route = "camera_ocr",
        title = "Cámara OCR",
        icon = Icons.Default.CameraAlt
    )

    data object Detail : Screen(
        route = "detail/{infographicId}",
        title = "Detalle",
        icon = Icons.Default.Description
    ) {
        fun createRoute(infographicId: Long) = "detail/$infographicId"
    }

    data object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        icon = Icons.Default.Description
    )

    data object Home : Screen(
        route = "home",
        title = "Inicio",
        icon = Icons.Default.Home
    )

    companion object {
        val bottomNavItems = listOf(Home, WebView, Gallery, Auth)
    }
}

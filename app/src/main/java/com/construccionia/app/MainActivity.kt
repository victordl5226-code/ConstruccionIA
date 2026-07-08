package com.construccionia.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import timber.log.Timber
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.construccionia.app.auth.AuthScreen
import com.construccionia.app.auth.AuthViewModel
import com.construccionia.app.auth.SyncViewModel
import com.construccionia.app.editor.EditorScreen
import com.construccionia.app.editor.EditorViewModel
import com.construccionia.app.ui.navigation.Screen
import com.construccionia.app.ui.screens.CameraOcrScreen
import com.construccionia.app.ui.screens.DashboardScreen
import com.construccionia.app.ui.screens.DemoScreen
import com.construccionia.app.ui.screens.DetailScreen
import com.construccionia.app.ui.screens.GalleryScreen
import com.construccionia.app.ui.screens.HomeScreen
import com.construccionia.app.ui.screens.ModelViewerScreen
import com.construccionia.app.ui.screens.OcrScreen
import com.construccionia.app.ui.screens.WebViewScreen
import com.construccionia.app.ui.components.OfflineBanner
import com.construccionia.app.ui.theme.ConstruccionIATheme
import com.construccionia.app.ui.theme.LocalThemeManager
import com.construccionia.app.ui.theme.ThemeManager
import com.construccionia.app.viewmodel.DashboardViewModel
import com.construccionia.app.viewmodel.GalleryViewModel
import com.construccionia.app.viewmodel.WebViewViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Registro para solicitar permisos de almacenamiento
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Algunos permisos no fueron concedidos. La funcionalidad puede estar limitada.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos necesarios
        requestStoragePermissions()

        try {
            // Instalar Splash Screen
            installSplashScreen()

            setContent {
                ConstruccionIATheme {
                    ConstruccionIApp()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fatal en onCreate")
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            // Si falla, intentamos con un contenido mínimo
            setContent {
                androidx.compose.material3.Text(
                    "Error al iniciar: ${e.localizedMessage}",
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }
    }



    /** Solicita permisos de almacenamiento según la versión de Android. */
    private fun requestStoragePermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Android 12 y anteriores
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        val needsRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsRequest) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

// ── Composable principal ──

@Composable
fun ConstruccionIApp() {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val isDarkMode by themeManager.isDarkModeFlow.collectAsState(initial = null)
    val systemDark = isSystemInDarkTheme()
    val useDark = isDarkMode ?: systemDark

    val navController = rememberNavController()

    // ViewModels compartidos (inyectados por Hilt)
    val webViewViewModel: WebViewViewModel = hiltViewModel()
    val galleryViewModel: GalleryViewModel = hiltViewModel()

    CompositionLocalProvider(LocalThemeManager provides themeManager) {
        ConstruccionIATheme(darkTheme = useDark) {
            Scaffold(
                bottomBar = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    NavigationBar {
                        // 4 items de navegación principal: Inicio, Generar IA, Galería, Cuenta
                        Screen.bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true

                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                    // Pantalla de Inicio (Home/Dashboard principal)
                    composable(Screen.Home.route) {
                        HomeScreen(
                            onNavigateToGenerator = {
                                navController.navigate(Screen.WebView.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToGallery = {
                                navController.navigate(Screen.Gallery.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToCamera = {
                                navController.navigate(Screen.CameraOcr.route)
                            },
                            onNavigateToEditor = {
                                // Abrir editor requiere una URI, redirigimos a galería primero
                                navController.navigate(Screen.Gallery.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateTo3DViewer = {
                                navController.navigate(Screen.ModelViewer.route)
                            },
                            onNavigateToOcr = {
                                navController.navigate(Screen.CameraOcr.route)
                            },
                            onNavigateToExport = {
                                navController.navigate(Screen.Gallery.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToSync = {
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToDashboard = {
                                navController.navigate(Screen.Dashboard.route)
                            },
                            onNavigateToDemo = {
                                navController.navigate(Screen.Demo.route)
                            }
                        )
                    }

                    // Pantalla WebView (Generador IA)
                    composable(Screen.WebView.route) {
                        WebViewScreen(viewModel = webViewViewModel)
                    }

                    // Pantalla Galería
                    composable(Screen.Gallery.route) {
                        GalleryScreen(
                            viewModel = galleryViewModel,
                            navController = navController
                        )
                    }

                    // Pantalla OCR (desde galería)
                    composable(
                        route = Screen.Ocr.route,
                        arguments = listOf(
                            navArgument("imagePath") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
                        OcrScreen(
                            viewModel = galleryViewModel,
                            imagePath = imagePath,
                            navController = navController
                        )
                    }

                    // Pantalla OCR (standalone, desde demo)
                    composable(
                        route = Screen.OcrStandalone.route,
                        arguments = listOf(
                            navArgument("imagePath") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
                        OcrScreen(
                            viewModel = galleryViewModel,
                            imagePath = imagePath,
                            navController = navController
                        )
                    }

                    // Cámara para OCR
                    composable(Screen.CameraOcr.route) {
                        CameraOcrScreen(
                            onBack = { navController.popBackStack() },
                            onImageCaptured = { imagePath ->
                                navController.navigate(
                                    com.construccionia.app.ui.navigation.Screen.OcrStandalone
                                        .createRoute(java.net.URLEncoder.encode(imagePath, "UTF-8"))
                                )
                            }
                        )
                    }

                    // Pantalla Demo
                    composable(Screen.Demo.route) {
                        DemoScreen(
                            viewModel = galleryViewModel,
                            navController = navController
                        )
                    }

                    // Editor Explosionado
                    composable(
                        route = Screen.Editor.route,
                        arguments = listOf(
                            navArgument("imageUri") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                        val editorViewModel: EditorViewModel = hiltViewModel()
                        EditorScreen(
                            viewModel = editorViewModel,
                            imageUri = java.net.URLDecoder.decode(imageUri, "UTF-8"),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Autenticación + Sincronización (Cuenta)
                    composable(Screen.Auth.route) {
                        val authViewModel: AuthViewModel = hiltViewModel()
                        val syncViewModel: SyncViewModel = hiltViewModel()
                        AuthScreen(
                            authViewModel = authViewModel,
                            syncViewModel = syncViewModel,
                            onAuthenticated = {
                                navController.popBackStack()
                            },
                            onNavigateToDashboard = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    // Visor 3D
                    composable(Screen.ModelViewer.route) {
                        ModelViewerScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Dashboard / Estadísticas
                    composable(Screen.Dashboard.route) {
                        val dashboardViewModel: DashboardViewModel = hiltViewModel()
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Detalle de infografía
                    composable(
                        route = Screen.Detail.route,
                        arguments = listOf(
                            navArgument("infographicId") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val infographicId = backStackEntry.arguments?.getLong("infographicId") ?: return@composable
                        val infographic = galleryViewModel.galleryState.value.infographics
                            .find { it.id == infographicId }
                        if (infographic != null) {
                            DetailScreen(
                                infographic = infographic,
                                onBack = { navController.popBackStack() },
                                onShare = { uri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir infografía"))
                                },
                                onRename = { newName ->
                                    galleryViewModel.renameInfographic(infographic, newName)
                                },
                                onDelete = {
                                    galleryViewModel.deleteInfographic(infographic)
                                    navController.popBackStack()
                                },
                                onOpenOcr = {
                                    navController.navigate(
                                        com.construccionia.app.ui.navigation.Screen.OcrStandalone
                                            .createRoute(java.net.URLEncoder.encode(infographic.filePath, "UTF-8"))
                                    )
                                },
                                onOpenEditor = {
                                    navController.navigate(
                                        com.construccionia.app.ui.navigation.Screen.Editor
                                            .createRoute(java.net.URLEncoder.encode(infographic.filePath, "UTF-8"))
                                    )
                                },
                                onExportPpt = {
                                    galleryViewModel.exportSingleToPpt(infographic.filePath)
                                },
                                onExportPdf = {
                                    galleryViewModel.exportSingleToPdf(infographic.filePath)
                                },
                                onOpen3D = {
                                    navController.navigate(
                                        com.construccionia.app.ui.navigation.Screen.ModelViewer.route
                                    )
                                }
                            )
                        }
                    }
                }

                // Banner offline superpuesto en la parte superior
                OfflineBanner(
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
    }
}

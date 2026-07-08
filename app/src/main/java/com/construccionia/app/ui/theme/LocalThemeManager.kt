package com.construccionia.app.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal que provee el [ThemeManager] a toda la UI.
 * Se inicializa en [com.construccionia.app.ConstruccionIApp] y permite que
 * cualquier screen pueda cambiar el tema sin pasar por parámetros.
 */
val LocalThemeManager = compositionLocalOf<ThemeManager> {
    error("No ThemeManager provided. Ensure LocalThemeManager is set in ConstruccionIApp()")
}

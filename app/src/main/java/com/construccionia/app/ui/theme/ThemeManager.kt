package com.construccionia.app.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore para persistir la preferencia de tema.
 */
private val Context.themeDataStore by preferencesDataStore(name = "theme_settings")

/**
 * Gestiona el modo oscuro/claro manual, independiente de la configuración del sistema.
 * Persiste la preferencia en DataStore y expone un Flow para reaccionar a los cambios.
 */
class ThemeManager(private val context: Context) {
    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode")

    /**
     * Flow que emite:
     * - `true` si el usuario forzó modo oscuro
     * - `false` si el usuario forzó modo claro
     * - `null` si no hay preferencia manual (seguir al sistema)
     */
    val isDarkModeFlow: Flow<Boolean?> = context.themeDataStore.data.map { preferences ->
        preferences[isDarkModeKey] // null = no establecido = seguir al sistema
    }

    /** Alterna el modo oscuro/claro manual. */
    suspend fun toggleDarkMode(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[isDarkModeKey] = enabled
        }
    }

    /** Desactiva el modo manual y vuelve a seguir la configuración del sistema. */
    suspend fun resetToSystem() {
        context.themeDataStore.edit { preferences ->
            preferences.remove(isDarkModeKey)
        }
    }
}

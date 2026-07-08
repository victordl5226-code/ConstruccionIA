package com.construccionia.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.construccionia.app.data.local.InfographicDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel para la pantalla de Dashboard / Estadísticas.
 * Expone el conteo total de infografías desde la base de datos local.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val infographicDao: InfographicDao
) : ViewModel() {

    /** Total de infografías almacenadas localmente. */
    val totalInfographics: StateFlow<Int> = infographicDao.count()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )
}

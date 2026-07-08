package com.construccionia.feature.export.presentation

import com.construccionia.core.common.AppException
import com.construccionia.core.common.FileOutput

/**
 * Estados de la pantalla de exportación.
 */
sealed class ExportUiState {
    data object Idle : ExportUiState()
    data object Exporting : ExportUiState()
    data class Success(val output: FileOutput) : ExportUiState()
    data class Error(val exception: AppException) : ExportUiState()
}

package com.construccionia.feature.ocr.presentation

import com.construccionia.core.common.AppException
import com.construccionia.feature.ocr.domain.model.OcrResult

/**
 * Estados de la pantalla de OCR.
 */
sealed class OcrUiState {
    data object Idle : OcrUiState()
    data object Loading : OcrUiState()
    data class TextDetected(val result: OcrResult) : OcrUiState()
    data class Error(val exception: AppException) : OcrUiState()
}

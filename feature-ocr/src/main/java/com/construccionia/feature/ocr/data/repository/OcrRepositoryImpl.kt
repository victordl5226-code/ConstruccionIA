package com.construccionia.feature.ocr.data.repository

import android.graphics.BitmapFactory
import com.construccionia.core.common.AppResult
import com.construccionia.core.common.ProcessingException
import com.construccionia.feature.ocr.data.mlkit.MlKitTextRecognizer
import com.construccionia.feature.ocr.domain.model.OcrResult
import com.construccionia.feature.ocr.domain.model.TextBlock
import com.construccionia.feature.ocr.domain.repository.OcrRepository
import com.construccionia.core.common.ImageBytes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrRepositoryImpl @Inject constructor(
    private val textRecognizer: MlKitTextRecognizer
) : OcrRepository {

    override suspend fun recognizeText(image: ImageBytes): AppResult<OcrResult> {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
                ?: return AppResult.Error(
                    ProcessingException("No se pudo decodificar la imagen para OCR")
                )

            val startTime = System.currentTimeMillis()
            val mlKitResult = textRecognizer.recognize(bitmap)
            val processingTime = System.currentTimeMillis() - startTime

            val textBlocks = mlKitResult.textBlocks.map { block ->
                val mlKitRect = block.boundingBox
                TextBlock(
                    text = block.text,
                    boundingBox = if (mlKitRect != null) {
                        BoundingBox(
                            left = mlKitRect.left,
                            top = mlKitRect.top,
                            right = mlKitRect.right,
                            bottom = mlKitRect.bottom
                        )
                    } else {
                        BoundingBox(0, 0, 0, 0)
                    },
                    confidence = 0.85f, // ML Kit no proporciona confianza por bloque
                    lines = block.lines.map { it.text }
                )
            }

            val result = OcrResult(
                text = mlKitResult.text,
                textBlocks = textBlocks,
                confidence = if (textBlocks.isNotEmpty()) 0.85f else 0f,
                processingTimeMs = processingTime
            )

            AppResult.Success(result)
        } catch (e: Exception) {
            AppResult.Error(
                ProcessingException("Error en OCR: ${e.message}", e)
            )
        }
    }

    override suspend fun isOcrAvailable(): Boolean {
        return try {
            // ML Kit está disponible en todos los dispositivos con Google Play Services
            true
        } catch (e: Exception) {
            false
        }
    }
}

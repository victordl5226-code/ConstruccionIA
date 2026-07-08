package com.construccionia.feature.generation.data.repository

import android.graphics.Bitmap
import com.construccionia.core.common.AppException
import com.construccionia.core.common.AppResult
import com.construccionia.core.common.UnknownException
import com.construccionia.feature.generation.data.mapper.GeminiMapper
import com.construccionia.feature.generation.data.remote.GeminiApiService
import com.construccionia.feature.generation.domain.model.GeneratedImage
import com.construccionia.feature.generation.domain.model.GenerationPrompt
import com.construccionia.feature.generation.domain.repository.GeminiRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepositoryImpl @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val geminiMapper: GeminiMapper
) : GeminiRepository {

    override suspend fun generateImage(prompt: GenerationPrompt): AppResult<GeneratedImage> {
        return try {
            val bitmap: Bitmap = geminiApiService.generateImage(prompt.toFullPrompt())
            val generatedImage = geminiMapper.mapToGeneratedImage(bitmap, prompt)
            AppResult.Success(generatedImage)
        } catch (e: AppException) {
            AppResult.Error(e)
        } catch (e: Exception) {
            AppResult.Error(UnknownException("Error al generar imagen: ${e.message}", e))
        }
    }

    override suspend fun isApiAvailable(): AppResult<Boolean> {
        return try {
            val available = geminiApiService.isApiAvailable()
            AppResult.Success(available)
        } catch (e: AppException) {
            AppResult.Error(e)
        } catch (e: Exception) {
            AppResult.Error(UnknownException("Error al verificar disponibilidad: ${e.message}", e))
        }
    }
}

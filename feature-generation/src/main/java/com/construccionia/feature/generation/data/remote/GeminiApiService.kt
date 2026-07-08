package com.construccionia.feature.generation.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.construccionia.core.common.AppException
import com.construccionia.core.common.AuthenticationException
import com.construccionia.core.common.NetworkException
import com.construccionia.core.common.ProcessingException
import com.construccionia.core.common.ServerException
import com.construccionia.core.common.UnknownException
import com.construccionia.core.common.extension.scaleToMaxSize
import com.construccionia.core.network.ApiConstants
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio que encapsula la comunicación con la API de Gemini
 * para la generación de imágenes usando el SDK oficial de Google.
 *
 * Utiliza el modelo Gemini 1.5 Pro para generar contenido visual
 * a partir de descripciones textuales.
 *
 * ═══════════════════════════════════════════════════════════════
 *  EJEMPLO DE USO DE LA API DE GEMINI EN KOTLIN
 * ═══════════════════════════════════════════════════════════════
 *
 *  val generativeModel = GenerativeModel(
 *      modelName = "gemini-1.5-pro",
 *      apiKey = "TU_API_KEY"
 *  )
 *
 *  // Generar contenido con imagen
 *  val response = generativeModel.generateContent("prompt aquí")
 *
 *  // Extraer imagen de la respuesta
 *  response.candidates.firstOrNull()?.content?.parts?.firstOrNull()
 *
 * ═══════════════════════════════════════════════════════════════
 */
@Singleton
class GeminiApiService @Inject constructor(
    private val generativeModel: GenerativeModel
) {
    companion object {
        private const val TAG = "GeminiApiService"
        private const val MAX_IMAGE_DIMENSION = 2048
        private const val IMAGE_QUALITY = 90
    }

    /**
     * Genera una imagen a partir de un prompt de texto usando Gemini 1.5 Pro.
     *
     * El SDK de Google Generative AI permite enviar prompts de texto
     * y recibir respuestas multimodales que pueden incluir texto e imágenes.
     *
     * @param prompt Texto descriptivo para generar la imagen
     * @return Bitmap con la imagen generada
     * @throws AppException si ocurre algún error en el proceso
     */
    suspend fun generateImage(prompt: String): Bitmap = withContext(Dispatchers.IO) {
        try {
            // 1. Enviar el prompt al modelo Gemini
            // La API de Gemini 1.5 Pro acepta prompts de texto y puede
            // generar respuestas que contienen texto e imágenes
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                }
            )

            // 2. Validar la respuesta
            val candidate = response.candidates.firstOrNull()
                ?: throw ProcessingException("No se recibieron candidatos en la respuesta")

            val part = candidate.content?.parts?.firstOrNull()
                ?: throw ProcessingException("No se recibieron partes en la respuesta")

            // 3. Extraer la imagen de la respuesta
            // El SDK devuelve la imagen como un inlineImage o como un URI
            val imageData: ByteArray = when {
                part.inlineData != null -> {
                    // La imagen viene como datos inline (base64)
                    part.inlineData.data.toByteArray()
                }
                part.fileUri != null -> {
                    // La imagen viene como URI (descargar)
                    downloadImageFromUri(part.fileUri)
                }
                else -> {
                    throw ProcessingException(
                        "Formato de respuesta no soportado. " +
                        "Esperaba inlineData o fileUri, pero se recibió: ${part.javaClass.simpleName}"
                    )
                }
            }

            // 4. Decodificar y optimizar el bitmap
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: throw ProcessingException("No se pudo decodificar la imagen generada")

            // 5. Escalar si es necesario para mantener tamaño manejable
            bitmap.scaleToMaxSize(MAX_IMAGE_DIMENSION)

        } catch (e: com.google.ai.client.generativeai.type.GenerateContentException) {
            // Error específico del SDK de Gemini
            val message = e.message ?: "Error del modelo Gemini"
            throw when {
                message.contains("API_KEY", ignoreCase = true) ||
                message.contains("unauthorized", ignoreCase = true) ||
                message.contains("403", ignoreCase = true) ||
                message.contains("permission", ignoreCase = true) ->
                    AuthenticationException("API Key inválida o sin permisos", e)
                message.contains("429", ignoreCase = true) ||
                message.contains("rate", ignoreCase = true) ||
                message.contains("quota", ignoreCase = true) ->
                    ServerException(429, "Límite de cuota excedido. Intenta más tarde.", e)
                message.contains("500", ignoreCase = true) ||
                message.contains("503", ignoreCase = true) ->
                    ServerException(500, "Error del servidor de Gemini", e)
                else -> ServerException(0, message, e)
            }
        } catch (e: java.net.SocketTimeoutException) {
            throw NetworkException("Tiempo de espera agotado. La conexión tardó demasiado.", e)
        } catch (e: java.net.UnknownHostException) {
            throw NetworkException("No se puede conectar al servidor. Verifica tu conexión a internet.", e)
        } catch (e: java.io.IOException) {
            throw NetworkException("Error de conexión: ${e.message}", e)
        } catch (e: SecurityException) {
            throw AuthenticationException("Error de seguridad al acceder a la API", e)
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw UnknownException("Error inesperado: ${e.message}", e)
        }
    }

    /**
     * Verifica si la API de Gemini está disponible y funcionando.
     */
    suspend fun isApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(
                content {
                    text("Responde solo con 'OK' si estás funcionando correctamente.")
                }
            )
            response.candidates.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Descarga una imagen desde una URI remota.
     */
    private fun downloadImageFromUri(uri: String): ByteArray {
        val urlConnection: URLConnection = URL(uri).openConnection()
        urlConnection.connectTimeout = (ApiConstants.CONNECT_TIMEOUT * 1000).toInt()
        urlConnection.readTimeout = (ApiConstants.READ_TIMEOUT * 1000).toInt()

        val inputStream = urlConnection.getInputStream()
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead)
        }
        inputStream.close()
        return byteArrayOutputStream.toByteArray()
    }
}

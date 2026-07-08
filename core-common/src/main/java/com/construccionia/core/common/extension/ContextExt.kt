package com.construccionia.core.common.extension

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import com.construccionia.core.common.ImageBytes

/**
 * Extensiones útiles para Context.
 */

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo ?: return false
        return networkInfo.isConnected
    }
}

fun Context.cacheDirSize(): Long {
    return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
}

/**
 * Carga una imagen desde un URI y la convierte en [ImageBytes].
 * Útil para procesar imágenes seleccionadas de galería o capturadas con cámara.
 *
 * Lee los bytes directamente del InputStream y obtiene las dimensiones
 * usando [BitmapFactory.Options] con [BitmapFactory.Options.inJustDecodeBounds],
 * evitando cargar el bitmap completo en memoria y preservando la calidad original.
 *
 * @param uri URI de la imagen a cargar (content:// o file://)
 * @return [ImageBytes] con los bytes, ancho y alto de la imagen, o null si falla
 */
fun ContentResolver.loadImageBytes(uri: Uri): ImageBytes? {
    return try {
        val inputStream = openInputStream(uri) ?: return null
        val bytes = inputStream.use { stream -> stream.readBytes() }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        ImageBytes(
            bytes = bytes,
            width = options.outWidth,
            height = options.outHeight,
            mimeType = getType(uri) ?: "image/png"
        )
    } catch (e: Exception) {
        null
    }
}

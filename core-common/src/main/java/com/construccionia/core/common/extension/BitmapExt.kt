package com.construccionia.core.common.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.construccionia.core.common.ImageBytes
import java.io.ByteArrayOutputStream

/**
 * Extensiones útiles para Bitmap.
 */

fun Bitmap.toImageBytes(mimeType: String = "image/png"): ImageBytes {
    val stream = ByteArrayOutputStream()
    val format = when (mimeType) {
        "image/jpeg", "image/jpg" -> Bitmap.CompressFormat.JPEG
        "image/webp" -> Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.PNG
    }
    compress(format, 100, stream)
    return ImageBytes(
        bytes = stream.toByteArray(),
        width = width,
        height = height,
        mimeType = mimeType
    )
}

fun ByteArray.toBitmap(): Bitmap? {
    return BitmapFactory.decodeByteArray(this, 0, this.size)
}

fun Bitmap.scaleToMaxSize(maxSize: Int = 2048): Bitmap {
    val scale = minOf(
        maxSize.toFloat() / width,
        maxSize.toFloat() / height,
        1f
    )
    if (scale >= 1f) return this
    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()
    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}

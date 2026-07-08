package com.construccionia.core.common

/**
 * Representa una imagen como un arreglo de bytes con metadatos.
 */
data class ImageBytes(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val mimeType: String = "image/png"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ImageBytes
        return bytes.contentEquals(other.bytes) &&
                width == other.width &&
                height == other.height &&
                mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

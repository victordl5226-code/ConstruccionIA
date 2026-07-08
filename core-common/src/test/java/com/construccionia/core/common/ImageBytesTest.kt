package com.construccionia.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la igualdad de [ImageBytes].
 *
 * ImageBytes es una data class que sobreescribe equals y hashCode
 * para comparar ByteArray por contenido en lugar de por referencia.
 *
 * Verifica:
 * - La igualdad estructural (contentEquals para ByteArray)
 * - La desigualdad cuando cambian bytes, dimensiones o mimeType
 * - El contrato de hashCode
 */
class ImageBytesTest {

    // ──────────────────────────────────────────────────────────────
    // Igualdad
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `dosImageBytesIguales_sonEquals()`() {
        // Given
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val image1 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 100,
            height = 200,
            mimeType = "image/png"
        )
        val image2 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 100,
            height = 200,
            mimeType = "image/png"
        )

        // Then
        assertEquals("Dos ImageBytes con el mismo contenido deben ser iguales", image1, image2)
        assertEquals(
            "Sus hashCodes deben ser iguales",
            image1.hashCode(),
            image2.hashCode()
        )
    }

    @Test
    fun `dosImageBytesVacios_conMismosParametros_sonEquals()`() {
        // Given
        val image1 = ImageBytes(
            bytes = ByteArray(0),
            width = 0,
            height = 0,
            mimeType = "image/png"
        )
        val image2 = ImageBytes(
            bytes = ByteArray(0),
            width = 0,
            height = 0,
            mimeType = "image/png"
        )

        // Then
        assertEquals("ImageBytes vacíos con mismos parámetros deben ser iguales", image1, image2)
    }

    @Test
    fun `imageBytes_esIgualASiMismo()`() {
        // Given
        val image = ImageBytes(
            bytes = byteArrayOf(0x01, 0x02),
            width = 10,
            height = 10
        )

        // Then
        assertEquals("Un ImageBytes debe ser igual a sí mismo", image, image)
    }

    @Test
    fun `imageBytes_noEsIgualANull()`() {
        // Given
        val image = ImageBytes(
            bytes = byteArrayOf(0x01),
            width = 10,
            height = 10
        )

        // Then
        assertFalse("ImageBytes no debe ser igual a null", image.equals(null))
    }

    @Test
    fun `imageBytes_noEsIgualAOtroTipoDeObjeto()`() {
        // Given
        val image = ImageBytes(
            bytes = byteArrayOf(0x01),
            width = 10,
            height = 10
        )

        // Then
        assertFalse("ImageBytes no debe ser igual a un String", image.equals("not-an-image-bytes"))
        assertFalse("ImageBytes no debe ser igual a un Int", image.equals(42))
    }

    // ──────────────────────────────────────────────────────────────
    // Desigualdad - bytes diferentes
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `imageBytesConDiferentesBytes_noSonEquals()`() {
        // Given
        val image1 = ImageBytes(
            bytes = byteArrayOf(0x01, 0x02, 0x03),
            width = 100,
            height = 100
        )
        val image2 = ImageBytes(
            bytes = byteArrayOf(0x0A, 0x0B, 0x0C),
            width = 100,
            height = 100
        )

        // Then
        assertNotEquals(
            "ImageBytes con diferentes bytes no deben ser iguales",
            image1,
            image2
        )
    }

    @Test
    fun `imageBytesConBytesParcialmenteDiferentes_noSonEquals()`() {
        // Given: mismos bytes excepto el último
        val baseBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val image1 = ImageBytes(
            bytes = baseBytes.copyOf(),
            width = 50,
            height = 50
        )
        val image2 = ImageBytes(
            bytes = baseBytes.copyOf().also { it[it.size - 1] = 0xFF },
            width = 50,
            height = 50
        )

        // Then
        assertNotEquals(
            "Un solo byte diferente debe hacer que no sean iguales",
            image1,
            image2
        )
    }

    @Test
    fun `imageBytesConDiferenteLongitudDeBytes_noSonEquals()`() {
        // Given
        val image1 = ImageBytes(
            bytes = byteArrayOf(0x01, 0x02, 0x03),
            width = 100,
            height = 100
        )
        val image2 = ImageBytes(
            bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04),
            width = 100,
            height = 100
        )

        // Then
        assertNotEquals(
            "ImageBytes con diferente longitud de bytes no deben ser iguales",
            image1,
            image2
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Desigualdad - dimensiones diferentes
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `imageBytesConDiferenteDimension_noSonEquals()`() {
        // Given
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val image1 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 100,
            height = 100
        )
        val image2 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 200,
            height = 100
        )

        // Then
        assertNotEquals(
            "ImageBytes con diferente width no deben ser iguales",
            image1,
            image2
        )
    }

    @Test
    fun `imageBytesConDiferenteAltura_noSonEquals()`() {
        // Given
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val image1 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 100,
            height = 100
        )
        val image2 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 100,
            height = 200
        )

        // Then
        assertNotEquals(
            "ImageBytes con diferente height no deben ser iguales",
            image1,
            image2
        )
    }

    @Test
    fun `imageBytesConDimensionesIntercambiadas_noSonEquals()`() {
        // Given
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val image1 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 200,
            height = 100
        )
        val image2 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 100,
            height = 200
        )

        // Then
        assertNotEquals(
            "ImageBytes con width y height intercambiados no deben ser iguales",
            image1,
            image2
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Desigualdad - mimeType diferente
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `imageBytesConDiferenteMimeType_noSonEquals()`() {
        // Given
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val image1 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 100,
            height = 100,
            mimeType = "image/png"
        )
        val image2 = ImageBytes(
            bytes = bytes.copyOf(),
            width = 100,
            height = 100,
            mimeType = "image/jpeg"
        )

        // Then
        assertNotEquals(
            "ImageBytes con diferente mimeType no deben ser iguales",
            image1,
            image2
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Contrato de hashCode
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `hashCode_esConsistenteEnLlamadasRepetidas()`() {
        // Given
        val image = ImageBytes(
            bytes = byteArrayOf(0x01, 0x02, 0x03),
            width = 100,
            height = 100,
            mimeType = "image/png"
        )

        // When
        val hash1 = image.hashCode()
        val hash2 = image.hashCode()
        val hash3 = image.hashCode()

        // Then
        assertEquals("hashCode debe ser consistente", hash1, hash2)
        assertEquals("hashCode debe ser consistente", hash2, hash3)
    }

    @Test
    fun `hashCode_esDiferenteParaObjetosDiferentes()`() {
        // Given
        val image1 = ImageBytes(
            bytes = byteArrayOf(0x01),
            width = 10,
            height = 10
        )
        val image2 = ImageBytes(
            bytes = byteArrayOf(0x02),
            width = 10,
            height = 10
        )

        // Then
        assertNotEquals(
            "Objetos diferentes deben tener hashCode diferente (alta probabilidad)",
            image1.hashCode(),
            image2.hashCode()
        )
    }

    @Test
    fun `imageBytesConMimeTypePorDefecto_usaPng()`() {
        // Given
        val image = ImageBytes(
            bytes = byteArrayOf(0x01),
            width = 10,
            height = 10
        )

        // Then
        assertEquals(
            "El mimeType por defecto debe ser image/png",
            "image/png",
            image.mimeType
        )
    }
}

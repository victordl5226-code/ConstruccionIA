package com.construccionia.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para [AppResult].
 *
 * Verifica el comportamiento funcional de la clase sellada AppResult:
 * - Creación de Success y Error
 * - Propiedades de estado (isSuccess, isError)
 * - Funciones de transformación (map, flatMap)
 * - Funciones de extracción (getOrNull)
 * - Funciones de efecto secundario (onSuccess, onError)
 */
class AppResultTest {

    // ──────────────────────────────────────────────────────────────
    // Success
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `Success_contieneDatos()`() {
        // Given
        val data = "datos de prueba"

        // When
        val result: AppResult<String> = AppResult.Success(data)

        // Then
        assertTrue("isSuccess debe ser true", result.isSuccess)
        assertFalse("isError debe ser false", result.isError)
        assertEquals("El dato debe coincidir", data, (result as AppResult.Success).data)
    }

    @Test
    fun `Success_conNumero_contieneValorCorrecto()`() {
        // Given
        val number = 42

        // When
        val result: AppResult<Int> = AppResult.Success(number)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(number, (result as AppResult.Success).data)
    }

    @Test
    fun `Success_conNullPermitido_puedeContenerNull()`() {
        // Given
        val data: String? = null

        // When
        val result: AppResult<String?> = AppResult.Success(data)

        // Then
        assertTrue(result.isSuccess)
        assertNull("Success puede contener null si T es nullable", result.getOrNull())
    }

    // ──────────────────────────────────────────────────────────────
    // Error
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `Error_contieneExcepcion()`() {
        // Given
        val exception = InvalidInputException("Entrada inválida")

        // When
        val result: AppResult<String> = AppResult.Error(exception)

        // Then
        assertFalse("isSuccess debe ser false", result.isSuccess)
        assertTrue("isError debe ser true", result.isError)
        assertEquals(
            "La excepción debe coincidir",
            exception,
            (result as AppResult.Error).exception
        )
        assertEquals(
            "El mensaje de la excepción debe coincidir",
            "Entrada inválida",
            result.exception.message
        )
    }

    @Test
    fun `Error_conNetworkException_mantieneTipoConcreto()`() {
        // Given
        val networkException = NetworkException("Sin conexión")

        // When
        val result: AppResult<String> = AppResult.Error(networkException)

        // Then
        assertTrue(result.isError)
        val error = result as AppResult.Error
        assertTrue(
            "La excepción debe ser NetworkException",
            error.exception is NetworkException
        )
    }

    @Test
    fun `Error_conServerException_contieneCodigoHTTP()`() {
        // Given
        val serverException = ServerException(503, "Servicio no disponible")

        // When
        val result: AppResult<String> = AppResult.Error(serverException)

        // Then
        assertTrue(result.isError)
        val error = result as AppResult.Error
        assertTrue(error.exception is ServerException)
        assertEquals(
            "El código HTTP debe ser 503",
            503,
            (error.exception as ServerException).code
        )
    }

    // ──────────────────────────────────────────────────────────────
    // isSuccess / isError
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `isSuccess_esTrueEnSuccess()`() {
        // When
        val result = AppResult.Success("ok")

        // Then
        assertTrue("Success.isSuccess debe ser true", result.isSuccess)
    }

    @Test
    fun `isSuccess_esFalseEnError()`() {
        // When
        val result = AppResult.Error(NetworkException())

        // Then
        assertFalse("Error.isSuccess debe ser false", result.isSuccess)
    }

    @Test
    fun `isError_esTrueEnError()`() {
        // When
        val result = AppResult.Error(NetworkException())

        // Then
        assertTrue("Error.isError debe ser true", result.isError)
    }

    @Test
    fun `isError_esFalseEnSuccess()`() {
        // When
        val result = AppResult.Success("ok")

        // Then
        assertFalse("Success.isError debe ser false", result.isError)
    }

    // ──────────────────────────────────────────────────────────────
    // getOrNull
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `getOrNull_retornaDatoEnSuccess()`() {
        // Given
        val data = "datos"

        // When
        val result = AppResult.Success(data)

        // Then
        assertEquals("getOrNull debe retornar el dato", data, result.getOrNull())
    }

    @Test
    fun `getOrNull_retornaNullEnError()`() {
        // Given
        val result = AppResult.Error(NetworkException())

        // Then
        assertNull("getOrNull debe retornar null en Error", result.getOrNull())
    }

    // ──────────────────────────────────────────────────────────────
    // map
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `map_transform()`() {
        // Given
        val result: AppResult<Int> = AppResult.Success(42)

        // When
        val mapped: AppResult<String> = result.map { "El número es $it" }

        // Then
        assertTrue(mapped.isSuccess)
        assertEquals(
            "El transform debe aplicarse al dato",
            "El número es 42",
            (mapped as AppResult.Success).data
        )
    }

    @Test
    fun `map_noTransformaError()`() {
        // Given
        val exception = InvalidInputException("error")
        val result: AppResult<Int> = AppResult.Error(exception)

        // When
        val mapped: AppResult<String> = result.map { "Nunca se ejecuta" }

        // Then
        assertTrue("Error.map debe seguir siendo Error", mapped.isError)
        assertEquals(
            "La excepción debe mantenerse",
            exception,
            (mapped as AppResult.Error).exception
        )
    }

    @Test
    fun `map_cadenaTransformaciones()`() {
        // Given
        val result: AppResult<Int> = AppResult.Success(10)

        // When
        val result1 = result.map { it * 2 }
        val result2 = result1.map { "Valor: $it" }

        // Then
        assertEquals("Valor: 20", (result2 as AppResult.Success).data)
    }

    // ──────────────────────────────────────────────────────────────
    // flatMap
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `flatMap_transform()`() {
        // Given
        val result: AppResult<Int> = AppResult.Success(5)

        // When
        val flatMapped: AppResult<String> = result.flatMap {
            if (it > 0) {
                AppResult.Success("Positivo: $it")
            } else {
                AppResult.Error(InvalidInputException("Negativo"))
            }
        }

        // Then
        assertTrue(flatMapped.isSuccess)
        assertEquals(
            "Positivo: 5",
            (flatMapped as AppResult.Success).data
        )
    }

    @Test
    fun `flatMap_puedeRetornarError()`() {
        // Given
        val result: AppResult<Int> = AppResult.Success(-1)

        // When
        val flatMapped: AppResult<String> = result.flatMap {
            if (it > 0) {
                AppResult.Success("Positivo: $it")
            } else {
                AppResult.Error(InvalidInputException("El valor debe ser positivo"))
            }
        }

        // Then
        assertTrue("flatMap puede retornar Error", flatMapped.isError)
        val error = flatMapped as AppResult.Error
        assertTrue(error.exception is InvalidInputException)
        assertEquals("El valor debe ser positivo", error.exception.message)
    }

    @Test
    fun `flatMap_noTransformaError()`() {
        // Given
        val exception = ServerException(500)
        val result: AppResult<Int> = AppResult.Error(exception)

        // When
        val flatMapped: AppResult<String> = result.flatMap {
            AppResult.Success("Esto no debería ejecutarse")
        }

        // Then
        assertTrue("Error.flatMap debe seguir siendo Error", flatMapped.isError)
        assertEquals(
            "La excepción original debe mantenerse",
            exception,
            (flatMapped as AppResult.Error).exception
        )
    }

    // ──────────────────────────────────────────────────────────────
    // onSuccess / onError
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `onSuccess_seEjecutaEnSuccess()`() {
        // Given
        val result = AppResult.Success("hola")
        var sideEffect = ""

        // When
        result.onSuccess { sideEffect = it }

        // Then
        assertEquals("onSuccess debe ejecutar la acción", "hola", sideEffect)
    }

    @Test
    fun `onSuccess_noSeEjecutaEnError()`() {
        // Given
        val result: AppResult<String> = AppResult.Error(NetworkException())
        var sideEffect = "no modificado"

        // When
        result.onSuccess { sideEffect = "modificado" }

        // Then
        assertEquals(
            "onSuccess no debe ejecutarse en Error",
            "no modificado",
            sideEffect
        )
    }

    @Test
    fun `onError_seEjecutaEnError()`() {
        // Given
        val exception = NetworkException("Error de red")
        val result: AppResult<String> = AppResult.Error(exception)
        var capturedException: AppException? = null

        // When
        result.onError { capturedException = it }

        // Then
        assertNotNull("onError debe ejecutar la acción", capturedException)
        assertEquals("Error de red", capturedException?.message)
    }

    @Test
    fun `onError_noSeEjecutaEnSuccess()`() {
        // Given
        val result = AppResult.Success("ok")
        var sideEffect = false

        // When
        result.onError { sideEffect = true }

        // Then
        assertFalse("onError no debe ejecutarse en Success", sideEffect)
    }

    // ──────────────────────────────────────────────────────────────
    // Companion object (factory methods)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `companion_success_creaSuccess()`() {
        // When
        val result = AppResult.success(123)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(123, (result as AppResult.Success).data)
    }

    @Test
    fun `companion_error_creaError()`() {
        // When
        val result = AppResult.error(NetworkException())

        // Then
        assertTrue(result.isError)
    }

    // ──────────────────────────────────────────────────────────────
    // Type erasure / covariance
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `AppResult_esCovariante_enTipoGenerico()`() {
        // Given: AppResult<out T> permite asignar tipos más específicos
        val stringResult: AppResult<String> = AppResult.Success("texto")
        val anyResult: AppResult<Any> = stringResult

        // Then
        assertTrue(anyResult.isSuccess)
    }

    @Test
    fun `Error_esTipoNothing_yAsignableACualquierTipo()`() {
        // Given: Error es AppResult<Nothing>, covariantemente es AppResult<CualquierTipo>
        val error: AppResult<String> = AppResult.Error(NetworkException())
        val errorInt: AppResult<Int> = AppResult.Error(InvalidInputException())

        // Then
        assertTrue(error.isError)
        assertTrue(errorInt.isError)
    }
}

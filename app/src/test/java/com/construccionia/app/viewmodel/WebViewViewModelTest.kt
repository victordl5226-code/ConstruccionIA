package com.construccionia.app.viewmodel

import android.app.Application
import com.construccionia.app.webview.ImageEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockApplication = mockk<Application>(relaxed = true)

    private lateinit var viewModel: WebViewViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = WebViewViewModel(mockApplication)
        // Avanzar para que el init complete la recolección del flow
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── uiState valores iniciales ──

    @Test
    fun `webViewState starts with default values`() = runTest {
        val state = viewModel.webViewState.value
        assertTrue("isLoading should start as true", state.isLoading)
        assertEquals(0, state.loadingProgress)
        assertEquals("", state.currentUrl)
        assertNull("errorMessage should be null", state.errorMessage)
    }

    @Test
    fun `detectedImages starts as empty list`() = runTest {
        assertTrue(viewModel.detectedImages.value.isEmpty())
    }

    @Test
    fun `downloadState starts as Idle`() = runTest {
        assertTrue(viewModel.downloadState.value is DownloadState.Idle)
    }

    @Test
    fun `jsInterface is properly initialized`() = runTest {
        assertNotNull(viewModel.jsInterface)
        assertEquals(WebViewViewModel.GEMINI_URL, "https://gemini.google.com")
    }

    // ── onPageStarted / onPageFinished ──

    @Test
    fun `onPageStarted updates state with loading and url`() = runTest {
        val url = "https://gemini.google.com/test"
        viewModel.onPageStarted(url)

        val state = viewModel.webViewState.value
        assertTrue(state.isLoading)
        assertEquals(url, state.currentUrl)
    }

    @Test
    fun `onPageFinished updates state with not loading and url`() = runTest {
        val url = "https://gemini.google.com/result"
        viewModel.onPageFinished(url)

        val state = viewModel.webViewState.value
        assertFalse("isLoading should be false after page finish", state.isLoading)
        assertEquals(url, state.currentUrl)
    }

    @Test
    fun `onPageStarted then onPageFinished transitions correctly`() = runTest {
        viewModel.onPageStarted("https://gemini.google.com/start")
        assertTrue(viewModel.webViewState.value.isLoading)

        viewModel.onPageFinished("https://gemini.google.com/start")
        assertFalse(viewModel.webViewState.value.isLoading)
    }

    // ── onProgressChanged ──

    @Test
    fun `onProgressChanged updates loading progress`() = runTest {
        viewModel.onProgressChanged(50)
        assertEquals(50, viewModel.webViewState.value.loadingProgress)

        viewModel.onProgressChanged(100)
        assertEquals(100, viewModel.webViewState.value.loadingProgress)

        viewModel.onProgressChanged(0)
        assertEquals(0, viewModel.webViewState.value.loadingProgress)
    }

    // ── onWebViewError / clearError ──

    @Test
    fun `onWebViewError sets error message and stops loading`() = runTest {
        viewModel.onWebViewError(404, "Not Found")

        val state = viewModel.webViewState.value
        assertFalse("isLoading should be false on error", state.isLoading)
        assertEquals("Error 404: Not Found", state.errorMessage)
    }

    @Test
    fun `clearError removes error message`() = runTest {
        viewModel.onWebViewError(500, "Internal Error")
        assertNotNull(viewModel.webViewState.value.errorMessage)

        viewModel.clearError()
        assertNull("errorMessage should be null after clear", viewModel.webViewState.value.errorMessage)
    }

    @Test
    fun `multiple error calls override previous error`() = runTest {
        viewModel.onWebViewError(404, "First")
        viewModel.onWebViewError(503, "Second")

        assertEquals("Error 503: Second", viewModel.webViewState.value.errorMessage)
    }

    // ── reloadWebView ──

    @Test
    fun `reloadWebView clears error and sets loading`() = runTest {
        viewModel.onWebViewError(404, "Not Found")
        viewModel.reloadWebView()

        val state = viewModel.webViewState.value
        assertTrue("isLoading should be true after reload", state.isLoading)
        assertNull("errorMessage should be null after reload", state.errorMessage)
    }

    // ── downloadState methods ──

    @Test
    fun `downloadImage sets state to Downloading`() = runTest {
        viewModel.downloadImage("https://example.com/image.png")
        advanceUntilIdle()

        // downloadImage llama a jsInterface.downloadImage que emite un evento.
        // Inicialmente el estado cambia a Downloading (en el método mismo)
        assertTrue(viewModel.downloadState.value is DownloadState.Downloading)
    }

    @Test
    fun `resetDownloadState returns to Idle`() = runTest {
        viewModel.downloadImage("https://example.com/image.png")
        viewModel.resetDownloadState()

        assertTrue(viewModel.downloadState.value is DownloadState.Idle)
    }

    @Test
    fun `downloadState transitions through Idle to Downloading`() = runTest {
        assertTrue(viewModel.downloadState.value is DownloadState.Idle)

        viewModel.downloadImage("https://example.com/test.png")
        assertTrue(viewModel.downloadState.value is DownloadState.Downloading)
    }

    // ── Detección de imágenes via jsInterface ──

    @Test
    fun `jsInterface onImageDetected adds image to detectedImages`() = runTest {
        viewModel.jsInterface.onImageDetected(
            imageUrl = "https://example.com/gen-image.png",
            altText = "Generated infographic"
        )
        advanceUntilIdle()

        val images = viewModel.detectedImages.value
        assertTrue(
            "detectedImages should contain the detected URL",
            images.contains("https://example.com/gen-image.png")
        )
        assertEquals(1, images.size)
    }

    @Test
    fun `jsInterface onImageDetected with multiple images adds all`() = runTest {
        viewModel.jsInterface.onImageDetected("https://example.com/img1.png", "img1")
        viewModel.jsInterface.onImageDetected("https://example.com/img2.png", "img2")
        viewModel.jsInterface.onImageDetected("https://example.com/img3.png", "img3")
        advanceUntilIdle()

        val images = viewModel.detectedImages.value
        assertEquals(3, images.size)
        assertTrue(images.contains("https://example.com/img1.png"))
        assertTrue(images.contains("https://example.com/img2.png"))
        assertTrue(images.contains("https://example.com/img3.png"))
    }

    @Test
    fun `duplicate image urls are added multiple times to list`() = runTest {
        // Nota: detectedImages es un List<String>, permite duplicados
        viewModel.jsInterface.onImageDetected("https://example.com/img.png", "alt")
        viewModel.jsInterface.onImageDetected("https://example.com/img.png", "alt")
        advanceUntilIdle()

        assertEquals(2, viewModel.detectedImages.value.size)
    }

    @Test
    fun `onImageDetected updates downloadState to Ready`() = runTest {
        viewModel.jsInterface.onImageDetected(
            imageUrl = "https://example.com/ready.png",
            altText = "Ready image"
        )
        advanceUntilIdle()

        val state = viewModel.downloadState.value
        assertTrue("Should be Ready state", state is DownloadState.Ready)
        assertEquals(
            "https://example.com/ready.png",
            (state as DownloadState.Ready).imageUrl
        )
    }

    // ── Limpieza de detectedImages ──
    // No existe método clearDetectedImages en WebViewViewModel,
    // pero verificamos que la lista se gestiona correctamente

    @Test
    fun `detectedImages accumulates images across multiple detections`() = runTest {
        viewModel.jsInterface.onImageDetected("https://example.com/a.png", "a")
        advanceUntilIdle()
        assertEquals(1, viewModel.detectedImages.value.size)

        viewModel.jsInterface.onImageDetected("https://example.com/b.png", "b")
        advanceUntilIdle()
        assertEquals(2, viewModel.detectedImages.value.size)
    }

    // ── WebViewUiState data class ──

    @Test
    fun `webViewUiState data class properties`() = runTest {
        val state = WebViewUiState(
            isLoading = false,
            loadingProgress = 75,
            currentUrl = "https://gemini.google.com",
            errorMessage = null
        )

        assertEquals(false, state.isLoading)
        assertEquals(75, state.loadingProgress)
        assertEquals("https://gemini.google.com", state.currentUrl)
        assertNull(state.errorMessage)
    }

    @Test
    fun `downloadState sealed class variants`() = runTest {
        assertTrue(DownloadState.Idle is DownloadState)
        assertTrue(DownloadState.Downloading is DownloadState)
        assertTrue(DownloadState.Ready("url") is DownloadState)
        assertTrue(DownloadState.Success("msg", "/path") is DownloadState)
        assertTrue(DownloadState.Error("err") is DownloadState)
    }

    @Test
    fun `DownloadState Ready contains imageUrl`() = runTest {
        val ready = DownloadState.Ready("https://example.com/img.png")
        assertEquals("https://example.com/img.png", ready.imageUrl)
    }

    @Test
    fun `DownloadState Success contains message and filePath`() = runTest {
        val success = DownloadState.Success("Saved!", "/downloads/img.png")
        assertEquals("Saved!", success.message)
        assertEquals("/downloads/img.png", success.filePath)
    }

    @Test
    fun `DownloadState Error contains message`() = runTest {
        val error = DownloadState.Error("Download failed")
        assertEquals("Download failed", error.message)
    }
}

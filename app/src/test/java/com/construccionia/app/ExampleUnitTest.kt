package com.construccionia.app

import com.construccionia.app.data.models.Infographic
import com.construccionia.app.webview.ImageEvent
import com.construccionia.app.webview.ImageJavaScriptInterface
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios de ejemplo para ConstruccionIA.
 */
class ExampleUnitTest {

    @Test
    fun `infographic data class creation`() {
        val testUriString = "https://example.com/image.png"
        val infographic = Infographic(
            id = 1L,
            name = "test_infographic",
            uriString = testUriString,
            filePath = "/test.png",
            isDemo = false,
            createdAt = 1000L
        )

        assertEquals(1L, infographic.id)
        assertEquals("test_infographic", infographic.name)
        assertEquals("/test.png", infographic.filePath)
        assertEquals(testUriString, infographic.uriString)
    }

    @Test
    fun `image detected event creation`() {
        val event = ImageEvent.Detected(
            imageUrl = "https://example.com/image.png",
            altText = "Test image"
        )

        assertEquals("https://example.com/image.png", event.imageUrl)
        assertEquals("Test image", event.altText)
    }

    @Test
    fun `image downloaded event creation`() {
        val event = ImageEvent.Downloaded(
            success = true,
            filePath = "/downloads/test.png",
            fileName = "test.png",
            message = "Success"
        )

        assertTrue(event.success)
        assertEquals("/downloads/test.png", event.filePath)
    }

    @Test
    fun `gallery ui state default values`() {
        val state = com.construccionia.app.viewmodel.GalleryUiState()

        assertEquals(true, state.isLoading) // Empieza en carga hasta que Room emite
        assertTrue(state.infographics.isEmpty())
        assertTrue(state.selectedIds.isEmpty())
    }

    @Test
    fun `web view ui state default values`() {
        val state = com.construccionia.app.viewmodel.WebViewUiState()

        assertEquals(true, state.isLoading)
        assertEquals(0, state.loadingProgress)
        assertEquals("", state.currentUrl)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `js injection script is not empty`() {
        val context = mockk<android.content.Context>(relaxed = true)
        val jsInterface = ImageJavaScriptInterface(context)
        val script = jsInterface.getInjectionScript()

        assertNotNull(script)
        assertTrue(script.isNotEmpty())
        assertTrue(script.contains("AndroidImageBridge"))
        assertTrue(script.contains("MutationObserver"))
    }
}


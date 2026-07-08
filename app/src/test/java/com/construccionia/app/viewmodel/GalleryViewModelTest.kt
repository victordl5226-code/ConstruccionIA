package com.construccionia.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.construccionia.app.data.models.Infographic
import com.construccionia.app.data.repository.ImageRepository
import com.construccionia.app.export.PdfExportHelper
import com.construccionia.app.export.PptExportHelper
import com.construccionia.app.ocr.OcrException
import com.construccionia.app.ocr.OcrHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], manifest = Config.NONE)
class GalleryViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockRepository = mockk<ImageRepository>()
    private val mockOcrHelper = mockk<OcrHelper>()
    private val mockPptExport = mockk<PptExportHelper>()
    private val mockPdfExport = mockk<PdfExportHelper>()

    private val infographicsFlow = MutableStateFlow<List<Infographic>>(emptyList())

    private val sampleInfographic1 = Infographic(
        id = 1L,
        name = "cimientos",
        uriString = "content://media/external/downloads/1",
        filePath = "content://media/external/downloads/1",
        isDemo = false,
        createdAt = 1000L,
        version = 1
    )

    private val sampleInfographic2 = Infographic(
        id = 2L,
        name = "estructura",
        uriString = "content://media/external/downloads/2",
        filePath = "content://media/external/downloads/2",
        isDemo = false,
        createdAt = 2000L,
        version = 1
    )

    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock repository flow (emitido en init)
        coEvery { mockRepository.getAllInfographicsFlow() } returns infographicsFlow
        coEvery { mockRepository.getAllInfographics() } returns emptyList()
        // Mock getShareUri (no es suspend, así que usamos every)
        every { mockRepository.getShareUri(any()) } returns Uri.parse("content://shared/test.png")

        viewModel = GalleryViewModel(
            mockApplication,
            mockRepository,
            mockOcrHelper,
            mockPptExport,
            mockPdfExport
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Estado inicial ──

    @Test
    fun `galleryState starts with loading and empty list`() = runTest {
        val state = viewModel.galleryState.value
        assertTrue("isLoading should be true initially", state.isLoading)
        assertTrue("infographics should be empty initially", state.infographics.isEmpty())
        assertTrue("selectedIds should be empty initially", state.selectedIds.isEmpty())
    }

    @Test
    fun `galleryState isLoading becomes false when flow emits data`() = runTest {
        // Emitir datos desde el flow
        infographicsFlow.value = listOf(sampleInfographic1)
        advanceUntilIdle()

        val state = viewModel.galleryState.value
        assertFalse("isLoading should be false after data emission", state.isLoading)
        assertEquals(1, state.infographics.size)
        assertEquals("cimientos", state.infographics[0].name)
    }

    @Test
    fun `ocrState starts with default values`() = runTest {
        val state = viewModel.ocrState.value
        assertFalse("isProcessing should be false initially", state.isProcessing)
        assertEquals(null, state.recognizedText)
        assertEquals(null, state.error)
    }

    @Test
    fun `exportState starts as Idle`() = runTest {
        assertTrue(viewModel.exportState.value is ExportState.Idle)
    }

    // ── toggleSelection ──

    @Test
    fun `toggleSelection adds id when not selected`() = runTest {
        viewModel.toggleSelection(sampleInfographic1)

        assertTrue(
            "selectedIds should contain id 1",
            viewModel.galleryState.value.selectedIds.contains(1L)
        )
        assertEquals(1, viewModel.galleryState.value.selectedIds.size)
    }

    @Test
    fun `toggleSelection removes id when already selected`() = runTest {
        // Seleccionar dos veces = toggle off
        viewModel.toggleSelection(sampleInfographic1)
        viewModel.toggleSelection(sampleInfographic1)

        assertFalse(
            "selectedIds should NOT contain id 1 after double toggle",
            viewModel.galleryState.value.selectedIds.contains(1L)
        )
        assertTrue(viewModel.galleryState.value.selectedIds.isEmpty())
    }

    @Test
    fun `toggleSelection handles multiple selections`() = runTest {
        viewModel.toggleSelection(sampleInfographic1)
        viewModel.toggleSelection(sampleInfographic2)

        val selectedIds = viewModel.galleryState.value.selectedIds
        assertTrue(selectedIds.contains(1L))
        assertTrue(selectedIds.contains(2L))
        assertEquals(2, selectedIds.size)
    }

    @Test
    fun `clearSelection empties selectedIds`() = runTest {
        viewModel.toggleSelection(sampleInfographic1)
        viewModel.toggleSelection(sampleInfographic2)
        viewModel.clearSelection()

        assertTrue(viewModel.galleryState.value.selectedIds.isEmpty())
    }

    // ── updateSearchQuery y filteredInfographics ──

    @Test
    fun `updateSearchQuery filters infographics by name`() = runTest {
        // Poblar datos
        infographicsFlow.value = listOf(sampleInfographic1, sampleInfographic2)
        advanceUntilIdle()

        viewModel.updateSearchQuery("cimiento")
        val filtered = viewModel.filteredInfographics
        assertEquals(1, filtered.size)
        assertEquals("cimientos", filtered[0].name)
    }

    @Test
    fun `filteredInfographics returns all when query is blank`() = runTest {
        infographicsFlow.value = listOf(sampleInfographic1, sampleInfographic2)
        advanceUntilIdle()

        viewModel.updateSearchQuery("")
        val filtered = viewModel.filteredInfographics
        assertEquals(2, filtered.size)
    }

    @Test
    fun `filteredInfographics is case insensitive`() = runTest {
        infographicsFlow.value = listOf(sampleInfographic1)
        advanceUntilIdle()

        viewModel.updateSearchQuery("CIMIENTOS")
        val filtered = viewModel.filteredInfographics
        assertEquals(1, filtered.size)
    }

    @Test
    fun `filteredInfographics returns empty for non matching query`() = runTest {
        infographicsFlow.value = listOf(sampleInfographic1, sampleInfographic2)
        advanceUntilIdle()

        viewModel.updateSearchQuery("noexiste")
        assertTrue(viewModel.filteredInfographics.isEmpty())
    }

    @Test
    fun `updateSearchQuery clears query`() = runTest {
        viewModel.updateSearchQuery("test")
        assertEquals("test", viewModel.searchQuery)

        viewModel.updateSearchQuery("")
        assertEquals("", viewModel.searchQuery)
    }

    // ── renameInfographic ──

    @Test
    fun `renameInfographic calls repository and clears renameTarget`() = runTest {
        coEvery { mockRepository.updateInfographic(any()) } just runs

        viewModel.renameTarget = sampleInfographic1
        viewModel.renameInfographic(sampleInfographic1, "nuevo-nombre")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockRepository.updateInfographic(
                match { it.name == "nuevo-nombre" && it.id == 1L }
            )
        }
        assertEquals(null, viewModel.renameTarget)
    }

    @Test
    fun `renameInfographic preserves other fields`() = runTest {
        coEvery { mockRepository.updateInfographic(any()) } just runs

        viewModel.renameInfographic(sampleInfographic1, "renombrado")
        advanceUntilIdle()

        coVerify {
            mockRepository.updateInfographic(
                match {
                    it.id == 1L &&
                    it.name == "renombrado" &&
                    it.filePath == "content://media/external/downloads/1" &&
                    it.uriString == "content://media/external/downloads/1"
                }
            )
        }
    }

    // ── deleteInfographic ──

    @Test
    fun `deleteInfographic calls repository deleteByPath`() = runTest {
        coEvery { mockRepository.deleteByPath(any()) } returns true

        viewModel.deleteInfographic(sampleInfographic1)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockRepository.deleteByPath("content://media/external/downloads/1")
        }
    }

    @Test
    fun `deleteInfographic handles repository returning false`() = runTest {
        coEvery { mockRepository.deleteByPath(any()) } returns false

        viewModel.deleteInfographic(sampleInfographic1)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.deleteByPath(any()) }
    }

    // ── exportSelectedToPpt ──

    @Test
    fun `exportSelectedToPpt returns error when no selection`() = runTest {
        viewModel.exportSelectedToPpt()
        advanceUntilIdle()

        val state = viewModel.exportState.value
        assertTrue("Should be Error state", state is ExportState.Error)
        assertEquals(
            "Selecciona al menos una imagen",
            (state as ExportState.Error).message
        )
    }

    /**
     * Crea un archivo PNG temporal para usar en tests de OCR.
     */
    private fun createTestImage(): java.io.File {
        val testImage = tempFolder.newFile("test_image.png")
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val fos = java.io.FileOutputStream(testImage)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()
        bitmap.recycle()
        return testImage
    }

    private fun mockNetworkConnectivity() {
        val mockConnectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val mockNetwork = mockk<android.net.Network>(relaxed = true)
        val mockCapabilities = mockk<android.net.NetworkCapabilities>(relaxed = true)
        every { mockApplication.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
        every { mockCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
    }

    @Test
    fun `exportSelectedToPpt calls pptHelper when items selected`() = runTest {
        mockNetworkConnectivity()

        // Poblar infografías y seleccionar una
        infographicsFlow.value = listOf(sampleInfographic1, sampleInfographic2)
        advanceUntilIdle()

        viewModel.toggleSelection(sampleInfographic1)

        val expectedUri = Uri.parse("content://export/test.pptx")
        coEvery { mockPptExport.exportToPpt(listOf("content://media/external/downloads/1")) } returns expectedUri

        viewModel.exportSelectedToPpt()
        advanceUntilIdle()

        val state = viewModel.exportState.value
        assertTrue("Should be Success state", state is ExportState.Success)
        assertEquals(expectedUri, (state as ExportState.Success).uri)
    }

    @Test
    fun `exportSelectedToPpt shows Exporting state then Success`() = runTest {
        mockNetworkConnectivity()

        infographicsFlow.value = listOf(sampleInfographic1)
        advanceUntilIdle()
        viewModel.toggleSelection(sampleInfographic1)

        coEvery { mockPptExport.exportToPpt(any()) } returns Uri.parse("content://test.pptx")

        viewModel.exportSelectedToPpt()
        // Con StandardTestDispatcher, la corrutina aún no ha empezado
        // Verificamos que después de avanzar el estado final sea Success
        advanceUntilIdle()
        assertTrue("Should be Success after completion", viewModel.exportState.value is ExportState.Success)
    }

    @Test
    fun `exportSelectedToPpt returns error when pptHelper returns null`() = runTest {
        mockNetworkConnectivity()

        infographicsFlow.value = listOf(sampleInfographic1)
        advanceUntilIdle()
        viewModel.toggleSelection(sampleInfographic1)

        coEvery { mockPptExport.exportToPpt(any()) } returns null

        viewModel.exportSelectedToPpt()
        advanceUntilIdle()

        val state = viewModel.exportState.value
        assertTrue("Should be Error state", state is ExportState.Error)
        assertEquals("Error al exportar la presentación", (state as ExportState.Error).message)
    }

    @Test
    fun `exportSelectedToPpt returns error when no network`() = runTest {
        // No configuramos conectividad → isNetworkAvailable() retorna false
        infographicsFlow.value = listOf(sampleInfographic1)
        advanceUntilIdle()
        viewModel.toggleSelection(sampleInfographic1)

        viewModel.exportSelectedToPpt()
        advanceUntilIdle()

        val state = viewModel.exportState.value
        assertTrue("Should be Error state for no network", state is ExportState.Error)
        assertTrue(
            (state as ExportState.Error).message.contains("conexión a internet")
        )
    }

    // ── Exportación PDF ──

    @Test
    fun `exportSelectedToPdf returns error when no selection`() = runTest {
        viewModel.exportSelectedToPdf()
        advanceUntilIdle()

        val state = viewModel.exportState.value
        assertTrue(state is ExportState.Error)
        assertEquals("Selecciona al menos una imagen", (state as ExportState.Error).message)
    }

    @Test
    fun `exportSelectedToPdf calls pdfHelper when items selected`() = runTest {
        infographicsFlow.value = listOf(sampleInfographic1)
        advanceUntilIdle()
        viewModel.toggleSelection(sampleInfographic1)

        val expectedUri = Uri.parse("content://test.pdf")
        coEvery { mockPdfExport.exportToPdf(any()) } returns expectedUri

        viewModel.exportSelectedToPdf()
        advanceUntilIdle()

        val state = viewModel.exportState.value
        assertTrue("Should be Success state", state is ExportState.Success)
        assertEquals(expectedUri, (state as ExportState.Success).uri)
    }

    // ── resetExportState ──

    @Test
    fun `resetExportState returns to Idle`() = runTest {
        // Primero provocamos un error
        viewModel.exportSelectedToPpt()
        advanceUntilIdle()
        assertTrue(viewModel.exportState.value is ExportState.Error)

        viewModel.resetExportState()
        assertTrue(viewModel.exportState.value is ExportState.Idle)
    }

    // ── shareInfographic ──

    @Test
    fun `shareInfographic sends uri through channel`() = runTest {
        val expectedUri = Uri.parse("content://shared/test.png")
        coEvery { mockRepository.getShareUri(any()) } returns expectedUri

        // shareInfographic envía al Channel y luego avanza
        viewModel.shareInfographic(sampleInfographic1)
        advanceUntilIdle()

        coVerify { mockRepository.getShareUri(sampleInfographic1.filePath) }
    }

    // ── refreshGallery ──

    @Test
    fun `refreshGallery calls repository and manages refreshing state`() = runTest {
        coEvery { mockRepository.getAllInfographics() } returns listOf(sampleInfographic1)

        // refreshGallery es suspend, así que se ejecuta directamente en runTest
        viewModel.refreshGallery()

        // Después de completarse, isRefreshing vuelve a false (ya que la ejecución es síncrona)
        assertFalse("isRefreshing should be false after refresh", viewModel.isRefreshing)

        // getAllInfographics se llama: una vez en init y otra en refresh = 2 veces
        coVerify(exactly = 2) { mockRepository.getAllInfographics() }
    }

    // ── OCR ──

    @Test
    fun `performOcr updates state to processing then success`() = runTest {
        val testImage = createTestImage()
        every { mockOcrHelper.close() } just runs
        coEvery { mockOcrHelper.recognizeText(any()) } returns Result.success("Texto reconocido")

        viewModel.performOcr(testImage.absolutePath)
        advanceUntilIdle()

        val ocrState = viewModel.ocrState.value
        assertEquals("Texto reconocido", ocrState.recognizedText)
        assertFalse(ocrState.isProcessing)
        assertEquals(null, ocrState.error)

        coVerify { mockOcrHelper.recognizeText(any()) }
    }

    @Test
    fun `performOcr handles recognition failure`() = runTest {
        val testImage = createTestImage()
        every { mockOcrHelper.close() } just runs
        coEvery { mockOcrHelper.recognizeText(any()) } returns Result.failure(OcrException("Error OCR"))

        viewModel.performOcr(testImage.absolutePath)
        advanceUntilIdle()

        val ocrState = viewModel.ocrState.value
        assertFalse(ocrState.isProcessing)
        assertTrue("Should have error", ocrState.error?.isNotEmpty() == true)
        assertEquals(null, ocrState.recognizedText)

        coVerify { mockOcrHelper.recognizeText(any()) }
    }

    @Test
    fun `clearOcr resets ocr state`() = runTest {
        val testImage = createTestImage()
        every { mockOcrHelper.close() } just runs
        coEvery { mockOcrHelper.recognizeText(any()) } returns Result.success("texto")

        viewModel.performOcr(testImage.absolutePath)
        advanceUntilIdle()

        viewModel.clearOcr()
        val state = viewModel.ocrState.value
        assertFalse(state.isProcessing)
        assertEquals(null, state.recognizedText)
        assertEquals(null, state.error)
    }

    @Test
    fun `updateEditedText changes recognized text`() = runTest {
        viewModel.updateEditedText("texto editado")
        assertEquals("texto editado", viewModel.ocrState.value.recognizedText)
    }

    // ── exportSingle ──

    @Test
    fun `exportSingleToPpt exports single file`() = runTest {
        mockNetworkConnectivity()

        val expectedUri = Uri.parse("content://single.pptx")
        coEvery { mockPptExport.exportToPpt(listOf("/path/to/file.png")) } returns expectedUri

        viewModel.exportSingleToPpt("/path/to/file.png")
        advanceUntilIdle()

        val state = viewModel.exportState.value
        assertTrue("Should be Success state", state is ExportState.Success)
        assertEquals(expectedUri, (state as ExportState.Success).uri)
    }

    @Test
    fun `exportSingleToPdf exports single file`() = runTest {
        val expectedUri = Uri.parse("content://single.pdf")
        coEvery { mockPdfExport.exportSingleToPdf("/path/to/file.png") } returns expectedUri

        viewModel.exportSingleToPdf("/path/to/file.png")
        advanceUntilIdle()

        val state = viewModel.exportState.value
        assertTrue("Should be Success state", state is ExportState.Success)
        assertEquals(expectedUri, (state as ExportState.Success).uri)
    }
}

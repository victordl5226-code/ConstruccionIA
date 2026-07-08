package com.construccionia.feature.export.data.apachepoi

import android.content.Context
import com.construccionia.core.common.FileOutput
import com.construccionia.core.common.ProcessingException
import com.construccionia.feature.export.domain.model.ExportConfig
import com.construccionia.feature.export.domain.model.SlideContent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.sl.usermodel.PictureData
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import java.awt.Color
import java.awt.Rectangle
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de generación de PowerPoint usando Apache POI.
 * Crea presentaciones .pptx con diapositivas que contienen título,
 * cuerpo de texto e imágenes.
 */
@Singleton
class ApachePoiPptxGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) : PptxGenerator {

    override suspend fun generate(
        slides: List<SlideContent>,
        config: ExportConfig,
        outputPath: String
    ): FileOutput = withContext(Dispatchers.IO) {
        try {
            val presentation = XMLSlideShow()
            val slideWidth = presentation.pageSlideSize.width
            val slideHeight = presentation.pageSlideSize.height

            // Crear diapositivas
            slides.forEachIndexed { index, content ->
                val slide: XSLFSlide = presentation.createSlide()

                // Título
                val titleBox = slide.createTextBox()
                titleBox.text = content.title
                titleBox.anchor = Rectangle(50, 30, slideWidth - 100, 60)

                // Cuerpo de texto
                val bodyBox = slide.createTextBox()
                bodyBox.text = content.body
                bodyBox.anchor = Rectangle(50, 100, slideWidth - 100, slideHeight - 200)

                // Imagen (si existe)
                content.image?.let { imageBytes ->
                    try {
                        val pictureIndex = presentation.addPicture(
                            ByteArrayInputStream(imageBytes.bytes),
                            PictureData.PictureType.PNG
                        )
                        val picture = slide.createPicture(pictureIndex)
                        picture.anchor = Rectangle(
                            50, 100,
                            (slideWidth - 100).coerceAtMost(imageBytes.width),
                            (slideHeight - 200).coerceAtMost(imageBytes.height)
                        )
                    } catch (_: Exception) {
                        // Si falla la imagen, continuamos sin ella
                    }
                }

                // Número de diapositiva
                if (config.includeSlideNumbers) {
                    val numBox = slide.createTextBox()
                    numBox.text = "${index + 1} / ${slides.size}"
                    numBox.anchor = Rectangle(slideWidth - 100, slideHeight - 40, 80, 30)
                }
            }

            // Guardar archivo
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { outputStream ->
                presentation.write(outputStream)
            }
            presentation.close()

            FileOutput(
                path = outputFile.absolutePath,
                mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                fileName = outputFile.name
            )
        } catch (e: Exception) {
            throw ProcessingException("Error al generar PowerPoint: ${e.message}", e)
        }
    }
}

package com.construccionia.app.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import timber.log.Timber
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Crea presentaciones PowerPoint (.pptx) sin dependencias de java.awt.
 *
 * Genera manualmente el XML OOXML necesario y lo empaqueta en un ZIP.
 * Compatible con PowerPoint, Google Slides y LibreOffice.
 */
class PptExportHelper(private val context: Context) {

    companion object {
        private const val SUBDIR = "ConstruccionIA"
    }

    fun exportToPpt(imagePaths: List<String>): Uri? {
        return try {
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                SUBDIR
            )
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val pptFile = File(downloadsDir, "infografia_$timestamp.pptx")

            FileOutputStream(pptFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    generatePptx(zos, imagePaths)
                }
            }

            Timber.d("PPT exportado: ${pptFile.absolutePath}")

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pptFile
            )
        } catch (e: Exception) {
            Timber.e(e, "Error exportando PPT")
            null
        }
    }

    private fun generatePptx(zos: ZipOutputStream, imagePaths: List<String>) {
        // 1. [Content_Types].xml
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        zos.write(contentTypesXml().toByteArray())
        zos.closeEntry()

        // 2. _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write(relsXml().toByteArray())
        zos.closeEntry()

        // 3. ppt/_rels/presentation.xml.rels
        zos.putNextEntry(ZipEntry("ppt/_rels/presentation.xml.rels"))
        zos.write(presentationRelsXml(imagePaths.size).toByteArray())
        zos.closeEntry()

        // 4. ppt/presentation.xml
        zos.putNextEntry(ZipEntry("ppt/presentation.xml"))
        zos.write(presentationXml(imagePaths.size).toByteArray())
        zos.closeEntry()

        // 5. ppt/slideMasters/slideMaster1.xml
        zos.putNextEntry(ZipEntry("ppt/slideMasters/slideMaster1.xml"))
        zos.write(slideMasterXml().toByteArray())
        zos.closeEntry()

        // 5b. ppt/slideMasters/_rels/slideMaster1.xml.rels
        zos.putNextEntry(ZipEntry("ppt/slideMasters/_rels/slideMaster1.xml.rels"))
        zos.write(slideMasterRelsXml().toByteArray())
        zos.closeEntry()

        // 6. ppt/slideLayouts/slideLayout1.xml
        zos.putNextEntry(ZipEntry("ppt/slideLayouts/slideLayout1.xml"))
        zos.write(slideLayoutXml().toByteArray())
        zos.closeEntry()

        // 6b. ppt/slideLayouts/_rels/slideLayout1.xml.rels
        zos.putNextEntry(ZipEntry("ppt/slideLayouts/_rels/slideLayout1.xml.rels"))
        zos.write(slideLayoutRelsXml().toByteArray())
        zos.closeEntry()

        // 7. ppt/theme/theme1.xml
        zos.putNextEntry(ZipEntry("ppt/theme/theme1.xml"))
        zos.write(themeXml().toByteArray())
        zos.closeEntry()

        // 8. Imágenes y diapositivas
        imagePaths.forEachIndexed { index, path ->
            val imgBytes = loadImageBytes(path) ?: return@forEachIndexed
            val slideNum = index + 1

            // Imagen: ppt/media/image{slideNum}.png
            zos.putNextEntry(ZipEntry("ppt/media/image$slideNum.png"))
            zos.write(imgBytes)
            zos.closeEntry()

            // Relaciones de la diapositiva con su imagen
            zos.putNextEntry(ZipEntry("ppt/slides/_rels/slide${slideNum}.xml.rels"))
            zos.write(slideRelsXml(slideNum).toByteArray())
            zos.closeEntry()

            // Diapositiva: ppt/slides/slide{slideNum}.xml
            zos.putNextEntry(ZipEntry("ppt/slides/slide$slideNum.xml"))
            zos.write(slideXml(slideNum).toByteArray())
            zos.closeEntry()
        }

        // 9. ppt/presProps.xml
        zos.putNextEntry(ZipEntry("ppt/presProps.xml"))
        zos.write(presPropsXml().toByteArray())
        zos.closeEntry()

        // 10. ppt/tableStyles.xml
        zos.putNextEntry(ZipEntry("ppt/tableStyles.xml"))
        zos.write(tableStylesXml().toByteArray())
        zos.closeEntry()

        // 11. ppt/viewProps.xml
        zos.putNextEntry(ZipEntry("ppt/viewProps.xml"))
        zos.write(viewPropsXml().toByteArray())
        zos.closeEntry()
    }

    private fun loadImageBytes(path: String): ByteArray? {
        return try {
            val bitmap = BitmapFactory.decodeFile(path) ?: return null
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            bitmap.recycle()
            stream.toByteArray()
        } catch (e: Exception) {
            Timber.w(e, "Error cargando imagen: $path")
            null
        }
    }

    // ─── XML Templates ───

    private fun contentTypesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  <Override PartName="/ppt/presProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presProps+xml"/>
  <Override PartName="/ppt/viewProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.viewProps+xml"/>
  <Override PartName="/ppt/tableStyles.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.tableStyles+xml"/>
</Types>"""

    private fun relsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>"""

    private fun presentationRelsXml(slideCount: Int): String {
        val sb = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdMaster" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
  <Relationship Id="rIdTheme" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
  <Relationship Id="rIdPresProps" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/presProps" Target="presProps.xml"/>
  <Relationship Id="rIdViewProps" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/viewProps" Target="viewProps.xml"/>
  <Relationship Id="rIdTableStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/tableStyles" Target="tableStyles.xml""")
        for (i in 1..slideCount) {
            sb.append("""
  <Relationship Id="rIdSlide$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide$i.xml"/>""")
        }
        sb.append("""
</Relationships>""")
        return sb.toString()
    }

    private fun presentationXml(slideCount: Int): String {
        val sb = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst>
    <p:sldMasterId id="2147483648" r:id="rIdMaster"/>
  </p:sldMasterIdLst>
  <p:sldIdLst>""")
        for (i in 1..slideCount) {
            sb.append("""
    <p:sldId id="$i" r:id="rIdSlide$i"/>""")
        }
        sb.append("""
  </p:sldIdLst>
  <p:sldSz cx="12192000" cy="6858000"/>
  <p:notesSz cx="6858000" cy="9144000"/>
  <p:defaultTextStyle/>
</p:presentation>""")
        return sb.toString()
    }

    private fun slideMasterXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:nvPr>
          <p:cpholder/>
        </p:nvPr>
        <p:nvGrpSpPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr/>
    </p:spTree>
  </p:cSld>
  <p:sldLayoutIdLst>
    <p:sldLayoutId id="2147483649" r:id="rIdLayout"/>
  </p:sldLayoutIdLst>
</p:sldMaster>"""

    private fun slideLayoutXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:nvPr/>
        <p:nvGrpSpPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr/>
    </p:spTree>
  </p:cSld>
</p:sldLayout>"""

    private fun themeXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="ConstruccionIA Theme">
  <a:themeElements>
    <a:clrScheme name="Default">
      <a:dk1><a:srgbClr val="000000"/></a:dk1>
      <a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>
      <a:dk2><a:srgbClr val="44546A"/></a:dk2>
      <a:lt2><a:srgbClr val="E7E6E6"/></a:lt2>
      <a:accent1><a:srgbClr val="1565C0"/></a:accent1>
      <a:accent2><a:srgbClr val="00897B"/></a:accent2>
      <a:accent3><a:srgbClr val="FF6F00"/></a:accent3>
      <a:accent4><a:srgbClr val="C62828"/></a:accent4>
      <a:accent5><a:srgbClr val="6A1B9A"/></a:accent5>
      <a:accent6><a:srgbClr val="2E7D32"/></a:accent6>
      <a:hlink><a:srgbClr val="0563C1"/></a:hlink>
      <a:folHlink><a:srgbClr val="954F72"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="Default">
      <a:majorFont><a:latin typeface="Calibri Light"/><a:ea typeface=""/><a:cs typeface=""/></a:majorFont>
      <a:minorFont><a:latin typeface="Calibri"/><a:ea typeface=""/><a:cs typeface=""/></a:minorFont>
    </a:fontScheme>
    <a:fmtScheme name="Default">
      <a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:gradFill rotWithShape="1"><a:gsLst><a:gs pos="0"><a:schemeClr val="phClr"/></a:gs></a:gsLst></a:gradFill></a:fillStyleLst>
      <a:lnStyleLst><a:ln w="6350"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst>
      <a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst>
      <a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst>
    </a:fmtScheme>
  </a:themeElements>
</a:theme>"""

    private fun presPropsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentationPr xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"/>
"""

    private fun viewPropsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:viewPr xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:normalViewPr>
    <p:restoredLeft autoAdjust="false" sz="0.5"/>
    <p:restoredTop autoAdjust="false" sz="0.5"/>
  </p:normalViewPr>
</p:viewPr>"""

    private fun tableStylesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:tblStyleLst xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" def="{5C22544A-7EE6-4342-B048-85BDC9FD1C3A}"/>"""

    private fun slideMasterRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdLayout" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rIdTheme" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>"""

    private fun slideLayoutRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdTheme" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>"""

    private fun slideRelsXml(slideNum: Int) = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdImg$slideNum" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/image$slideNum.png"/>
</Relationships>"""

    private fun slideXml(slideNum: Int): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:nvPr/>
        <p:nvGrpSpPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr/>
      <p:pic>
        <p:nvPicPr>
          <p:cNvPr id="$slideNum" name="Infografia $slideNum"/>
          <p:cNvPicPr/>
          <p:nvPr/>
        </p:nvPicPr>
        <p:blipFill>
          <a:blip r:embed="rIdImg$slideNum"/>
          <a:stretch>
            <a:fillRect/>
          </a:stretch>
        </p:blipFill>
        <p:spPr>
          <a:xfrm>
            <a:off x="0" y="0"/>
            <a:ext cx="12192000" cy="6858000"/>
          </a:xfrm>
          <a:prstGeom prst="rect">
            <a:avLst/>
          </a:prstGeom>
        </p:spPr>
      </p:pic>
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr>
    <a:masterClrMapping/>
  </p:clrMapOvr>
</p:sld>"""
    }
}

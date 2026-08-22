/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.export

import com.greenshark.quickattend.data.AttendanceSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The xlsx is assembled by hand rather than by a spreadsheet library, so these
 * check the two things that would silently produce a file Excel refuses to
 * open: a missing part, and XML that does not parse.
 */
class XlsxExporterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val document = ExportDocument(
        title = "Clase de Yoga",
        subtitle = "Attendance record · all attendants",
        metadata = listOf("Host" to "Carlos Jiménez", "Location" to "Salon C201"),
        columns = listOf(
            ExportColumn(title = "Attendant"),
            ExportColumn(title = "On time"),
            ExportColumn(title = "Signature")
        ),
        rows = listOf(
            ExportRow(
                cells = listOf("Ana & Sons", "12", ""),
                signature = AttendanceSignature.Code("QA-7F3A-91C2-88DE")
            ),
            ExportRow(
                cells = listOf("<script>", "3", ""),
                signature = AttendanceSignature.None
            )
        ),
        notes = listOf("On time counts full."),
        fileName = "yoga"
    )

    @Test
    fun `writes every part an xlsx reader needs`() {
        val written = XlsxExporter.write(document, folder.target())

        assertNotNull(written)
        ZipFile(written).use { zip ->
            val names = zip.entries().toList().map { it.name }.toSet()
            assertEquals(
                setOf(
                    "[Content_Types].xml",
                    "_rels/.rels",
                    "xl/workbook.xml",
                    "xl/_rels/workbook.xml.rels",
                    "xl/styles.xml",
                    "xl/worksheets/sheet1.xml"
                ),
                names
            )
        }
    }

    @Test
    fun `every part is well formed xml`() {
        val written = requireNotNull(XlsxExporter.write(document, folder.target()))

        ZipFile(written).use { zip ->
            zip.entries().toList().forEach { entry ->
                val parsed = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(zip.getInputStream(entry))
                assertNotNull("${entry.name} did not parse", parsed.documentElement)
            }
        }
    }

    @Test
    fun `escapes markup so a name never breaks the sheet`() {
        val written = requireNotNull(XlsxExporter.write(document, folder.target()))

        val sheet = written.readPart("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("Ana &amp; Sons"))
        assertTrue(sheet.contains("&lt;script&gt;"))
    }

    @Test
    fun `writes counts as numbers and signatures as their code`() {
        val written = requireNotNull(XlsxExporter.write(document, folder.target()))

        val sheet = written.readPart("xl/worksheets/sheet1.xml")
        // A numeric cell carries a bare <v>, a text one an inline string.
        assertTrue(sheet.contains("<v>12</v>"))
        assertTrue(sheet.contains("QA-7F3A-91C2-88DE"))
    }

    @Test
    fun `cell references carry both column and row`() {
        val written = requireNotNull(XlsxExporter.write(document, folder.target()))

        val sheet = written.readPart("xl/worksheets/sheet1.xml")
        // The title sits alone on row 1, so its cell must be exactly A1.
        assertTrue(sheet.contains("""<c r="A1""""))
        // Three columns means the last one is C, never a bare letter.
        assertTrue(Regex("""<c r="C\d+"""").containsMatchIn(sheet))
    }

    @Test
    fun `writes column widths with a dot whatever the device locale is`() {
        val spanish = Locale.forLanguageTag("es-DO")
        val previous = Locale.getDefault()
        Locale.setDefault(spanish)
        try {
            val written = requireNotNull(XlsxExporter.write(document, folder.target()))

            val sheet = written.readPart("xl/worksheets/sheet1.xml")
            // A locale that writes decimals with a comma would produce
            // width="21,6", which no spreadsheet reader accepts.
            assertFalse(sheet.contains(Regex("""width="\d+,\d+"""")))
            assertTrue(sheet.contains(Regex("""width="\d+\.\d+"""")))
        } finally {
            Locale.setDefault(previous)
        }
    }

    private fun TemporaryFolder.target(): File = File(newFolder(), "yoga.xlsx")

    private fun File.readPart(name: String): String = ZipFile(this).use { zip ->
        zip.getInputStream(zip.getEntry(name)).bufferedReader().readText()
    }
}

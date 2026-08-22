/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.export

import com.greenshark.quickattend.data.AttendanceSignature
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes an [ExportDocument] as a real .xlsx.
 *
 * An xlsx is a zip of XML parts, so it is written directly rather than pulling
 * in a spreadsheet library: Apache POI is tens of megabytes of jar that Android
 * cannot dex comfortably, and the sheet needed here is a plain table.
 *
 * Numbers are written as numbers rather than text, so a host can sort and total
 * a column the moment they open the file. Signatures are written as their code:
 * a cell cannot hold a picture the way a PDF page can, and the code is what
 * makes a row checkable anyway.
 */
object XlsxExporter {

    /** Returns null when the file could not be written, so callers can report it. */
    fun write(document: ExportDocument, target: File): File? = try {
        target.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(target)).use { zip ->
            zip.writePart(name = "[Content_Types].xml", content = CONTENT_TYPES)
            zip.writePart(name = "_rels/.rels", content = ROOT_RELS)
            zip.writePart(name = "xl/workbook.xml", content = workbook())
            zip.writePart(name = "xl/_rels/workbook.xml.rels", content = WORKBOOK_RELS)
            zip.writePart(name = "xl/styles.xml", content = STYLES)
            zip.writePart(name = "xl/worksheets/sheet1.xml", content = sheet(document))
        }
        target
    } catch (e: IOException) {
        null
    }

    private fun ZipOutputStream.writePart(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun workbook(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="$NS_SPREADSHEET" xmlns:r="$NS_RELATIONSHIPS">
          <sheets><sheet name="$SHEET_NAME" sheetId="1" r:id="rId1"/></sheets>
        </workbook>
    """.collapsed()

    // ---- sheet --------------------------------------------------------------

    private fun sheet(document: ExportDocument): String {
        val rows = StringBuilder()
        var rowIndex = 1

        rowIndex = rows.appendRow(rowIndex) { row ->
            row.textCell(column = 1, value = document.title, style = STYLE_TITLE)
        }
        if (document.subtitle.isNotBlank()) {
            rowIndex = rows.appendRow(rowIndex) { row ->
                row.textCell(column = 1, value = document.subtitle, style = STYLE_SUBTITLE)
            }
        }
        rowIndex = rows.appendBlank(rowIndex)

        document.metadata.forEach { (label, value) ->
            rowIndex = rows.appendRow(rowIndex) { row ->
                row.textCell(column = 1, value = label, style = STYLE_META_LABEL)
                row.textCell(column = 2, value = value, style = STYLE_BODY)
            }
        }
        rowIndex = rows.appendBlank(rowIndex)

        rowIndex = rows.appendRow(rowIndex) { row ->
            document.columns.forEachIndexed { index, column ->
                row.textCell(column = index + 1, value = column.title, style = STYLE_HEADER)
            }
            // Signatures need a column of their own even when the document's
            // last column is the placeholder the PDF fills with an image.
            if (!document.hasSignatureColumn()) {
                row.textCell(
                    column = document.columns.size + 1,
                    value = SIGNATURE_HEADER,
                    style = STYLE_HEADER
                )
            }
        }

        document.rows.forEach { exportRow ->
            rowIndex = rows.appendRow(rowIndex) { row ->
                document.columns.forEachIndexed { index, _ ->
                    val isSignature =
                        index == document.columns.lastIndex && document.hasSignatureColumn()
                    val value = if (isSignature) {
                        exportRow.signature.spreadsheetValue()
                    } else {
                        exportRow.cells.getOrElse(index) { "" }
                    }
                    row.autoCell(column = index + 1, value = value)
                }
                if (!document.hasSignatureColumn()) {
                    row.textCell(
                        column = document.columns.size + 1,
                        value = exportRow.signature.spreadsheetValue(),
                        style = STYLE_CODE
                    )
                }
            }
        }

        if (document.notes.isNotEmpty()) {
            rowIndex = rows.appendBlank(rowIndex)
            document.notes.forEach { note ->
                rowIndex = rows.appendRow(rowIndex) { row ->
                    row.textCell(column = 1, value = note, style = STYLE_NOTE)
                }
            }
        }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="$NS_SPREADSHEET">
              ${document.columnDefinitions()}
              <sheetData>$rows</sheetData>
            </worksheet>
        """.collapsed()
    }

    /** Column widths, so a name or an email is readable without dragging. */
    private fun ExportDocument.columnDefinitions(): String {
        val count = if (hasSignatureColumn()) columns.size else columns.size + 1
        val definitions = (1..count).joinToString(separator = "") { index ->
            val column = columns.getOrNull(index - 1)
            val width = when {
                column == null -> SIGNATURE_WIDTH
                column.title.equals(SIGNATURE_COLUMN_TITLE, ignoreCase = true) -> SIGNATURE_WIDTH
                else -> (column.weight * WIDTH_PER_WEIGHT).coerceIn(MIN_WIDTH, MAX_WIDTH)
            }
            // Locale.ROOT because an XML attribute wants "21.6", and the
            // default locale of the device would happily write "21,6".
            val formatted = String.format(Locale.ROOT, "%.1f", width)
            """<col min="$index" max="$index" width="$formatted" customWidth="1"/>"""
        }
        return "<cols>$definitions</cols>"
    }

    private fun ExportDocument.hasSignatureColumn(): Boolean =
        columns.lastOrNull()?.title.equals(SIGNATURE_COLUMN_TITLE, ignoreCase = true)

    private fun AttendanceSignature.spreadsheetValue(): String = displayCode ?: NO_SIGNATURE

    // ---- row and cell building ----------------------------------------------

    private inline fun StringBuilder.appendRow(rowIndex: Int, build: (SheetRow) -> Unit): Int {
        val row = SheetRow(rowIndex)
        build(row)
        append(row.render())
        return rowIndex + 1
    }

    private fun StringBuilder.appendBlank(rowIndex: Int): Int {
        append("<row r=\"$rowIndex\"/>")
        return rowIndex + 1
    }

    /**
     * One row of cells. It holds its own index so every cell can carry a full
     * reference such as "C7", which is what a spreadsheet reader keys on.
     */
    private class SheetRow(private val rowIndex: Int) {

        private val cells = StringBuilder()

        /** Whole numbers become numeric cells; everything else stays text. */
        fun autoCell(column: Int, value: String) {
            val number = value.toLongOrNull()
            if (number == null) {
                textCell(column = column, value = value, style = STYLE_BODY)
            } else {
                cells.append("<c r=\"${reference(column)}\" s=\"$STYLE_BODY\"><v>$number</v></c>")
            }
        }

        /**
         * Written as an inline string rather than through a shared-strings part,
         * which costs a few bytes and saves a whole part plus its bookkeeping.
         */
        fun textCell(column: Int, value: String, style: Int) {
            cells.append(
                "<c r=\"${reference(column)}\" s=\"$style\" t=\"inlineStr\">" +
                        "<is><t xml:space=\"preserve\">${value.escapedForXml()}</t></is></c>"
            )
        }

        fun render(): String = "<row r=\"$rowIndex\">$cells</row>"

        private fun reference(column: Int): String = columnLetter(column) + rowIndex
    }

    private fun columnLetter(column: Int): String {
        var remaining = column
        val letters = StringBuilder()
        while (remaining > 0) {
            val index = (remaining - 1) % ALPHABET_SIZE
            letters.insert(0, 'A' + index)
            remaining = (remaining - 1) / ALPHABET_SIZE
        }
        return letters.toString()
    }

    private fun String.escapedForXml(): String = buildString(capacity = length) {
        this@escapedForXml.forEach { character ->
            when {
                character == '&' -> append("&amp;")
                character == '<' -> append("&lt;")
                character == '>' -> append("&gt;")
                character == '"' -> append("&quot;")
                character == '\'' -> append("&apos;")
                // Control characters are not legal in XML 1.0 at all.
                character.code < 0x20 && character != '\t' && character != '\n' -> Unit
                else -> append(character)
            }
        }
    }

    /** Keeps the raw strings readable in source without shipping the whitespace. */
    private fun String.collapsed(): String =
        trimIndent().lineSequence().joinToString(separator = "") { it.trim() }

    // ---- static parts -------------------------------------------------------

    private const val NS_SPREADSHEET =
        "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_RELATIONSHIPS =
        "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    private const val SHEET_NAME = "Attendance"
    private const val SIGNATURE_COLUMN_TITLE = "Signature"
    private const val SIGNATURE_HEADER = "Signature"
    private const val NO_SIGNATURE = "--"

    private const val ALPHABET_SIZE = 26
    private const val WIDTH_PER_WEIGHT = 9f
    private const val MIN_WIDTH = 8f
    private const val MAX_WIDTH = 32f
    private const val SIGNATURE_WIDTH = 22f

    /** Indices into the cellXfs list of [STYLES], in the order declared there. */
    private const val STYLE_BODY = 0
    private const val STYLE_TITLE = 1
    private const val STYLE_SUBTITLE = 2
    private const val STYLE_HEADER = 3
    private const val STYLE_META_LABEL = 4
    private const val STYLE_CODE = 5
    private const val STYLE_NOTE = 6

    private val CONTENT_TYPES = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
    """.collapsed()

    private val ROOT_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="$NS_RELATIONSHIPS/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.collapsed()

    private val WORKBOOK_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="$NS_RELATIONSHIPS/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="$NS_RELATIONSHIPS/styles" Target="styles.xml"/>
        </Relationships>
    """.collapsed()

    private val STYLES = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="$NS_SPREADSHEET">
          <fonts count="6">
            <font><sz val="11"/><name val="Calibri"/></font>
            <font><b/><sz val="16"/><name val="Calibri"/></font>
            <font><sz val="11"/><color rgb="FF6E6E6E"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><name val="Calibri"/></font>
            <font><sz val="10"/><color rgb="FF00695C"/><name val="Consolas"/></font>
            <font><sz val="9"/><color rgb="FF6E6E6E"/><name val="Calibri"/></font>
          </fonts>
          <fills count="3">
            <fill><patternFill patternType="none"/></fill>
            <fill><patternFill patternType="gray125"/></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFD6F7EF"/><bgColor indexed="64"/></patternFill></fill>
          </fills>
          <borders count="2">
            <border><left/><right/><top/><bottom/><diagonal/></border>
            <border><left/><right/><top/><bottom style="thin"><color rgb="FFBBBBBB"/></bottom><diagonal/></border>
          </borders>
          <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
          <cellXfs count="7">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
            <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="0" fontId="5" fillId="0" borderId="0" xfId="0" applyFont="1"/>
          </cellXfs>
          <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
    """.collapsed()
}

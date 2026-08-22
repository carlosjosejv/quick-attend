/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Base64
import com.greenshark.quickattend.data.AttendanceSignature
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Writes an [ExportDocument] as an A4 PDF using the platform's own PDF writer,
 * so exporting works offline and adds no dependency.
 *
 * The table paginates on its own: the header row and the column titles repeat
 * on every page, and a row is never split across a page break.
 */
object PdfExporter {

    /** Returns null when the file could not be written, so callers can report it. */
    fun write(document: ExportDocument, target: File): File? {
        val pdf = PdfDocument()
        return try {
            renderPages(document = document, pdf = pdf)
            target.parentFile?.mkdirs()
            FileOutputStream(target).use(pdf::writeTo)
            target
        } catch (e: IOException) {
            null
        } finally {
            pdf.close()
        }
    }

    private fun renderPages(document: ExportDocument, pdf: PdfDocument) {
        val paints = TablePaints()
        val widths = document.columnWidths()
        var pageNumber = 1
        var page = pdf.startPage(pageInfo(pageNumber))
        var canvas = page.canvas
        var y = canvas.drawHeader(document = document, paints = paints)
        y = canvas.drawColumnTitles(document = document, widths = widths, top = y, paints = paints)

        document.rows.forEachIndexed { index, row ->
            val height = row.height(paints)
            if (y + height > CONTENT_BOTTOM) {
                canvas.drawPageNumber(pageNumber, paints)
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(pageInfo(pageNumber))
                canvas = page.canvas
                y = canvas.drawHeader(document = document, paints = paints, continued = true)
                y = canvas.drawColumnTitles(document, widths, y, paints)
            }
            canvas.drawRow(
                row = row,
                document = document,
                widths = widths,
                top = y,
                height = height,
                striped = index % 2 == 1,
                paints = paints
            )
            y += height
        }

        y = canvas.drawEmptyNotice(document = document, top = y, paints = paints)
        canvas.drawNotes(document = document, top = y, paints = paints)
        canvas.drawPageNumber(pageNumber, paints)
        pdf.finishPage(page)
    }

    private fun pageInfo(pageNumber: Int): PdfDocument.PageInfo =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

    // ---- header -------------------------------------------------------------

    private fun Canvas.drawHeader(
        document: ExportDocument,
        paints: TablePaints,
        continued: Boolean = false
    ): Float {
        var y = MARGIN + paints.title.textSize
        drawText(document.title, MARGIN, y, paints.title)

        val subtitle = if (continued) "${document.subtitle} (continued)" else document.subtitle
        if (subtitle.isNotBlank()) {
            y += paints.subtitle.textSize + 4f
            drawText(subtitle, MARGIN, y, paints.subtitle)
        }

        if (!continued && document.metadata.isNotEmpty()) {
            y += LINE_GAP
            // Two columns of label/value pairs, so a long schedule line still fits.
            val columnWidth = CONTENT_WIDTH / METADATA_COLUMNS
            document.metadata.forEachIndexed { index, (label, value) ->
                val column = index % METADATA_COLUMNS
                val rowIndex = index / METADATA_COLUMNS
                val x = MARGIN + column * columnWidth
                val lineY = y + (rowIndex + 1) * METADATA_LINE_HEIGHT
                drawText("$label:", x, lineY, paints.metaLabel)
                drawText(
                    value.ellipsized(paints.metaValue, columnWidth - METADATA_VALUE_INSET - 4f),
                    x + METADATA_VALUE_INSET,
                    lineY,
                    paints.metaValue
                )
            }
            val rows = (document.metadata.size + METADATA_COLUMNS - 1) / METADATA_COLUMNS
            y += rows * METADATA_LINE_HEIGHT
        }

        y += LINE_GAP
        drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paints.rule)
        return y + LINE_GAP
    }

    // ---- table --------------------------------------------------------------

    private fun ExportDocument.columnWidths(): List<Float> {
        val total = columns.sumOf { it.weight.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
        return columns.map { CONTENT_WIDTH * it.weight / total }
    }

    private fun Canvas.drawColumnTitles(
        document: ExportDocument,
        widths: List<Float>,
        top: Float,
        paints: TablePaints
    ): Float {
        drawRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + HEADER_HEIGHT, paints.headerFill)
        var x = MARGIN
        document.columns.forEachIndexed { index, column ->
            val width = widths[index]
            drawCell(
                text = column.title,
                left = x,
                width = width,
                baseline = top + HEADER_HEIGHT - CELL_PADDING_VERTICAL,
                alignment = column.alignment,
                paint = paints.columnTitle
            )
            x += width
        }
        return top + HEADER_HEIGHT
    }

    /** A row is as tall as a line of text, or as the signature image it carries. */
    private fun ExportRow.height(paints: TablePaints): Float =
        if (signature is AttendanceSignature.Drawn) SIGNATURE_ROW_HEIGHT else ROW_HEIGHT

    private fun Canvas.drawRow(
        row: ExportRow,
        document: ExportDocument,
        widths: List<Float>,
        top: Float,
        height: Float,
        striped: Boolean,
        paints: TablePaints
    ) {
        if (striped) {
            drawRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + height, paints.stripeFill)
        }

        var x = MARGIN
        document.columns.forEachIndexed { index, column ->
            val width = widths[index]
            val isSignature = index == document.columns.lastIndex && document.hasSignatureColumn()
            if (isSignature) {
                drawSignature(
                    signature = row.signature,
                    left = x,
                    width = width,
                    top = top,
                    height = height,
                    paints = paints
                )
            } else {
                drawCell(
                    text = row.cells.getOrElse(index) { "" }
                        .ellipsized(paints.cell, width - 2 * CELL_PADDING_HORIZONTAL),
                    left = x,
                    width = width,
                    baseline = top + height / 2f + paints.cell.textSize / 3f,
                    alignment = column.alignment,
                    paint = paints.cell
                )
            }
            x += width
        }
        drawLine(MARGIN, top + height, PAGE_WIDTH - MARGIN, top + height, paints.rowRule)
    }

    private fun ExportDocument.hasSignatureColumn(): Boolean =
        columns.lastOrNull()?.title.equals(SIGNATURE_COLUMN_TITLE, ignoreCase = true)

    private fun Canvas.drawSignature(
        signature: AttendanceSignature,
        left: Float,
        width: Float,
        top: Float,
        height: Float,
        paints: TablePaints
    ) {
        when (signature) {
            is AttendanceSignature.Drawn -> {
                val bitmap = signature.imageBase64.decodeSignature()
                if (bitmap == null) {
                    drawCell(
                        text = signature.code,
                        left = left,
                        width = width,
                        baseline = top + height / 2f + paints.code.textSize / 3f,
                        alignment = ColumnAlignment.CENTER,
                        paint = paints.code
                    )
                    return
                }
                // Fitted inside the cell so a wide signature never bleeds into
                // the column next to it.
                val available = RectF(
                    left + CELL_PADDING_HORIZONTAL,
                    top + CELL_PADDING_VERTICAL,
                    left + width - CELL_PADDING_HORIZONTAL,
                    top + height - SIGNATURE_CODE_HEIGHT
                )
                val scale = minOf(
                    available.width() / bitmap.width,
                    available.height() / bitmap.height
                )
                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale
                val destination = RectF(
                    available.centerX() - drawWidth / 2f,
                    available.centerY() - drawHeight / 2f,
                    available.centerX() + drawWidth / 2f,
                    available.centerY() + drawHeight / 2f
                )
                drawBitmap(bitmap, null, destination, paints.image)
                drawCell(
                    text = signature.code,
                    left = left,
                    width = width,
                    baseline = top + height - CELL_PADDING_VERTICAL,
                    alignment = ColumnAlignment.CENTER,
                    paint = paints.code
                )
            }

            is AttendanceSignature.Code -> drawCell(
                text = signature.value,
                left = left,
                width = width,
                baseline = top + height / 2f + paints.code.textSize / 3f,
                alignment = ColumnAlignment.CENTER,
                paint = paints.code
            )

            AttendanceSignature.None -> drawCell(
                text = NO_SIGNATURE,
                left = left,
                width = width,
                baseline = top + height / 2f + paints.cell.textSize / 3f,
                alignment = ColumnAlignment.CENTER,
                paint = paints.muted
            )
        }
    }

    // ---- footer -------------------------------------------------------------

    private fun Canvas.drawEmptyNotice(
        document: ExportDocument,
        top: Float,
        paints: TablePaints
    ): Float {
        if (!document.isEmpty) return top
        val y = top + ROW_HEIGHT
        // drawCell sets the alignment of whatever paint it is handed, and this
        // one is also used for empty signature cells, so it is put back here
        // rather than trusted to still be left aligned.
        paints.muted.textAlign = Paint.Align.LEFT
        drawText(EMPTY_NOTICE, MARGIN, y, paints.muted)
        return y
    }

    private fun Canvas.drawNotes(document: ExportDocument, top: Float, paints: TablePaints) {
        if (document.notes.isEmpty()) return
        var y = top + LINE_GAP * 2
        document.notes.forEach { note ->
            if (y > CONTENT_BOTTOM + LINE_GAP) return
            y += NOTE_LINE_HEIGHT
            drawText("• ${note.ellipsized(paints.note, CONTENT_WIDTH - 12f)}", MARGIN, y, paints.note)
        }
    }

    private fun Canvas.drawPageNumber(pageNumber: Int, paints: TablePaints) {
        drawText(
            "Page $pageNumber",
            PAGE_WIDTH - MARGIN,
            PAGE_HEIGHT - MARGIN / 2f,
            paints.pageNumber
        )
    }

    // ---- drawing helpers ----------------------------------------------------

    private fun Canvas.drawCell(
        text: String,
        left: Float,
        width: Float,
        baseline: Float,
        alignment: ColumnAlignment,
        paint: Paint
    ) {
        val x = when (alignment) {
            ColumnAlignment.START -> left + CELL_PADDING_HORIZONTAL
            ColumnAlignment.CENTER -> left + width / 2f
            ColumnAlignment.END -> left + width - CELL_PADDING_HORIZONTAL
        }
        paint.textAlign = when (alignment) {
            ColumnAlignment.START -> Paint.Align.LEFT
            ColumnAlignment.CENTER -> Paint.Align.CENTER
            ColumnAlignment.END -> Paint.Align.RIGHT
        }
        drawText(text, x, baseline, paint)
    }

    /** Trims with an ellipsis so a long name never overruns its column. */
    private fun String.ellipsized(paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f || paint.measureText(this) <= maxWidth) return this
        var end = length
        while (end > 1 && paint.measureText(substring(0, end) + ELLIPSIS) > maxWidth) end--
        return substring(0, end) + ELLIPSIS
    }

    private fun String.decodeSignature(): Bitmap? = try {
        val bytes = Base64.decode(substringAfter(delimiter = "base64,", missingDelimiterValue = this), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: IllegalArgumentException) {
        null
    }

    // ---- geometry, in PostScript points (72 per inch), A4 at 72 dpi ----------

    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842

    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val CONTENT_BOTTOM = PAGE_HEIGHT - 90f

    private const val LINE_GAP = 10f
    private const val HEADER_HEIGHT = 24f
    private const val ROW_HEIGHT = 22f
    private const val SIGNATURE_ROW_HEIGHT = 52f
    private const val SIGNATURE_CODE_HEIGHT = 12f
    private const val METADATA_LINE_HEIGHT = 14f
    private const val METADATA_VALUE_INSET = 62f
    private const val METADATA_COLUMNS = 2
    private const val NOTE_LINE_HEIGHT = 12f
    private const val CELL_PADDING_HORIZONTAL = 6f
    private const val CELL_PADDING_VERTICAL = 7f

    private const val SIGNATURE_COLUMN_TITLE = "Signature"
    private const val NO_SIGNATURE = "--"
    private const val ELLIPSIS = "…"
    private const val EMPTY_NOTICE = "This event has not run yet, so there is nothing to report."
}

/** Every paint the page needs, built once instead of per row. */
private class TablePaints {
    private val sans: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val sansBold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    private val mono: Typeface = Typeface.create("monospace", Typeface.NORMAL)

    val title = text(size = 20f, typeface = sansBold, color = INK)
    val subtitle = text(size = 11f, typeface = sans, color = MUTED)
    val metaLabel = text(size = 9f, typeface = sansBold, color = MUTED)
    val metaValue = text(size = 9f, typeface = sans, color = INK)
    val columnTitle = text(size = 9f, typeface = sansBold, color = INK)
    val cell = text(size = 9.5f, typeface = sans, color = INK)
    val code = text(size = 8f, typeface = mono, color = ACCENT_INK)
    val muted = text(size = 9f, typeface = sans, color = MUTED)
    val note = text(size = 8f, typeface = sans, color = MUTED)
    val pageNumber = text(size = 8f, typeface = sans, color = MUTED).apply {
        textAlign = Paint.Align.RIGHT
    }

    val headerFill = fill(ACCENT_TINT)
    val stripeFill = fill(STRIPE)
    val rule = stroke(color = INK, width = 1f)
    val rowRule = stroke(color = HAIRLINE, width = 0.5f)
    val image = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    private fun text(size: Float, typeface: Typeface, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = size
        this.typeface = typeface
        this.color = color
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = width
        style = Paint.Style.STROKE
    }

    private companion object {
        val INK = Color.parseColor("#111111")
        val MUTED = Color.parseColor("#6E6E6E")
        val HAIRLINE = Color.parseColor("#DDDDDD")
        val STRIPE = Color.parseColor("#F7F7F7")
        val ACCENT_TINT = Color.parseColor("#D6F7EF")
        val ACCENT_INK = Color.parseColor("#00695C")
    }
}

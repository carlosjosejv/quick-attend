/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.export

import com.greenshark.quickattend.data.AttendanceSignature
import java.text.Normalizer

/** The two shapes an attendance record can be exported in. */
enum class ExportFormat(val extension: String, val mimeType: String) {
    PDF(
        extension = "pdf",
        mimeType = "application/pdf"
    ),
    EXCEL(
        extension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
}

/** How a column's cells line up, which is all the writers need to know. */
enum class ColumnAlignment {
    START,
    CENTER,
    END
}

data class ExportColumn(
    val title: String,
    /** Share of the page width this column takes, relative to the other columns. */
    val weight: Float = 1f,
    val alignment: ColumnAlignment = ColumnAlignment.START
)

/**
 * One row of an exported record. [signature] is drawn as an image in the PDF
 * when the attendant has one, and always written as its code in the
 * spreadsheet, because a cell cannot hold a picture the way a page can.
 */
data class ExportRow(
    val cells: List<String>,
    val signature: AttendanceSignature = AttendanceSignature.None
)

/**
 * A finished export, independent of the format it ends up in. Both writers read
 * this and nothing else, so a PDF and a spreadsheet of the same record always
 * carry the same numbers.
 */
data class ExportDocument(
    val title: String,
    val subtitle: String = "",
    /** Label/value pairs printed above the table, e.g. "Host" to "Carlos". */
    val metadata: List<Pair<String, String>> = emptyList(),
    val columns: List<ExportColumn> = emptyList(),
    val rows: List<ExportRow> = emptyList(),
    /** Short lines under the table, e.g. what the score is made of. */
    val notes: List<String> = emptyList(),
    /** Base name of the written file, without an extension. */
    val fileName: String = "attendance"
) {
    val isEmpty: Boolean get() = rows.isEmpty()

    /** True when at least one row signs with a drawn image rather than a code. */
    val hasDrawnSignatures: Boolean
        get() = rows.any { it.signature is AttendanceSignature.Drawn }

    fun fileNameFor(format: ExportFormat): String = "$fileName.${format.extension}"
}

/**
 * Strips whatever a file name cannot hold, so a title can be used as one.
 *
 * Accents are folded rather than dropped: this app's events and attendants are
 * largely Spanish, and turning "Salón C201" into "sal-n-c-201" would make a
 * shared file harder to recognise than "salon-c-201".
 */
fun String.toFileNameSlug(fallback: String = "attendance"): String {
    val folded = Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(regex = COMBINING_MARKS, replacement = "")
    val slug = folded
        .lowercase()
        .replace(regex = NON_SLUG_CHARACTERS, replacement = "-")
        .trim('-')
    return slug.ifBlank { fallback }.take(n = MAX_FILE_NAME_LENGTH)
}

/** The accent marks NFD splits off, which is what folding removes. */
private val COMBINING_MARKS = Regex("\\p{Mn}+")

private val NON_SLUG_CHARACTERS = Regex("[^a-z0-9]+")

private const val MAX_FILE_NAME_LENGTH = 60

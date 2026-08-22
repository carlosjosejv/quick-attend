/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.export

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceSignature
import com.greenshark.quickattend.export.ColumnAlignment
import com.greenshark.quickattend.export.ExportColumn
import com.greenshark.quickattend.export.ExportDocument
import com.greenshark.quickattend.export.ExportRow
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * What the spreadsheet holds, laid out as the grid it opens as. A cell cannot
 * carry a drawing, so the signature column shows the code here exactly as the
 * written file does.
 */
@Composable
fun ExportTablePreview(
    document: ExportDocument,
    modifier: Modifier = Modifier,
    maxRows: Int = DEFAULT_MAX_ROWS
) {
    val shown = document.rows.take(n = maxRows)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .background(color = White),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier
                    .background(color = Turquoise.copy(alpha = 0.18f))
                    .padding(vertical = 8.dp)
            ) {
                document.columns.forEach { column ->
                    TableCell(
                        text = column.title,
                        width = column.previewWidth(),
                        alignment = column.alignment,
                        bold = true
                    )
                }
            }

            shown.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .background(color = if (index % 2 == 1) Ghost else White)
                        .padding(vertical = 8.dp)
                ) {
                    document.columns.forEachIndexed { columnIndex, column ->
                        val isSignature = columnIndex == document.columns.lastIndex &&
                                column.title.equals(SIGNATURE_TITLE, ignoreCase = true)
                        TableCell(
                            text = if (isSignature) {
                                row.signature.displayCode ?: NO_SIGNATURE
                            } else {
                                row.cells.getOrElse(columnIndex) { "" }
                            },
                            width = column.previewWidth(),
                            alignment = column.alignment,
                            monospace = isSignature
                        )
                    }
                }
            }
        }

        if (document.rows.size > shown.size) {
            Text(
                text = stringResource(R.string.export_rows, document.rows.size),
                color = Gray,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: Dp,
    alignment: ColumnAlignment,
    bold: Boolean = false,
    monospace: Boolean = false
) {
    Text(
        text = text.ifBlank { NO_SIGNATURE },
        color = if (monospace) Turquoise else Black,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = if (monospace) FontFamily.Monospace else null,
        fontSize = if (monospace) 11.sp else 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = when (alignment) {
            ColumnAlignment.START -> TextAlign.Start
            ColumnAlignment.CENTER -> TextAlign.Center
            ColumnAlignment.END -> TextAlign.End
        },
        modifier = Modifier
            .width(width = width)
            .padding(horizontal = 8.dp)
    )
}

/** The weight the writers use, turned into something a phone can scroll. */
private fun ExportColumn.previewWidth(): Dp =
    (weight * WIDTH_PER_WEIGHT).dp.coerceIn(MIN_WIDTH, MAX_WIDTH)

private const val DEFAULT_MAX_ROWS = 20
private const val WIDTH_PER_WEIGHT = 52f
private val MIN_WIDTH = 64.dp
private val MAX_WIDTH = 180.dp
private const val SIGNATURE_TITLE = "Signature"
private const val NO_SIGNATURE = "--"

@Preview(showBackground = true, widthDp = 380)
@Composable
fun ExportTablePreviewPreview() {
    QuickAttendTheme {
        ExportTablePreview(
            document = ExportDocument(
                title = "Clase de Yoga",
                columns = listOf(
                    ExportColumn(title = "Attendant", weight = 2.4f),
                    ExportColumn(title = "On time", weight = 0.9f, alignment = ColumnAlignment.CENTER),
                    ExportColumn(title = "Score", weight = 0.9f, alignment = ColumnAlignment.CENTER),
                    ExportColumn(title = "Signature", weight = 2.2f, alignment = ColumnAlignment.CENTER)
                ),
                rows = listOf(
                    ExportRow(
                        cells = listOf("Carlos Jiménez", "12", "96", ""),
                        signature = AttendanceSignature.Code("QA-7F3A-91C2-88DE")
                    ),
                    ExportRow(
                        cells = listOf("Ana Reyes", "3", "31", ""),
                        signature = AttendanceSignature.None
                    )
                )
            ),
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}

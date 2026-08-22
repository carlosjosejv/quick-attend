/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.export

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.export.ExportFormat
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/** Picks the file an export is written as. */
@Composable
fun ExportFormatSelector(
    selected: ExportFormat,
    onSelect: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        ExportFormat.entries.forEach { format ->
            FormatChip(
                format = format,
                selected = format == selected,
                enabled = enabled,
                onSelect = { onSelect(format) }
            )
        }
    }
}

@Composable
private fun RowScope.FormatChip(
    format: ExportFormat,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onSelect,
        enabled = enabled,
        modifier = Modifier.weight(weight = 1f),
        label = { Text(text = stringResource(format.labelRes)) },
        leadingIcon = {
            Icon(
                painter = painterResource(format.iconRes),
                contentDescription = null,
                modifier = Modifier.size(size = 18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Turquoise,
            selectedLabelColor = White,
            selectedLeadingIconColor = White,
            labelColor = Black
        )
    )
}

private val ExportFormat.labelRes: Int
    get() = when (this) {
        ExportFormat.PDF -> R.string.export_format_pdf
        ExportFormat.EXCEL -> R.string.export_format_excel
    }

@get:DrawableRes
private val ExportFormat.iconRes: Int
    get() = when (this) {
        ExportFormat.PDF -> R.drawable.picture_as_pdf
        ExportFormat.EXCEL -> R.drawable.table_view
    }

@Preview(showBackground = true, widthDp = 380)
@Composable
fun ExportFormatSelectorPreview() {
    QuickAttendTheme {
        ExportFormatSelector(
            selected = ExportFormat.PDF,
            onSelect = {},
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}

/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.host

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendeeSort
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/** Chips that reorder the attendee list. */
@Composable
fun AttendeeSortRow(
    selected: AttendeeSort,
    onSelect: (AttendeeSort) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        AttendeeSort.entries.forEach { sort ->
            FilterChip(
                selected = sort == selected,
                onClick = { onSelect(sort) },
                label = { Text(text = stringResource(sort.labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Turquoise,
                    selectedLabelColor = White,
                    labelColor = Black
                )
            )
        }
    }
}

private val AttendeeSort.labelRes: Int
    get() = when (this) {
        AttendeeSort.NAME -> R.string.sort_by_name
        AttendeeSort.SCORE -> R.string.sort_by_score
        AttendeeSort.ABSENCES -> R.string.sort_by_absences
    }

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendeeSortRowPreview() {
    QuickAttendTheme {
        AttendeeSortRow(
            selected = AttendeeSort.SCORE,
            onSelect = {},
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}

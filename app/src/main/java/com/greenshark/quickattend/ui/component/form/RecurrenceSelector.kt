/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/** Daily / Weekly / Monthly picker for how the event repeats. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceSelector(
    selected: Recurrence,
    onSelected: (Recurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = Recurrence.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, recurrence ->
            SegmentedButton(
                selected = recurrence == selected,
                onClick = { onSelected(recurrence) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Turquoise,
                    activeContentColor = White,
                    activeBorderColor = Turquoise,
                    inactiveContentColor = Black
                )
            ) {
                Text(text = stringResource(recurrence.labelRes()))
            }
        }
    }
}

private fun Recurrence.labelRes(): Int = when (this) {
    Recurrence.DAILY -> R.string.recurrence_daily
    Recurrence.WEEKLY -> R.string.recurrence_weekly
    Recurrence.MONTHLY -> R.string.recurrence_monthly
}

@Preview(showBackground = true)
@Composable
fun RecurrenceSelectorPreview() {
    QuickAttendTheme {
        RecurrenceSelector(
            selected = Recurrence.WEEKLY,
            onSelected = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

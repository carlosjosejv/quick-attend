/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.TimeRange
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise

private enum class EditingField { NONE, START, END }

/** One selected day and the 24h range the event runs on it. */
@Composable
fun DayTimeRangeRow(
    dayLabel: String,
    range: TimeRange,
    onRangeChange: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(EditingField.NONE) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Text(
                text = dayLabel,
                color = Black,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.width(width = 72.dp)
            )

            TimeButton(
                minutes = range.startMinutes,
                onClick = { editing = EditingField.START },
                modifier = Modifier.weight(weight = 1f)
            )

            Text(text = "-", color = Gray, fontSize = 16.sp)

            TimeButton(
                minutes = range.endMinutes,
                onClick = { editing = EditingField.END },
                modifier = Modifier.weight(weight = 1f)
            )
        }

        if (!range.isValid) {
            Text(
                text = stringResource(R.string.error_end_before_start),
                color = Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 80.dp)
            )
        }
    }

    when (editing) {
        EditingField.START -> TimeInputDialog(
            title = stringResource(R.string.start_time),
            initialMinutes = range.startMinutes,
            onConfirm = { minutes ->
                onRangeChange(range.copy(startMinutes = minutes))
                editing = EditingField.NONE
            },
            onDismiss = { editing = EditingField.NONE }
        )

        EditingField.END -> TimeInputDialog(
            title = stringResource(R.string.end_time),
            initialMinutes = range.endMinutes,
            onConfirm = { minutes ->
                onRangeChange(range.copy(endMinutes = minutes))
                editing = EditingField.NONE
            },
            onDismiss = { editing = EditingField.NONE }
        )

        EditingField.NONE -> Unit
    }
}

@Composable
private fun TimeButton(minutes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(size = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Turquoise)
    ) {
        Text(
            text = TimeRange.formatMinutes(minutes),
            color = Black,
            fontSize = 16.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DayTimeRangeRowPreview() {
    QuickAttendTheme {
        DayTimeRangeRow(
            dayLabel = "Mon",
            range = TimeRange(startMinutes = 13 * 60, endMinutes = 17 * 60),
            onRangeChange = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DayTimeRangeRowInvalidPreview() {
    QuickAttendTheme {
        DayTimeRangeRow(
            dayLabel = "Wed",
            range = TimeRange(startMinutes = 17 * 60, endMinutes = 9 * 60),
            onRangeChange = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

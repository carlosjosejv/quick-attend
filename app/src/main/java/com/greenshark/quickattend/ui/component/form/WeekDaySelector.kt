/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * Weekday chips for a weekly schedule. Day keys are [DayOfWeek.getValue],
 * 1 = Monday .. 7 = Sunday.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekDaySelector(
    selectedDays: Set<Int>,
    onToggleDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val days = remember(locale) {
        DayOfWeek.entries.map { it.value to it.getDisplayName(TextStyle.SHORT, locale) }
    }

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        days.forEach { (dayKey, label) ->
            FilterChip(
                selected = dayKey in selectedDays,
                onClick = { onToggleDay(dayKey) },
                label = { Text(text = label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Turquoise,
                    selectedLabelColor = White,
                    labelColor = Black
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeekDaySelectorPreview() {
    QuickAttendTheme {
        WeekDaySelector(
            selectedDays = setOf(1, 3, 5),
            onToggleDay = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.TimeRange
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * Full schedule editor: recurrence, which days, and the hour range of each day.
 * A daily schedule has a single implicit day, so it skips day selection.
 */
@Composable
fun ScheduleEditor(
    schedule: EventSchedule,
    onRecurrenceChange: (Recurrence) -> Unit,
    onToggleDay: (dayKey: Int) -> Unit,
    onRangeChange: (dayKey: Int, range: TimeRange) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null
) {
    val locale = LocalConfiguration.current.locales[0]

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        RecurrenceSelector(selected = schedule.recurrence, onSelected = onRecurrenceChange)

        val selectedKeys = schedule.days.map { it.dayKey }.toSet()
        when (schedule.recurrence) {
            Recurrence.DAILY -> Text(
                text = stringResource(R.string.schedule_daily_hint),
                color = Gray,
                fontSize = 14.sp
            )

            Recurrence.WEEKLY -> WeekDaySelector(
                selectedDays = selectedKeys,
                onToggleDay = onToggleDay
            )

            Recurrence.MONTHLY -> MonthDaySelector(
                selectedDays = selectedKeys,
                onToggleDay = onToggleDay
            )
        }

        if (schedule.days.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.hours_per_day),
                color = Gray,
                fontSize = 14.sp
            )
            schedule.orderedDays.forEach { day ->
                DayTimeRangeRow(
                    dayLabel = day.label(schedule.recurrence, locale),
                    range = day.range,
                    onRangeChange = { onRangeChange(day.dayKey, it) }
                )
            }
        }

        if (errorText != null) {
            Text(text = errorText, color = Red, fontSize = 12.sp)
        }
    }
}

private fun ScheduleDay.label(recurrence: Recurrence, locale: java.util.Locale): String =
    when (recurrence) {
        Recurrence.DAILY -> "All days"
        Recurrence.WEEKLY -> DayOfWeek.of(dayKey).getDisplayName(TextStyle.SHORT, locale)
        Recurrence.MONTHLY -> "Day $dayKey"
    }

@Preview(showBackground = true)
@Composable
fun ScheduleEditorPreview() {
    QuickAttendTheme {
        ScheduleEditor(
            schedule = EventSchedule(
                recurrence = Recurrence.WEEKLY,
                days = listOf(
                    ScheduleDay(1, TimeRange(13 * 60, 17 * 60)),
                    ScheduleDay(3, TimeRange(9 * 60, 11 * 60))
                )
            ),
            onRecurrenceChange = {},
            onToggleDay = {},
            onRangeChange = { _, _ -> },
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

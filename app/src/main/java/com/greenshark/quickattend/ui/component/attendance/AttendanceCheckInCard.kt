/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.data.ON_TIME_THRESHOLD_MINUTES
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.White
import java.time.format.DateTimeFormatter

/**
 * The record for one session: the exact time attendance was taken, how that
 * compares to the scheduled start, and the timeline that puts it in context.
 */
@Composable
fun AttendanceCheckInCard(
    record: AttendanceRecord,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = remember(record.date, locale) {
        record.date.format(DateTimeFormatter.ofPattern(DATE_PATTERN, locale))
    }
    val absent = record.status == AttendanceStatus.ABSENT

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Text(
                    text = dateLabel,
                    color = Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f)
                )
                AttendanceStatusChip(status = record.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (absent) R.drawable.timer_off else R.drawable.timer
                    ),
                    contentDescription = null,
                    tint = record.status.color,
                    modifier = Modifier.size(size = 40.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(space = 2.dp)) {
                    Text(
                        text = stringResource(
                            if (absent) R.string.not_checked_in else R.string.checked_in_at
                        ),
                        color = Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = record.checkInLabel ?: NO_TIME,
                        color = if (absent) Gray else Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = record.deviationLabel(),
                color = record.status.color,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )

            AttendanceTimeline(record = record)
        }
    }
}

/** One line saying how far the check-in was from the scheduled start. */
@Composable
private fun AttendanceRecord.deviationLabel(): String {
    val minutes = minutesFromStart ?: return stringResource(R.string.session_missed)
    return when {
        minutes < 0 -> stringResource(R.string.minutes_early, -minutes)
        minutes <= ON_TIME_THRESHOLD_MINUTES ->
            stringResource(R.string.within_grace_period, ON_TIME_THRESHOLD_MINUTES)

        else -> stringResource(R.string.minutes_late, minutes)
    }
}

private const val DATE_PATTERN = "EEEE, d MMM yyyy"

private const val NO_TIME = "--:--"

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendanceCheckInCardOnTimePreview() {
    QuickAttendTheme {
        AttendanceCheckInCard(record = previewRecord(minutesFromStart = 2L))
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendanceCheckInCardLatePreview() {
    QuickAttendTheme {
        AttendanceCheckInCard(record = previewRecord(minutesFromStart = 17L))
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendanceCheckInCardAbsentPreview() {
    QuickAttendTheme {
        AttendanceCheckInCard(record = previewRecord(minutesFromStart = null))
    }
}

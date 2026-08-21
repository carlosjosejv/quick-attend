/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import java.time.format.DateTimeFormatter

/**
 * One past session in the history list: when it was, the exact time attendance
 * was recorded, and how that was graded. Tapping it features that session at the
 * top of the screen.
 */
@Composable
fun AttendanceRecordRow(
    record: AttendanceRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = remember(record.date, locale) {
        record.date.format(DateTimeFormatter.ofPattern(ROW_DATE_PATTERN, locale))
    }
    val absent = record.status == AttendanceStatus.ABSENT

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Ghost),
        border = if (selected) BorderStroke(width = 2.dp, color = Turquoise) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 2.dp)
            ) {
                Text(
                    text = dateLabel,
                    color = Black,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = if (absent) {
                        stringResource(R.string.not_checked_in)
                    } else {
                        stringResource(R.string.scheduled_at, record.scheduledStartLabel)
                    },
                    color = Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Text(
                text = record.checkInLabel ?: EMPTY_TIME,
                color = if (absent) Gray else Black,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1
            )

            AttendanceStatusChip(status = record.status, compact = true)
        }
    }
}

private const val ROW_DATE_PATTERN = "EEE, d MMM"

private const val EMPTY_TIME = "--:--"

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendanceRecordRowPreview() {
    QuickAttendTheme {
        Column(
            modifier = Modifier.padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            AttendanceRecordRow(
                record = previewRecord(minutesFromStart = 2L),
                onClick = {},
                selected = true
            )
            AttendanceRecordRow(record = previewRecord(minutesFromStart = 19L), onClick = {})
            AttendanceRecordRow(record = previewRecord(minutesFromStart = null), onClick = {})
        }
    }
}

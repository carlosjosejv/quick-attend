/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.attendance

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.ui.theme.Amber
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise

/**
 * The one colour every attendance surface uses for a status, so the chip, the
 * timeline, the ring and the chart always agree on what green and red mean.
 */
internal val AttendanceStatus.color: Color
    get() = when (this) {
        AttendanceStatus.ON_TIME -> Turquoise
        AttendanceStatus.LATE -> Amber
        AttendanceStatus.ABSENT -> Red
    }

@get:StringRes
internal val AttendanceStatus.labelRes: Int
    get() = when (this) {
        AttendanceStatus.ON_TIME -> R.string.attendance_status_on_time
        AttendanceStatus.LATE -> R.string.attendance_status_late
        AttendanceStatus.ABSENT -> R.string.attendance_status_absent
    }

/** Colour coded pill naming an attendance status. */
@Composable
fun AttendanceStatusChip(
    status: AttendanceStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier
            .background(
                color = status.color.copy(alpha = 0.18f),
                shape = RoundedCornerShape(size = 32.dp)
            )
            .padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 2.dp else 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size = if (compact) 8.dp else 10.dp)
                .background(color = status.color, shape = CircleShape)
        )
        Text(
            text = stringResource(status.labelRes),
            color = Black,
            fontWeight = FontWeight.Medium,
            fontSize = if (compact) 12.sp else 14.sp,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, widthDp = 260)
@Composable
fun AttendanceStatusChipPreview() {
    QuickAttendTheme {
        Row(
            modifier = Modifier.padding(all = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            AttendanceStatus.entries.forEach { status ->
                AttendanceStatusChip(status = status)
            }
        }
    }
}

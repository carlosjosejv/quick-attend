/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.host

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.greenshark.quickattend.data.AttendanceSummary
import com.greenshark.quickattend.data.EventAttendanceOverview
import com.greenshark.quickattend.ui.component.attendance.AttendanceBreakdown
import com.greenshark.quickattend.ui.component.attendance.scoreColor
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

/**
 * How the event is doing as a whole: how many people are on it, how many times
 * it has run, and the score every record pooled together comes to.
 */
@Composable
fun EventOverviewCard(
    overview: EventAttendanceOverview,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            OverviewTile(
                label = stringResource(R.string.overview_attendants),
                value = overview.attendees.toString()
            )
            OverviewTile(
                label = stringResource(R.string.overview_sessions),
                value = overview.sessions.toString()
            )
            OverviewTile(
                label = stringResource(R.string.overview_average),
                value = overview.score.toString(),
                valueColor = overview.summary.scoreColor()
            )
        }
        AttendanceBreakdown(summary = overview.summary)
    }
}

@Composable
private fun RowScope.OverviewTile(label: String, value: String, valueColor: Color = Black) {
    Column(
        modifier = Modifier
            .weight(weight = 1f)
            .background(color = Ghost, shape = RoundedCornerShape(size = 16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 2.dp)
    ) {
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            maxLines = 1
        )
        Text(text = label, color = Gray, fontSize = 13.sp, maxLines = 1)
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun EventOverviewCardPreview() {
    QuickAttendTheme {
        EventOverviewCard(
            overview = EventAttendanceOverview(
                attendees = 24,
                sessions = 12,
                summary = AttendanceSummary(onTime = 210, late = 44, absent = 34)
            ),
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}

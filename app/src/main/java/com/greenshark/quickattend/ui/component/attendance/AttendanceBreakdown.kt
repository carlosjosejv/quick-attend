/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.data.AttendanceSummary
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

/** How many sessions ended on time, late or missed, one tile per status. */
@Composable
fun AttendanceBreakdown(
    summary: AttendanceSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        AttendanceStatus.entries.forEach { status ->
            BreakdownTile(status = status, count = summary.countOf(status))
        }
    }
}

@Composable
private fun RowScope.BreakdownTile(status: AttendanceStatus, count: Int) {
    Column(
        modifier = Modifier
            .weight(weight = 1f)
            .background(color = Ghost, shape = RoundedCornerShape(size = 16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Text(
            text = count.toString(),
            color = Black,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            maxLines = 1
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(size = 8.dp)
                    .background(color = status.color, shape = CircleShape)
            )
            Text(
                text = stringResource(status.labelRes),
                color = Gray,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendanceBreakdownPreview() {
    QuickAttendTheme {
        AttendanceBreakdown(
            summary = AttendanceSummary(onTime = 12, late = 3, absent = 1),
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}

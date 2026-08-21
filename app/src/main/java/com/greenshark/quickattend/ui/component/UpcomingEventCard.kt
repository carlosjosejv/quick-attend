/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventOccurrence
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.EventStatus
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.TimeRange
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.Green
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Compact summary of a single run of an event. Sized by the caller so the same
 * card works inside the home carousel and in a full width list.
 */
@Composable
fun UpcomingEventCard(
    occurrence: EventOccurrence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayName = remember(occurrence.date, locale) {
        occurrence.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    }

    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = Turquoise.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = dayName, color = Black, fontSize = 12.sp, maxLines = 1)
                Text(
                    text = occurrence.date.dayOfMonth.toString(),
                    color = Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 2.dp)
            ) {
                Text(
                    text = occurrence.event.title,
                    color = Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.schedule),
                        contentDescription = null,
                        tint = Gray,
                        modifier = Modifier.size(size = 16.dp)
                    )
                    Text(
                        text = occurrence.range.format(),
                        color = Gray,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.location_on),
                        contentDescription = null,
                        tint = Gray,
                        modifier = Modifier.size(size = 16.dp)
                    )
                    Text(
                        text = occurrence.event.location,
                        color = Gray,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (occurrence.status == EventStatus.IN_PROGRESS) {
                val inProgress = stringResource(R.string.status_in_progress)
                Box(
                    modifier = Modifier
                        .size(size = 10.dp)
                        .background(color = Green, shape = CircleShape)
                        .semantics { contentDescription = inProgress }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 300)
@Composable
fun UpcomingEventCardPreview() {
    QuickAttendTheme {
        UpcomingEventCard(
            occurrence = EventOccurrence(
                event = Event(
                    id = "1",
                    title = "Clase de Yoga",
                    hostName = "Carlos Jiménez",
                    location = "Salon C201",
                    organizerId = "1",
                    organizerName = "Carlos Jiménez",
                    schedule = EventSchedule(
                        recurrence = Recurrence.WEEKLY,
                        days = listOf(ScheduleDay(1, TimeRange(13 * 60, 17 * 60)))
                    )
                ),
                date = LocalDate.of(2026, 8, 24),
                range = TimeRange(13 * 60, 17 * 60),
                status = EventStatus.IN_PROGRESS
            ),
            onClick = {},
            modifier = Modifier.width(width = 260.dp)
        )
    }
}

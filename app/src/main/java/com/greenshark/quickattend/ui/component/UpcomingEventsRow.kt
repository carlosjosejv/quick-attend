/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventOccurrence
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.TimeRange
import com.greenshark.quickattend.data.upcomingOccurrences
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import java.time.LocalDateTime

/** How many upcoming runs the home carousel shows before "view all" is needed. */
const val UPCOMING_EVENTS_PREVIEW_COUNT = 7

/**
 * Horizontal carousel of the next few event runs, headed by a "view all" link
 * to the full list.
 */
@Composable
fun UpcomingEventsRow(
    occurrences: List<EventOccurrence>,
    onEventClick: (EventOccurrence) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.my_upcoming_events),
                color = Black,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            TextButton(onClick = onViewAll) {
                Text(
                    text = stringResource(R.string.view_all),
                    color = Turquoise,
                    fontSize = 16.sp
                )
                Icon(
                    painter = painterResource(R.drawable.arrow_right_alt),
                    contentDescription = null,
                    tint = Turquoise,
                    modifier = Modifier.size(size = 20.dp)
                )
            }
        }

        if (occurrences.isEmpty()) {
            Text(
                text = stringResource(R.string.no_upcoming_events),
                color = Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
            ) {
                items(
                    items = occurrences,
                    key = { occurrence -> occurrence.key }
                ) { occurrence ->
                    UpcomingEventCard(
                        occurrence = occurrence,
                        onClick = { onEventClick(occurrence) },
                        modifier = Modifier.width(width = 260.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun UpcomingEventsRowPreview() {
    QuickAttendTheme {
        UpcomingEventsRow(
            occurrences = previewOccurrences(),
            onEventClick = {},
            onViewAll = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun UpcomingEventsRowEmptyPreview() {
    QuickAttendTheme {
        UpcomingEventsRow(occurrences = emptyList(), onEventClick = {}, onViewAll = {})
    }
}

/** Shared sample data for the previews in this file and the upcoming events screen. */
internal fun previewOccurrences(
    now: LocalDateTime = LocalDateTime.of(2026, 8, 24, 14, 30)
): List<EventOccurrence> = listOf(
    previewEvent(
        id = "1",
        title = "Clase de Yoga",
        location = "Salon C201",
        days = listOf(ScheduleDay(1, TimeRange(13 * 60, 17 * 60)))
    ),
    previewEvent(
        id = "2",
        title = "Taller de Compose",
        location = "Laboratorio 3",
        days = listOf(ScheduleDay(2, TimeRange(9 * 60, 11 * 60)))
    ),
    previewEvent(
        id = "3",
        title = "Reunión de equipo",
        location = "Sala de juntas",
        days = listOf(ScheduleDay(4, TimeRange(15 * 60, 16 * 60)))
    )
).upcomingOccurrences(now = now, withinDays = 14)

private fun previewEvent(
    id: String,
    title: String,
    location: String,
    days: List<ScheduleDay>
): Event = Event(
    id = id,
    title = title,
    hostName = "Carlos Jiménez",
    organizerId = "1",
    organizerName = "Carlos Jiménez",
    location = location,
    schedule = EventSchedule(recurrence = Recurrence.WEEKLY, days = days)
)

/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.host

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.EventStatus
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.TimeRange
import com.greenshark.quickattend.data.daysSummary
import com.greenshark.quickattend.data.timeSummary
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.Green
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * One event in the host's list: what it is, when it runs, and how many people
 * signed up. Unlike the attendant's card this shows the whole schedule rather
 * than a single date, because a host manages the series and not one session.
 */
@Composable
fun HostEventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    attendeeCount: Int? = null,
    status: EventStatus? = null
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Text(
                    text = event.title,
                    color = Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f)
                )
                if (status == EventStatus.IN_PROGRESS) {
                    val inProgress = stringResource(R.string.status_in_progress)
                    Box(
                        modifier = Modifier
                            .size(size = 10.dp)
                            .background(color = Green, shape = CircleShape)
                            .semantics { contentDescription = inProgress }
                    )
                }
            }

            DetailLine(icon = R.drawable.schedule, text = event.schedule.scheduleLine())
            if (event.location.isNotBlank()) {
                DetailLine(icon = R.drawable.location_on, text = event.location)
            }
            if (attendeeCount != null) {
                DetailLine(
                    icon = R.drawable.attend,
                    text = stringResource(R.string.attendee_count, attendeeCount),
                    tint = Turquoise
                )
            }
        }
    }
}

@Composable
private fun DetailLine(@DrawableRes icon: Int, text: String, tint: Color = Gray) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size = 16.dp)
        )
        Text(
            text = text,
            color = tint,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** "Mon, Wed · 13:00 - 17:00", the whole series on one line. */
internal fun EventSchedule.scheduleLine(): String = "${daysSummary()} · ${timeSummary()}"

@Preview(showBackground = true, widthDp = 380)
@Composable
fun HostEventCardPreview() {
    QuickAttendTheme {
        HostEventCard(
            event = previewHostEvent(),
            onClick = {},
            attendeeCount = 24,
            status = EventStatus.IN_PROGRESS,
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}

internal fun previewHostEvent(): Event = Event(
    id = "1",
    title = "Clase de Yoga",
    description = "Sesión semanal de yoga para principiantes.",
    hostName = "Carlos Jiménez",
    organizerId = "1",
    organizerName = "Carlos Jiménez",
    location = "Salon C201",
    address = "Av. Máximo Gómez 72, Santo Domingo",
    schedule = EventSchedule(
        recurrence = Recurrence.WEEKLY,
        days = listOf(
            ScheduleDay(dayKey = 1, range = TimeRange(13 * 60, 17 * 60)),
            ScheduleDay(dayKey = 3, range = TimeRange(13 * 60, 17 * 60))
        )
    )
)

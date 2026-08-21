/**
 * Created by Carlos Jiménez on 20/08/26.
 */
package com.greenshark.quickattend.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.TimeRange
import com.greenshark.quickattend.data.daysSummary
import com.greenshark.quickattend.data.timeSummary
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.White

@Composable
fun EventItem(event: Event, host: Boolean = false) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Turquoise, shape = RoundedCornerShape(16.dp))
                .clip(shape = RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .background(
                        color = White,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    )
                    .clip(shape = RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(all = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = event.title,
                            fontWeight = FontWeight.Bold,
                            color = Black,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Box(
                            modifier = Modifier.background(
                                color = Turquoise.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(size = 16.dp)
                            )
                        ) {
                            Text(
                                text = stringResource(if (host) R.string.host else R.string.attendant),
                                color = Black,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.calendar_today),
                            tint = Black,
                            contentDescription = null
                        )
                        Text(
                            text = "${stringResource(R.string.date)}, ${event.schedule.daysSummary()}",
                            color = Black,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.schedule),
                            tint = Black,
                            contentDescription = null
                        )
                        Text(
                            text = "${stringResource(R.string.time)}, ${event.schedule.timeSummary()}",
                            color = Black,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.location_on),
                            tint = Black,
                            contentDescription = null
                        )
                        Text(
                            text = "${stringResource(R.string.location)}, ${event.location}",
                            color = Black,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventItemPreview() {
    QuickAttendTheme() {
        EventItem(
            event = Event(
                id = "1",
                title = "Clase de Yoga",
                description = "",
                hostName = "Carlos Jiménez",
                location = "Salon C201",
                organizerId = "1",
                organizerName = "Carlos Jiménez",
                schedule = EventSchedule(
                    recurrence = Recurrence.WEEKLY,
                    days = listOf(ScheduleDay(1, TimeRange(13 * 60, 17 * 60)))
                )
            )
        )
    }
}
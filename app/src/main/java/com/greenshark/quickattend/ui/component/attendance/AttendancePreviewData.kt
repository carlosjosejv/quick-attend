/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.attendance

import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.TimeRange
import java.time.LocalDate

/** Sample data shared by every attendance preview in this package and the screen. */

/** A Monday, so the weekly fixture below lands on its own schedule. */
private val PREVIEW_DATE: LocalDate = LocalDate.of(2026, 8, 24)

private const val PREVIEW_START_HOUR = 13

/** [minutesFromStart] is null for an absence and negative for arriving early. */
internal fun previewRecord(
    minutesFromStart: Long? = 2L,
    date: LocalDate = PREVIEW_DATE
): AttendanceRecord {
    val start = date.atTime(PREVIEW_START_HOUR, 0)
    return AttendanceRecord(
        eventId = "1",
        eventTitle = "Clase de Yoga",
        date = date,
        scheduledStart = start,
        checkInAt = minutesFromStart?.let(start::plusMinutes)
    )
}

/** Eight weekly sessions with a bit of everything in them, oldest first. */
internal fun previewRecords(): List<AttendanceRecord> {
    val offsets = listOf(-4L, 1L, 12L, null, 3L, 0L, 27L, 2L)
    return offsets.mapIndexed { index, minutes ->
        previewRecord(
            minutesFromStart = minutes,
            date = PREVIEW_DATE.minusWeeks((offsets.size - 1 - index).toLong())
        )
    }
}

internal fun previewAttendanceEvent(): Event = Event(
    id = "1",
    title = "Clase de Yoga",
    description = "Sesión semanal de yoga para principiantes.",
    hostName = "Carlos Jiménez",
    organizerId = "1",
    organizerName = "Carlos Jiménez",
    location = "Salon C201",
    schedule = EventSchedule(
        recurrence = Recurrence.WEEKLY,
        days = listOf(ScheduleDay(dayKey = 1, range = TimeRange(PREVIEW_START_HOUR * 60, 17 * 60)))
    )
)

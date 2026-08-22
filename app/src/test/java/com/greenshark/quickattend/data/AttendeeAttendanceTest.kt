/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The host's roll is assembled rather than stored: only check-ins exist in the
 * backend, so every absence on it is worked out against the event's schedule.
 */
class AttendeeAttendanceTest {

    /** A Monday, so the weekly event below lands on its own schedule. */
    private val today = LocalDate.of(2026, 8, 24)

    private val now: LocalDateTime = today.atTime(18, 0)

    private val event = Event(
        id = "event-1",
        title = "Clase de Yoga",
        schedule = EventSchedule(
            recurrence = Recurrence.WEEKLY,
            days = listOf(ScheduleDay(dayKey = 1, range = TimeRange(13 * 60, 17 * 60)))
        )
    )

    private val ana = UserProfile(id = "ana", name = "Ana Reyes", email = "ana@example.com")
    private val luis = UserProfile(id = "luis", name = "Luis Pérez", email = "luis@example.com")

    @Test
    fun `someone who never came still appears, with every session absent`() {
        val attendees = event.attendeeAttendance(
            profiles = listOf(ana),
            checkIns = emptyList(),
            now = now,
            withinDays = 21
        )

        val ana = attendees.single()
        assertEquals(0, ana.summary.attended)
        assertTrue(ana.summary.absent > 0)
        assertEquals(null, ana.lastAttended)
    }

    @Test
    fun `check-ins are matched to the attendant who made them`() {
        val attendees = event.attendeeAttendance(
            profiles = listOf(ana, luis),
            checkIns = listOf(checkIn(userId = "ana", date = today, minutesLate = 1)),
            now = now,
            withinDays = 21
        )

        assertEquals(1, attendees.first { it.profile.id == "ana" }.summary.onTime)
        assertEquals(0, attendees.first { it.profile.id == "luis" }.summary.onTime)
    }

    @Test
    fun `a check-in from another event never counts`() {
        val attendees = event.attendeeAttendance(
            profiles = listOf(ana),
            checkIns = listOf(
                checkIn(userId = "ana", date = today, minutesLate = 1, eventId = "other-event")
            ),
            now = now,
            withinDays = 21
        )

        assertEquals(0, attendees.single().summary.attended)
    }

    @Test
    fun `the overview pools every attendant's record`() {
        val attendees = event.attendeeAttendance(
            profiles = listOf(ana, luis),
            checkIns = listOf(
                checkIn(userId = "ana", date = today, minutesLate = 1),
                checkIn(userId = "luis", date = today, minutesLate = 30)
            ),
            now = now,
            withinDays = 21
        )

        val overview = attendees.overview()
        assertEquals(2, overview.attendees)
        assertEquals(1, overview.summary.onTime)
        assertEquals(1, overview.summary.late)
        // Every attendant is scheduled for the same sessions, so the count of
        // sessions is one attendant's history, not the sum of them all.
        assertEquals(attendees.first().records.size, overview.sessions)
    }

    @Test
    fun `the roll is signed with the most recent session actually attended`() {
        val lastWeek = today.minusWeeks(1)
        val attendees = event.attendeeAttendance(
            profiles = listOf(ana),
            checkIns = listOf(checkIn(userId = "ana", date = lastWeek, minutesLate = 2)),
            now = now,
            withinDays = 21
        )

        assertEquals(lastWeek, attendees.single().lastAttended?.date)
    }

    @Test
    fun `search matches on name and on email, and ignores case`() {
        val attendees = listOf(attendance(ana), attendance(luis))

        assertEquals(1, attendees.filteredBy("ana").size)
        assertEquals(1, attendees.filteredBy("LUIS@EXAMPLE").size)
        assertEquals(2, attendees.filteredBy("  ").size)
        assertTrue(attendees.filteredBy("nobody").isEmpty())
    }

    @Test
    fun `sorting puts the worst attendance first when the host asks for it`() {
        val good = attendance(ana, AttendanceSummary(onTime = 10))
        val poor = attendance(luis, AttendanceSummary(onTime = 1, absent = 9))

        assertEquals(poor, listOf(good, poor).sortedBy(AttendeeSort.ABSENCES).first())
        assertEquals(good, listOf(poor, good).sortedBy(AttendeeSort.SCORE).first())
        assertEquals(ana, listOf(poor, good).sortedBy(AttendeeSort.NAME).first().profile)
    }

    @Test
    fun `a schedule is unchanged when the same days come back in another order`() {
        val monday = ScheduleDay(dayKey = 1, range = TimeRange(13 * 60, 17 * 60))
        val wednesday = ScheduleDay(dayKey = 3, range = TimeRange(13 * 60, 17 * 60))

        val original = EventSchedule(Recurrence.WEEKLY, listOf(monday, wednesday))
        val reordered = EventSchedule(Recurrence.WEEKLY, listOf(wednesday, monday))
        val moved = EventSchedule(
            recurrence = Recurrence.WEEKLY,
            days = listOf(monday, wednesday.copy(range = TimeRange(14 * 60, 17 * 60)))
        )

        assertTrue(original.sameHoursAs(reordered))
        assertFalse(original.sameHoursAs(moved))
    }

    private fun attendance(
        profile: UserProfile,
        summary: AttendanceSummary = AttendanceSummary()
    ) = AttendeeAttendance(profile = profile, summary = summary)

    private fun checkIn(
        userId: String,
        date: LocalDate,
        minutesLate: Long,
        eventId: String = "event-1"
    ) = AttendanceCheckIn(
        eventId = eventId,
        userId = userId,
        date = date,
        checkInAt = date.atTime(13, 0).plusMinutes(minutesLate)
    )
}

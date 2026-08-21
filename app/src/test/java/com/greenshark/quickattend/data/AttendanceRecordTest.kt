package com.greenshark.quickattend.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AttendanceRecordTest {

    /** 2026-08-24 is a Monday, which the weekly fixture below leans on. */
    private val monday: LocalDate = LocalDate.of(2026, 8, 24)

    private val yoga = Event(
        id = "yoga",
        title = "Clase de Yoga",
        organizerId = "organizer",
        location = "Salon C201",
        schedule = EventSchedule(
            recurrence = Recurrence.WEEKLY,
            days = listOf(ScheduleDay(dayKey = 1, range = TimeRange(13 * 60, 17 * 60)))
        )
    )

    @Test
    fun `checking in before the start is on time`() {
        assertEquals(AttendanceStatus.ON_TIME, record(minutes = -3).status)
    }

    @Test
    fun `the last minute of the grace period is still on time`() {
        val record = record(minutes = ON_TIME_THRESHOLD_MINUTES)
        assertEquals(AttendanceStatus.ON_TIME, record.status)
        assertEquals(5L, record.minutesLate)
    }

    @Test
    fun `a minute past the grace period is late`() {
        assertEquals(AttendanceStatus.LATE, record(minutes = ON_TIME_THRESHOLD_MINUTES + 1).status)
    }

    @Test
    fun `seconds inside the threshold minute do not tip a check-in into late`() {
        val start = monday.atTime(13, 0)
        val record = AttendanceRecord(
            eventId = yoga.id,
            date = monday,
            scheduledStart = start,
            checkInAt = start.plusMinutes(ON_TIME_THRESHOLD_MINUTES).plusSeconds(59)
        )
        assertEquals(AttendanceStatus.ON_TIME, record.status)
    }

    @Test
    fun `a session with no check-in is an absence`() {
        val record = record(minutes = null)
        assertEquals(AttendanceStatus.ABSENT, record.status)
        assertEquals(0L, record.minutesLate)
        assertEquals(null, record.checkInLabel)
    }

    @Test
    fun `labels use the 24h clock the rest of the app does`() {
        val record = record(minutes = 7)
        assertEquals("13:07", record.checkInLabel)
        assertEquals("13:00", record.scheduledStartLabel)
        assertEquals("13:05", record.onTimeUntilLabel)
    }

    @Test
    fun `late counts half of what being on time counts`() {
        assertEquals(100, AttendanceSummary(onTime = 4).score)
        assertEquals(50, AttendanceSummary(late = 4).score)
        assertEquals(0, AttendanceSummary(absent = 4).score)
        // 250 credits over 4 sessions is 62.5, and a half rounds up.
        assertEquals(63, AttendanceSummary(onTime = 2, late = 1, absent = 1).score)
    }

    @Test
    fun `an empty history scores full marks`() {
        val summary = AttendanceSummary()
        assertEquals(0, summary.total)
        assertEquals(100, summary.score)
        assertEquals(0f, summary.ratioOf(AttendanceStatus.ON_TIME), 0f)
    }

    @Test
    fun `history summarizes the records it is built from`() {
        val summary = listOf(
            record(minutes = 0),
            record(minutes = 2),
            record(minutes = 20),
            record(minutes = null)
        ).summarize()

        assertEquals(AttendanceSummary(onTime = 2, late = 1, absent = 1), summary)
        assertEquals(3, summary.attended)
        assertEquals(0.25f, summary.ratioOf(AttendanceStatus.ABSENT), 0f)
    }

    @Test
    fun `history covers every past session and turns the missing ones into absences`() {
        // Four Mondays have gone by, and only two of them were scanned.
        val now = monday.atTime(14, 0)
        val checkIns = listOf(
            checkIn(date = monday, minutes = 2),
            checkIn(date = monday.minusWeeks(2), minutes = 30)
        )

        val history = yoga.attendanceHistory(checkIns = checkIns, now = now, withinDays = 22)

        assertEquals(
            listOf(
                monday.minusWeeks(3),
                monday.minusWeeks(2),
                monday.minusWeeks(1),
                monday
            ),
            history.map { it.date }
        )
        assertEquals(
            listOf(
                AttendanceStatus.ABSENT,
                AttendanceStatus.LATE,
                AttendanceStatus.ABSENT,
                AttendanceStatus.ON_TIME
            ),
            history.map { it.status }
        )
        assertEquals(AttendanceSummary(onTime = 1, late = 1, absent = 2), history.summarize())
    }

    @Test
    fun `a session that has not started yet is not part of the history`() {
        // 12:00 on the day of the class, an hour before it starts.
        val history = yoga.attendanceHistory(
            checkIns = emptyList(),
            now = monday.atTime(12, 0),
            withinDays = 8
        )
        assertEquals(listOf(monday.minusWeeks(1)), history.map { it.date })
    }

    @Test
    fun `check-ins recorded for another event are ignored`() {
        val history = yoga.attendanceHistory(
            checkIns = listOf(checkIn(date = monday, minutes = 1, eventId = "pilates")),
            now = monday.atTime(14, 0),
            withinDays = 0
        )
        assertTrue(history.single().status == AttendanceStatus.ABSENT)
    }

    private fun record(minutes: Long?, date: LocalDate = monday): AttendanceRecord {
        val start = date.atTime(13, 0)
        return AttendanceRecord(
            eventId = yoga.id,
            eventTitle = yoga.title,
            date = date,
            scheduledStart = start,
            checkInAt = minutes?.let(start::plusMinutes)
        )
    }

    private fun checkIn(
        date: LocalDate,
        minutes: Long,
        eventId: String = yoga.id
    ): AttendanceCheckIn = AttendanceCheckIn(
        eventId = eventId,
        userId = "user",
        date = date,
        checkInAt = date.atTime(13, 0).plusMinutes(minutes)
    )
}

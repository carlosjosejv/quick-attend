package com.greenshark.quickattend.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CheckInWindowTest {

    /** 2026-08-24 is a Monday, which the weekly fixture leans on. */
    private val monday: LocalDate = LocalDate.of(2026, 8, 24)

    private val yoga = event(
        recurrence = Recurrence.WEEKLY,
        days = listOf(ScheduleDay(dayKey = 1, range = TimeRange(13 * 60, 17 * 60)))
    )

    @Test
    fun `a scan during the session lands on that session`() {
        val occurrence = yoga.checkInOccurrence(monday.atTime(14, 30))
        assertEquals(monday, occurrence?.date)
    }

    @Test
    fun `a scan inside the early window lands on the session about to start`() {
        val occurrence = yoga.checkInOccurrence(monday.atTime(12, 45))
        assertEquals(monday, occurrence?.date)
    }

    @Test
    fun `a scan before the early window is refused`() {
        assertNull(yoga.checkInOccurrence(monday.atTime(12, 0)))
    }

    @Test
    fun `a scan after the session ended is refused`() {
        assertNull(yoga.checkInOccurrence(monday.atTime(17, 1)))
    }

    @Test
    fun `a scan on a day the event does not run is refused`() {
        assertNull(yoga.checkInOccurrence(monday.plusDays(1).atTime(14, 0)))
    }

    @Test
    fun `a session running up to midnight is open right until it ends`() {
        val overnight = event(
            recurrence = Recurrence.DAILY,
            days = listOf(ScheduleDay(ScheduleDay.DAILY_KEY, TimeRange(22 * 60, 24 * 60)))
        )
        assertEquals(monday, overnight.checkInOccurrence(monday.atTime(23, 59))?.date)
        // The run ends at midnight, so the following minute belongs to nothing.
        assertNull(overnight.checkInOccurrence(monday.plusDays(1).atTime(0, 1)))
    }

    @Test
    fun `a daily event files each scan under its own day`() {
        val standup = event(
            recurrence = Recurrence.DAILY,
            days = listOf(ScheduleDay(ScheduleDay.DAILY_KEY, TimeRange(9 * 60, 10 * 60)))
        )
        assertEquals(monday, standup.checkInOccurrence(monday.atTime(9, 5))?.date)
        assertEquals(
            monday.plusDays(1),
            standup.checkInOccurrence(monday.plusDays(1).atTime(8, 45))?.date
        )
    }

    private fun event(recurrence: Recurrence, days: List<ScheduleDay>) = Event(
        id = "event",
        title = "Clase de Yoga",
        organizerId = "organizer",
        schedule = EventSchedule(recurrence = recurrence, days = days)
    )
}

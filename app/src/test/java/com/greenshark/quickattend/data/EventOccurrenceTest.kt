package com.greenshark.quickattend.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EventOccurrenceTest {

    /** 2026-08-24 is a Monday, which every weekly fixture below leans on. */
    private val monday: LocalDate = LocalDate.of(2026, 8, 24)

    private val yoga = event(
        id = "yoga",
        recurrence = Recurrence.WEEKLY,
        days = listOf(ScheduleDay(dayKey = 1, range = TimeRange(13 * 60, 17 * 60)))
    )
    private val standup = event(
        id = "standup",
        recurrence = Recurrence.DAILY,
        days = listOf(ScheduleDay(dayKey = ScheduleDay.DAILY_KEY, range = TimeRange(9 * 60, 10 * 60)))
    )

    @Test
    fun `occurrence is in progress between start and end`() {
        val occurrence = yoga.occurrenceOn(monday, monday.atTime(14, 30))
        assertEquals(EventStatus.IN_PROGRESS, occurrence?.status)
    }

    @Test
    fun `occurrence is upcoming before it starts and finished after it ends`() {
        assertEquals(EventStatus.UPCOMING, yoga.occurrenceOn(monday, monday.atTime(9, 0))?.status)
        assertEquals(EventStatus.FINISHED, yoga.occurrenceOn(monday, monday.atTime(18, 0))?.status)
    }

    @Test
    fun `weekly event does not occur on other week days`() {
        assertNull(yoga.occurrenceOn(monday.plusDays(1), monday.atTime(9, 0)))
    }

    @Test
    fun `daily event occurs on every date`() {
        val now = monday.atTime(8, 0)
        assertEquals(monday, standup.occurrenceOn(monday, now)?.date)
        assertEquals(monday.plusDays(3), standup.occurrenceOn(monday.plusDays(3), now)?.date)
    }

    @Test
    fun `monthly event only occurs on its day of month`() {
        val monthly = event(
            id = "monthly",
            recurrence = Recurrence.MONTHLY,
            days = listOf(ScheduleDay(dayKey = 24, range = TimeRange(10 * 60, 11 * 60)))
        )
        val now = monday.atTime(8, 0)
        assertEquals(monday, monthly.occurrenceOn(monday, now)?.date)
        assertNull(monthly.occurrenceOn(monday.plusDays(1), now))
    }

    @Test
    fun `main occurrence prefers the one running over a later one`() {
        val evening = event(
            id = "evening",
            recurrence = Recurrence.WEEKLY,
            days = listOf(ScheduleDay(dayKey = 1, range = TimeRange(19 * 60, 20 * 60)))
        )
        val main = listOf(evening, yoga).mainOccurrenceOn(monday, monday.atTime(14, 30))
        assertEquals("yoga", main?.event?.id)
        assertEquals(EventStatus.IN_PROGRESS, main?.status)
    }

    @Test
    fun `main occurrence falls back to the next one still ahead`() {
        val evening = event(
            id = "evening",
            recurrence = Recurrence.WEEKLY,
            days = listOf(ScheduleDay(dayKey = 1, range = TimeRange(19 * 60, 20 * 60)))
        )
        val main = listOf(evening, yoga).mainOccurrenceOn(monday, monday.atTime(18, 0))
        assertEquals("evening", main?.event?.id)
        assertEquals(EventStatus.UPCOMING, main?.status)
    }

    @Test
    fun `a day with nothing left has no main occurrence`() {
        assertNull(listOf(yoga).mainOccurrenceOn(monday, monday.atTime(18, 0)))
        assertNull(listOf(yoga).mainOccurrenceOn(monday.minusDays(1), monday.atTime(9, 0)))
    }

    @Test
    fun `upcoming occurrences are ordered by start and skip finished runs`() {
        val now: LocalDateTime = monday.atTime(14, 30)
        val upcoming = listOf(yoga, standup).upcomingOccurrences(now = now, withinDays = 8)

        // Today's stand-up ended at 10:00, so the yoga class running now comes first.
        assertEquals(
            listOf(
                "yoga" to monday,
                "standup" to monday.plusDays(1),
                "standup" to monday.plusDays(2)
            ),
            upcoming.take(3).map { it.event.id to it.date }
        )
        assertEquals(upcoming.map { it.start }.sorted(), upcoming.map { it.start })
    }

    @Test
    fun `an end of midnight rolls into the next day`() {
        val overnight = event(
            id = "overnight",
            recurrence = Recurrence.DAILY,
            days = listOf(ScheduleDay(ScheduleDay.DAILY_KEY, TimeRange(22 * 60, 24 * 60)))
        )
        val occurrence = overnight.occurrenceOn(monday, monday.atTime(23, 0))
        assertEquals(monday.plusDays(1).atStartOfDay(), occurrence?.end)
        assertEquals(EventStatus.IN_PROGRESS, occurrence?.status)
    }

    private fun event(id: String, recurrence: Recurrence, days: List<ScheduleDay>) = Event(
        id = id,
        title = id,
        organizerId = "organizer",
        location = "Salon C201",
        schedule = EventSchedule(recurrence = recurrence, days = days)
    )
}

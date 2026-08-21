/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import java.time.LocalDate
import java.time.LocalDateTime

/** Where an occurrence sits relative to the current time. */
enum class EventStatus {
    UPCOMING,
    IN_PROGRESS,
    FINISHED
}

/**
 * One concrete run of a recurring [Event] on a single date. The schedule only
 * says "every Monday from 13:00", so the UI needs this to know which day it is
 * talking about and whether that run is running, still to come, or already over.
 */
data class EventOccurrence(
    val event: Event,
    val date: LocalDate,
    val range: TimeRange,
    val status: EventStatus = EventStatus.UPCOMING
) {
    /** Stable identity for lazy lists: the same event on two dates is two items. */
    val key: String get() = "${event.id}@$date"

    val start: LocalDateTime get() = date.atTime(range.start)

    /** An end of 24:00 belongs to the next day, which [TimeRange.end] cannot hold. */
    val end: LocalDateTime
        get() = if (range.endMinutes >= MINUTES_PER_DAY) {
            date.plusDays(1).atStartOfDay()
        } else {
            date.atTime(range.end)
        }
}

/** The [ScheduleDay.dayKey] that [date] maps to under this recurrence. */
fun EventSchedule.dayKeyFor(date: LocalDate): Int = when (recurrence) {
    Recurrence.DAILY -> ScheduleDay.DAILY_KEY
    Recurrence.WEEKLY -> date.dayOfWeek.value
    Recurrence.MONTHLY -> date.dayOfMonth
}

/** The hours this schedule runs on [date], or null when it does not run at all. */
fun EventSchedule.rangeOn(date: LocalDate): TimeRange? = rangeFor(dayKeyFor(date))

fun Event.occurrenceOn(date: LocalDate, now: LocalDateTime): EventOccurrence? =
    schedule.rangeOn(date)?.let { range ->
        EventOccurrence(event = this, date = date, range = range).let {
            it.copy(status = it.statusAt(now))
        }
    }

/**
 * What to feature for [date]: whatever is running right now, otherwise the next
 * run that has not finished yet. Null when the day has nothing left on it.
 */
fun List<Event>.mainOccurrenceOn(date: LocalDate, now: LocalDateTime): EventOccurrence? =
    mapNotNull { it.occurrenceOn(date, now) }
        .filterNot { it.status == EventStatus.FINISHED }
        .minByOrNull { it.start }

/**
 * Every run of these events from [now] up to [withinDays] days ahead, ordered by
 * start time. Runs that already finished are dropped, so the first item is
 * whatever is happening now or comes next.
 */
fun List<Event>.upcomingOccurrences(
    now: LocalDateTime,
    withinDays: Int = DEFAULT_LOOKAHEAD_DAYS
): List<EventOccurrence> {
    val from = now.toLocalDate()
    return (0..withinDays)
        .flatMap { offset ->
            val date = from.plusDays(offset.toLong())
            mapNotNull { it.occurrenceOn(date, now) }
        }
        .filterNot { it.status == EventStatus.FINISHED }
        .sortedBy { it.start }
}

/**
 * Runs of this event that already started, from [withinDays] ago up to [now],
 * oldest first. A run counts as past the moment it starts, so today's session
 * is already there while it is still going.
 */
fun Event.pastOccurrences(
    now: LocalDateTime,
    withinDays: Int = DEFAULT_HISTORY_DAYS
): List<EventOccurrence> {
    val today = now.toLocalDate()
    return (withinDays downTo 0)
        .mapNotNull { offset -> occurrenceOn(today.minusDays(offset.toLong()), now) }
        .filterNot { it.start.isAfter(now) }
}

private fun EventOccurrence.statusAt(now: LocalDateTime): EventStatus = when {
    now.isBefore(start) -> EventStatus.UPCOMING
    now.isBefore(end) -> EventStatus.IN_PROGRESS
    else -> EventStatus.FINISHED
}

/** Far enough ahead that a monthly event always shows up at least once. */
const val DEFAULT_LOOKAHEAD_DAYS = 62

private const val MINUTES_PER_DAY = 24 * 60

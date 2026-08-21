/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** How often an event repeats. */
enum class Recurrence {
    DAILY,
    WEEKLY,
    MONTHLY
}

/**
 * A range of hours inside a single day, stored as minutes from midnight so it
 * serializes to a plain number in any backend and is always 24h based.
 */
data class TimeRange(
    val startMinutes: Int = 8 * 60,
    val endMinutes: Int = 9 * 60
) {
    val start: LocalTime get() = LocalTime.ofSecondOfDay(startMinutes * 60L)
    val end: LocalTime get() = LocalTime.ofSecondOfDay(endMinutes * 60L)

    /** An end at or before the start means the user still has to fix it. */
    val isValid: Boolean get() = endMinutes > startMinutes

    fun format(): String = "${formatMinutes(startMinutes)} - ${formatMinutes(endMinutes)}"

    companion object {
        fun formatMinutes(minutes: Int): String =
            "%02d:%02d".format(minutes / 60, minutes % 60)
    }
}

/**
 * One selected day plus the hours the event runs on it.
 *
 * [dayKey] meaning depends on the recurrence:
 * - [Recurrence.DAILY]: always [DAILY_KEY], there is a single range for every day.
 * - [Recurrence.WEEKLY]: [DayOfWeek.getValue] (1 = Monday .. 7 = Sunday).
 * - [Recurrence.MONTHLY]: day of month, 1..31.
 */
data class ScheduleDay(
    val dayKey: Int = DAILY_KEY,
    val range: TimeRange = TimeRange()
) {
    companion object {
        const val DAILY_KEY = 0
    }
}

data class EventSchedule(
    val recurrence: Recurrence = Recurrence.WEEKLY,
    val days: List<ScheduleDay> = emptyList()
) {
    /** Days sorted the way they are displayed, so summaries stay stable. */
    val orderedDays: List<ScheduleDay> get() = days.sortedBy { it.dayKey }

    fun rangeFor(dayKey: Int): TimeRange? = days.firstOrNull { it.dayKey == dayKey }?.range

    fun isSelected(dayKey: Int): Boolean = days.any { it.dayKey == dayKey }
}

/** "Every day", "Mon, Wed, Fri" or "Day 1, 15 of each month". */
fun EventSchedule.daysSummary(locale: Locale = Locale.getDefault()): String = when (recurrence) {
    Recurrence.DAILY -> "Every day"
    Recurrence.WEEKLY -> orderedDays
        .joinToString { DayOfWeek.of(it.dayKey).getDisplayName(TextStyle.SHORT, locale) }
        .ifEmpty { "No days selected" }

    Recurrence.MONTHLY -> orderedDays
        .joinToString(prefix = "Day ") { it.dayKey.toString() }
        .takeIf { orderedDays.isNotEmpty() } ?: "No days selected"
}

/** A single range when every day shares one, otherwise the span it fits inside. */
fun EventSchedule.timeSummary(): String {
    val ranges = days.map { it.range }
    if (ranges.isEmpty()) return "--:-- - --:--"
    val distinct = ranges.distinct()
    if (distinct.size == 1) return distinct.first().format()
    return "${TimeRange.formatMinutes(ranges.minOf { it.startMinutes })} - " +
            TimeRange.formatMinutes(ranges.maxOf { it.endMinutes })
}

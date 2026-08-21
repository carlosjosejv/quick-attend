/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** How one run of an event went for a single attendant. */
enum class AttendanceStatus {
    ON_TIME,
    LATE,
    ABSENT
}

/** Minutes after the scheduled start that still count as arriving on time. */
const val ON_TIME_THRESHOLD_MINUTES = 5L

/** How far back attendance is looked at, long enough for a monthly event to show. */
const val DEFAULT_HISTORY_DAYS = 90

/**
 * A check-in exactly as the backend stores it: the moment an attendant recorded
 * their attendance for one run of an event. Absences are never stored, they are
 * simply the runs that have no check-in.
 */
data class AttendanceCheckIn(
    val eventId: String = "",
    val userId: String = "",
    /** Date of the run this check-in belongs to, not necessarily the scan date. */
    val date: LocalDate = LocalDate.now(),
    val checkInAt: LocalDateTime = LocalDateTime.now()
)

/**
 * One past run of an event judged against the hour it was scheduled for.
 * [checkInAt] being null is what makes a run an absence, so a record exists for
 * every session whether the attendant showed up or not.
 */
data class AttendanceRecord(
    val eventId: String,
    val eventTitle: String = "",
    val date: LocalDate,
    val scheduledStart: LocalDateTime,
    val checkInAt: LocalDateTime? = null
) {
    /** Stable identity for lazy lists: one record per event and date. */
    val key: String get() = "$eventId@$date"

    /**
     * Minutes between the scheduled start and the check-in, negative when the
     * attendant arrived early. Null for an absence.
     */
    val minutesFromStart: Long?
        get() = checkInAt?.let { Duration.between(scheduledStart, it).toMinutes() }

    /** Minutes after the scheduled start, floored at zero. Zero when absent. */
    val minutesLate: Long get() = minutesFromStart?.coerceAtLeast(0L) ?: 0L

    val status: AttendanceStatus
        get() {
            val minutes = minutesFromStart ?: return AttendanceStatus.ABSENT
            return if (minutes <= ON_TIME_THRESHOLD_MINUTES) {
                AttendanceStatus.ON_TIME
            } else {
                AttendanceStatus.LATE
            }
        }

    /** Exact clock time the attendance was recorded, e.g. "13:07". */
    val checkInLabel: String? get() = checkInAt?.format(HOUR_MINUTE)

    val scheduledStartLabel: String get() = scheduledStart.format(HOUR_MINUTE)

    /** Last minute that still counts as on time, e.g. "13:05". */
    val onTimeUntilLabel: String
        get() = scheduledStart.plusMinutes(ON_TIME_THRESHOLD_MINUTES).format(HOUR_MINUTE)
}

/**
 * Counts over a set of records plus the score they add up to. Showing up late
 * is still showing up, so it earns partial credit instead of costing as much as
 * an absence.
 */
data class AttendanceSummary(
    val onTime: Int = 0,
    val late: Int = 0,
    val absent: Int = 0
) {
    val total: Int get() = onTime + late + absent

    val attended: Int get() = onTime + late

    /** 0..100. A history with nothing in it scores full marks rather than zero. */
    val score: Int
        get() {
            if (total == 0) return FULL_CREDIT
            return ((onTime * FULL_CREDIT + late * LATE_CREDIT) / total.toDouble()).roundToInt()
        }

    fun countOf(status: AttendanceStatus): Int = when (status) {
        AttendanceStatus.ON_TIME -> onTime
        AttendanceStatus.LATE -> late
        AttendanceStatus.ABSENT -> absent
    }

    /** Share of the history [status] takes up, 0f..1f, for bars and rings. */
    fun ratioOf(status: AttendanceStatus): Float =
        if (total == 0) 0f else countOf(status) / total.toFloat()

    private companion object {
        const val FULL_CREDIT = 100
        const val LATE_CREDIT = 50
    }
}

fun List<AttendanceRecord>.summarize(): AttendanceSummary = AttendanceSummary(
    onTime = count { it.status == AttendanceStatus.ON_TIME },
    late = count { it.status == AttendanceStatus.LATE },
    absent = count { it.status == AttendanceStatus.ABSENT }
)

/**
 * Every run of this event that already started, oldest first, paired with the
 * check-in recorded for it. Runs nothing was scanned for become absences, which
 * is the only way an absence ever enters the app.
 */
fun Event.attendanceHistory(
    checkIns: List<AttendanceCheckIn>,
    now: LocalDateTime,
    withinDays: Int = DEFAULT_HISTORY_DAYS
): List<AttendanceRecord> {
    val byDate = checkIns.filter { it.eventId == id }.associateBy { it.date }
    return pastOccurrences(now = now, withinDays = withinDays).map { occurrence ->
        AttendanceRecord(
            eventId = id,
            eventTitle = title,
            date = occurrence.date,
            scheduledStart = occurrence.start,
            checkInAt = byDate[occurrence.date]?.checkInAt
        )
    }
}

private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

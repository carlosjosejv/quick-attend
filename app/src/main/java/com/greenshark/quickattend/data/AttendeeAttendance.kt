/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import java.time.LocalDateTime

/**
 * One attendant's whole relationship with one event: who they are, every past
 * session judged against the schedule, and what those sessions add up to.
 *
 * This is what the host's attendee list and per-attendee view both read, and
 * what an export is built from, so all three always agree on the numbers.
 */
data class AttendeeAttendance(
    val profile: UserProfile,
    /** Every past session, oldest first, absences included. */
    val records: List<AttendanceRecord> = emptyList(),
    val summary: AttendanceSummary = AttendanceSummary()
) {
    /** Stable identity for lazy lists. */
    val key: String get() = profile.id

    /** Newest first, which is the order a history list reads in. */
    val recentFirst: List<AttendanceRecord> get() = records.asReversed()

    val lastAttended: AttendanceRecord?
        get() = records.lastOrNull { it.status != AttendanceStatus.ABSENT }
}

/**
 * How every attendant did on this event. [profiles] drives the list, so someone
 * who signed up and never came still appears, with every session as an absence.
 */
fun Event.attendeeAttendance(
    profiles: List<UserProfile>,
    checkIns: List<AttendanceCheckIn>,
    now: LocalDateTime = LocalDateTime.now(),
    withinDays: Int = DEFAULT_HISTORY_DAYS
): List<AttendeeAttendance> {
    val byUser = checkIns.groupBy { it.userId }
    return profiles.map { profile ->
        val records = attendanceHistory(
            checkIns = byUser[profile.id].orEmpty(),
            now = now,
            withinDays = withinDays
        )
        AttendeeAttendance(
            profile = profile,
            records = records,
            summary = records.summarize()
        )
    }
}

/** Counts across every attendant, for the header of the host's event detail. */
data class EventAttendanceOverview(
    val attendees: Int = 0,
    /** Past sessions the event has already run, the same for every attendant. */
    val sessions: Int = 0,
    val summary: AttendanceSummary = AttendanceSummary()
) {
    /** Average score of the event, which is the score of every record pooled. */
    val score: Int get() = summary.score
}

fun List<AttendeeAttendance>.overview(): EventAttendanceOverview = EventAttendanceOverview(
    attendees = size,
    sessions = maxOfOrNull { it.records.size } ?: 0,
    summary = AttendanceSummary(
        onTime = sumOf { it.summary.onTime },
        late = sumOf { it.summary.late },
        absent = sumOf { it.summary.absent }
    )
)

/** How the host can order the attendee list. */
enum class AttendeeSort {
    NAME,
    SCORE,
    ABSENCES
}

fun List<AttendeeAttendance>.sortedBy(sort: AttendeeSort): List<AttendeeAttendance> = when (sort) {
    AttendeeSort.NAME -> sortedBy { it.profile.displayName.lowercase() }
    AttendeeSort.SCORE -> sortedByDescending { it.summary.score }
    AttendeeSort.ABSENCES -> sortedByDescending { it.summary.absent }
}

/** Case insensitive match on name or email, for the attendee list's search box. */
fun List<AttendeeAttendance>.filteredBy(query: String): List<AttendeeAttendance> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return this
    return filter { attendee ->
        attendee.profile.displayName.contains(trimmed, ignoreCase = true) ||
                attendee.profile.email.contains(trimmed, ignoreCase = true)
    }
}

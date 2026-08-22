/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import java.time.LocalDate
import java.time.LocalDateTime

/** Whether the attendance that was just recorded is going to count for anything. */
enum class ApprovalState {
    /** The attendant is on the roll, so the record counts straight away. */
    ENROLLED,

    /** They were not on the roll; the host has been asked and has not answered. */
    PENDING_APPROVAL,

    /** The host already turned them down, so the record stays out of the roll. */
    REJECTED
}

/**
 * What one scan produced: the attendance that is now in the backend, the run it
 * belongs to, and what still has to happen for it to count.
 *
 * The record is built rather than stored a second time, so the on time or late
 * verdict shown at the moment of scanning is worked out by exactly the same code
 * as the one on the history screen and in an export.
 */
data class RecordedAttendance(
    val event: Event,
    val date: LocalDate,
    val scheduledStart: LocalDateTime,
    val checkInAt: LocalDateTime,
    /** True when the scan found an existing check-in instead of writing one. */
    val alreadyRecorded: Boolean = false,
    val approval: ApprovalState = ApprovalState.ENROLLED,
    /** How far the attendant was from the venue, when the event checks that. */
    val distanceMeters: Double? = null
) {
    val record: AttendanceRecord
        get() = AttendanceRecord(
            eventId = event.id,
            eventTitle = event.title,
            date = date,
            scheduledStart = scheduledStart,
            checkInAt = checkInAt
        )

    val status: AttendanceStatus get() = record.status

    /** True while the record is not part of the roll, so nothing exports it yet. */
    val needsHostApproval: Boolean get() = approval != ApprovalState.ENROLLED
}

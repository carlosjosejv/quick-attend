/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.greenshark.quickattend.data.AttendanceCheckIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * Wire shape of a check-in. The run's [date] is stored next to the timestamp
 * instead of being derived from it, because an event that ends after midnight
 * is scanned on a different calendar day than the one it belongs to.
 */
data class AttendanceDto(
    val userId: String = "",
    val eventId: String = "",
    /** ISO date (yyyy-MM-dd) of the run this check-in belongs to. */
    val date: String = "",
    /** Epoch millis of the scan, which is the exact time shown to the user. */
    val checkInAt: Long = 0L
)

fun AttendanceDto.toCheckIn(zone: ZoneId = ZoneId.systemDefault()): AttendanceCheckIn? {
    if (checkInAt <= 0L) return null
    val checkIn = Instant.ofEpochMilli(checkInAt).atZone(zone).toLocalDateTime()
    return AttendanceCheckIn(
        eventId = eventId,
        userId = userId,
        date = date.toLocalDateOrNull() ?: checkIn.toLocalDate(),
        checkInAt = checkIn
    )
}

fun AttendanceCheckIn.toDto(zone: ZoneId = ZoneId.systemDefault()): AttendanceDto = AttendanceDto(
    userId = userId,
    eventId = eventId,
    date = date.toString(),
    checkInAt = checkInAt.atZone(zone).toInstant().toEpochMilli()
)

/**
 * Deterministic id: one document per attendant, event and run. Scanning twice
 * lands on the same document instead of piling up rows the roll would have to
 * de-duplicate later.
 */
fun attendanceId(userId: String, eventId: String, date: LocalDate): String =
    "${userId}_${eventId}_$date"

private fun String.toLocalDateOrNull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (e: DateTimeParseException) {
    null
}

/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.repository

import com.greenshark.quickattend.data.AttendanceCheckIn
import com.greenshark.quickattend.data.DataResult

/**
 * Reading side of attendance. Only check-ins are stored; the absences the UI
 * shows are worked out from the event schedule, so nothing has to be written
 * for a session the attendant skipped.
 */
interface AttendanceRepository {

    /** Check-ins this user recorded for one event, in no particular order. */
    suspend fun getCheckIns(eventId: String, userId: String): DataResult<List<AttendanceCheckIn>>

    /** Every check-in recorded for one event, across all its attendants. */
    suspend fun getEventCheckIns(eventId: String): DataResult<List<AttendanceCheckIn>>
}

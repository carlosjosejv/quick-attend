/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.greenshark.quickattend.data.AttendanceCheckIn
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.repository.AttendanceRepository
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class FirestoreAttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AttendanceRepository {

    private val attendance get() = firestore.collection(COLLECTION_ATTENDANCE)

    override suspend fun getCheckIns(
        eventId: String,
        userId: String
    ): DataResult<List<AttendanceCheckIn>> = runCatchingFirestore {
        attendance
            .whereEqualTo(FIELD_EVENT_ID, eventId)
            .whereEqualTo(FIELD_USER_ID, userId)
            .get().await().toCheckIns()
    }

    override suspend fun getEventCheckIns(
        eventId: String
    ): DataResult<List<AttendanceCheckIn>> = runCatchingFirestore {
        attendance.whereEqualTo(FIELD_EVENT_ID, eventId).get().await().toCheckIns()
    }

    override suspend fun getCheckIn(
        eventId: String,
        userId: String,
        date: LocalDate
    ): DataResult<AttendanceCheckIn?> = runCatchingFirestore {
        attendance
            .document(attendanceId(userId = userId, eventId = eventId, date = date))
            .get().await()
            .toObject(AttendanceDto::class.java)
            ?.toCheckIn()
    }

    override suspend fun recordCheckIn(
        checkIn: AttendanceCheckIn
    ): DataResult<AttendanceCheckIn> = runCatchingFirestore {
        val document = attendance.document(
            attendanceId(
                userId = checkIn.userId,
                eventId = checkIn.eventId,
                date = checkIn.date
            )
        )

        // The first scan of a session is the one that counts as the arrival, so
        // a repeat scan reports the stored time rather than overwriting it.
        val existing = document.get().await().toObject(AttendanceDto::class.java)?.toCheckIn()
        if (existing != null) return@runCatchingFirestore existing

        document.set(checkIn.toDto()).await()
        checkIn
    }

    private fun QuerySnapshot.toCheckIns(): List<AttendanceCheckIn> =
        toObjects(AttendanceDto::class.java).mapNotNull { it.toCheckIn() }

    private companion object {
        const val COLLECTION_ATTENDANCE = "attendance"
        const val FIELD_EVENT_ID = "eventId"
        const val FIELD_USER_ID = "userId"
    }
}

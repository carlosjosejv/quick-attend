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

    private fun QuerySnapshot.toCheckIns(): List<AttendanceCheckIn> =
        toObjects(AttendanceDto::class.java).mapNotNull { it.toCheckIn() }

    private companion object {
        const val COLLECTION_ATTENDANCE = "attendance"
        const val FIELD_EVENT_ID = "eventId"
        const val FIELD_USER_ID = "userId"
    }
}

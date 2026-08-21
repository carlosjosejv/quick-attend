/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.greenshark.quickattend.data.AttendanceCheckIn
import com.greenshark.quickattend.data.DataError
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
    ): DataResult<List<AttendanceCheckIn>> = try {
        val checkIns = attendance
            .whereEqualTo(FIELD_EVENT_ID, eventId)
            .whereEqualTo(FIELD_USER_ID, userId)
            .get().await()
            .toObjects(AttendanceDto::class.java)
            .mapNotNull { it.toCheckIn() }
        DataResult.Success(checkIns)
    } catch (e: FirebaseFirestoreException) {
        DataResult.Failure(e.code.toDataError())
    } catch (e: Exception) {
        DataResult.Failure(DataError.UNKNOWN)
    }

    private fun FirebaseFirestoreException.Code.toDataError(): DataError = when (this) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> DataError.NETWORK

        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> DataError.UNAUTHORIZED

        FirebaseFirestoreException.Code.NOT_FOUND -> DataError.NOT_FOUND
        else -> DataError.UNKNOWN
    }

    private companion object {
        const val COLLECTION_ATTENDANCE = "attendance"
        const val FIELD_EVENT_ID = "eventId"
        const val FIELD_USER_ID = "userId"
    }
}

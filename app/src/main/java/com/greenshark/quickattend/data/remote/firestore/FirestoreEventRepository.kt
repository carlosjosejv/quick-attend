/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.repository.EventRepository
import kotlinx.coroutines.tasks.await

class FirestoreEventRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EventRepository {

    private val events get() = firestore.collection(COLLECTION_EVENTS)

    private val enrollments get() = firestore.collection(COLLECTION_ENROLLMENTS)

    override fun newEventId(): String = events.document().id

    override suspend fun createEvent(event: Event): DataResult<Event> {
        val now = System.currentTimeMillis()
        val stored = event.copy(createdAt = now, updatedAt = now)
        return runCatchingFirestore {
            events.document(stored.id).set(stored.toDto()).await()
            stored
        }
    }

    override suspend fun updateEvent(event: Event): DataResult<Event> {
        val stored = event.copy(updatedAt = System.currentTimeMillis())
        return runCatchingFirestore {
            events.document(stored.id).set(stored.toDto()).await()
            stored
        }
    }

    override suspend fun getEvent(eventId: String): DataResult<Event> = runCatchingFirestore {
        val snapshot = events.document(eventId).get().await()
        snapshot.toObject(EventDto::class.java)?.toEvent()
            ?: return DataResult.Failure(DataError.NOT_FOUND)
    }

    override suspend fun getEventsByOrganizer(organizerId: String): DataResult<List<Event>> =
        runCatchingFirestore {
            events.whereEqualTo(FIELD_ORGANIZER_ID, organizerId).get().await()
                .toObjects(EventDto::class.java)
                .map { it.toEvent() }
        }

    override suspend fun getEventsByAttendee(attendeeId: String): DataResult<List<Event>> =
        runCatchingFirestore {
            val eventIds = enrollments
                .whereEqualTo(FIELD_USER_ID, attendeeId).get().await()
                .toObjects(EnrollmentDto::class.java)
                .map { it.eventId }
                .filter { it.isNotBlank() }
                .distinct()

            if (eventIds.isEmpty()) {
                emptyList()
            } else {
                // A document id query only takes so many values at a time, so the
                // ids are asked for in batches and stitched back together.
                eventIds.chunked(MAX_IDS_PER_QUERY).flatMap { batch ->
                    events.whereIn(FieldPath.documentId(), batch).get().await()
                        .toObjects(EventDto::class.java)
                        .map { it.toEvent() }
                }
            }
        }

    override suspend fun joinEvent(eventId: String, attendeeId: String): DataResult<Unit> =
        runCatchingFirestore {
            val enrollment = EnrollmentDto(
                userId = attendeeId,
                eventId = eventId,
                joinedAt = System.currentTimeMillis()
            )
            enrollments.document(enrollmentId(attendeeId, eventId)).set(enrollment).await()
            Unit
        }

    private inline fun <T> runCatchingFirestore(block: () -> T): DataResult<T> = try {
        DataResult.Success(block())
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
        const val COLLECTION_EVENTS = "events"
        const val COLLECTION_ENROLLMENTS = "enrollments"
        const val FIELD_ORGANIZER_ID = "organizerId"
        const val FIELD_USER_ID = "userId"

        /** Firestore's cap on the values a single "in" filter accepts. */
        const val MAX_IDS_PER_QUERY = 30
    }
}

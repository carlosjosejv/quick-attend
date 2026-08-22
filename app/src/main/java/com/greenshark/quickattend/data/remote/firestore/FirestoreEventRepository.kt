/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    private val attendance get() = firestore.collection(COLLECTION_ATTENDANCE)

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

    override suspend fun getAttendeeIds(eventId: String): DataResult<List<String>> =
        runCatchingFirestore {
            enrollments
                .whereEqualTo(FIELD_EVENT_ID, eventId)
                .orderBy(FIELD_JOINED_AT, Query.Direction.ASCENDING)
                .get().await()
                .toObjects(EnrollmentDto::class.java)
                .map { it.userId }
                .filter { it.isNotBlank() }
                .distinct()
        }

    override suspend fun removeAttendee(eventId: String, attendeeId: String): DataResult<Unit> =
        runCatchingFirestore {
            enrollments.document(enrollmentId(attendeeId, eventId)).delete().await()
            Unit
        }

    override suspend fun deleteEvent(eventId: String): DataResult<Unit> = runCatchingFirestore {
        // Sign-ups and check-ins are separate collections, so nothing cascades
        // on its own; leaving them behind would keep the event on the home
        // screen of everyone who joined it.
        deleteWhereEventIs(collection = enrollments, eventId = eventId)
        deleteWhereEventIs(collection = attendance, eventId = eventId)
        events.document(eventId).delete().await()
        Unit
    }

    /** Deletes every document of one collection that belongs to an event. */
    private suspend fun deleteWhereEventIs(
        collection: CollectionReference,
        eventId: String
    ) {
        val documents = collection.whereEqualTo(FIELD_EVENT_ID, eventId).get().await().documents
        // A write batch caps how much it takes, so the deletes go out in chunks.
        documents.chunked(MAX_WRITES_PER_BATCH).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { document -> batch.delete(document.reference) }
            batch.commit().await()
        }
    }

    private companion object {
        const val COLLECTION_EVENTS = "events"
        const val COLLECTION_ENROLLMENTS = "enrollments"
        const val COLLECTION_ATTENDANCE = "attendance"
        const val FIELD_ORGANIZER_ID = "organizerId"
        const val FIELD_USER_ID = "userId"
        const val FIELD_EVENT_ID = "eventId"
        const val FIELD_JOINED_AT = "joinedAt"

        /** Firestore's cap on the values a single "in" filter accepts. */
        const val MAX_IDS_PER_QUERY = 30

        /** Firestore's cap on the operations a single write batch accepts. */
        const val MAX_WRITES_PER_BATCH = 500
    }
}

/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

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
        const val FIELD_ORGANIZER_ID = "organizerId"
    }
}

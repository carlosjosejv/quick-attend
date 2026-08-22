/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.JoinRequest
import com.greenshark.quickattend.data.JoinRequestStatus
import com.greenshark.quickattend.data.joinRequestId
import com.greenshark.quickattend.data.repository.JoinRequestRepository
import kotlinx.coroutines.tasks.await

class FirestoreJoinRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : JoinRequestRepository {

    private val requests get() = firestore.collection(COLLECTION_JOIN_REQUESTS)

    override suspend fun requestJoin(request: JoinRequest): DataResult<JoinRequest> =
        runCatchingFirestore {
            val id = joinRequestId(userId = request.userId, eventId = request.eventId)
            val document = requests.document(id)

            // Read before write: a host who already answered must not have the
            // question put to them again the next time the attendant scans.
            val existing = document.get().await().toObject(JoinRequestDto::class.java)
            if (existing != null) return@runCatchingFirestore existing.toJoinRequest()

            val stored = request.copy(
                id = id,
                status = JoinRequestStatus.PENDING,
                requestedAt = request.requestedAt.takeIf { it > 0L }
                    ?: System.currentTimeMillis()
            )
            document.set(stored.toDto()).await()
            stored
        }

    override suspend fun getPendingRequestsForHost(
        hostId: String
    ): DataResult<List<JoinRequest>> = runCatchingFirestore {
        requests
            .whereEqualTo(FIELD_HOST_ID, hostId)
            .whereEqualTo(FIELD_STATUS, JoinRequestStatus.PENDING.name)
            .get().await().toJoinRequests()
    }

    override suspend fun getRequestsForEvent(eventId: String): DataResult<List<JoinRequest>> =
        runCatchingFirestore {
            requests
                .whereEqualTo(FIELD_EVENT_ID, eventId)
                .get().await().toJoinRequests()
        }

    override suspend fun getRequest(
        eventId: String,
        userId: String
    ): DataResult<JoinRequest?> = runCatchingFirestore {
        requests.document(joinRequestId(userId = userId, eventId = eventId))
            .get().await()
            .toObject(JoinRequestDto::class.java)
            ?.toJoinRequest()
    }

    override suspend fun setStatus(
        requestId: String,
        status: JoinRequestStatus
    ): DataResult<Unit> = runCatchingFirestore {
        requests.document(requestId).update(FIELD_STATUS, status.name).await()
        Unit
    }

    /**
     * Newest first, sorted here rather than by the query. Adding an orderBy to
     * an equality filter makes Firestore demand a composite index, and a
     * missing one fails the whole read — too high a price for putting a handful
     * of requests in order.
     */
    private fun QuerySnapshot.toJoinRequests(): List<JoinRequest> =
        toObjects(JoinRequestDto::class.java)
            .map { it.toJoinRequest() }
            .sortedByDescending { it.requestedAt }

    private companion object {
        const val COLLECTION_JOIN_REQUESTS = "joinRequests"
        const val FIELD_HOST_ID = "hostId"
        const val FIELD_EVENT_ID = "eventId"
        const val FIELD_STATUS = "status"
    }
}

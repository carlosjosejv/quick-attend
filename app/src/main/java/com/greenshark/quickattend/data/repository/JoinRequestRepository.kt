/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.repository

import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.JoinRequest
import com.greenshark.quickattend.data.JoinRequestStatus

/**
 * Requests from people who scanned an event they were not signed up for.
 *
 * Kept apart from sign-ups on purpose: a request is a question for the host,
 * and only answering it yes turns into the enrollment that the roll and the
 * exports read.
 */
interface JoinRequestRepository {

    /**
     * Files the request, or leaves the existing one alone when there already is
     * one. Scanning a second time must not reopen a request the host already
     * answered, so an approved or rejected request comes back untouched.
     */
    suspend fun requestJoin(request: JoinRequest): DataResult<JoinRequest>

    /** Every request waiting on this host, newest first. */
    suspend fun getPendingRequestsForHost(hostId: String): DataResult<List<JoinRequest>>

    /** Every request filed against one event, whatever its status. */
    suspend fun getRequestsForEvent(eventId: String): DataResult<List<JoinRequest>>

    /** What one person's request on one event says, or null when they never filed one. */
    suspend fun getRequest(eventId: String, userId: String): DataResult<JoinRequest?>

    /**
     * Answers a request. Approving is the only thing that makes the attendant's
     * already recorded check-ins count, so the caller signs them up too.
     */
    suspend fun setStatus(requestId: String, status: JoinRequestStatus): DataResult<Unit>
}

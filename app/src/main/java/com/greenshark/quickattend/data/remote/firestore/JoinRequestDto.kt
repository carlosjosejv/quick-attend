/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.greenshark.quickattend.data.JoinRequest
import com.greenshark.quickattend.data.JoinRequestStatus
import com.greenshark.quickattend.data.joinRequestId

/**
 * Wire shape of a join request. [status] travels as its name rather than its
 * ordinal, so adding a state later cannot silently reinterpret stored rows.
 */
data class JoinRequestDto(
    val id: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val hostId: String = "",
    val status: String = JoinRequestStatus.PENDING.name,
    val requestedAt: Long = 0L,
    val firstCheckInAt: Long = 0L
)

fun JoinRequestDto.toJoinRequest(): JoinRequest = JoinRequest(
    id = id.ifBlank { joinRequestId(userId = userId, eventId = eventId) },
    eventId = eventId,
    eventTitle = eventTitle,
    userId = userId,
    userName = userName,
    userEmail = userEmail,
    hostId = hostId,
    status = status.toJoinRequestStatus(),
    requestedAt = requestedAt,
    firstCheckInAt = firstCheckInAt
)

fun JoinRequest.toDto(): JoinRequestDto = JoinRequestDto(
    id = id.ifBlank { joinRequestId(userId = userId, eventId = eventId) },
    eventId = eventId,
    eventTitle = eventTitle,
    userId = userId,
    userName = userName,
    userEmail = userEmail,
    hostId = hostId,
    status = status.name,
    requestedAt = requestedAt,
    firstCheckInAt = firstCheckInAt
)

/** An unknown name means a row written by a newer build; treat it as pending. */
private fun String.toJoinRequestStatus(): JoinRequestStatus =
    JoinRequestStatus.entries.firstOrNull { it.name == this } ?: JoinRequestStatus.PENDING

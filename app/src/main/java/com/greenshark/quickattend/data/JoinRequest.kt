/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Where a request to join an event stands. */
enum class JoinRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

/**
 * Someone scanned the QR of an event they were never signed up for.
 *
 * Their attendance is recorded either way — turning a scanner away at the door
 * loses the only record that they were there. What the host decides is whether
 * that record counts: approving signs them up, and a sign-up is what the roll
 * and every export are built from, so an ignored request simply never shows up
 * anywhere while its check-ins sit harmlessly in the backend.
 */
data class JoinRequest(
    val id: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val userId: String = "",
    /** Copied in at request time so the host's list needs no second lookup. */
    val userName: String = "",
    val userEmail: String = "",
    /** Host who decides, so pending requests can be found without the events. */
    val hostId: String = "",
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    val requestedAt: Long = 0L,
    /** First session they scanned into, which is what the host is judging. */
    val firstCheckInAt: Long = 0L
) {
    /** Stable identity for lazy lists. */
    val key: String get() = id.ifBlank { "${userId}_$eventId" }

    val isPending: Boolean get() = status == JoinRequestStatus.PENDING

    val requestedAtDateTime: LocalDateTime?
        get() = requestedAt.takeIf { it > 0L }?.toLocalDateTime()

    val firstCheckInDateTime: LocalDateTime?
        get() = firstCheckInAt.takeIf { it > 0L }?.toLocalDateTime()

    /** Never blank, so the host's list never shows a nameless row. */
    val displayName: String
        get() = userName.ifBlank { userEmail.substringBefore('@').ifBlank { FALLBACK_NAME } }

    private fun Long.toLocalDateTime(): LocalDateTime =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

    private companion object {
        const val FALLBACK_NAME = "Attendant"
    }
}

/** Deterministic id, so scanning again updates one request instead of adding another. */
fun joinRequestId(userId: String, eventId: String): String = "${userId}_$eventId"

fun List<JoinRequest>.pending(): List<JoinRequest> = filter { it.isPending }

/** Pending requests per event, which is what badges the host's event list. */
fun List<JoinRequest>.pendingCountByEvent(): Map<String, Int> =
    pending().groupingBy { it.eventId }.eachCount()

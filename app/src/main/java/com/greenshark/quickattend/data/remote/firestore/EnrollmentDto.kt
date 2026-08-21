/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

/**
 * Links a user to an event they signed up for. Kept in its own collection
 * instead of an array on the event so that saving an event never rewrites its
 * attendee list, and so one event can hold more attendees than a document fits.
 */
data class EnrollmentDto(
    val userId: String = "",
    val eventId: String = "",
    val joinedAt: Long = 0L
)

/** Deterministic id, so signing up twice updates one document instead of adding another. */
fun enrollmentId(userId: String, eventId: String): String = "${userId}_$eventId"

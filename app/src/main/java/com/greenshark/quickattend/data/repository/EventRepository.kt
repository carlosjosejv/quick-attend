/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.repository

import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event

/**
 * The only contract the UI knows about. Firestore is today's implementation;
 * a REST backed one can replace it without touching the presentation layer.
 */
interface EventRepository {

    /** Reserves a unique id up front so the QR code can be shown before saving. */
    fun newEventId(): String

    suspend fun createEvent(event: Event): DataResult<Event>

    suspend fun updateEvent(event: Event): DataResult<Event>

    suspend fun getEvent(eventId: String): DataResult<Event>

    suspend fun getEventsByOrganizer(organizerId: String): DataResult<List<Event>>

    /** Events the user signed up to attend, which is what the home screen lists. */
    suspend fun getEventsByAttendee(attendeeId: String): DataResult<List<Event>>

    /** Signs the user up to attend an event. Doing it twice changes nothing. */
    suspend fun joinEvent(eventId: String, attendeeId: String): DataResult<Unit>

    /** Ids of everyone signed up for the event, in the order they joined. */
    suspend fun getAttendeeIds(eventId: String): DataResult<List<String>>

    /**
     * Drops the attendant's sign-up. Their past check-ins are left alone, so
     * removing someone by mistake and adding them back restores their history.
     */
    suspend fun removeAttendee(eventId: String, attendeeId: String): DataResult<Unit>

    /** Deletes the event along with every sign-up and check-in recorded for it. */
    suspend fun deleteEvent(eventId: String): DataResult<Unit>
}

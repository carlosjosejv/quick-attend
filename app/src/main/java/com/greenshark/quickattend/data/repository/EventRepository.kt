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
}

/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.MapLocation
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.TimeRange

/**
 * Wire shape of an event. Every field has a default and only primitives or
 * lists of maps are used, so the same documents can later be served by a JSON
 * API without changing the stored data.
 */
data class EventDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val hostName: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val location: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = MapLocation.DEFAULT_RADIUS_METERS,
    val locationValidationEnabled: Boolean = false,
    val recurrence: String = Recurrence.WEEKLY.name,
    val days: List<Map<String, Any>> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

private const val FIELD_DAY_KEY = "dayKey"
private const val FIELD_START = "startMinutes"
private const val FIELD_END = "endMinutes"

fun Event.toDto(): EventDto = EventDto(
    id = id,
    title = title,
    description = description,
    hostName = hostName,
    organizerId = organizerId,
    organizerName = organizerName,
    location = location,
    address = address,
    latitude = mapLocation?.latitude,
    longitude = mapLocation?.longitude,
    radiusMeters = mapLocation?.radiusMeters ?: MapLocation.DEFAULT_RADIUS_METERS,
    locationValidationEnabled = locationValidationEnabled,
    recurrence = schedule.recurrence.name,
    days = schedule.days.map { day ->
        mapOf(
            FIELD_DAY_KEY to day.dayKey.toLong(),
            FIELD_START to day.range.startMinutes.toLong(),
            FIELD_END to day.range.endMinutes.toLong()
        )
    },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun EventDto.toEvent(): Event = Event(
    id = id,
    title = title,
    description = description,
    hostName = hostName,
    organizerId = organizerId,
    organizerName = organizerName,
    location = location,
    address = address,
    mapLocation = if (latitude != null && longitude != null) {
        MapLocation(latitude, longitude, radiusMeters)
    } else {
        null
    },
    locationValidationEnabled = locationValidationEnabled,
    schedule = EventSchedule(
        recurrence = runCatching { Recurrence.valueOf(recurrence) }.getOrDefault(Recurrence.WEEKLY),
        days = days.mapNotNull { it.toScheduleDay() }
    ),
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Map<String, Any>.toScheduleDay(): ScheduleDay? {
    val dayKey = (this[FIELD_DAY_KEY] as? Number)?.toInt() ?: return null
    val start = (this[FIELD_START] as? Number)?.toInt() ?: return null
    val end = (this[FIELD_END] as? Number)?.toInt() ?: return null
    return ScheduleDay(dayKey = dayKey, range = TimeRange(start, end))
}

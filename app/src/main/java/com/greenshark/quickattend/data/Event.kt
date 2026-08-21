package com.greenshark.quickattend.data

/**
 * Geographic point used to validate that an attendant is physically at the
 * event. Kept optional because picking it on a map is not wired up yet.
 */
data class MapLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** Metres around the point that still count as "at the event". */
    val radiusMeters: Int = DEFAULT_RADIUS_METERS
) {
    companion object {
        const val DEFAULT_RADIUS_METERS = 75
    }
}

data class Event(
    /** Unique id of the event, also the QR payload. */
    val id: String = "",
    val title: String = "",
    val description: String = "",
    /** Person running the event, which is not always the organizer account. */
    val hostName: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    /** Human readable room or venue, e.g. "Salon C201". */
    val location: String = "",
    val address: String = "",
    val mapLocation: MapLocation? = null,
    /** Pro only: require the attendant to be inside [mapLocation] to check in. */
    val locationValidationEnabled: Boolean = false,
    val schedule: EventSchedule = EventSchedule(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

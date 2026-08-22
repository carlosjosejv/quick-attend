/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Where the attendant was standing when they scanned. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    /** How sure the phone is about the fix, in metres. */
    val accuracyMeters: Float = 0f
)

/**
 * Great circle distance between the event's point and where the scan happened.
 *
 * Haversine rather than [android.location.Location.distanceBetween], so the
 * rule that decides whether a check-in counts can be unit tested off device.
 */
fun MapLocation.distanceMetersTo(point: GeoPoint): Double {
    val latDelta = Math.toRadians(point.latitude - latitude)
    val lngDelta = Math.toRadians(point.longitude - longitude)
    val startLat = Math.toRadians(latitude)
    val endLat = Math.toRadians(point.latitude)

    val a = sin(latDelta / 2).let { it * it } +
            cos(startLat) * cos(endLat) * sin(lngDelta / 2).let { it * it }
    return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
}

/**
 * Whether the scan counts as happening at the venue.
 *
 * The phone's own margin of error is added to the radius, because a fix that is
 * only accurate to 40 m would otherwise lock out someone standing in the room
 * whenever the venue's radius is tighter than that. The margin is capped so a
 * hopeless fix cannot wave through someone across town.
 */
fun MapLocation.accepts(point: GeoPoint): Boolean =
    distanceMetersTo(point) <= radiusMeters + point.accuracyMeters.coerceIn(
        minimumValue = 0f,
        maximumValue = MAX_ACCURACY_ALLOWANCE_METERS
    )

/** "120 m" or "1.4 km", for telling someone why they were turned down. */
fun formatDistance(meters: Double, locale: Locale = Locale.getDefault()): String =
    if (meters < METERS_PER_KM) {
        "${meters.roundToInt()} m"
    } else {
        "%.1f km".format(locale, meters / METERS_PER_KM)
    }

/** Beyond this a fix is too vague to stretch the venue's radius any further. */
const val MAX_ACCURACY_ALLOWANCE_METERS = 50f

private const val EARTH_RADIUS_METERS = 6_371_008.8
private const val METERS_PER_KM = 1_000.0

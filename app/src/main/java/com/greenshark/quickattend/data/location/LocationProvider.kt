/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.location

import com.greenshark.quickattend.data.GeoPoint

/** Why a location fix could not be produced. */
enum class LocationError {
    /** The app was never granted permission, or it was taken away again. */
    PERMISSION_DENIED,

    /** Location is switched off on the phone entirely. */
    DISABLED,

    /** Nothing came back before the wait ran out, e.g. indoors with no signal. */
    UNAVAILABLE
}

sealed interface LocationResult {
    data class Success(val point: GeoPoint) : LocationResult
    data class Failure(val error: LocationError) : LocationResult
}

/**
 * Where the phone is right now. Behind an interface so the check-in rules can be
 * exercised in tests, and so the fused provider can replace the framework one
 * later without the scan screen noticing.
 */
interface LocationProvider {

    /** True when at least coarse location was granted, so nothing is asked for twice. */
    fun hasPermission(): Boolean

    /** True when location is switched on at all, which permission alone does not say. */
    fun isLocationEnabled(): Boolean

    /** One fix, or why there is none. Never throws on a missing permission. */
    suspend fun currentLocation(): LocationResult
}

/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import com.greenshark.quickattend.data.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * A single fix from the platform's own location manager.
 *
 * The framework provider is used rather than the fused one from Play services:
 * checking in needs one coordinate at one moment, not a continuous stream, and
 * this keeps the app installable on a phone with no Google services on it.
 */
class AndroidLocationProvider(
    context: Context,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) : LocationProvider {

    private val appContext = context.applicationContext

    private val locationManager: LocationManager? get() = appContext.getSystemService()

    override fun hasPermission(): Boolean = LOCATION_PERMISSIONS.any { permission ->
        ContextCompat.checkSelfPermission(appContext, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun isLocationEnabled(): Boolean =
        locationManager?.let(LocationManagerCompat::isLocationEnabled) == true

    override suspend fun currentLocation(): LocationResult {
        if (!hasPermission()) return LocationResult.Failure(LocationError.PERMISSION_DENIED)

        val manager = locationManager
            ?: return LocationResult.Failure(LocationError.UNAVAILABLE)
        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            return LocationResult.Failure(LocationError.DISABLED)
        }

        val fix = withTimeoutOrNull(timeoutMillis) {
            manager.enabledProviders().firstNotNullOfOrNull { provider ->
                manager.awaitCurrentLocation(provider)
            }
        } ?: manager.bestLastKnownLocation()

        return fix?.let { LocationResult.Success(it.toGeoPoint()) }
            ?: LocationResult.Failure(LocationError.UNAVAILABLE)
    }

    /** GPS first, because it is the only one accurate enough for a tight radius. */
    private fun LocationManager.enabledProviders(): List<String> =
        PREFERRED_PROVIDERS.filter { provider ->
            try {
                isProviderEnabled(provider)
            } catch (e: SecurityException) {
                false
            } catch (e: IllegalArgumentException) {
                // The phone does not have this provider at all.
                false
            }
        }

    private suspend fun LocationManager.awaitCurrentLocation(
        provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }

        try {
            LocationManagerCompat.getCurrentLocation(
                this,
                provider,
                signal,
                Executor(Runnable::run)
            ) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        } catch (e: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        } catch (e: IllegalArgumentException) {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    /** Last resort when no provider answers in time, e.g. deep inside a building. */
    private fun LocationManager.bestLastKnownLocation(): Location? = try {
        PREFERRED_PROVIDERS
            .mapNotNull { provider -> getLastKnownLocation(provider) }
            .filter { System.currentTimeMillis() - it.time <= MAX_LAST_KNOWN_AGE_MILLIS }
            .minByOrNull { it.accuracy }
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private companion object {
        val LOCATION_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val PREFERRED_PROVIDERS = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        /** Long enough for a cold GPS fix, short enough not to strand the scan. */
        const val DEFAULT_TIMEOUT_MILLIS = 12_000L

        /** Older than this and the phone may well have left the venue since. */
        const val MAX_LAST_KNOWN_AGE_MILLIS = 2 * 60 * 1000L
    }
}

private fun Location.toGeoPoint(): GeoPoint = GeoPoint(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = if (hasAccuracy()) accuracy else 0f
)

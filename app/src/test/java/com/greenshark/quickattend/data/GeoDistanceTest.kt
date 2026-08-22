package com.greenshark.quickattend.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GeoDistanceTest {

    /** Av. Máximo Gómez 72, Santo Domingo, with the default 75 m radius. */
    private val venue = MapLocation(latitude = 18.48604, longitude = -69.93121)

    @Test
    fun `the venue is zero metres from itself`() {
        val here = GeoPoint(latitude = venue.latitude, longitude = venue.longitude)
        assertEquals(0.0, venue.distanceMetersTo(here), 0.001)
    }

    @Test
    fun `a tenth of a degree of latitude is about eleven kilometres`() {
        val north = GeoPoint(latitude = venue.latitude + 0.1, longitude = venue.longitude)
        assertEquals(11_119.0, venue.distanceMetersTo(north), 5.0)
    }

    @Test
    fun `a point inside the radius is accepted`() {
        // Roughly 33 m north of the venue.
        val inside = GeoPoint(latitude = venue.latitude + 0.0003, longitude = venue.longitude)
        assertTrue(venue.accepts(inside))
    }

    @Test
    fun `a point well outside the radius is refused`() {
        val outside = GeoPoint(latitude = venue.latitude + 0.005, longitude = venue.longitude)
        assertFalse(venue.accepts(outside))
    }

    @Test
    fun `a vague fix stretches the radius by its own margin of error`() {
        // Roughly 111 m out, which is past the 75 m radius on its own.
        val point = GeoPoint(latitude = venue.latitude + 0.001, longitude = venue.longitude)
        assertFalse(venue.accepts(point))
        assertFalse(venue.accepts(point.copy(accuracyMeters = 20f)))
        assertTrue(venue.accepts(point.copy(accuracyMeters = 40f)))
    }

    @Test
    fun `a hopeless fix cannot stretch the radius without limit`() {
        val faraway = GeoPoint(
            latitude = venue.latitude + 0.01,
            longitude = venue.longitude,
            accuracyMeters = 5_000f
        )
        assertFalse(venue.accepts(faraway))
    }

    @Test
    fun `distances read in metres up close and kilometres far away`() {
        assertEquals("120 m", formatDistance(119.6, Locale.US))
        assertEquals("1.4 km", formatDistance(1_412.0, Locale.US))
    }
}

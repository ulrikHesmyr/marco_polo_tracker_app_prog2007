package com.example.marco_polo

import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class GeolocationUnitTest {

    @Test
    fun testCalculateDistance() {
        // Sample coordinates for known locations
        val lat1 = 40.748817  // Latitude for the Empire State Building
        val lon1 = -73.985428 // Longitude for the Empire State Building
        val lat2 = 40.689247  // Latitude for the Statue of Liberty
        val lon2 = -74.044502 // Longitude for the Statue of Liberty

        // Expected distance between Empire State Building and Statue of Liberty
        val expectedDistance = 8210f  // Approximately 8.2 kilometers

        val calculatedDistance = calculateDistance(lat1, lon1, lat2, lon2)

        // Allowing a margin of error due to slight variations in distance calculations
        val delta = 100f
        assertEquals(expectedDistance, calculatedDistance, delta)
    }

    @Test
    fun testCalculateBearing() {
        // Sample coordinates for known locations
        val lat1 = 40.748817  // Latitude for the Empire State Building
        val lon1 = -73.985428 // Longitude for the Empire State Building
        val lat2 = 40.689247  // Latitude for the Statue of Liberty
        val lon2 = -74.044502 // Longitude for the Statue of Liberty

        // Expected bearing (approximate)
        val expectedBearing = 199.8  // Approximate value in degrees

        val calculatedBearing = calculateBearing(lat1, lon1, lat2, lon2)

        // Allowing a margin of error due to slight variations in bearing calculations
        val delta = 0.5
        assertEquals(expectedBearing, calculatedBearing, delta)
    }
}

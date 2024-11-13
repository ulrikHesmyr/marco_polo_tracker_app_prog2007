package com.example.marco_polo

import org.junit.Test
import org.junit.Assert.assertEquals

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
        val expectedBearing = 217.0 // Approximate value in degrees
        println(expectedBearing)
        val calculatedBearing = calculateBearing(lat1, lon1, lat2, lon2)

        // Allowing a margin of error due to slight variations in bearing calculations
        val delta = 1.0  // Allowing for a slightly larger error margin
        assertEquals(expectedBearing, calculatedBearing, delta)
    }
}

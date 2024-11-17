package com.example.marco_polo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Calculates the bearing (direction) from one geographic coordinate to another.
 *
 * The bearing is calculated as the angle (in degrees) between the line connecting two points
 * on the Earth's surface and the north direction.
 *
 * @param lat1 Latitude of the starting point in decimal degrees.
 * @param lon1 Longitude of the starting point in decimal degrees.
 * @param lat2 Latitude of the destination point in decimal degrees.
 * @param lon2 Longitude of the destination point in decimal degrees.
 * @return The bearing angle in degrees (0° to 360°) relative to the north.
 */
fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    // Convert latitude and longitude from degrees to radians
    val lat1Rad = Math.toRadians(lat1)
    val lon1Rad = Math.toRadians(lon1)
    val lat2Rad = Math.toRadians(lat2)
    val lon2Rad = Math.toRadians(lon2)

    // Calculate the difference in longitude
    val deltaLon = lon2Rad - lon1Rad

    // Compute the components of the bearing
    val y = sin(deltaLon) * cos(lat2Rad)
    val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLon)

    // Calculate the initial bearing in radians and convert to degrees
    var bearing = Math.toDegrees(atan2(y, x))

    // Normalize the bearing to the range [0, 360]
    bearing = (bearing + 360) % 360

    return bearing
}
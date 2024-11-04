package com.example.marco_polo.compass

import kotlin.math.*

fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    // Convert latitude and longitude from degrees to radians
    val lat1Rad = Math.toRadians(lat1)
    val lon1Rad = Math.toRadians(lon1)
    val lat2Rad = Math.toRadians(lat2)
    val lon2Rad = Math.toRadians(lon2)

    // Calculate the difference in longitudes
    val deltaLon = lon2Rad - lon1Rad

    // Calculate the components of the bearing formula
    val y = sin(deltaLon) * cos(lat2Rad)
    val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLon)

    // Calculate the initial bearing in radians, then convert to degrees
    var bearing = Math.toDegrees(atan2(y, x))

    // Normalize the bearing to 0-360 degrees
    bearing = (bearing + 360) % 360

    return bearing
}

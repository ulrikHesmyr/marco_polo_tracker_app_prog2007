package com.example.marco_polo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    // Your existing calculateBearing implementation
    val lat1Rad = Math.toRadians(lat1)
    val lon1Rad = Math.toRadians(lon1)
    val lat2Rad = Math.toRadians(lat2)
    val lon2Rad = Math.toRadians(lon2)

    val deltaLon = lon2Rad - lon1Rad
    val y = sin(deltaLon) * cos(lat2Rad)
    val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLon)

    var bearing = Math.toDegrees(atan2(y, x))
    bearing = (bearing + 360) % 360

    return bearing
}
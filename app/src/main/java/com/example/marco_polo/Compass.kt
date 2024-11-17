package com.example.marco_polo

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Calculates the bearing from one geographic coordinate to another, relative to the geographical
 * north.
 *
 * The bearing is calculated as the angle (in degrees) between the two geolocations
 *
 * @param lat1 Latitude of the starting point, which will be the geolocation
 * of the current device.
 * @param lon1 Longitude of the starting point
 * @param lat2 Latitude of the destination point, which will be the geolocation
 * of the peer's device.
 * @param lon2 Longitude of the destination point
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

/**
 * Calculates the bearing to the peer's geolocation based on the bearing between the user's
 * geolocation and the peer's geolocation followed by calculating the bearing to the magnetic
 * north. Then the bearing to the magnetic north is subtracted from the bearing of the peer's
 * geolocations to return the angle of which the arrow of the main screen is pointing so that it
 * points towards the peer's location.
 *
 * @param event The sensor event containing new data.
 * @see calculateBearing
 */
fun MainActivity.calculateAngleDirection(event : SensorEvent){
    when (event.sensor.type) {
        Sensor.TYPE_ACCELEROMETER -> gravity = event.values
        Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values
    }

    if (gravity != null && geomagnetic != null) {
        val rotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)
        if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthInRadians = orientation[0]
            val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

            bearingToMagneticNorth = (azimuthInDegrees + 360) % 360

            val targetBearing = calculateBearing(myLocation.lat, myLocation.long, peerLocation.lat, peerLocation.long)
            angleDifference = ((targetBearing - bearingToMagneticNorth + 360) % 360).toFloat()
        }
    }
}
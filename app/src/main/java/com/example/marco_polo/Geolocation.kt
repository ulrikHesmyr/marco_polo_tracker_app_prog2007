package com.example.marco_polo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import org.json.JSONObject

/**
 * Checks if location permission is granted and requests it if not.
 *
 * If the permission is already granted, starts location updates.
 */
fun MainActivity.checkLocationPermission() {
    if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        startLocationUpdates()
    }
}

/**
 * Starts location updates using high-accuracy settings.
 *
 * Configures the location request and registers a callback to handle location updates.
 * Throws an exception if location permission is not granted.
 */
fun MainActivity.startLocationUpdates() {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, geolocationUpdateInterval)
        .setMinUpdateIntervalMillis(geolocationUpdateInterval / 2)
        .build()

    /**
     * Callback function invoked when a new location is available.
     */
    locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location? = locationResult.lastLocation
            location?.let {
                val lat1 = it.latitude
                val lon1 = it.longitude

                emitGeolocation(lat1, lon1)

                val lat2 = peerLocation.lat
                val lon2 = peerLocation.long

                val calculatedDistance = calculateDistance(lat1, lon1, lat2, lon2)
                myLocation = Geolocation(lat1, lon1)
                distance = calculatedDistance
            }
        }
    }

    if (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    } else {
        throw Exception("Location permission not granted. Can not request location updates.")
    }
}

/**
 * Calculates the distance between two geographical coordinates.
 *
 * @param lat1 Latitude of the first location.
 * @param lon1 Longitude of the first location.
 * @param lat2 Latitude of the second location.
 * @param lon2 Longitude of the second location.
 * @return Distance between the two coordinates in meters.
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

/**
 * Sends the current geolocation to the peer via a socket.
 *
 * @param lat Latitude of the current location.
 * @param lon Longitude of the current location.
 */
fun MainActivity.emitGeolocation(lat: Double, lon: Double) {
    val locationData = JSONObject().apply {
        put("latitude", lat)
        put("longitude", lon)
    }
    socket.emit("sent-geolocation", locationData)
}

/**
 * Stops location updates to conserve resources.
 */
fun MainActivity.stopLocationUpdates() {
    fusedLocationClient.removeLocationUpdates(locationCallback)
}

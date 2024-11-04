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

fun MainActivity.startLocationUpdates() {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
        .setMinUpdateIntervalMillis(5000L)
        .build()

    // Callback function which is being called upon at the interval of get geolocation
    locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location? = locationResult.lastLocation
            location?.let {
                val lat1 = it.latitude
                val lon1 = it.longitude

                // Emit the current location to the peer
                emitGeolocation(lat1, lon1)

                // Use peerLocation from SocketClient as the target coordinates
                val lat2 = peerLocation.lat
                val lon2 = peerLocation.long

                // Calculate the distance to the peer's coordinates
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
    }
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

fun MainActivity.emitGeolocation(lat: Double, lon: Double) {
    val locationData = JSONObject().apply {
        put("latitude", lat)
        put("longitude", lon)
    }
    socket.emit("sent-geolocation", locationData)
}

fun MainActivity.stopLocationUpdates() {
    fusedLocationClient.removeLocationUpdates(locationCallback)
}
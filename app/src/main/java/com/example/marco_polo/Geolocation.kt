package com.example.marco_polo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import org.json.JSONObject

/**
 * Checks if location permission is granted and requests it if not.
 *
 * If the permission is already granted, starts location updates.
 * @see startLocationUpdates
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
 * Throws an exception if high accuracy location permission is not granted. This can occur
 * even when the user has granted permission through the permission prompt, but accessing GPS
 * location is disabled in settings.
 */
fun MainActivity.startLocationUpdates() {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, geolocationUpdateInterval)
        .setMinUpdateIntervalMillis(geolocationUpdateInterval / 2)
        .build()

    /**
     * Callback function invoked when a new location is available which updates the geolocation of
     * the current user, emits it to the other peer and calculates the distance to the peer.
     *
     * @see emitGeolocation
     * @see calculateDistance
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
 * Calculates the distance between the two peer's geographical coordinates.
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
 * Constructs a JSON string of the user's geolocation and sends/emits it to the websocket server
 * which then broadcasts it to the peer's device.
 *
 * @param lat Latitude of the current geolocation.
 * @param lon Longitude of the current geolocation.
 */
fun MainActivity.emitGeolocation(lat: Double, lon: Double) {
    val locationData = JSONObject().apply {
        put("latitude", lat)
        put("longitude", lon)
    }
    socket.emit("sent-geolocation", locationData)
}

/**
 * Idempotent function that stops location updates.
 *
 * May be called upon even though location callback is already removed.
 */
fun MainActivity.stopLocationUpdates() {
    fusedLocationClient.removeLocationUpdates(locationCallback)
}

/**
 * Function that checks if Google Play Services are available on the device, which is necessary to
 * access the device's location.
 *
 * @return True if services are available, otherwise false.
 */
fun MainActivity.checkGooglePlayServices(): Boolean {
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
    return if (resultCode != ConnectionResult.SUCCESS) {
        if (googleApiAvailability.isUserResolvableError(resultCode)) {
            googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
        } else {
            Log.e("GooglePlayServices", "This device is not supported.")
            finish()
        }
        false
    } else {
        true
    }
}

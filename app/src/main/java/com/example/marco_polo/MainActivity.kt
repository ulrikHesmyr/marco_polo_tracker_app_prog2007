package com.example.marco_polo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.marco_polo.ui.theme.Marco_poloTheme
import com.google.android.gms.location.*
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class Geolocation(val lat: Double, val long: Double)

class MainActivity : ComponentActivity() {

    lateinit var socket: Socket
    //private val serverURL = "https://marco-polo-websocket-server.onrender.com/"
    private val serverURL = "https://marcopoloserver.rocks/"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    var distance by mutableFloatStateOf(0f)
    var peerLocation by mutableStateOf(Geolocation(0.0, 0.0))

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startLocationUpdates()
            } else {
                Log.e("Permission", "Location permission denied by user")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            socket = IO.socket(serverURL)
        } catch (e : Exception) {
            println(e.printStackTrace())
        }

        socket.connect()

        setContent {
            Marco_poloTheme {
                LandingScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
    }

    fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
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

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun emitGeolocation(lat: Double, lon: Double) {
        val locationData = JSONObject().apply {
            put("latitude", lat)
            put("longitude", lon)
        }
        socket.emit("sent-geolocation", locationData)
    }
}
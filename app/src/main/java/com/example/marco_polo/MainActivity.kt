package com.example.marco_polo

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.*

class Geolocation(val lat: Double, val long: Double)

class MainActivity : ComponentActivity(), SensorEventListener {

    lateinit var socket: Socket
    private val serverURL = "https://marcopoloserver.rocks/"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    var distance by mutableFloatStateOf(0f)
    var peerLocation by mutableStateOf(Geolocation(0.0, 0.0))

    // Compass variables
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    var bearingToMagneticNorth by mutableStateOf(0f)
    var angleDifference by mutableStateOf(0f)

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

        println("Angle between ${calculateBearing(60.806055, 10.674635, 60.806055, 10.668627)}")

        try {
            socket = IO.socket(serverURL)
        } catch (e: Exception) {
            println(e.printStackTrace())
        }

        socket.connect()

        // Initialize SensorManager for compass
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        setContent {
            Marco_poloTheme {
                LandingScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
        stopLocationUpdates()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        // Register compass sensors
        accelerometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> gravity = event.values
            Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values
        }

        if (gravity != null && geomagnetic != null) {
            val R = FloatArray(9)
            val I = FloatArray(9)
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                val azimuthInRadians = orientation[0] // azimuth in radians
                val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

                // Normalize to 0-360 degrees
                bearingToMagneticNorth = (azimuthInDegrees + 360) % 360

                // Use peerLocation from SocketClient as the target coordinates
                val targetBearing = calculateBearing(60.806055, 10.674635, 60.806055, 10.668627) // Example target coordinates
                angleDifference = ((targetBearing - bearingToMagneticNorth + 360) % 360).toFloat()
                println("Angle difference: $angleDifference")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Respond to accuracy changes if needed
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

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
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
}

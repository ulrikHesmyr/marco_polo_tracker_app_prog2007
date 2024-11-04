package com.example.marco_polo

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.marco_polo.ui.theme.Marco_poloTheme
import com.google.android.gms.location.*
import io.socket.client.IO
import io.socket.client.Socket

class Geolocation(val lat: Double, val long: Double)

class MainActivity : ComponentActivity(), SensorEventListener {

    lateinit var socket: Socket
    private val serverURL = "https://marcopoloserver.rocks/"
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var locationCallback: LocationCallback
    var distance by mutableFloatStateOf(0f)
    var peerLocation by mutableStateOf(Geolocation(0.0, 0.0))
    var myLocation by mutableStateOf(Geolocation(0.0, 0.0))

    // Compass variables
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    var bearingToMagneticNorth by mutableStateOf(0f)
    var angleDifference by mutableStateOf(0f)

    val requestPermissionLauncher =
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

        //println("Angle between ${calculateBearing(60.806055, 10.674635, 60.806055, 10.668627)}")

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
                val targetBearing = calculateBearing(myLocation.lat, myLocation.long, peerLocation.lat, peerLocation.long)
                angleDifference = ((targetBearing - bearingToMagneticNorth + 360) % 360).toFloat()
                println("Angle difference: $angleDifference")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
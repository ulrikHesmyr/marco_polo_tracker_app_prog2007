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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import io.socket.client.IO
import io.socket.client.Socket

class Geolocation(val lat: Double, val long: Double)

class MainActivity : ComponentActivity(), SensorEventListener {

    lateinit var socket: Socket
    private val serverURL = "http://www.marcopoloserver.rocks/"
    val geolocationUpdateInterval = 100L
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
    private var bearingToMagneticNorth by mutableFloatStateOf(0f)
    var angleDifference by mutableFloatStateOf(0f)

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
        if (checkGooglePlayServices()) {
            // Initialize location services
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            checkLocationPermission()
        }

        socket = IO.socket(serverURL)
        socket.connect()

        // Initialize SensorManager for compass
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        registerListeners()

        setContent {
            Marco_poloTheme {
                LandingScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.off()
        socket.disconnect()
        stopLocationUpdates()
        sensorManager.unregisterListener(this)
    }

    private fun checkGooglePlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            } else {
                Log.e("GooglePlayServices", "This device is not supported.")
                finish() // End the activity if services aren't available
            }
            false
        } else {
            true
        }
    }



    override fun onResume() {
        super.onResume()
        // Register compass sensors
        registerListeners()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

//        val currentTime = System.currentTimeMillis()
//        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
//            return // Skip this update if within the interval
//        }
//        lastUpdateTime = currentTime

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
                val azimuthInRadians = orientation[0] // azimuth in radians
                val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

                // Normalize to 0-360 degrees
                bearingToMagneticNorth = (azimuthInDegrees + 360) % 360

                // Use peerLocation from SocketClient as the target coordinates
                val targetBearing = calculateBearing(myLocation.lat, myLocation.long, peerLocation.lat, peerLocation.long)
                angleDifference = ((targetBearing - bearingToMagneticNorth + 360) % 360).toFloat()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun registerListeners(){
        accelerometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }
}
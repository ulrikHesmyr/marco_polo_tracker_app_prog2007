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
import com.example.marco_polo.ui.theme.MarcoPoloTheme
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import io.socket.client.IO
import io.socket.client.Socket

/**
 * Data class representing geographical coordinates.
 *
 * @param lat Latitude of the location.
 * @param long Longitude of the location.
 */
class Geolocation(val lat: Double, val long: Double)

/**
 * Main activity for the Marco Polo application.
 *
 * This activity handles geolocation, compass functionality, and socket communication.
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    lateinit var socket: Socket ///< Socket instance for server communication.
    private val serverURL = "http://www.marcopoloserver.rocks/" ///< Server URL for socket connection.
    val geolocationUpdateInterval = 100L ///< Interval for geolocation updates in milliseconds.
    lateinit var fusedLocationClient: FusedLocationProviderClient ///< Client for location updates.
    lateinit var locationCallback: LocationCallback ///< Callback for location updates.
    var distance by mutableFloatStateOf(0f) ///< Distance to the peer location.
    var peerLocation by mutableStateOf(Geolocation(0.0, 0.0)) ///< Current peer's location.
    var myLocation by mutableStateOf(Geolocation(0.0, 0.0)) ///< Current user's location.

    // Compass variables
    private lateinit var sensorManager: SensorManager ///< Manages device sensors.
    private var accelerometer: Sensor? = null ///< Accelerometer sensor.
    private var magnetometer: Sensor? = null ///< Magnetometer sensor.
    private var gravity: FloatArray? = null ///< Gravity data from accelerometer.
    private var geomagnetic: FloatArray? = null ///< Geomagnetic data from magnetometer.
    private var bearingToMagneticNorth by mutableFloatStateOf(0f) ///< Bearing to magnetic north.
    var angleDifference by mutableFloatStateOf(0f) ///< Angle difference to peer's direction.

    /**
     * Registers a permission launcher for location access.
     */
    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startLocationUpdates()
            } else {
                Log.e("Permission", "Location permission denied by user")
            }
        }

    /**
     * Called when the activity is created.
     *
     * Initializes location services, compass sensors, and socket communication.
     *
     * @param savedInstanceState The saved state of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkGooglePlayServices()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            checkLocationPermission()
        }

        socket = IO.socket(serverURL)
        socket.connect()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        registerListeners()

        setContent {
            MarcoPoloTheme {
                LandingScreen()
            }
        }
    }

    /**
     * Called when the activity is destroyed.
     *
     * Cleans up resources such as sockets, location updates, and sensor listeners.
     */
    override fun onDestroy() {
        super.onDestroy()
        socket.off()
        socket.disconnect()
        stopLocationUpdates()
        sensorManager.unregisterListener(this)
    }

    /**
     * Checks if Google Play Services are available.
     *
     * @return True if services are available, otherwise false.
     */
    private fun checkGooglePlayServices(): Boolean {
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

    /**
     * Called when the activity is resumed.
     *
     * Re-registers compass sensors.
     */
    override fun onResume() {
        super.onResume()
        registerListeners()
    }

    /**
     * Called when the activity is paused.
     *
     * Unregisters compass sensors.
     */
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    /**
     * Handles sensor data changes.
     *
     * Updates the angle difference to the peer based on compass and geolocation data.
     *
     * @param event The sensor event containing new data.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

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

    /**
     * Handles changes in sensor accuracy.
     *
     * @param sensor The sensor whose accuracy has changed.
     * @param accuracy The new accuracy of the sensor.
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Registers the accelerometer and magnetometer listeners.
     */
    private fun registerListeners() {
        accelerometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }
}

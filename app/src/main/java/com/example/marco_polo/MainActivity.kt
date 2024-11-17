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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.marco_polo.ui.theme.MarcoPoloTheme
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
    private val serverURL = "http://www.marcopoloserver.rocks/" ///< Server URL for peer connection connection.

    val geolocationUpdateInterval = 100L ///< Interval for geolocation updates in milliseconds.
    lateinit var fusedLocationClient: FusedLocationProviderClient ///< Client for location updates.
    lateinit var locationCallback: LocationCallback ///< Callback for location updates.
    var distance by mutableFloatStateOf(0f) ///< Distance to the peer location.
    var peerLocation by mutableStateOf(Geolocation(0.0, 0.0)) ///< Current peer's location.
    var myLocation by mutableStateOf(Geolocation(0.0, 0.0)) ///< Current user's location.
    var permittedLocation by mutableStateOf(true);

    // Compass variables
    private lateinit var sensorManager: SensorManager ///< Manages device sensors.
    private var accelerometer: Sensor? = null ///< Accelerometer sensor.
    private var magnetometer: Sensor? = null ///< Magnetometer sensor.
    var gravity: FloatArray? = null ///< Gravity data from accelerometer.
    var geomagnetic: FloatArray? = null ///< Geomagnetic data from magnetometer.
    var bearingToMagneticNorth by mutableFloatStateOf(0f) ///< Bearing to magnetic north.
    var angleDifference by mutableFloatStateOf(0f) ///< Angle difference to peer's direction.

    // Registers a permission launcher for location access.
    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->

            if (!isGranted) {
                permittedLocation = false;
            } else {
                permittedLocation = true;
            }
        }

    /**
     * Function which is called when activity is created. The function initializes the location API
     * client (if google play services is available on the device). In addition it initializes the
     * websocket connection, compass sensor manager and the UI.
     *
     * @param savedInstanceState The saved state of the activity.
     * @see checkGooglePlayServices
     * @see registerListeners
     * @see LandingScreen
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkGooglePlayServices()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
     * Cleans up resources by removing socket event listeners, stop location updates, and
     * unregister sensor listeners.
     * @see stopLocationUpdates
     */
    override fun onDestroy() {
        super.onDestroy()
        socket.off()
        socket.disconnect()
        stopLocationUpdates()
        sensorManager.unregisterListener(this)
    }

    /**
     * Called when the activity is resumed.
     *
     * Re-registers compass sensors.
     * @see registerListeners
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
     * Handles sensor data changes which will trigger the the function to calculate the angle of
     * the direction from the device's location with its coherent orientation to the peer's location
     *
     * @param event The sensor event containing new data.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        calculateAngleDirection(event);
    }

    /**
     * Required dummy function due to abstract SensorEventListener is an abstract class/interface
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

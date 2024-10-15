package com.example.marco_polo

import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marco_polo.ui.theme.Marco_poloTheme
import java.lang.Math.toDegrees

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var azimuthAngle by mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)

        setContent {
            Marco_poloTheme {
                AppNavigation(azimuthAngle)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }

        val success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthInRadians = orientationAngles[0]
            azimuthAngle = toDegrees(azimuthInRadians.toDouble()).toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }
}

// Navigation functionality between screens
@Composable
fun AppNavigation(azimuthAngle: Float) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "connect_screen") {
        composable("connect_screen") { ConnectScreen(navController) }
        composable("main_screen") { MainScreen(navController, azimuthAngle) }
    }
}

// Initial screen to connect
@Composable
fun ConnectScreen(navController: NavHostController) {
    // State to hold the text input
    var sessionId by remember { mutableStateOf("") }

    Scaffold(
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // TextField to input the session ID
                    OutlinedTextField(
                        value = sessionId,
                        onValueChange = { sessionId = it },
                        label = { Text("Enter session ID") },
                        placeholder = { Text("Enter session ID") },
                        singleLine = true,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Connect button
                    Button(
                        onClick = {
                            navController.navigate("main_screen")
                        }
                    ) {
                        Text(text = "Connect")
                    }
                }
            }
        }
    )
}

@Composable
fun MainScreen(navController: NavHostController, azimuthAngle: Float) {
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Connected with: User123", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF00FF00))
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight(0.8f)
                ) {
                    Text(text = "Direction to other person", fontSize = 20.sp, modifier = Modifier.padding(10.dp))

                    // Arrow/Compass Box with rotating arrow based on azimuthAngle
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↑",
                            fontSize = 150.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .graphicsLayer(rotationZ = azimuthAngle)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Distance placeholder text
                    Text(text = "Distance: 352 meters", fontSize = 16.sp)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Ping Button placeholder
                    Button(onClick = { /* Ping functionality */ }) {
                        Text(text = "Ping Button")
                    }
                }

                // Exit and Chat buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { navController.navigate("connect_screen") },
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(8.dp)
                    ) {
                        Text(text = "Exit")
                    }

                    Button(
                        onClick = { /* Chat functionality */ },
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(8.dp)
                    ) {
                        Text(text = "Chat")
                    }
                }
            }
        }
    )
}


// Preview Functions
@Preview(showBackground = true)
@Composable
fun ConnectScreenPreview() {
    Marco_poloTheme {
        ConnectScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Marco_poloTheme {
        MainScreen(navController = rememberNavController(), azimuthAngle = 0f)
    }
}
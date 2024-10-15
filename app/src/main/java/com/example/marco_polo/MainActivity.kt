package com.example.marco_polo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marco_polo.ui.theme.Marco_poloTheme
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Distance state moved here
    private var distance by mutableStateOf(0f)

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permission", "Location permission granted by user")
                startLocationUpdates()
            } else {
                Log.e("Permission", "Location permission denied by user")
            }
        }

    // LocationRequest and LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("Lifecycle", "MainActivity started")

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize LocationRequest and LocationCallback
        createLocationRequest()
        createLocationCallback()

        // Check location permission and request if not granted
        checkLocationPermission()

        setContent {
            Marco_poloTheme {
                AppNavigation(
                    distance = distance
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop location updates to prevent memory leaks
        stopLocationUpdates()
    }

    // Function to check if permission is granted
    private fun checkLocationPermission() {
        Log.d("Permission", "Checking location permissions")

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Permission", "Requesting location permission")
            requestLocationPermission()
        } else {
            Log.d("Permission", "Location permission granted")
            startLocationUpdates()
        }
    }

    // Function to request location permission
    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Adjusted createLocationRequest() function for older API
    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000L
            fastestInterval = 5000L
        }
    }

    // Function to create LocationCallback
    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location? = locationResult.lastLocation
                location?.let {
                    val lat1 = it.latitude
                    val lon1 = it.longitude

                    Log.d("Location", "Device location: lat=$lat1, lon=$lon1")

                    // Fixed target coordinates
                    val lat2 = 60.7902966
                    val lon2 = 10.6831604

                    // Calculate the distance to target coordinates
                    val calculatedDistance = calculateDistance(lat1, lon1, lat2, lon2)
                    Log.d("Location", "Calculated Distance: $calculatedDistance meters")

                    // Update the distance in the UI
                    distance = calculatedDistance
                } ?: run {
                    Log.e("LocationError", "Failed to retrieve location, location is null")
                }
            }
        }
    }

    // Function to start location updates
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationError", "Location permission not granted")
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    // Function to stop location updates
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Function to calculate the distance between two coordinates using Location.distanceBetween
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] // Returns the distance in meters
    }
}

// Navigation functionality between screens
@Composable
fun AppNavigation(distance: Float) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "connect_screen") {
        composable("connect_screen") { ConnectScreen(navController) }
        composable("main_screen") { MainScreen(navController, distance) }
    }
}

// Initial screen to connect
@Composable
fun ConnectScreen(navController: NavHostController) {
    // State to hold the session ID input
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
                            Log.d("ConnectScreen", "Connect button pressed")
                            // Navigate to the main screen
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

// Main Screen showing the distance
@Composable
fun MainScreen(navController: NavHostController, distance: Float) {
    Scaffold(
        topBar = {
            // Top bar showing connected user
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Connected with: User123",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
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
                    // Text field above the compass
                    Text(
                        text = "Direction to other person",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(10.dp)
                    )

                    // Arrow/Compass Box with centered arrow
                    Box(
                        modifier = Modifier
                            .size(200.dp) // Circle size
                            .background(Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↑",
                            fontSize = 200.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-50).dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dynamic distance display
                    Text(
                        text = "Distance: ${distance.toInt()} meters",
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Exit and Chat Buttons at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Exit Button
                    Button(
                        onClick = {
                            Log.d("MainScreen", "Exit button pressed")
                            navController.navigate("connect_screen")
                        },
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(8.dp)
                    ) {
                        Text(text = "Exit")
                    }

                    // Chat Button
                    Button(
                        onClick = { /* Chat functionality placeholder */ },
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
        MainScreen(navController = rememberNavController(), distance = 0f)
    }
}

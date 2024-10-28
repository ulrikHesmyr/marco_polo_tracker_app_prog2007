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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marco_polo.ui.theme.Marco_poloTheme
import com.example.marco_polo.socket_client.SocketClient
import com.google.android.gms.location.*
import androidx.activity.viewModels


class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val socketClient: SocketClient by viewModels()
    private var distance by mutableStateOf(0f)

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
        checkLocationPermission()

        setContent {
            Marco_poloTheme {
                AppNavigation()
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()

        socketClient.connect()

        NavHost(navController = navController, startDestination = "initial_screen") {
            composable("initial_screen") { InitialScreen(navController) }
            composable("create_screen") { CreateScreen(navController, socketClient) }
            composable("connect_screen") { ConnectScreen(navController, socketClient) }
            composable("main_screen") { MainScreen(navController, socketClient) }
        }

        DisposableEffect(Unit) {
            onDispose {
                socketClient.disconnect()
                stopLocationUpdates()
            }
        }
    }

    private fun checkLocationPermission() {
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

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location? = locationResult.lastLocation
                location?.let {
                    val lat1 = it.latitude
                    val lon1 = it.longitude

                    // Emit the current location to the peer
                    socketClient.emitGeolocation(lat1, lon1)

                    // Use peerLocation from SocketClient as the target coordinates
                    val lat2 = socketClient.peerLocation.value.lat
                    val lon2 = socketClient.peerLocation.value.long

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

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    @Composable
    fun InitialScreen(navController: NavHostController) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = { navController.navigate("create_screen") }) {
                Text("Create room")
            }
            Button(onClick = { navController.navigate("connect_screen") }) {
                Text("Connect to room")
            }
        }
    }

    @Composable
    fun CreateScreen(navController: NavHostController, socketClient: SocketClient) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Find your friend! Initialize a room to get a RoomID.")

            if (socketClient.roomID.value != "") {
                Text("Room created successfully, your room ID is: ${socketClient.roomID.value}")
            } else {
                Button(onClick = { socketClient.initializePeerConnection() }) {
                    Text("Create room")
                }
            }

            if (socketClient.peerConnected.value) {
                navController.navigate("main_screen")
            }
        }
    }

    @Composable
    fun ConnectScreen(navController: NavHostController, socketClient: SocketClient) {
        var sessionId by remember { mutableStateOf("") }

        if (socketClient.peerConnected.value) {
            navController.navigate("main_screen")
        }

        Scaffold(content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = sessionId,
                        onValueChange = { sessionId = it },
                        label = { Text("Enter session ID") },
                        placeholder = { Text("Enter session ID") },
                        singleLine = true,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    Button(
                        onClick = {
                            println("session/room ID is $sessionId")
                            socketClient.joinPeerConnection(sessionId)
                        }
                    ) {
                        Text("Connect")
                    }

                    if (socketClient.errorMessage.value != "") {
                        Text(socketClient.errorMessage.value)
                    }
                }
            }
        })
    }

    @Composable
    fun MainScreen(navController: NavHostController, socketClient: SocketClient) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Connected room: ${socketClient.roomID.value}",
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
                        Text("Direction to other person", fontSize = 20.sp, modifier = Modifier.padding(10.dp))

                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.White, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("↑", fontSize = 200.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-50).dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text("Distance: ${distance.toInt()} meters", fontSize = 16.sp)

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { socketClient.leaveRoom(); navController.navigate("initial_screen") },
                            modifier = Modifier.wrapContentSize().padding(8.dp)
                        ) {
                            Text("Exit")
                        }
                    }
                }
            }
        )
    }
}

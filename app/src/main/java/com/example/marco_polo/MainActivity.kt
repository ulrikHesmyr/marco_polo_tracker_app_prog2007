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
import com.example.marco_polo.ui.theme.Marco_poloTheme
import com.google.android.gms.location.*
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class Geolocation(val lat: Double, val long: Double)

class MainActivity : ComponentActivity() {

    private lateinit var socket: Socket
    //private val serverURL = "https://marco-polo-websocket-server.onrender.com/"
    private val serverURL = "https://marcopoloserver.rocks/"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var distance by mutableFloatStateOf(0f)
    private var peerLocation by mutableStateOf(Geolocation(0.0, 0.0))

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
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
    }

    @Composable
    fun AppNavigation() {
        var roomID by remember { mutableStateOf("") }
        var peersConnected by remember { mutableStateOf(false)}
        var onCreateScreen by remember {mutableStateOf(false)}
        var onConnectScreen by remember {mutableStateOf(false)}

        //Adding socket event listeners
        LaunchedEffect(Unit) {

            // Event listener function when peer connection is established
            socket.on("peers-connected") { args ->
                peersConnected = true
                if(args.isNotEmpty()){
                    roomID = args[0] as String
                }

                // Starting the location emittance
                checkLocationPermission()
            }
            socket.on("peer-disconnected") { _ ->
                peersConnected = false

                // Stopping the location emittance
                stopLocationUpdates()
            }
        }

        if (peersConnected){
            MainScreen(roomID=roomID, leaveRoom = {peersConnected = false; onCreateScreen = false; onConnectScreen = false; roomID = ""; stopLocationUpdates()})
        } else if(!onConnectScreen && !onCreateScreen){
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = { onCreateScreen = true}) {
                    Text("Create room")
                }
                Button(onClick = { onConnectScreen = true }) {
                    Text("Connect to room")
                }
            }
        } else if (onConnectScreen){
            ConnectScreen(back = {onConnectScreen = false})
        } else {
            CreateScreen(back = {onCreateScreen = false}, roomID=roomID, updateRoomID = {input -> roomID = input})
        }

        // Clean up when composable is disposed
        DisposableEffect(Unit) {
            onDispose {

                //Idempotent function that is being called in case the app is closed but the socket is not disconnected yet
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

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    @Composable
    fun CreateScreen(back: () -> Unit, roomID : String, updateRoomID : (String) -> Unit) {

        LaunchedEffect(Unit) {
            socket.on("room-created") { args ->
                if (args.isNotEmpty()) {
                    updateRoomID(args[0] as String)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Find your friend! Initialize a room to get a RoomID.")

            if (roomID != "") {
                Text("Room created successfully, your room ID is: $roomID")
            }
            Row {
                Button(onClick = {back()}){
                    Text("Return")
                }
                if (roomID == ""){
                    Button(onClick = { socket.emit("initialize-peer-connection") }) {
                        Text("Create room")
                    }
                }
            }


            }
        }

    @Composable
    fun ConnectScreen(back : () -> Unit) {
        var sessionId by remember { mutableStateOf("") }
        var roomConnectionError by remember {mutableStateOf("")}

        LaunchedEffect(Unit) {
            socket.on("error") {args ->
                if(args.isNotEmpty()){
                    roomConnectionError = args[0] as String
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
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
                Text(roomConnectionError)
                Row {
                    Button(onClick = {back()}){
                        Text("Return")
                    }
                    Button(
                        onClick = {
                            socket.emit("join-peer-connection", sessionId)
                        }
                    ) {
                        Text("Connect")
                    }
                }

            }
        }
    }

    @Composable
    fun MainScreen(roomID : String, leaveRoom : () -> Unit) {

        LaunchedEffect(Unit){
            socket.on("got-geolocation") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val latitude = data.getDouble("latitude")
                    val longitude = data.getDouble("longitude")
                    peerLocation = Geolocation(latitude, longitude)
                }
            }
        }
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
                        text = "Connected room: $roomID",
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
                            onClick = { socket.emit("leave-room"); leaveRoom() },
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(8.dp)
                        ) {
                            Text("Exit")
                        }
                    }
                }
            }
        )
    }
    fun emitGeolocation(lat: Double, lon: Double) {
        val locationData = JSONObject().apply {
            put("latitude", lat)
            put("longitude", lon)
        }
        socket.emit("sent-geolocation", locationData)
    }
}



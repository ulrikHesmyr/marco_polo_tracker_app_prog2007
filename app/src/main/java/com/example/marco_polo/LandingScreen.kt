package com.example.marco_polo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marco_polo.ui.theme.MarcoPoloTheme

/**
 * Composable function for the landing screen of the Marco Polo app.
 *
 * This screen manages the main navigation logic and user interactions for creating
 * or connecting to a room, as well as handling peer connections.
 */
@Composable
fun MainActivity.LandingScreen() {
    // State variables to track the room ID, peer connections, and screen navigation
    var roomID by remember { mutableStateOf("") }
    var peersConnected by remember { mutableStateOf(false) }
    var onCreateScreen by remember { mutableStateOf(false) }
    var onConnectScreen by remember { mutableStateOf(false) }

    /**
     * Adds a socket event listener for the "peers-connected" event which indicates that the
     * peer connection is successfully established.
     *
     * Adds a socket event listener for the "peers-disconnected" event which indicates the the peer
     * has either left the room, lost connection or closed the app.
     *
     * Wrapped in a `LaunchedEffect` composable to prevent duplicate event listeners on re-renders.
     */
    LaunchedEffect(Unit) {

        /**
         * The socket event listener callback updates the room ID and set the peersConnected flag so
         * that the user is navigated to the main screen.
         *
         * When the peer connection is established, we also want to start emitting the geolocation of
         * the device and therefore start the geolocation emitting procedure which firstly need to check
         * the permissions.
         *
         * @see checkLocationPermission
         */
        socket.on("peers-connected") { args ->
            peersConnected = true
            if (args.isNotEmpty()) {
                roomID = args[0] as String
            }

            // Start emitting location updates
            checkLocationPermission()
        }

        /**
         * The socket event listener callback of the "peer-disconnected" event updates the
         * peersConnected flag so that the user is navigated back to the previous screen
         * (either the connect or the create room screen).
         *
         * Stops the geolocation emittance to the peer due to disconnection
         *
         * @see stopLocationUpdates
         */
        socket.on("peer-disconnected") { _ ->
            peersConnected = false

            // Stop emitting location updates
            stopLocationUpdates()
        }
    }

    /**
     * Applies the app theme and manages the UI structure.
     */
    MarcoPoloTheme(dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                peersConnected -> {
                    // Main screen displayed when peers are connected
                    MainScreen(
                        roomID = roomID,
                        leaveRoom = {
                            peersConnected = false
                            onCreateScreen = false
                            onConnectScreen = false
                            roomID = ""
                            stopLocationUpdates()
                        }
                    )
                }
                !onConnectScreen && !onCreateScreen -> {
                    // Default landing page for creating or connecting to a room
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Marco Polo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 50.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(horizontal = 25.dp)
                                .fillMaxWidth(),
                            thickness = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onCreateScreen = true },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Create room", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onConnectScreen = true },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Connect to room", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                onConnectScreen -> {
                    // Connect screen for joining a room with a callback function to navigate back
                    ConnectScreen(back = { onConnectScreen = false })
                }
                else -> {
                    // Create screen for creating a new room with callback functions
                    CreateScreen(
                        back = { onCreateScreen = false },
                        roomID = roomID,
                        updateRoomID = { input -> roomID = input }
                    )
                }
            }
        }
    }

    /**
     * Stops location updates when the composable is disposed.
     */
    DisposableEffect(Unit) {
        onDispose {
            stopLocationUpdates()
        }
    }
}

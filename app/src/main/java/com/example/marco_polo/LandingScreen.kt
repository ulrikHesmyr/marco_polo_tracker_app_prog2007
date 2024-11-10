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

@Composable
fun MainActivity.LandingScreen() {
    var roomID by remember { mutableStateOf("") }
    var peersConnected by remember { mutableStateOf(false) }
    var onCreateScreen by remember { mutableStateOf(false) }
    var onConnectScreen by remember { mutableStateOf(false) }

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

    MarcoPoloTheme (dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.background) {
            if (peersConnected) {
                MainScreen(roomID=roomID, leaveRoom = {
                    peersConnected = false
                    onCreateScreen = false
                    onConnectScreen = false
                    roomID = ""
                    stopLocationUpdates()
                })
            } else if (!onConnectScreen && !onCreateScreen) {
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
            } else if (onConnectScreen) {
                ConnectScreen(back = { onConnectScreen = false })
            } else {
                CreateScreen(back = { onCreateScreen = false }, roomID = roomID, updateRoomID = { input -> roomID = input })
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopLocationUpdates()
        }
    }
}
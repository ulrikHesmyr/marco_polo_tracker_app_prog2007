package com.example.marco_polo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
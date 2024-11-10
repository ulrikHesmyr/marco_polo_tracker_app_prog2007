package com.example.marco_polo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainActivity.CreateScreen(back: () -> Unit, roomID : String, updateRoomID : (String) -> Unit) {

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
        if (roomID != "") {
            Text(
                text = "Room created successfully, your room ID is: $roomID",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = "Find your friend! Initialize a room to get a RoomID.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row {
            Button(
                onClick = { back() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Return", color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (roomID == "") {
                Button(
                    onClick = { socket.emit("initialize-peer-connection") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Create room", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
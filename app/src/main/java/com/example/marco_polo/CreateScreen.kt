package com.example.marco_polo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable function for the "Create Room" screen.
 *
 * This screen allows users to create a new room and displays the generated room ID once the room
 * is created.
 *
 * @param back A callback function triggered when the "Return" button is pressed to navigate the
 * user back to the landing screen.
 * @param roomID The ID of the room if it has been created, otherwise an empty string.
 * @param updateRoomID A callback function to update the room ID when it is received from the server.
 */
@Composable
fun MainActivity.CreateScreen(back: () -> Unit, roomID: String, updateRoomID: (String) -> Unit) {

    /**
     * Adds a socket listener for the "room-created" event.
     *
     * This listener updates the room ID when the server emits the "room-created" event.
     */
    LaunchedEffect(Unit) {
        socket.on("room-created") { args ->
            if (args.isNotEmpty()) {
                updateRoomID(args[0] as String)
            }
        }
    }

    /**
     * Main UI layout for the "Create Room" screen.
     */
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display the room ID if it exists, otherwise show description
        if (roomID != "") {
            Text(
                text = "Room created successfully, your room ID is:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = roomID,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = "Find your friend! Initialize a room to get a room ID.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Buttons for navigation and creating a room
        Row {
            Button(
                // Onclick event listener to go back to the landing screen
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
                    // Emit event to initialize a room
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

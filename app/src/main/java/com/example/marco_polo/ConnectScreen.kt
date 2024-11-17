package com.example.marco_polo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Composable function for the "Connect to Room" screen.
 *
 * This screen allows users to input a room ID to connect to an existing room.
 * It handles errors if the connection fails and provides navigation back to the previous screen.
 *
 * @param back A callback function triggered when the "Return" button is pressed.
 */
@Composable
fun MainActivity.ConnectScreen(back: () -> Unit) {
    // State variables for session ID input and connection error messages
    var sessionId by remember { mutableStateOf("") } ///< Room ID entered by the user.
    var roomConnectionError by remember { mutableStateOf("") } ///< Error message for connection failures.

    /**
     * Adds a socket event listener for the "error" event.
     *
     * Updates the error message state when the server emits an "error" event.
     * Wrapped in a `LaunchedEffect` to prevent duplicate event listeners on re-renders.
     */
    LaunchedEffect(Unit) {
        socket.on("error") { args ->
            if (args.isNotEmpty()) {
                roomConnectionError = args[0] as String
            }
        }
    }

    /**
     * Main UI layout for the "Connect to Room" screen.
     */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Prompt to enter room ID
            Text(text = "Enter room ID", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            // Input field for room ID
            OutlinedTextField(
                value = sessionId,
                onValueChange = { sessionId = it },
                placeholder = { Text("Enter room ID", color = MaterialTheme.colorScheme.onSecondary) },
                singleLine = true,
                modifier = Modifier
                    .padding(bottom = 20.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Display error message if connection fails
            Text(
                text = roomConnectionError,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )

            // Buttons for navigation and connecting to a room
            Row {
                Button(
                    onClick = { back() }, // Navigate back to the previous screen
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Return", color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        socket.emit("join-peer-connection", sessionId) // Emit event to join the room
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Connect", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

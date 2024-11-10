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

@Composable
fun MainActivity.ConnectScreen(back: () -> Unit) {
    var sessionId by remember { mutableStateOf("") }
    var roomConnectionError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        socket.on("error") { args ->
            if (args.isNotEmpty()) {
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
                placeholder = { Text("Enter RoomID", color = MaterialTheme.colorScheme.onSecondary) },
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
            Text(
                text = roomConnectionError,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
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
                Button(
                    onClick = {
                        socket.emit("join-peer-connection", sessionId)
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
package com.example.marco_polo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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


@Composable
fun MainActivity.ConnectScreen(back : () -> Unit) {
    var sessionId by remember { mutableStateOf("") }
    var roomConnectionError by remember { mutableStateOf("") }

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
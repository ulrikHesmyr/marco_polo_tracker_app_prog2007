package com.example.marco_polo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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
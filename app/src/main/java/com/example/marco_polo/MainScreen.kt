package com.example.marco_polo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
fun MainActivity.MainScreen(roomID : String, leaveRoom : () -> Unit) {

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
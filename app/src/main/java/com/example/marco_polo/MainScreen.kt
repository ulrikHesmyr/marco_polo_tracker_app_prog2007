package com.example.marco_polo

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import androidx.compose.material3.ButtonDefaults
import com.example.marco_polo.ui.theme.MarcoPoloTheme

/**
 * Displays the main screen of the Marco Polo app with distance and direction to peer. The screen
 * is visible only when the user is connected to a room where there is another user that
 * is connected.
 *
 * @param roomID The ID of the peer connection room the user is connected to.
 * @param leaveRoom A callback function triggered when the user exits the room.
 */
@Composable
fun MainActivity.MainScreen(roomID: String, leaveRoom: () -> Unit) {

    // Establish a socket listener for receiving geolocation data
    LaunchedEffect(Unit) {
        socket.on("got-geolocation") { args ->

            // Parses the JSON data received by the peer and updates the peerLocation data member
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val latitude = data.getDouble("latitude")
                val longitude = data.getDouble("longitude")
                peerLocation = Geolocation(latitude, longitude)
            }
        }
    }

    // Applies the Marco Polo app theme and sets up the screen layout.
    MarcoPoloTheme(dynamicColor = false) {
        Scaffold(

            // The top bar showing the connected room's ID.
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Connected room: $roomID",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },

            // The main content of the screen, including direction and distance information.
            content = { padding ->


                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight(0.8f)
                    ) {

                        /**
                         * Displays the direction and distance to peer geolocation
                         * if the user has permitted location access to the app
                         */
                        if(!permittedLocation){
                            Text(
                                "The app requires to access your location, please grant access in settings",
                                fontSize = 25.sp,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                "Direction to peer",
                                fontSize = 25.sp,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            // Displays an arrow pointing towards the peer's direction.
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(MaterialTheme.colorScheme.surface, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.arrow_image),
                                    contentDescription = "Arrow to point direction",
                                    modifier = Modifier.graphicsLayer(rotationZ = angleDifference)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Displays the distance to the peer in meters.
                            Text(
                                "Distance:",
                                fontSize = 25.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            Text(
                                "${distance.toInt()} meters",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }


                    // Displays a button to leave the room.
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
                                .padding(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Exit", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }


            }
        )
    }
}

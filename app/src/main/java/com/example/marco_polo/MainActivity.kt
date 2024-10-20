package com.example.marco_polo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marco_polo.ui.theme.Marco_poloTheme
import com.example.marco_polo.socket_client.SocketClient

class MainActivity : ComponentActivity() {

    private lateinit var socketClient: SocketClient
    private val WEBSOCKET_URL = "https://marco-polo-websocket-server.onrender.com/" //"http://10.0.2.2:3000" //"http://10.212.174.70:80"

    override fun onCreate(savedInstanceState: Bundle?) {
        socketClient = SocketClient()
        socketClient.initializeSocket(WEBSOCKET_URL)
        socketClient.connect()
        super.onCreate(savedInstanceState)
        setContent {
            Marco_poloTheme {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socketClient.disconnect()
    }

    // Navigation functionality between screens
    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "connect_screen") {
            composable("connect_screen") { ConnectScreen(navController) }
            composable("main_screen") { MainScreen(navController) }
        }
    }

    // Initial screen to connect
    @Composable
    fun ConnectScreen(navController: NavHostController) {
        // State to hold the text input
        var sessionId by remember { mutableStateOf("") }

        Scaffold(
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // TextField to input the session ID
                        OutlinedTextField(
                            value = sessionId,
                            onValueChange = { sessionId = it },
                            label = { Text("Enter session ID") },
                            placeholder = { Text("Enter session ID") },
                            singleLine = true,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // Connect button
                        Button(
                            onClick = {
                                println("session/room ID is $sessionId")
                                socketClient.joinPeerConnection(sessionId)
                            //navController.navigate("main_screen")
                            }
                        ) {
                            Text(text = "Connect")
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun MainScreen(navController: NavHostController) {
        var socketClient = SocketClient()
        Scaffold(
            topBar = {
                // Top bar showing connected user
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Connected with: User123", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                        // Text field above the compass
                        Text(text = "Direction to other person", fontSize = 20.sp, modifier = Modifier.padding(10.dp))

                        // Arrow/Compass Box with centered arrow
                        Box(
                            modifier = Modifier
                                .size(200.dp) // Circle size
                                .background(Color.White, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↑",
                                fontSize = 200.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(y = (-50).dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Distance (Placeholder text)
                        Text(text = "Distance: 352 meters", fontSize = 16.sp)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Ping Button (Placeholder)
                        Button(onClick = { /* Ping functionality placeholder */ }) {
                            Text(text = "Ping Button")
                        }
                    }

                    // Exit and Chat Buttons at the bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Exit Button
                        Button(
                            onClick = { navController.navigate("connect_screen") },
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(8.dp)
                        ) {
                            Text(text = "Exit")
                        }

                        // Chat Button
                        Button(
                            onClick = { /* Chat functionality placeholder */ },
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(8.dp)
                        ) {
                            Text(text = "Chat")
                        }
                    }
                }
            }
        )
    }


    // Preview Functions
    @Preview(showBackground = true)
    @Composable
    fun ConnectScreenPreview() {
        Marco_poloTheme {
            ConnectScreen(navController = rememberNavController())
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun MainScreenPreview() {
        Marco_poloTheme {
            MainScreen(navController = rememberNavController())
        }
    }
}






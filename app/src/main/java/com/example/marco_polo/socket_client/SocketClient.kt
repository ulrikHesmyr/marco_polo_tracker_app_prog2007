package com.example.marco_polo.socket_client

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

class Geolocation(val lat : Double, val long : Double)

class SocketClient : ViewModel() {

    private lateinit var socket: Socket
    private val geolocationEmitDelaySeconds : Long = 2
    private val serverURL = "https://marco-polo-websocket-server.onrender.com/"
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var geolocationInterval: Runnable


    var roomID = mutableStateOf("")
    var peerConnected = mutableStateOf(false)
    var peerLocation = mutableStateOf(Geolocation(0.0,0.0))
    var errorMessage = mutableStateOf("")
        private set

    // Function to establish a websocket connection to the socket.io server and add event listeners
    fun connect(){
        try {
            socket = IO.socket(serverURL) // Set server URL
            setupSocketListeners() // Set
            socket.connect()
        } catch (e : Exception){
            e.printStackTrace()
        }
    }

    // Function to be called when composable is de-rendered to remove all socket event listeners and close connection to socket server
    fun disconnect() {
        // Ensure socket is initialized before proceeding
        if (::socket.isInitialized) {
            // Remove listeners before disconnecting, to avoid any issues during disconnection
            socket.off("room-created", onRoomCreated)
            socket.off("peers-connected", onPeersConnected)
            socket.off("got-geolocation", onGotGeoLocation)
            socket.off("peer-disconnected", onPeerDisconnected)
            socket.off("error", onError)

            // Now check if socket is connected and disconnect if true
            if (socket.connected()) {
                socket.disconnect()
            }
        }
    }

    // Function to handle "room-created" event
    private val onRoomCreated = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            roomID.value = args[0] as String
        }
    }

    // Event listener function to handle when a peer connection is established
    private val onPeersConnected = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            peerConnected.value = true
            roomID.value = args[0] as String
            startGeolocationEmit()
        }
    }

    // Updating the peer's geolocation
    private val onGotGeoLocation = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            val data = args[0] as JSONObject
            val latitude = data.getDouble("latitude")
            val longitude = data.getDouble("longitude")
            peerLocation.value = Geolocation(latitude, longitude)
            println("$latitude, $longitude")
        }
    }

    // Event listener function for when the peer has disconnected from the peer connection stopping the emit interval and handle state of peer connection for UI
    private val onPeerDisconnected = Emitter.Listener { _ ->
        peerConnected.value = false
        stopGeolocationEmit()
    }

    // Event listener function to handle if the room does not exist or if it is full
    private val onError = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            errorMessage.value = args[0] as String
        }
    }

    // Set up the socket event listeners
    private fun setupSocketListeners() {

        socket.on("room-created", onRoomCreated)
        socket.on("peers-connected", onPeersConnected)
        socket.on("got-geolocation", onGotGeoLocation)
        socket.on("peer-disconnected", onPeerDisconnected)
        socket.on("error", onError)
    }

    // Function to start emitting the geolocation to the peer when the connection is established
    private fun startGeolocationEmit() {
        val locationData = JSONObject()
        locationData.put("latitude", 40.7128)  // Example latitude
        locationData.put("longitude", -74.0060)  // Example longitude

        geolocationInterval = object : Runnable {
            override fun run() {
                socket.emit("sent-geolocation", locationData)
                handler.postDelayed(this, geolocationEmitDelaySeconds * 1000)
            }
        }

        // Start emitting geolocation data
        handler.post(geolocationInterval)

    }

    private fun stopGeolocationEmit(){
        handler.removeCallbacks(geolocationInterval)
    }

    // Emit the initialize-peer-connection event
    fun initializePeerConnection() {
        socket.emit("initialize-peer-connection")
    }

    // Emit the join-peer-connection event
    fun joinPeerConnection(roomId: String) {
        socket.emit("join-peer-connection", roomId)
    }

    // Leave the room
    fun leaveRoom() {
        roomID.value = ""
        peerConnected.value = false
        socket.emit("leave-room")
    }
}


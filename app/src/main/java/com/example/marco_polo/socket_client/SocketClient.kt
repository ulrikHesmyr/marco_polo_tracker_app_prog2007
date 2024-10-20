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


    fun connect(){
        try {
            socket = IO.socket(serverURL) // Set server URL
            setupSocketListeners() // Set
            socket.connect()
        } catch (e : Exception){
            e.printStackTrace()
        }
    }

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

    private val onPeersConnected = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            peerConnected.value = true
            roomID.value = args[0] as String
            startGeolocationEmit()
        }
    }

    private val onGotGeoLocation = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            val data = args[0] as JSONObject
            val latitude = data.getDouble("latitude")
            val longitude = data.getDouble("longitude")
            peerLocation.value = Geolocation(latitude, longitude)
            println("$latitude, $longitude")
        }
    }

    private val onPeerDisconnected = Emitter.Listener { _ ->
        peerConnected.value = false
        stopGeolocationEmit()
    }

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

    // Start emitting geolocation every 5 seconds
    private fun startGeolocationEmit() {
        val locationData = JSONObject()
        locationData.put("latitude", 40.7128)  // Example latitude
        locationData.put("longitude", -74.0060)  // Example longitude

        geolocationInterval = object : Runnable {
            override fun run() {
                socket.emit("sent-geolocation", locationData)
                handler.postDelayed(this, geolocationEmitDelaySeconds * 1000) // Emit every 5 seconds
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
        socket.emit("leave-room")
    }
}


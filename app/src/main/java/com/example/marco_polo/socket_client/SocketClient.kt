package com.example.marco_polo.socket_client

import android.os.Handler
import android.os.Looper
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URISyntaxException

class SocketClient {

    private lateinit var socket: Socket
    private val handler = Handler(Looper.getMainLooper()) // To run code on the main thread
    private val geolocationEmitDelaySeconds : Long = 2;

    // Initialize the Socket.IO client
    fun initializeSocket(serverUrl: String) {
        println("Initializing socket connection")
        try {
            socket = IO.socket(serverUrl) // Set server URL
            setupSocketListeners() // Setup event listeners
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun connect(){
        socket.connect()
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
            val roomID = args[0] as String
            handler.post {
                // Use the roomID here
                println("Room created: $roomID")
                // You can also add a callback or pass the roomID to the MainActivity
            }
        }
    }

    private val onPeersConnected = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            val connectedRoomID = args[0] as String
            handler.post {
                println("Peer connected in room: $connectedRoomID")
                startGeolocationEmit() // Start emitting geolocation
            }
        }
    }

    private val onGotGeoLocation = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            val data = args[0] as JSONObject
            val latitude = data.getDouble("latitude")
            val longitude = data.getDouble("longitude")
            handler.post {
                println("Peer location: Latitude $latitude, Longitude $longitude")
            }
        }
    }

    private val onPeerDisconnected = Emitter.Listener { _ ->
        handler.post {
            println("Peer disconnected.")
        }
    }

    private val onError = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            val errorMessage = args[0] as String
            handler.post {
                println("Error: $errorMessage")
            }
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

        val geolocationInterval = object : Runnable {
            override fun run() {
                socket.emit("sent-geolocation", locationData)
                handler.postDelayed(this, geolocationEmitDelaySeconds * 1000) // Emit every 5 seconds
            }
        }

        // Start emitting geolocation data
        handler.post(geolocationInterval)
    }

    // Emit the initialize-peer-connection event
    fun initializePeerConnection() {
        socket.emit("initialize-peer-connection")
    }

    // Emit the join-peer-connection event
    fun joinPeerConnection(roomID: String) {
        socket.emit("join-peer-connection", roomID)
    }

    // Leave the room
    fun leaveRoom() {
        socket.emit("leave-room")
    }
}


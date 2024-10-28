package com.example.marco_polo.socket_client

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

class Geolocation(val lat: Double, val long: Double)

class SocketClient : ViewModel() {

    private lateinit var socket: Socket
    private val geolocationEmitDelaySeconds: Long = 2
    private val serverURL = "https://marco-polo-websocket-server.onrender.com/"
    private val handler = Handler(Looper.getMainLooper())

    var roomID = mutableStateOf("")
    var peerConnected = mutableStateOf(false)
    var peerLocation = mutableStateOf(Geolocation(0.0, 0.0))
    var errorMessage = mutableStateOf("")
        private set

    // Function to establish a websocket connection
    fun connect() {
        try {
            socket = IO.socket(serverURL)
            setupSocketListeners()
            socket.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Disconnect from the server
    fun disconnect() {
        if (::socket.isInitialized) {
            socket.off("room-created", onRoomCreated)
            socket.off("peers-connected", onPeersConnected)
            socket.off("got-geolocation", onGotGeoLocation)
            socket.off("peer-disconnected", onPeerDisconnected)
            socket.off("error", onError)
            if (socket.connected()) {
                socket.disconnect()
            }
        }
    }

    private val onRoomCreated = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            roomID.value = args[0] as String
        }
    }

    private val onPeersConnected = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            if (!peerConnected.value) {
                peerConnected.value = true
            }
            if (roomID.value != args[0] as String) {
                roomID.value = args[0] as String
            }
            startGeolocationEmit()
        }
    }

    private val onGotGeoLocation = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            val data = args[0] as JSONObject
            val latitude = data.getDouble("latitude")
            val longitude = data.getDouble("longitude")
            if (!(peerLocation.value.lat == latitude && peerLocation.value.long == longitude)) {
                peerLocation.value = Geolocation(latitude, longitude)
            }
        }
    }

    private val onPeerDisconnected = Emitter.Listener {
        if (peerConnected.value) {
            peerConnected.value = false
            stopGeolocationEmit()
        }
    }

    private val onError = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            errorMessage.value = args[0] as String
        }
    }

    // Set up socket event listeners
    private fun setupSocketListeners() {
        socket.on("room-created", onRoomCreated)
        socket.on("peers-connected", onPeersConnected)
        socket.on("got-geolocation", onGotGeoLocation)
        socket.on("peer-disconnected", onPeerDisconnected)
        socket.on("error", onError)
    }

    // Function to emit dynamic geolocation to the server
    fun emitGeolocation(lat: Double, lon: Double) {
        val locationData = JSONObject().apply {
            put("latitude", lat)
            put("longitude", lon)
        }
        socket.emit("sent-geolocation", locationData)
    }

    // Schedule emitting geolocation every few seconds
    private fun startGeolocationEmit() {
        geolocationInterval = object : Runnable {
            override fun run() {
                handler.postDelayed(this, geolocationEmitDelaySeconds * 1000)
            }
        }
        handler.post(geolocationInterval)
    }

    private lateinit var geolocationInterval: Runnable

    private fun stopGeolocationEmit() {
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

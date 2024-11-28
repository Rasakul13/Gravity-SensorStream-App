package com.example.imu_gravity_sensor_stream

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpStreamingService : Service() {

    private lateinit var sharedViewModel: SharedViewModel

    private var streamingJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var isRunning = false

    companion object {
        const val CHANNEL_ID = "UDPStreamingChannel"
        const val NOTIFICATION_ID = 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        sharedViewModel = SharedViewModel()
        createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val selectedIp = intent?.getStringExtra("ipAddress") ?: return START_NOT_STICKY
        val port = intent.getIntExtra("port", 5555)

        if (!isRunning) {
            isRunning = true
            sharedViewModel.isStreaming.postValue(true) // Streaming started
            startForeground(NOTIFICATION_ID, createNotification())
            startStreaming(selectedIp, port)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.isStreaming.postValue(false) // Streaming stopped
        stopStreaming()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startStreaming(ipAddress: String, port: Int) {
        streamingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                udpSocket = DatagramSocket()
                while (isActive) {
                    val gravityData = SharedViewModel.gravityData.value ?: continue
                    val message = " 83, ${gravityData[0]}, ${gravityData[1]}, ${gravityData[2]}"
                    sendUdpPacket(message, ipAddress, port)
                    delay(100) // Send packets at a regular interval (e.g., 100ms)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) { // Ignore cancellation exceptions
                    Log.e("UdpStreamingService", "Error during streaming: ${e.message}")
                }
            } finally {
                udpSocket?.close() // Ensure the socket is closed
                Log.d("UdpStreamingService", "Socket closed, coroutine stopped.")
            }
        }
    }

    private fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        udpSocket?.close()
        udpSocket = null
        stopForeground(true)
        stopSelf()
        isRunning = false
    }

    private fun sendUdpPacket(message: String, ipAddress: String, port: Int) {
        try {
            val buffer = message.toByteArray()
            val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(ipAddress), port)
            udpSocket?.send(packet)
            Log.d("UdpStreamingService", "Packet sent: $message")
        } catch (e: Exception) {
            Log.e("UdpStreamingService", "Failed to send UDP packet: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "UDP Streaming",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("UDP Streaming")
            .setContentText("Streaming sensor data to the target device.")
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "UDP Streaming Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}

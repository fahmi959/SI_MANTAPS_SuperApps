package com.presensisiswainformatikabyfahmi.maps

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.presensisiswainformatikabyfahmi.User
import java.util.Timer
import java.util.TimerTask

class LocationTrackingService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var timer: Timer? = null
    private var trackingListener: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        startLocationUpdatesIfEnabled()
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "location_channel"
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pelacakan Lokasi Aktif")
            .setContentText("Aplikasi sedang melacak lokasi secara real-time.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pelacakan Lokasi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        startForeground(1, notification)
    }

    private fun startLocationUpdatesIfEnabled() {
        val currentUser = auth.currentUser ?: return
        val userDocRef = db.collection("users").document(currentUser.uid)

        trackingListener = userDocRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val user = snapshot.toObject(User::class.java) ?: return@addSnapshotListener

            if (user.trackingEnabled) {
                startTimer(currentUser.uid)
            } else {
                stopTimer()
            }
        }
    }

    private fun startTimer(uid: String) {
        if (timer != null) return  // prevent duplicate timers

        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (ActivityCompat.checkSelfPermission(
                        this@LocationTrackingService,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w("LocationService", "Permission not granted")
                    return
                }

                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val locationMap = mapOf(
                                "lastLatitude" to it.latitude,
                                "lastLongitude" to it.longitude,
                                "lastLocationTimestamp" to System.currentTimeMillis()
                            )

                            db.collection("users").document(uid)
                                .update(locationMap)
                        }
                    }
            }
        }, 0, 10000) // 10 detik
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingListener?.remove()
        stopTimer()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

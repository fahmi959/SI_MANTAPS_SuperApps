package com.presensisiswainformatikabyfahmi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.* // You're using FirebaseFirestore, so this might be redundant if not used elsewhere
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import java.net.URL

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class MyApplication : Application() {

// GANTI VERSI APLIKASI ver 3 Saat ini atau sesuaikan SAJA

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth // Declare FirebaseAuth

    // Konstanta untuk versi aplikasi saat ini.
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig // <-- Deklarasikan ini

    // Pastikan ini cocok dengan 'versionCode' di build.gradle (Module: app) Anda.
    companion object {
        const val APP_CURRENT_VERSION_CODE = 5 // <-- Sesuaikan dengan versionCode aplikasi Anda saat ini
        const val PREF_LAST_ACTIVE_TIME_KEY = "last_active_time"
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()

        // --- Inisialisasi Firebase Remote Config ---
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            // Atur interval fetch. Selama development bisa kecil (misal 0 untuk langsung fetch),
            // tapi di production sebaiknya lebih besar (misal 1 jam = 3600 detik)
            // Untuk development awal, set ke 0 agar perubahan langsung terlihat:
            .setMinimumFetchIntervalInSeconds(0) // Untuk development, set ke 0
            // Nanti jika sudah production, ubah ke 3600 (1 jam) atau lebih
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults) // Set nilai default dari XML yang sudah Anda buat
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MyApplication", "Remote Config defaults set successfully.")
                } else {
                    Log.e("MyApplication", "Failed to set Remote Config defaults: ${task.exception?.message}")
                }
            }
        // --- Akhir Inisialisasi Firebase Remote Config ---

        createNotificationChannel()
        listenToBroadcast()
        updateLastActiveTime()
    }

    // Call this method whenever the app comes to the foreground
    // You might want to call this in onResume() of your main activities
    fun updateLastActiveTime() {
        val editor = sharedPreferences.edit()
        editor.putLong("last_active_time", System.currentTimeMillis())
        editor.apply()
        Log.d("MyApplication", "Last active time updated: ${System.currentTimeMillis()}")
    }

    // Method to check if the session has expired
    fun isSessionExpired(): Boolean {
        val lastActiveTime = sharedPreferences.getLong("last_active_time", 0L)
        val currentTime = System.currentTimeMillis()
        val thirtyMinutesInMillis = 30 * 1000L // 30 detik tidak menggunakan aplikasi dalam milisecond

        if (lastActiveTime == 0L) { // No last active time recorded, assume new session or first launch
            return false // Or true, depending on desired behavior. False is safer here.
        }
        val expired = (currentTime - lastActiveTime) > thirtyMinutesInMillis
        Log.d("MyApplication", "Session check: currentTime=$currentTime, lastActiveTime=$lastActiveTime, expired=$expired")
        return expired
    }

    // Method to clear session data (Firebase Auth handles its own token, but good to be explicit)
    fun clearSession() {
        // Sign out from Firebase Auth
        auth.signOut()
        // Clear last active time from SharedPreferences
        val editor = sharedPreferences.edit()
        editor.remove("last_active_time")
        editor.apply()
        Log.d("MyApplication", "Session cleared and user signed out.")
    }

    private fun listenToBroadcast() {
        val dbRef = FirebaseFirestore.getInstance().collection("broadcasts")

        dbRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e("MyApplication", "Firestore error: ${error.message}")
                return@addSnapshotListener
            }

            for (doc in snapshots!!.documentChanges) {
                if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                    val data = doc.document.data
                    val title = data["title"] as? String
                    val message = data["message"] as? String
                    val imageUrl = data["imageUrl"] as? String

                    if (!title.isNullOrEmpty() && !message.isNullOrEmpty()) {
                        showNotification(title, message, imageUrl)

                        // Hapus dokumen setelah ditampilkan
                        doc.document.reference.delete()
                            .addOnSuccessListener {
                                Log.d("MyApplication", "Broadcast dihapus setelah dikirim")
                            }
                            .addOnFailureListener {
                                Log.e("MyApplication", "Gagal hapus broadcast: ${it.message}")
                            }
                    }
                }
            }
        }
    }

    private fun showNotification(title: String, message: String, imageUrl: String?) {
        val intent = Intent(this, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "broadcast_channel")
            .setSmallIcon(R.drawable.ic_notification) // Ganti dengan ikon kamu
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (!imageUrl.isNullOrEmpty()) {
            Thread {
                try {
                    val url = URL(imageUrl)
                    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null as Bitmap?)
                    )
                } catch (e: Exception) {
                    Log.e("Notification", "Gagal muat gambar: ${e.message}")
                } finally {
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(1, builder.build())
                }
            }.start()
        } else {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Broadcast Notification"
            val descriptionText = "Channel untuk notifikasi broadcast"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("broadcast_channel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Tambahkan getter untuk FirebaseRemoteConfig agar bisa diakses dari Activity
    fun getRemoteConfig(): FirebaseRemoteConfig {
        return firebaseRemoteConfig
    }
}
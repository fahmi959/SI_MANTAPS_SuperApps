package com.presensisiswainformatikabyfahmi

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Toast
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.presensisiswainformatikabyfahmi.databinding.ActivitySplashBinding
import com.presensisiswainformatikabyfahmi.login_register.LoginActivity
import com.presensisiswainformatikabyfahmi.maps.LocationTrackingService


class SplashActivity : AppCompatActivity() {

    private val ANIMATION_DURATION = 1000L
    private val SPLASH_DISPLAY_TIME = 2500L
    private val REQUEST_CODE_PERMISSIONS = 101

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        firebaseRemoteConfig = (application as MyApplication).getRemoteConfig()

        checkAndRequestPermissions()
        startSplashAnimations()
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)

        // Tambah izin RECORD_AUDIO di sini
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        } else {
            fetchRemoteConfigAndCheckUpdate()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            fetchRemoteConfigAndCheckUpdate()
        } else {
            Toast.makeText(this, "Semua izin diperlukan agar aplikasi berjalan optimal.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun fetchRemoteConfigAndCheckUpdate() {
        Handler(Looper.getMainLooper()).postDelayed({
            firebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val latestVersionCode = firebaseRemoteConfig.getLong("latest_app_version_code").toInt()
                        val apkDownloadUrl = firebaseRemoteConfig.getString("apk_download_url")
                        val updateTitle = firebaseRemoteConfig.getString("update_dialog_title")
                        val updateMessage = firebaseRemoteConfig.getString("update_dialog_message")

                        val currentAppVersion = MyApplication.APP_CURRENT_VERSION_CODE

                        if (latestVersionCode > currentAppVersion) {
                            showForceUpdateDialog(updateTitle, updateMessage, apkDownloadUrl)
                        } else {
                            checkTrackingAndStart()
                        }
                    } else {
                        checkTrackingAndStart()
                    }
                }
        }, SPLASH_DISPLAY_TIME)
    }

    private fun checkTrackingAndStart() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            firestore.collection("tracking")
                .document("global") // misal satu dokumen global
                .get()
                .addOnSuccessListener { document ->
                    val trackingEnabled = document.getBoolean("trackingEnabled") ?: false
                    if (trackingEnabled) {
                        startLocationTrackingService()
                    }
                    startAppFlow()
                }
                .addOnFailureListener {
                    Log.e("SplashActivity", "Gagal cek trackingEnabled: ${it.message}")
                    startAppFlow()
                }
        } else {
            startAppFlow()
        }
    }

    private fun startLocationTrackingService() {
        val intent = Intent(this, LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun startAppFlow() {
        val app = application as MyApplication
        val currentUser = auth.currentUser

        if (currentUser != null) {
            if (app.isSessionExpired()) {
                app.clearSession()
                Toast.makeText(this, "Sesi Anda telah berakhir, silakan login kembali.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            } else {
                app.updateLastActiveTime()
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }
        } else {
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
        }
        finish()
    }

    private fun showForceUpdateDialog(title: String, message: String, downloadUrl: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Unduh & Perbarui") { _, _ ->
                if (downloadUrl.isNotEmpty()) {
                    try {
                        startApkDownload(downloadUrl)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal mengunduh APK: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Link unduhan tidak tersedia.", Toast.LENGTH_LONG).show()
                }
                finish()
            }
            .setNegativeButton("Keluar Aplikasi") { _, _ -> finish() }
            .show()
    }

    private fun startApkDownload(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Mengunduh Pembaruan")
            .setDescription("Sedang mengunduh versi terbaru aplikasi")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "mantap-update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(this, "Mengunduh pembaruan... Cek notifikasi atau folder Unduhan.", Toast.LENGTH_LONG).show()
    }

    private fun startSplashAnimations() {
        val logoFadeIn = ObjectAnimator.ofFloat(binding.ivAppLogo, View.ALPHA, 0f, 1f)
        val logoScaleX = ObjectAnimator.ofFloat(binding.ivAppLogo, View.SCALE_X, 0.5f, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(binding.ivAppLogo, View.SCALE_Y, 0.5f, 1f)
        val appNameFadeIn = ObjectAnimator.ofFloat(binding.tvAppName, View.ALPHA, 0f, 1f).apply {
            startDelay = ANIMATION_DURATION / 2
        }

        AnimatorSet().apply {
            duration = ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(logoFadeIn, logoScaleX, logoScaleY)
            play(appNameFadeIn).after(logoFadeIn)
            start()
        }
    }
}

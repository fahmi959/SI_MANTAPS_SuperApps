package com.presensisiswainformatikabyfahmi.login_register

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.presensisiswainformatikabyfahmi.R
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var cbRememberMe: CheckBox
    private val PREFS_NAME = "loginPrefs"

    private lateinit var etUsernameLogin: EditText
    private lateinit var etPasswordLogin: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var btnLoginWithFace: Button

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 100 // Tetapkan kode permintaan izin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inisialisasi dulu semua view
        etUsernameLogin = findViewById(R.id.etUsernameLogin)
        etPasswordLogin = findViewById(R.id.etPasswordLogin)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)
        btnLoginWithFace = findViewById(R.id.btnLoginWithFace)

        // Baru akses shared preferences
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedUsername = sharedPref.getString("username", "")
        val savedPassword = sharedPref.getString("password", "")
        val remember = sharedPref.getBoolean("remember", false)

        if (remember) {
            etUsernameLogin.setText(savedUsername)
            etPasswordLogin.setText(savedPassword)
            cbRememberMe.isChecked = true
        }

        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        btnLoginWithFace.setOnClickListener {
            Toast.makeText(this, "Login wajah terintegrasi setelah login dengan username & password", Toast.LENGTH_LONG).show()
        }

        requestNotificationPermission()
    }



    // Fungsi untuk meminta izin notifikasi (hanya untuk Android 13+)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Jika izin belum diberikan, minta ke pengguna
                Log.d("LoginActivity", "Requesting POST_NOTIFICATIONS permission.")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                Log.d("LoginActivity", "Notification permission already granted.")
            }
        }
    }

    // Tangani hasil dari permintaan izin
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("LoginActivity", "Notification permission granted by user in LoginActivity.")
                Toast.makeText(this, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("LoginActivity", "Notification permission denied by user in LoginActivity.")
                Toast.makeText(this, "Izin notifikasi ditolak. Anda mungkin tidak menerima beberapa pemberitahuan.", Toast.LENGTH_LONG).show()
                // Opsional: berikan alasan kepada pengguna atau arahkan ke pengaturan
            }
        }
    }

    private fun loginUser() {
        val username = etUsernameLogin.text.toString().trim()
        val password = etPasswordLogin.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Harap isi username dan password.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val email = querySnapshot.documents[0].getString("email")
                    // PERINGATAN: Menyimpan password plaintext di Firestore sangat tidak aman!
                    // Anda harus menggunakan Firebase Authentication sepenuhnya untuk otentikasi.
                    // Jika Anda menggunakan Firebase Auth, password tidak akan disimpan plaintext.
                    // Untuk tujuan contoh ini, saya biarkan sesuai kode Anda, tetapi sangat disarankan untuk merubahnya.
                    val storedPassword = querySnapshot.documents[0].getString("password")

                    if (storedPassword == password) { // Membandingkan dengan password plaintext (TIDAK AMAN!)
                        email?.let { userEmail ->
                            // Melakukan sign-in ke Firebase Auth.
                            // Jika Anda sudah memverifikasi password via Firestore, langkah ini memastikan
                            // user juga terdaftar di Firebase Auth dan sesi Auth-nya aktif.
                            auth.signInWithEmailAndPassword(userEmail, password)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {

                                        // Login sudah berhasil login ()
                                        if (cbRememberMe.isChecked) {
                                            val editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                            editor.putString("username", username)
                                            editor.putString("password", password)
                                            editor.putBoolean("remember", true)
                                            editor.apply()
                                        } else {
                                            val editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                            editor.clear()
                                            editor.apply()
                                        }

                                        Toast.makeText(this, "Login berhasil, memverifikasi biometrik sistem...", Toast.LENGTH_SHORT).show()

                                        checkBiometricSupportAndAuthenticate()
                                    } else {
                                        // Ini akan terjadi jika password yang tersimpan di Firebase Auth
                                        // tidak cocok dengan password yang dimasukkan (meskipun cocok di Firestore)
                                        // atau ada masalah lain dengan akun Auth.
                                        Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } ?: Toast.makeText(this, "Email tidak ditemukan untuk username ini.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Password salah.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Username tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mencari username: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkBiometricSupportAndAuthenticate() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d("LoginActivity", "Perangkat mendukung biometrik sistem.")
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "Perangkat tidak memiliki hardware biometrik. Lanjut ke verifikasi wajah kustom.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, FaceRecognitionLoginActivity::class.java))
                finish()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Hardware biometrik sistem tidak tersedia atau sibuk. Lanjut ke verifikasi wajah kustom.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, FaceRecognitionLoginActivity::class.java))
                finish()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "Tidak ada sidik jari atau wajah terdaftar di perangkat. Lanjut ke verifikasi wajah kustom.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, FaceRecognitionLoginActivity::class.java))
                finish()
            }
            else -> {
                Toast.makeText(this, "Terjadi kesalahan tidak diketahui pada biometrik sistem. Lanjut ke verifikasi wajah kustom.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, FaceRecognitionLoginActivity::class.java))
                finish()
            }
        }
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Otentikasi biometrik sistem gagal: $errString", Toast.LENGTH_SHORT).show()
                    auth.signOut() // Logout pengguna jika biometrik sistem gagal
                    // Tetap di LoginActivity
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Otentikasi biometrik sistem berhasil! Memulai verifikasi wajah kustom...", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(applicationContext, FaceRecognitionLoginActivity::class.java))
                    finish() // Tutup LoginActivity
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Otentikasi biometrik sistem tidak dikenali.", Toast.LENGTH_SHORT).show()
                    auth.signOut() // Logout pengguna
                    // Tetap di LoginActivity
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verifikasi Identitas Anda")
            .setSubtitle("Gunakan sidik jari atau wajah perangkat untuk melanjutkan")
            .setNegativeButtonText("Batalkan")
            .build()
    }
}
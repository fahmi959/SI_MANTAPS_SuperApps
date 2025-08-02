package com.presensisiswainformatikabyfahmi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.presensisiswainformatikabyfahmi.belajar_mandiri.BelajarMandiriActivity
import com.presensisiswainformatikabyfahmi.chat.ChatActivity
import com.presensisiswainformatikabyfahmi.databinding.ActivityMainBinding
import com.presensisiswainformatikabyfahmi.login_register.LoginActivity
import com.presensisiswainformatikabyfahmi.maps.MapsActivity
import com.presensisiswainformatikabyfahmi.rpp.AttendanceReportActivity
import com.presensisiswainformatikabyfahmi.rpp.GenerateOTP_Presensi_Activity
import com.presensisiswainformatikabyfahmi.rpp.PdfViewerActivity
import com.presensisiswainformatikabyfahmi.rpp.PresensiActivity
import com.presensisiswainformatikabyfahmi.rpp.QuizActivity
import com.presensisiswainformatikabyfahmi.televisi.MainTelevisiActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    private lateinit var binding: ActivityMainBinding  // Gunakan binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()
        firebaseRemoteConfig = (application as MyApplication).getRemoteConfig()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        (application as MyApplication).updateLastActiveTime()

        firestoreDb.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        binding.tvWelcome.text = "Selamat datang, ${it.fullName}!"
                    }
                } else {
                    binding.tvWelcome.text = "Selamat datang, Pengguna!"
                }
            }
            .addOnFailureListener { e ->
                binding.tvWelcome.text = "Selamat datang, Pengguna!"
                Toast.makeText(this, "Failed to load user profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        applyDynamicMenuConfig()

        binding.imgBtnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.imgBtnPresensi.setOnClickListener {
            startActivity(Intent(this, PresensiActivity::class.java))
        }

        binding.imgBtnPrintAttendance.setOnClickListener {
            startActivity(Intent(this, AttendanceReportActivity::class.java))
        }

        binding.imgBtnGenerateOtp.setOnClickListener {
            startActivity(Intent(this, GenerateOTP_Presensi_Activity::class.java))
        }

        binding.imgBtnBroadcastMessage.setOnClickListener {
            startActivity(Intent(this, BroadcastActivity::class.java))
        }

        binding.imgBtnTelevisi.setOnClickListener {
            startActivity(Intent(this, MainTelevisiActivity::class.java))
        }

        binding.imgBtnMaps.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        binding.imgBtnMateri.setOnClickListener {
            startActivity(Intent(this, PdfViewerActivity::class.java))
        }

        binding.imgBtnQuiz.setOnClickListener {
            startActivity(Intent(this, QuizActivity::class.java))
        }

        binding.imgBtnKalkulator.setOnClickListener {
            startActivity(Intent(this, KalkulatorActivity::class.java))
        }

        binding.imgBtnBelajarMandiri.setOnClickListener {
            startActivity(Intent(this, BelajarMandiriActivity::class.java))
        }

        binding.imgBtnForumChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        binding.imgBtnNewFeatureX.setOnClickListener {
            Toast.makeText(this, "Membuka Fitur Baru X!", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, NewFeatureXActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            (application as MyApplication).clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun applyDynamicMenuConfig() {
        val menuConfigJsonString = firebaseRemoteConfig.getString("menu_config_json")
        try {
            val menuConfig = JSONObject(menuConfigJsonString)

            binding.imgBtnProfile.visibility =
                if (menuConfig.optBoolean("profile_visible", true)) View.VISIBLE else View.GONE
            binding.imgBtnPresensi.visibility =
                if (menuConfig.optBoolean("presensi_visible", true)) View.VISIBLE else View.GONE
            binding.imgBtnPrintAttendance.visibility =
                if (menuConfig.optBoolean("print_attendance_visible", true)) View.VISIBLE else View.GONE
            binding.imgBtnGenerateOtp.visibility =
                if (menuConfig.optBoolean("generate_otp_visible", true)) View.VISIBLE else View.GONE
            binding.imgBtnBroadcastMessage.visibility =
                if (menuConfig.optBoolean("broadcast_visible", false)) View.VISIBLE else View.GONE
            binding.imgBtnTelevisi.visibility =
                if (menuConfig.optBoolean("televisi_visible", false)) View.VISIBLE else View.GONE
            binding.imgBtnMaps.visibility =
                if (menuConfig.optBoolean("maps_visible", false)) View.VISIBLE else View.GONE
            binding.imgBtnMateri.visibility =
                if (menuConfig.optBoolean("materi_visible", true)) View.VISIBLE else View.GONE
            binding.imgBtnQuiz.visibility =
                if (menuConfig.optBoolean("quiz_visible", true)) View.VISIBLE else View.GONE
            binding.imgBtnKalkulator.visibility =
                if (menuConfig.optBoolean("kalkulator_visible", true)) View.VISIBLE else View.GONE
            binding.imgBtnBelajarMandiri.visibility =
                if (menuConfig.optBoolean("belajar_mandiri_visible", true)) View.VISIBLE else View.GONE
            binding.imgBtnForumChat.visibility =
                if (menuConfig.optBoolean("chat_visible", true)) View.VISIBLE else View.GONE
            binding.imgBtnNewFeatureX.visibility =
                if (menuConfig.optBoolean("new_feature_x_visible", false)) View.VISIBLE else View.GONE

        } catch (e: Exception) {
            Log.e("MainActivity", "Error parsing menu config JSON: ${e.message}")
            // Fallback default:
            binding.imgBtnProfile.visibility = View.VISIBLE
            binding.imgBtnPresensi.visibility = View.VISIBLE
            binding.imgBtnPrintAttendance.visibility = View.VISIBLE
            binding.imgBtnGenerateOtp.visibility = View.VISIBLE
            binding.imgBtnBroadcastMessage.visibility = View.VISIBLE
            binding.imgBtnTelevisi.visibility = View.VISIBLE
            binding.imgBtnMaps.visibility = View.VISIBLE
            binding.imgBtnMateri.visibility = View.VISIBLE
            binding.imgBtnQuiz.visibility = View.VISIBLE
            binding.imgBtnKalkulator.visibility = View.VISIBLE
            binding.imgBtnBelajarMandiri.visibility = View.VISIBLE
            binding.imgBtnForumChat.visibility = View.VISIBLE
            binding.imgBtnNewFeatureX.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        (application as MyApplication).updateLastActiveTime()
        applyDynamicMenuConfig()
    }
}

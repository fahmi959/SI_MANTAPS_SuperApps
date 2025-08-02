package com.presensisiswainformatikabyfahmi

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BroadcastActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etMessage: EditText
    private lateinit var etImageUrl: EditText
    private lateinit var btnSend: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcast)

        // Cek role admin
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                if (role == "admin") {
                    setupUI()
                } else {
                    Toast.makeText(this, "Akses ditolak: Bukan admin", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal cek role", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun sendBroadcast(title: String, message: String, imageUrl: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Buat dokumen broadcast sementara
        val data = mapOf(
            "title" to title,
            "message" to message,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        val docRef = firestore.collection("broadcasts").document()

        docRef.set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Broadcast dikirim!", Toast.LENGTH_SHORT).show()

                // Hapus broadcast setelah beberapa detik (simulasi sementara)
                docRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Broadcast otomatis dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menghapus broadcast", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengirim broadcast", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupUI() {
        etTitle = findViewById(R.id.etTitle)
        etMessage = findViewById(R.id.etMessage)
        etImageUrl = findViewById(R.id.etImageUrl)
        btnSend = findViewById(R.id.btnSend)

        btnSend.setOnClickListener {
            val title = etTitle.text.toString()
            val message = etMessage.text.toString()
            val imageUrl = etImageUrl.text.toString()

            if (title.isNotEmpty() && message.isNotEmpty()) {
                sendBroadcast(title, message, imageUrl)
            } else {
                Toast.makeText(this, "Isi semua kolom wajib!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

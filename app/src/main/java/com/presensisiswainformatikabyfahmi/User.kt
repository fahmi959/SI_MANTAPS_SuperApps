package com.presensisiswainformatikabyfahmi

import com.google.firebase.database.PropertyName


// Pastikan ini di package yang sama: com.presensisiswainformatikabyfahmi
data class User(
    val fullName: String = "",
    val dob: String = "",
    val email: String = "",
    val username: String = "",
    val faceEmbedding: List<Float>? = null, // Tambahkan field ini
    val profileImageUrl: String? = null, // Tambahkan field ini untuk URL foto profil
    val password: String? = null, // **WARNING: password disini bisa berbahaya jika tidak disimpan secara aman
    val lastLatitude: Double? = null, // Tambahkan field ini
    val lastLongitude: Double? = null, // Tambahkan field ini
    val lastLocationTimestamp: Long? = null ,
    @get:PropertyName("role") @set:PropertyName("role") var role: String? = null, // Ubah dari isAdmin ke role// Tambahkan timestamp kapan lokasi terakhir diupdate
    @get:PropertyName("trackingEnabled") @set:PropertyName("trackingEnabled")
var trackingEnabled: Boolean = false  // ✅ tambahkan ini
)
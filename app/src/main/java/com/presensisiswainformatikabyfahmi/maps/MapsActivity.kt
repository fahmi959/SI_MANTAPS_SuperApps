package com.presensisiswainformatikabyfahmi.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.presensisiswainformatikabyfahmi.R
import com.presensisiswainformatikabyfahmi.User
import com.presensisiswainformatikabyfahmi.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkUserRole()
        updateCurrentUserLocation() // Update lokasi pengguna saat Maps dibuka

        binding.switchRealtime.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (isAdmin) {
                updateRealtimeTrackingStatus(isChecked)
            } else {
                Toast.makeText(this, "Hanya admin yang dapat mengaktifkan pelacakan real-time", Toast.LENGTH_SHORT).show()
                binding.switchRealtime.isChecked = false
            }
        }
    }

    private fun checkUserRole() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get().addOnSuccessListener {
            val user = it.toObject<User>()
            isAdmin = user?.role == "admin"
            binding.switchRealtime.isEnabled = isAdmin
        }
    }

    private fun updateRealtimeTrackingStatus(enable: Boolean) {
        firestore.collection("config").document("location_tracking")
            .set(mapOf("enabled" to enable))
            .addOnSuccessListener {
                // Update semua user
                firestore.collection("users").get().addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.update("trackingEnabled", enable)
                    }
                }

                val message = if (enable) "Pelacakan lokasi real-time aktif" else "Pelacakan dimatikan"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this))
        loadUserLocations()
    }

    private fun updateCurrentUserLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                val lat = location.latitude
                val lng = location.longitude
                val timestamp = System.currentTimeMillis()

                firestore.collection("users").document(uid).update(
                    mapOf(
                        "lastLatitude" to lat,
                        "lastLongitude" to lng,
                        "lastLocationTimestamp" to timestamp
                    )
                ).addOnSuccessListener {
                    // Lokasi berhasil diperbarui
                }.addOnFailureListener {
                    Toast.makeText(this, "Gagal memperbarui lokasi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserLocations() {
        firestore.collection("users").get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val user = doc.toObject<User>() ?: continue
                val lat = user.lastLatitude
                val lng = user.lastLongitude
                if (lat != null && lng != null) {
                    val position = LatLng(lat, lng)

                    val lastUpdated = user.lastLocationTimestamp?.let { timestamp ->
                        val date = java.util.Date(timestamp)
                        val format = android.text.format.DateFormat.format("dd MMM yyyy, HH:mm", date)
                        "Last updated: $format"
                    } ?: "Last updated: Unknown"

                    val snippetText = "${user.email}\nUsername: ${user.username}\n$lastUpdated"

                    val marker = mMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(user.fullName)
                            .snippet(snippetText)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    marker?.tag = user.profileImageUrl
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal memuat lokasi pengguna", Toast.LENGTH_SHORT).show()
        }
    }

    // Custom InfoWindowAdapter untuk tampilkan info user + foto profil
    inner class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

        override fun getInfoContents(marker: Marker): View {
            val view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)

            val titleText = view.findViewById<TextView>(R.id.title)
            val snippetText = view.findViewById<TextView>(R.id.snippet)
            val imageView = view.findViewById<ImageView>(R.id.profile_image)

            titleText.text = marker.title
            snippetText.text = marker.snippet

            val imageUrl = marker.tag as? String
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(context).load(imageUrl).into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_default_profile)
            }

            return view
        }

        override fun getInfoWindow(marker: Marker): View? = null
    }
}

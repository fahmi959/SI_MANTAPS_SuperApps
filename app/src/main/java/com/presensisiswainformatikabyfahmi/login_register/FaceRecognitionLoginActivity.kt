package com.presensisiswainformatikabyfahmi.login_register

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.presensisiswainformatikabyfahmi.MainActivity
import com.presensisiswainformatikabyfahmi.R
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceRecognitionLoginActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvFaceFeedbackLogin: TextView

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var faceEmbedder: FaceEmbedder

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.6f)
        .enableTracking()
        .build()
    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    private var isLoginAttemptInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_recognition_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        previewView = findViewById(R.id.previewViewLogin)
        tvFaceFeedbackLogin = findViewById(R.id.tvFaceFeedbackLogin)

        cameraExecutor = Executors.newSingleThreadExecutor()

        faceEmbedder = FaceEmbedder()
        faceEmbedder.initialize(assets)

        startCamera() // Langsung mulai kamera, asumsi izin sudah diberikan sebelumnya
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(windowManager.defaultDisplay.rotation)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer { facesFound ->
                        runOnUiThread {
                            if (isLoginAttemptInProgress) {
                                tvFaceFeedbackLogin.text = "Mencoba login dengan wajah..."
                                return@runOnUiThread
                            }

                            if (facesFound > 0) {
                                tvFaceFeedbackLogin.text = "Wajah terdeteksi! Memverifikasi..."
                                takePhotoForLoginAutomatically()
                            } else {
                                tvFaceFeedbackLogin.text = "Tidak ada wajah terdeteksi. Posisikan wajah Anda."
                            }
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Gagal mengikat kamera", exc)
                Toast.makeText(this, "Gagal membuka kamera: ${exc.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoForLoginAutomatically() {
        if (isLoginAttemptInProgress) return

        val imageCapture = imageCapture ?: return

        isLoginAttemptInProgress = true

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    if (bitmap != null) {
                        processFaceForLogin(bitmap)
                    } else {
                        Toast.makeText(this@FaceRecognitionLoginActivity, "Gagal konversi gambar wajah", Toast.LENGTH_SHORT).show()
                        isLoginAttemptInProgress = false
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@FaceRecognitionLoginActivity, "Gagal menangkap wajah: ${exc.message}", Toast.LENGTH_SHORT).show()
                    isLoginAttemptInProgress = false
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            if (image.format == ImageFormat.JPEG) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                val matrix = Matrix()
                matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else if (image.planes.size >= 3 && image.format == ImageFormat.YUV_420_888) {
                val yBuffer = image.planes[0].buffer
                val uBuffer = image.planes[1].buffer
                val vBuffer = image.planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 90, out)
                val imageBytes = out.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                val matrix = Matrix()
                matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "imageProxyToBitmap error: ${e.message}", e)
            null
        }
    }

    private fun processFaceForLogin(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val rect = face.boundingBox
                    val safeRect = Rect(
                        rect.left.coerceAtLeast(0),
                        rect.top.coerceAtLeast(0),
                        rect.right.coerceAtMost(bitmap.width),
                        rect.bottom.coerceAtMost(bitmap.height)
                    )

                    if (safeRect.width() > 0 && safeRect.height() > 0) {
                        val croppedFace = Bitmap.createBitmap(bitmap, safeRect.left, safeRect.top, safeRect.width(), safeRect.height())
                        val currentEmbedding = faceEmbedder.getEmbedding(croppedFace)

                        if (currentEmbedding != null) {
                            val user = auth.currentUser
                            if (user != null) {
                                db.collection("users").document(user.uid)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        val storedEmbedding = doc.get("faceEmbedding") as? List<Double>
                                        if (storedEmbedding != null) {
                                            val storedFloat = storedEmbedding.map { it.toFloat() }
                                            val distance = faceEmbedder.calculateEuclideanDistance(currentEmbedding, storedFloat)

                                            val threshold = 1.0f // Kalibrasi sesuai kebutuhan
                                            if (distance < threshold) {
                                                Toast.makeText(this, "Login Wajah Berhasil!", Toast.LENGTH_SHORT).show()
                                                startActivity(Intent(this, MainActivity::class.java))
                                                finish()
                                            } else {
                                                Toast.makeText(this, "Wajah tidak cocok!", Toast.LENGTH_SHORT).show()
                                                auth.signOut()
                                                startActivity(Intent(this, LoginActivity::class.java))
                                                finish()
                                            }
                                        } else {
                                            Toast.makeText(this, "Belum mendaftar wajah.", Toast.LENGTH_SHORT).show()
                                            auth.signOut()
                                            startActivity(Intent(this, LoginActivity::class.java))
                                            finish()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Gagal mengambil data wajah.", Toast.LENGTH_SHORT).show()
                                        auth.signOut()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                            } else {
                                Toast.makeText(this, "Harap login manual dulu.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                        } else {
                            Toast.makeText(this, "Gagal membuat embedding wajah.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Bounding box wajah tidak valid.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Wajah tidak terdeteksi.", Toast.LENGTH_SHORT).show()
                }
                isLoginAttemptInProgress = false
            }
            .addOnFailureListener {
                Toast.makeText(this, "Deteksi wajah gagal.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Face detection failed", it)
                isLoginAttemptInProgress = false
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceDetector.close()
        faceEmbedder.close()
    }

    companion object {
        private const val TAG = "FaceRecognitionLogin"
    }
}

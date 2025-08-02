package com.presensisiswainformatikabyfahmi.login_register

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
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
import com.presensisiswainformatikabyfahmi.R
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceCaptureActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvFaceFeedback: TextView

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

    private var isFaceCapturedAndSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_capture)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        previewView = findViewById(R.id.previewViewCapture)
        tvFaceFeedback = findViewById(R.id.tvFaceFeedbackCapture)

        cameraExecutor = Executors.newSingleThreadExecutor()

        faceEmbedder = FaceEmbedder()
        faceEmbedder.initialize(assets)

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

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
                            if (isFaceCapturedAndSaved) {
                                tvFaceFeedback.text = "Wajah sudah didaftarkan."
                                return@runOnUiThread
                            }

                            if (facesFound > 0) {
                                tvFaceFeedback.text = "Wajah terdeteksi! Mengambil gambar..."
                                takePhotoForRegistrationAutomatically()
                            } else {
                                tvFaceFeedback.text = "Tidak ada wajah terdeteksi. Posisikan wajah Anda di bingkai."
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
                Log.e(TAG, "Gagal mengikat use cases kamera", exc)
                Toast.makeText(this, "Gagal membuka kamera: ${exc.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoForRegistrationAutomatically() {
        if (isFaceCapturedAndSaved) return

        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    if (bitmap != null) {
                        processFaceForRegistration(bitmap)
                    } else {
                        Toast.makeText(this@FaceCaptureActivity, "Gagal mengonversi gambar.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Pengambilan foto otomatis gagal: ${exc.message}", exc)
                    Toast.makeText(this@FaceCaptureActivity, "Pengambilan foto otomatis gagal: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        if (image.format == ImageFormat.JPEG) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            val matrix = Matrix()
            matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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

            val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height), 90, out)
            val imageBytes = out.toByteArray()
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            val matrix = Matrix()
            matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            Log.e(TAG, "Unsupported image format or insufficient planes: ${image.format}")
            return null
        }
    }

    private fun processFaceForRegistration(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val firstFace = faces[0]
                    val boundingBox = firstFace.boundingBox

                    val croppedRect = Rect(
                        boundingBox.left.coerceAtLeast(0),
                        boundingBox.top.coerceAtLeast(0),
                        boundingBox.right.coerceAtMost(bitmap.width),
                        boundingBox.bottom.coerceAtMost(bitmap.height)
                    )

                    if (croppedRect.width() > 0 && croppedRect.height() > 0) {
                        val croppedFaceBitmap = Bitmap.createBitmap(
                            bitmap,
                            croppedRect.left,
                            croppedRect.top,
                            croppedRect.width(),
                            croppedRect.height()
                        )

                        val faceEmbedding = faceEmbedder.getEmbedding(croppedFaceBitmap)
                        if (faceEmbedding != null) {
                            val userId = auth.currentUser?.uid
                            userId?.let { uid ->
                                db.collection("users").document(uid)
                                    .update("faceEmbedding", faceEmbedding)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Wajah berhasil didaftarkan!", Toast.LENGTH_LONG).show()
                                        isFaceCapturedAndSaved = true
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Gagal menyimpan data wajah: ${e.message}", Toast.LENGTH_SHORT).show()
                                        Log.e(TAG, "Error saving face embedding", e)
                                        isFaceCapturedAndSaved = false
                                    }
                            } ?: run {
                                Toast.makeText(this, "Pengguna tidak terautentikasi.", Toast.LENGTH_SHORT).show()
                                isFaceCapturedAndSaved = false
                                auth.signOut()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                        } else {
                            Toast.makeText(this, "Gagal mendapatkan embedding wajah dari model.", Toast.LENGTH_SHORT).show()
                            isFaceCapturedAndSaved = false
                        }
                    } else {
                        Toast.makeText(this, "Wajah terdeteksi terlalu kecil atau di luar batas.", Toast.LENGTH_SHORT).show()
                        isFaceCapturedAndSaved = false
                    }
                } else {
                    Toast.makeText(this, "Tidak ada wajah terdeteksi pada gambar.", Toast.LENGTH_SHORT).show()
                    isFaceCapturedAndSaved = false
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Deteksi wajah gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "ML Kit Face Detection failed", e)
                isFaceCapturedAndSaved = false
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceDetector.close()
        faceEmbedder.close()
    }

    companion object {
        private const val TAG = "FaceCaptureActivity"
    }
}

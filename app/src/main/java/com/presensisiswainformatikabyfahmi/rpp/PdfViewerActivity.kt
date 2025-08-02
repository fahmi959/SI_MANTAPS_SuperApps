package com.presensisiswainformatikabyfahmi.rpp

import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import com.presensisiswainformatikabyfahmi.R
import java.io.IOException

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var pdfView: PDFView
    private lateinit var editPageNumber: EditText
    private lateinit var buttonGoToPage: Button
    private var totalPages: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pdf_viewer)

        pdfView = findViewById(R.id.pdfView)
        editPageNumber = findViewById(R.id.editPageNumber)
        buttonGoToPage = findViewById(R.id.buttonGoToPage)

        val fileName = intent.getStringExtra("fileName") ?: "Buku-Informatika-Kelas-X.pdf"

        val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        pdfView.startAnimation(animation)

        try {
            val assetManager = assets
            val fileList = assetManager.list("") ?: arrayOf()

            Log.d("PdfViewer", "Isi assets: ${fileList.joinToString()}")

            if (fileList.contains(fileName)) {

                // Load PDF & ambil total halaman
                pdfView.fromAsset(fileName)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .spacing(8)
                    .onLoad { nbPages ->
                        totalPages = nbPages
                        Toast.makeText(this, "Total halaman: $nbPages", Toast.LENGTH_SHORT).show()
                    }
                    .load()

            } else {
                Toast.makeText(this, "PDF '$fileName' tidak ditemukan di assets.", Toast.LENGTH_LONG).show()
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membaca PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Terjadi kesalahan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }

        // Tombol cari halaman
        buttonGoToPage.setOnClickListener {
            val inputText = editPageNumber.text.toString()
            val pageNumber = inputText.toIntOrNull()

            if (pageNumber != null && pageNumber in 1..totalPages) {
                val correctedPage = (pageNumber + 10).coerceAtMost(totalPages) - 1
                pdfView.jumpTo(correctedPage, true)
            } else {
                Toast.makeText(this, "Halaman tidak valid (1–$totalPages)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        pdfView.startAnimation(animation)
        window.decorView.postDelayed({
            super.onBackPressed()
        }, 300)
    }
}

package com.presensisiswainformatikabyfahmi.rpp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.itextpdf.io.font.FontProgramFactory
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import java.io.File
import java.io.FileOutputStream
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.TextAlignment
import com.presensisiswainformatikabyfahmi.R
import kotlinx.coroutines.*
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class QuizActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var btnNext: Button
    private lateinit var tvCountdown: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private var questions: List<QuizQuestion> = emptyList()
    private var currentQuestionIndex = 0
    private val selectedAnswers = mutableMapOf<Int, Int>()

    private lateinit var musicPlayer: MediaPlayer
    private val countdownValues = listOf("3", "2", "1", "Mulai!")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz)

        val rootView = findViewById<View>(R.id.mainContainer)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemInsets.bottom)
            insets
        }

        viewFlipper = findViewById(R.id.viewFlipper)
        btnNext = findViewById(R.id.btnNext)
        tvCountdown = findViewById(R.id.tvCountdown)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)

        loadQuestionsFromAssets()
        populateViewFlipper()

        musicPlayer = MediaPlayer.create(this, R.raw.quiz_music)
        musicPlayer.start()

        startCountdownAnimation {
            tvCountdown.visibility = View.GONE
            viewFlipper.visibility = View.VISIBLE
            btnNext.visibility = View.VISIBLE
        }

        btnNext.setOnClickListener {
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                viewFlipper.showNext()
                if (currentQuestionIndex == questions.size - 1) {
                    btnNext.text = "Selesai"
                }
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Akhiri Tes")
                    .setMessage("Apakah Anda yakin ingin mengakhiri tes?")
                    .setPositiveButton("Ya") { _, _ ->
                        showResult()
                    }
                    .setNegativeButton("Tidak", null)
                    .show()
            }
        }
    }

    private fun startCountdownAnimation(onFinish: () -> Unit) {
        var index = 0
        val handler = Handler(mainLooper)

        val runnable = object : Runnable {
            override fun run() {
                if (index < countdownValues.size) {
                    tvCountdown.text = countdownValues[index]
                    val fade = AlphaAnimation(0f, 1f)
                    fade.duration = 600
                    tvCountdown.startAnimation(fade)
                    index++
                    handler.postDelayed(this, 1000)
                } else {
                    onFinish()
                }
            }
        }
        handler.post(runnable)
    }

    private fun loadQuestionsFromAssets() {
        val inputStream = assets.open("quiz-informatika.json")
        val reader = InputStreamReader(inputStream)
        val json = Gson().fromJson<Map<String, Any>>(reader, object : TypeToken<Map<String, Any>>() {}.type)
        val questionsJson = Gson().toJsonTree(json["questions"]).toString()
        val listType = object : TypeToken<List<QuizQuestion>>() {}.type
        questions = Gson().fromJson(questionsJson, listType)
        reader.close()
    }


    private fun populateViewFlipper() {
        val inflater = LayoutInflater.from(this)
        for ((index, question) in questions.withIndex()) {
            val view = inflater.inflate(R.layout.item_quiz_question, viewFlipper, false)

            val tvQuestion = view.findViewById<TextView>(R.id.tvQuestion)
            val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)

            tvQuestion.text = question.question
            question.options.forEachIndexed { i, option ->
                val radioButton = RadioButton(this)
                radioButton.text = option
                radioButton.id = i
                radioGroup.addView(radioButton)
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                selectedAnswers[index] = checkedId
            }

            val layout = view.findViewById<LinearLayout>(R.id.layoutSoal)
            val anim = layout.background as? AnimationDrawable
            anim?.setEnterFadeDuration(1000)
            anim?.setExitFadeDuration(2000)
            anim?.start()

            viewFlipper.addView(view)
        }
    }



    private fun showResult() {
        var correctCount = 0
        for ((i, question) in questions.withIndex()) {
            val selected = selectedAnswers[i]
            if (selected != null && selected == question.answerIndex) {
                correctCount++
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nama Peserta")
        val input = EditText(this)
        input.hint = "Masukkan nama lengkap Anda"
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val userName = input.text.toString().trim()
            if (userName.isNotEmpty()) {
                btnNext.visibility = View.GONE
                viewFlipper.visibility = View.GONE
                tvCountdown.visibility = View.GONE
                findViewById<LinearLayout>(R.id.loadingLayout).visibility = View.VISIBLE

                // Tambahkan animasi rotate ke loadingImage
                val rotate = RotateAnimation(
                    0f, 360f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                ).apply {
                    duration = 1000
                    repeatCount = Animation.INFINITE
                    interpolator = LinearInterpolator()
                }
                findViewById<ImageView>(R.id.loadingImage).startAnimation(rotate)

                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        for (i in 1..100 step 5) {
                            delay(40)
                            withContext(Dispatchers.Main) {
                                progressBar.progress = i
                                progressText.text = "Membuat sertifikat... $i%"
                            }
                        }
                        savePdfAutomatically(userName, correctCount, questions.size)
                    }
                }

            } else {
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun generateCertificatePdfToStream(name: String, score: Int, total: Int, outputStream: OutputStream) {
        val percentage = (score.toDouble() / total * 100).toInt()
        val date = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())

        val pdfWriter = PdfWriter(outputStream)
        val pdfDoc = PdfDocument(pdfWriter)
        val document = Document(pdfDoc)

        val pageSize = pdfDoc.defaultPageSize
        val firstPage = pdfDoc.addNewPage()
        val canvas = PdfCanvas(firstPage)

        // Background logo watermark
        val logoPath = saveDrawableToFile(R.drawable.logo_kemenag, "logo_kemenag.png")
        val bgImage = Image(ImageDataFactory.create(logoPath))
            .scaleToFit(400f, 400f)
            .setFixedPosition((pageSize.width - 400) / 2, (pageSize.height - 400) / 2)
            .setOpacity(0.08f)
        document.add(bgImage)

        // Border kotak sertifikat
        canvas.setLineWidth(3f)
            .setStrokeColor(ColorConstants.DARK_GRAY)
            .rectangle(20.0, 20.0, (pageSize.width - 40).toDouble(), (pageSize.height - 40).toDouble())
            .stroke()

        val fontStream = assets.open("fonts/GreatVibes-Regular.ttf")
        val fontData = fontStream.readBytes()
        val fontProgram = FontProgramFactory.createFont(fontData)
        val font = com.itextpdf.kernel.font.PdfFontFactory.createFont(fontProgram, "Identity-H", true)

        val title = Paragraph("SERTIFIKAT").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(30f).setFontColor(ColorConstants.BLUE)
        val subtitle = Paragraph("E-UJIAN INFORMATIKA KELAS X").setTextAlignment(TextAlignment.CENTER).setFontSize(16f)
        val awardedTo = Paragraph("Diberikan kepada:").setTextAlignment(TextAlignment.CENTER)
        val formattedName = name
            .trim()
            .replace(Regex("\\s+"), " ")
        val participantName = Paragraph(formattedName).setTextAlignment(TextAlignment.CENTER).setFont(font).setFontSize(28f)
        val body = Paragraph("Atas partisipasinya dalam mengikuti E-Ujian Informatika Kelas X\n" +
                "yang diselenggarakan oleh MAN 4 Tasikmalaya dengan perolehan skor:\n\n" +
                "$score dari $total soal").setTextAlignment(TextAlignment.CENTER)
        val nilaiBesar = Paragraph("$percentage").setTextAlignment(TextAlignment.CENTER).setFontSize(30f).setBold().setFontColor(ColorConstants.BLUE)
        val dateInfo = Paragraph("Tasikmalaya, $date").setTextAlignment(TextAlignment.RIGHT).setFontSize(12f)

        document.add(title)
        document.add(subtitle)
        document.add(Paragraph("\n"))
        document.add(awardedTo)
        document.add(participantName)
        document.add(Paragraph("\n"))
        document.add(body)
        document.add(nilaiBesar)
        document.add(Paragraph("\n\n"))
        document.add(dateInfo)

        val capPath = saveDrawableToFile(R.drawable.cap_stempel_logoman4, "cap_kamad.png")
        val ttdKepalaPath = saveDrawableToFile(R.drawable.ttd_kepala, "ttd_kamad.png")
        val ttdGuruPath = saveDrawableToFile(R.drawable.ttd_fahmi, "ttd_gurutik.png")

        val capImg = Image(ImageDataFactory.create(capPath)).scaleToFit(100.0F, 100.0F).setFixedPosition((pageSize.width - 350).toFloat(), 120f)
        val ttdKepalaImg = Image(ImageDataFactory.create(ttdKepalaPath)).scaleToFit(200.0F, 200.0F).setFixedPosition((pageSize.width - 180).toFloat(), 50f)
        val ttdGuruImg = Image(ImageDataFactory.create(ttdGuruPath)).scaleToFit(100.0F, 100.0F).setFixedPosition(80f, 100f)

        document.add(capImg)
        document.add(ttdKepalaImg)
        document.add(ttdGuruImg)

        val kepala = Paragraph("Kepala Madrasah").setTextAlignment(TextAlignment.RIGHT).setFixedPosition((pageSize.width - 270).toFloat(), 180f, 200f).setFontSize(12f)
        val nama_kepala = Paragraph("(___________________)").setTextAlignment(TextAlignment.RIGHT).setFixedPosition((pageSize.width - 250).toFloat(), 100f, 200f).setFontSize(12f)
        val guruTik = Paragraph("Guru Mata Pelajaran TIK").setFixedPosition(50f, 180f, 200f).setFontSize(12f)
        val nama_guruTik = Paragraph("(___________________)").setFixedPosition(50f, 100f, 200f).setFontSize(12f)

        document.add(kepala)
        document.add(nama_kepala)
        document.add(guruTik)
        document.add(nama_guruTik)

        document.close()
    }

    private fun savePdfAutomatically(name: String, score: Int, total: Int) {
        val fileName = "Sertifikat_${name}.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appFolder = File(downloadsDir, "MAN 4 Tasikmalaya/Sertifikat")
        if (!appFolder.exists()) appFolder.mkdirs()
        val file = File(appFolder, fileName)

        val outputStream = FileOutputStream(file)
        generateCertificatePdfToStream(name, score, total, outputStream)
        outputStream.close()

        runOnUiThread {
            progressBar.visibility = View.GONE
            progressText.visibility = View.GONE
            showSuccessDialog(file)
        }
    }



    private fun showSuccessDialog(file: File) {
        val filePath = file.absolutePath

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sertifikat Dibuat")
        builder.setMessage("Sertifikat berhasil disimpan di:\n\n$filePath")

        builder.setPositiveButton("Buka Sertifikat") { _, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(
                    FileProvider.getUriForFile(this, "$packageName.fileprovider", file),
                    "application/pdf"
                )
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivity(intent)
                finish()
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Tidak ada aplikasi pembuka PDF", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Selesai") { _, _ ->
            finish()
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun saveDrawableToFile(drawableId: Int, fileName: String): String {
        val drawable = resources.getDrawable(drawableId, null)
        val bitmap = (drawable as? BitmapDrawable)?.bitmap
        if (bitmap == null) {
            throw RuntimeException("Gagal mengubah drawable ke bitmap: $drawableId")
        }

        val file = File(cacheDir, fileName)
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()
        return file.absolutePath
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer.release()
    }
}

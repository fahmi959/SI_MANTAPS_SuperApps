package com.presensisiswainformatikabyfahmi.belajar_mandiri

import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.presensisiswainformatikabyfahmi.R

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var subtitleText: TextView
    private lateinit var youtubePlayerView: YouTubePlayerView
    private lateinit var handler: Handler

    private var ccEnabled = false
    private var captionIndex = 0
    private val simulatedCaptions = listOf(
        "Selamat datang di materi Informatika!",
        "Hari ini kita akan belajar tentang algoritma dasar.",
        "Perhatikan contoh berikut dengan baik.",
        "Jangan lupa catat poin pentingnya ya!",
        "Terima kasih sudah menyimak, sampai jumpa!"
    )

    private val updateSubtitleRunnable = object : Runnable {
        override fun run() {
            if (ccEnabled && captionIndex < simulatedCaptions.size) {
                subtitleText.text = simulatedCaptions[captionIndex++]
                subtitleText.visibility = View.VISIBLE
                handler.postDelayed(this, 5000)
            } else {
                subtitleText.visibility = View.GONE
                handler.removeCallbacks(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_player)

        val videoId = intent.getStringExtra("VIDEO_ID") ?: return
        val title = intent.getStringExtra("TITLE") ?: "Materi Informatika"

        subtitleText = findViewById(R.id.subtitleText)
        youtubePlayerView = findViewById(R.id.youtubePlayerView)
        handler = Handler(mainLooper)

        // Fade in animation
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_scale_rotate_in)
        findViewById<View>(R.id.cardContainer).startAnimation(fadeIn)

        lifecycle.addObserver(youtubePlayerView)
        youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.loadVideo(videoId, 0f)
            }
        })

        // Volume Control
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val volumeSeekBar = findViewById<SeekBar>(R.id.volumeSeekBar)
        volumeSeekBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Subtitle Toggle (CC)
        val ccToggle = findViewById<TextView>(R.id.ccToggle)
        ccToggle.setOnClickListener {
            ccEnabled = !ccEnabled
            if (ccEnabled) {
                captionIndex = 0
                handler.post(updateSubtitleRunnable)
                Toast.makeText(this, "Subtitle diaktifkan", Toast.LENGTH_SHORT).show()
            } else {
                subtitleText.visibility = View.GONE
                handler.removeCallbacks(updateSubtitleRunnable)
                Toast.makeText(this, "Subtitle dimatikan", Toast.LENGTH_SHORT).show()
            }
        }

        // Resolution Info (Statik)
        val resolutionInfo = findViewById<TextView>(R.id.resolutionInfo)
        resolutionInfo.text = "HD"
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateSubtitleRunnable)
        super.onDestroy()
    }
}

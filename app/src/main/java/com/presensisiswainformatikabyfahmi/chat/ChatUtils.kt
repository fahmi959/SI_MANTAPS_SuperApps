package com.presensisiswainformatikabyfahmi.chat

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.Executors

object ChatUtils {

    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null
    private val executor = Executors.newSingleThreadExecutor()

    fun startRecording(context: Context) {
        outputFile = File(context.cacheDir, "vn_${System.currentTimeMillis()}.3gp")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(outputFile!!.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            prepare()
            start()
        }
        isRecording = true
    }

    fun stopRecordingAndUpload(
        context: Context,
        driveService: Drive,
        userId: String,
        userName: String,
        photoUrl: String?,
        receiverId: String,
        text: String? = null
    ) {
        if (!isRecording) return
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        isRecording = false

        executor.execute {
            try {
                val fileMetadata = DriveFile().apply {
                    name = outputFile!!.name
                    parents = listOf("1iBoEc24SzbooDZXLAVJ_HZEXh8sJko9L") // Ganti dengan folder ID milikmu
                }
                val mediaContent = FileContent("audio/3gpp", outputFile!!)
                val file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id").execute()

                val voiceUrl = "https://drive.google.com/uc?export=view&id=${file.id}"

                sendMessage(
                    context,
                    userId,
                    userName,
                    photoUrl,
                    receiverId,
                    text,
                    voiceUrl
                )
            } catch (e: Exception) {
                Log.e("ChatUtils", "Upload ke Drive gagal: ${e.message}", e)
            }
        }
    }

    fun sendMessage(
        context: Context,
        userId: String,
        name: String,
        photoUrl: String?,
        receiverId: String,
        text: String?,
        voiceNoteUrl: String? = null
    ) {
        val ref = FirebaseDatabase.getInstance().getReference("chatMessages").push()

        val message = photoUrl?.let {
            ChatMessage(
                senderId = userId,
                senderName = name,
                senderPhotoUrl = it,
                receiverId = receiverId,
                message = text ?: "",
                voiceNoteUrl = voiceNoteUrl,
                timestamp = System.currentTimeMillis()
            )
        }

        ref.setValue(message)

        // Simpan juga ke lokal
        val existing = loadLocalChats(context).toMutableList()
        if (message != null) {
            existing.add(message)
        }
        saveLocalChats(context, existing)
    }

    fun saveLocalChats(context: Context, chats: List<ChatMessage>) {
        try {
            val file = File(context.filesDir, "local_chats.bin")
            ObjectOutputStream(file.outputStream()).use {
                it.writeObject(chats)
            }
        } catch (e: Exception) {
            Log.e("ChatUtils", "Gagal simpan lokal: ${e.message}", e)
        }
    }

    fun loadLocalChats(context: Context): List<ChatMessage> {
        val file = File(context.filesDir, "local_chats.bin")
        if (!file.exists()) return emptyList()
        return try {
            ObjectInputStream(file.inputStream()).use {
                @Suppress("UNCHECKED_CAST")
                it.readObject() as List<ChatMessage>
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun playVoiceMessage(context: Context, url: String) {
        val mediaPlayer = android.media.MediaPlayer()
        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepare()
            mediaPlayer.start()
            Toast.makeText(context, "Memutar voice note...", Toast.LENGTH_SHORT).show()

            mediaPlayer.setOnCompletionListener {
                mediaPlayer.release()
                Toast.makeText(context, "Selesai diputar", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal memutar voice note", Toast.LENGTH_SHORT).show()
        }
    }
}

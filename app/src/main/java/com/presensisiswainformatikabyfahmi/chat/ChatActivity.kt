package com.presensisiswainformatikabyfahmi.chat

import android.os.Bundle
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.presensisiswainformatikabyfahmi.R
import com.google.api.services.drive.Drive

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()

    private lateinit var inputEditText: EditText
    private lateinit var sendButton: ImageView
    private lateinit var recordButton: ImageView

    private lateinit var driveService: Drive

    // Simulasi data user login
    private val currentUserId = "user123"
    private val currentUserName = "Nama User"
    private val currentUserPhotoUrl: String? = null

    // ID penerima pesan
    private val receiverId: String by lazy {
        intent.getStringExtra("receiverId") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_room)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        inputEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        recordButton = findViewById(R.id.micButton)

        chatAdapter = ChatAdapter(this, chatList, currentUserId)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        setupListeners()
        loadChatsFromLocal()
        listenIncomingMessages()
    }

    private fun setupListeners() {
        sendButton.setOnClickListener {
            val text = inputEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                ChatUtils.sendMessage(
                    context = this,
                    userId = currentUserId,
                    name = currentUserName,
                    photoUrl = currentUserPhotoUrl ?: "",
                    receiverId = receiverId,
                    text = text,
                    voiceNoteUrl = null
                )
                inputEditText.setText("")
            }
        }

        recordButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    ChatUtils.startRecording(this)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    ChatUtils.stopRecordingAndUpload(
                        context = this,
                        driveService = driveService,
                        userId = currentUserId,
                        userName = currentUserName,
                        photoUrl = currentUserPhotoUrl ?: "",
                        receiverId = receiverId,
                        text = null
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun loadChatsFromLocal() {
        chatList.clear()
        chatList.addAll(ChatUtils.loadLocalChats(this))
        chatAdapter.notifyDataSetChanged()
    }

    private fun listenIncomingMessages() {
        val dbRef = FirebaseDatabase.getInstance().getReference("chatMessages")
        dbRef.orderByChild("timestamp").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chat = snapshot.getValue(ChatMessage::class.java)
                if (chat != null &&
                    (
                            (chat.senderId == currentUserId && chat.receiverId == receiverId) ||
                                    (chat.senderId == receiverId && chat.receiverId == currentUserId)
                            )
                ) {
                    chatList.add(chat)
                    chatAdapter.notifyItemInserted(chatList.size - 1)
                    chatRecyclerView.scrollToPosition(chatList.size - 1)
                    ChatUtils.saveLocalChats(this@ChatActivity, chatList)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
        })
    }
}

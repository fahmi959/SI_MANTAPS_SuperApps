package com.presensisiswainformatikabyfahmi.chat

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.presensisiswainformatikabyfahmi.R
import com.presensisiswainformatikabyfahmi.User
import com.presensisiswainformatikabyfahmi.UserSession


import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var messageBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messages: MutableList<ChatMessage>

    val db = FirebaseDatabase.getInstance().getReference("chatRooms_v2")

    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_room)

        messageBox = findViewById(R.id.messageEditText)
        sendBtn = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.chatRecyclerView)

        messages = mutableListOf()
        chatAdapter = ChatAdapter(messages, this)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        // 🔽 Ambil data user dulu dari Firestore
        fetchCurrentUser {
            loadMessages()
            sendBtn.setOnClickListener {
                val message = messageBox.text.toString().trim()
                if (!TextUtils.isEmpty(message)) {
                    sendMessage(message)
                    messageBox.setText("")
                }
            }
        }
    }

    private fun fetchCurrentUser(onComplete: () -> Unit) {
        val cachedUser = UserSession.currentUser
        if (cachedUser != null) {
            currentUser = cachedUser
            onComplete()
            return
        }

        val uid = FirebaseAuth.getInstance().uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        if (user != null) {
                            currentUser = user
                            UserSession.currentUser = user
                            onComplete()
                        } else {
                            Toast.makeText(this, "User data is null", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun loadMessages() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                for (data in snapshot.children) {
                    val msg = data.getValue(ChatMessage::class.java)
                    if (msg != null) messages.add(msg)
                }
                chatAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messages.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMessage(text: String) {
        val user = currentUser
        if (user != null) {
            val time = System.currentTimeMillis()
            val message = ChatMessage(
                senderUid = FirebaseAuth.getInstance().uid ?: "",
                senderName = user.fullName,
                message = text,
                timestamp = time,
                senderProfileUrl = user.profileImageUrl
            )
            db.push().setValue(message)
        }
    }
}

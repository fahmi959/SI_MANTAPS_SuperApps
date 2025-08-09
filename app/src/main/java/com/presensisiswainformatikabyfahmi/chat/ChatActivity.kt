package com.presensisiswainformatikabyfahmi.chat

import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.presensisiswainformatikabyfahmi.databinding.ChatRoomBinding

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ChatRoomBinding
    private lateinit var adapter: ChatAdapter
    private val auth = FirebaseAuth.getInstance()
    private val messages = ArrayList<ChatMessage>()
    private val userMap = mutableMapOf<String, Pair<String, String>>() // uid -> (fullName, profileImageUrl)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preloadUserData {
            setupRecyclerView()
            setupSendMessage()
            loadMessages()
        }
    }

    private fun preloadUserData(onComplete: () -> Unit) {
        FirebaseFirestore.getInstance().collection("users").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val uid = doc.id
                    val fullName = doc.getString("fullName") ?: "Pengguna"
                    val photoUrl = doc.getString("profileImageUrl") ?: ""
                    userMap[uid] = fullName to photoUrl
                }
                onComplete()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data user", Toast.LENGTH_SHORT).show()
                onComplete()
            }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages, auth.currentUser?.uid ?: "", userMap)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = adapter
    }

    private fun setupSendMessage() {
        binding.sendButton.setOnClickListener { sendMessage() }

        binding.messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
    }

    private fun sendMessage() {
        val text = binding.messageEditText.text.toString().trim()
        if (TextUtils.isEmpty(text)) return

        val currentUser = auth.currentUser ?: return
        val dbRef = FirebaseDatabase.getInstance().reference

        val senderName = userMap[currentUser.uid]?.first ?: "Saya"

        val messageId = dbRef.child("chat_messages").push().key ?: return
        val message = mapOf(
            "senderId" to currentUser.uid,
            "senderName" to senderName,
            "message" to text,
            "timestamp" to System.currentTimeMillis() / 1000
        )

        dbRef.child("chat_messages").child(messageId).setValue(message)
            .addOnSuccessListener { binding.messageEditText.text.clear() }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengirim pesan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMessages() {
        val dbRef = FirebaseDatabase.getInstance().reference
        dbRef.child("chat_messages")
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    for (child in snapshot.children) {
                        val senderId = child.child("senderId").getValue(String::class.java) ?: ""
                        val senderName = child.child("senderName").getValue(String::class.java) ?: ""
                        val messageText = child.child("message").getValue(String::class.java) ?: ""
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                        val message = ChatMessage(
                            senderId = senderId,
                            senderName = senderName,
                            message = messageText,
                            timestamp = Timestamp(timestamp, 0)
                        )
                        messages.add(message)
                    }
                    adapter.notifyDataSetChanged()
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Gagal memuat pesan", Toast.LENGTH_SHORT).show()
                }
            })
    }
}

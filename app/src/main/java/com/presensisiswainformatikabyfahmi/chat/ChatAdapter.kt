package com.presensisiswainformatikabyfahmi.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.presensisiswainformatikabyfahmi.R
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messageList: List<ChatMessage>,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val MSG_TYPE_LEFT = 0
    private val MSG_TYPE_RIGHT = 1

    override fun getItemViewType(position: Int): Int {
        val currentUid = FirebaseAuth.getInstance().uid
        return if (messageList[position].senderUid == currentUid) MSG_TYPE_RIGHT else MSG_TYPE_LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == MSG_TYPE_RIGHT)
            R.layout.item_chat_message_right
        else
            R.layout.item_chat_message_left
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount() = messageList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        (holder as MessageViewHolder).bind(message)
    }

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val senderName: TextView = view.findViewById(R.id.senderName)
        private val messageText: TextView = view.findViewById(R.id.messageText)
        private val timeText: TextView = view.findViewById(R.id.timeText)
        private val senderPhoto: ImageView = view.findViewById(R.id.senderPhoto)

        fun bind(msg: ChatMessage) {
            senderName.text = msg.senderName
            messageText.text = msg.message
            timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))

            if (!msg.senderProfileUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(msg.senderProfileUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .circleCrop()
                    .into(senderPhoto)
            } else {
                senderPhoto.setImageResource(R.drawable.ic_default_profile)
            }
        }
    }
}

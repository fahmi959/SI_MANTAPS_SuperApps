package com.presensisiswainformatikabyfahmi.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.presensisiswainformatikabyfahmi.R
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val context: Context,
    private val messageList: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val LEFT = 0
    private val RIGHT = 1

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == currentUserId) RIGHT else LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == RIGHT) R.layout.item_chat_message_right else R.layout.item_chat_message_left
        val view = LayoutInflater.from(context).inflate(layout, parent, false)
        return if (viewType == RIGHT) RightViewHolder(view) else LeftViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        if (holder is LeftViewHolder) holder.bind(message)
        if (holder is RightViewHolder) holder.bind(message)
    }

    override fun getItemCount(): Int = messageList.size

    inner class LeftViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(message: ChatMessage) {
            val name = itemView.findViewById<TextView>(R.id.senderName)
            val text = itemView.findViewById<TextView>(R.id.messageText)
            val time = itemView.findViewById<TextView>(R.id.timeText)
            val photo = itemView.findViewById<ImageView>(R.id.senderPhoto)

            name.text = message.senderName
            time.text = getTime(message.timestamp)
            Glide.with(context).load(message.senderPhotoUrl).into(photo)

            if (message.message != null) {
                text.text = message.message
                text.setTextColor(ContextCompat.getColor(context, R.color.black))
                text.setOnClickListener(null)
            } else {
                text.text = "[Voice Note]"
                text.setTextColor(ContextCompat.getColor(context, R.color.teal_700))
                text.setOnClickListener {
                    if (message.voiceNoteUrl != null) {
                        ChatUtils.playVoiceMessage(context, message.voiceNoteUrl)
                    } else {
                        Toast.makeText(context, "Voice note tidak tersedia", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    inner class RightViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(message: ChatMessage) {
            val name = itemView.findViewById<TextView>(R.id.senderName)
            val text = itemView.findViewById<TextView>(R.id.messageText)
            val time = itemView.findViewById<TextView>(R.id.timeText)
            val photo = itemView.findViewById<ImageView>(R.id.senderPhoto)

            name.text = "Saya"
            time.text = getTime(message.timestamp)
            Glide.with(context).load(message.senderPhotoUrl).into(photo)

            if (message.message != null) {
                text.text = message.message
                text.setTextColor(ContextCompat.getColor(context, R.color.black))
                text.setOnClickListener(null)
            } else {
                text.text = "[Voice Note]"
                text.setTextColor(ContextCompat.getColor(context, R.color.teal_700))
                text.setOnClickListener {
                    if (message.voiceNoteUrl != null) {
                        ChatUtils.playVoiceMessage(context, message.voiceNoteUrl)
                    } else {
                        Toast.makeText(context, "Voice note tidak tersedia", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

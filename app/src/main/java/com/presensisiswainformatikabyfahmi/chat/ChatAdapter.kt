package com.presensisiswainformatikabyfahmi.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.presensisiswainformatikabyfahmi.R
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String,
    private val userMap: Map<String, Pair<String, String>>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val LEFT = 0
    private val RIGHT = 1

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) RIGHT else LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == LEFT) {
            val view = inflater.inflate(R.layout.item_chat_message_left, parent, false)
            LeftMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_message_right, parent, false)
            RightMessageViewHolder(view)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(message.timestamp.toDate())

        val (fullName, photoUrl) = userMap[message.senderId] ?: ("Pengguna" to "")

        when (holder) {
            is LeftMessageViewHolder -> {
                holder.senderName.text = fullName
                holder.messageText.text = message.message
                holder.timeText.text = formattedTime

                Glide.with(holder.senderPhoto.context)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .into(holder.senderPhoto)
            }
            is RightMessageViewHolder -> {
                holder.senderName.text = fullName
                holder.messageText.text = message.message
                holder.timeText.text = formattedTime

                Glide.with(holder.senderPhoto.context)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .into(holder.senderPhoto)
            }
        }
    }

    class LeftMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val senderName: TextView = view.findViewById(R.id.senderName)
        val messageText: TextView = view.findViewById(R.id.messageText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val senderPhoto: ImageView = view.findViewById(R.id.senderPhoto)
    }

    class RightMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val senderName: TextView = view.findViewById(R.id.senderName)
        val messageText: TextView = view.findViewById(R.id.messageText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val senderPhoto: ImageView = view.findViewById(R.id.senderPhoto)
    }
}

package com.presensisiswainformatikabyfahmi.chat

import java.io.Serializable

data class ChatMessage(
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val voiceNoteUrl: String? = null
) : Serializable

package com.presensisiswainformatikabyfahmi.chat


data class ChatMessage(
    val senderUid: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val senderProfileUrl: String? = null
)

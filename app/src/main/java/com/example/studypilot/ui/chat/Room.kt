package com.example.studypilot.ui.chat

data class Room(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val subject: String = "",
    val createdBy: String = "",
    val createdByName: String = "",
    val memberCount: Int = 0,
    val createdAt: Long = 0L,
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

val SUBJECT_TAGS = listOf(
    "Mathematics", "Physics", "Chemistry", "Biology",
    "History", "Geography", "English", "Computer Science",
    "Economics", "General"
)
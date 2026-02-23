package com.example.studypilot.data

data class Note(
    val id: String = "",
    val userId: String = "",
    val subjectName: String = "",
    val mode: String = "",
    val sessionId: String = "",
    val title: String = "",
    val content: String = "",
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
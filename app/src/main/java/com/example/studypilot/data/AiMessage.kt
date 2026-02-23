package com.example.studypilot.data

data class AiMessage(
    val role: String,        // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false  // true while waiting for response
)
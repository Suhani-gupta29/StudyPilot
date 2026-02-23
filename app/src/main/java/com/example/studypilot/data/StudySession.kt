package com.example.studypilot.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_sessions",
    indices = [Index(value = ["userId"]), Index(value = ["timestamp"])]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val subjectName: String,
    val modeName: String,
    val totalDurationSeconds: Int,
    val elapsedSeconds: Int,
    val remainingSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val completed: Boolean
)
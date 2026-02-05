package com.example.studypilot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insertSession(session: StudySession)

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE subjectName = :subject ORDER BY timestamp DESC")
    fun getSessionsBySubject(subject: String): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSessionsForUser(userId: String): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE completed = 1 ORDER BY timestamp DESC")
    fun getCompletedSessions(): Flow<List<StudySession>>

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAllSessions()
}
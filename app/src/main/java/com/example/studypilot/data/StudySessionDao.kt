package com.example.studypilot.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    // CRITICAL: Flow queries automatically re-emit when the table changes
    @Query("""
        SELECT * FROM study_sessions 
        WHERE userId = :userId 
        AND date(timestamp / 1000, 'unixepoch', 'localtime') = :date
        ORDER BY timestamp DESC
    """)
    fun getTodaySessionsForUser(userId: String, date: String): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSessionsForUser(userId: String): Flow<List<StudySession>>

    // Sync version for debugging
    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getSessionsForUserSync(userId: String): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): StudySession?

    // CRITICAL: Insert triggers Flow re-emission automatically
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Update
    suspend fun updateSession(session: StudySession)

    @Delete
    suspend fun deleteSession(session: StudySession)

    @Query("DELETE FROM study_sessions WHERE userId = :userId")
    suspend fun deleteAllSessionsForUser(userId: String)
}
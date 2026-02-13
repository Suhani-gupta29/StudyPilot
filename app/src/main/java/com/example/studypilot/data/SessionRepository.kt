package com.example.studypilot.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SessionRepository(private val sessionDao: StudySessionDao) {

    // CRITICAL FIX: Use the DAO's Flow directly - Room handles emissions automatically
    fun getTodaySessionsForUser(userId: String): Flow<List<StudySession>> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        android.util.Log.d("SessionRepository", "Setting up Flow for user=$userId, today=$today")

        // Room automatically emits when the database changes
        return sessionDao.getTodaySessionsForUser(userId, today)
            .map { sessions ->
                android.util.Log.d("SessionRepository", "📊 Flow emitting: ${sessions.size} sessions for today from ${sessionDao.getSessionsForUserSync(userId).size} total")
                sessions.forEach { session ->
                    android.util.Log.d("SessionRepository", "  └─ ${session.subjectName}: completed=${session.completed}, mode=${session.modeName}")
                }
                sessions
            }
    }

    fun getSessionsForUser(userId: String): Flow<List<StudySession>> {
        return sessionDao.getSessionsForUser(userId)
    }

    suspend fun saveSession(session: StudySession) {
        android.util.Log.d("SessionRepository", "Saving session: subject=${session.subjectName}, completed=${session.completed}, mode=${session.modeName}")

        // CRITICAL: Room will automatically trigger Flow emission after this insert
        sessionDao.insertSession(session)

        android.util.Log.d("SessionRepository", "✅ Session saved - Room should emit to Flow subscribers now")
    }

    suspend fun deleteSession(session: StudySession) {
        sessionDao.deleteSession(session)
    }

    suspend fun updateSession(session: StudySession) {
        sessionDao.updateSession(session)
    }

    suspend fun getSessionById(sessionId: Long): StudySession? {
        return sessionDao.getSessionById(sessionId)
    }
}
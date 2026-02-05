package com.example.studypilot.data

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: StudySessionDao) {

    suspend fun saveSession(session: StudySession) {
        dao.insertSession(session)
    }

    fun getAllSessions(): Flow<List<StudySession>> {
        return dao.getAllSessions()
    }

    fun getSessionsForUser(userId: String): Flow<List<StudySession>> {
        return dao.getSessionsForUser(userId)
    }

    fun getSessionsBySubject(subject: String): Flow<List<StudySession>> {
        return dao.getSessionsBySubject(subject)
    }

    fun getCompletedSessions(): Flow<List<StudySession>> {
        return dao.getCompletedSessions()
    }

    suspend fun deleteAllSessions() {
        dao.deleteAllSessions()
    }
}
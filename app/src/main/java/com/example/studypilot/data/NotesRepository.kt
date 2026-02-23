package com.example.studypilot.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class NotesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getNotesForSubject(
        userId: String,
        subjectName: String,
        mode: String
    ): Flow<List<Note>> = callbackFlow {
        var listener: ListenerRegistration? = null

        try {
            listener = firestore.collection("notes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("subjectName", subjectName)
                .whereEqualTo("mode", mode)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("NotesRepository", "Error: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val notes = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Note::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("NotesRepository", "Parse error: ${e.message}")
                            null
                        }
                    } ?: emptyList()

                    // Sort in memory - pinned first, then by date
                    val sorted = notes.sortedWith(
                        compareByDescending<Note> { it.pinned }
                            .thenByDescending { it.updatedAt }
                    )

                    android.util.Log.d("NotesRepository", "Subject notes loaded: ${sorted.size}")
                    trySend(sorted).isSuccess
                }
        } catch (e: Exception) {
            android.util.Log.e("NotesRepository", "Listener setup failed: ${e.message}")
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
            android.util.Log.d("NotesRepository", "Subject notes listener removed")
        }
    }

    fun getNotesForSession(
        userId: String,
        sessionId: String
    ): Flow<List<Note>> = callbackFlow {
        var listener: ListenerRegistration? = null

        try {
            listener = firestore.collection("notes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("sessionId", sessionId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("NotesRepository", "Error: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val notes = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Note::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("NotesRepository", "Parse error: ${e.message}")
                            null
                        }
                    } ?: emptyList()

                    // Sort in memory
                    val sorted = notes.sortedWith(
                        compareByDescending<Note> { it.pinned }
                            .thenByDescending { it.updatedAt }
                    )

                    android.util.Log.d("NotesRepository", "Session notes loaded: ${sorted.size}")
                    trySend(sorted).isSuccess
                }
        } catch (e: Exception) {
            android.util.Log.e("NotesRepository", "Session listener setup failed: ${e.message}")
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
            android.util.Log.d("NotesRepository", "Session notes listener removed")
        }
    }

    suspend fun saveNote(note: Note): Result<String> {
        return try {
            val docRef = if (note.id.isEmpty()) {
                firestore.collection("notes").document()
            } else {
                firestore.collection("notes").document(note.id)
            }

            val noteToSave = note.copy(
                id = docRef.id,
                updatedAt = System.currentTimeMillis()
            )

            docRef.set(noteToSave).await()
            android.util.Log.d("NotesRepository", "Note saved: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("NotesRepository", "Save failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateNote(noteId: String, content: String): Result<Unit> {
        return try {
            firestore.collection("notes")
                .document(noteId)
                .update(
                    mapOf(
                        "content" to content,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("NotesRepository", "Update failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun togglePin(noteId: String, pinned: Boolean): Result<Unit> {
        return try {
            firestore.collection("notes")
                .document(noteId)
                .update(
                    mapOf(
                        "pinned" to pinned,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("NotesRepository", "Pin toggle failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            firestore.collection("notes")
                .document(noteId)
                .delete()
                .await()
            android.util.Log.d("NotesRepository", "Note deleted: $noteId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("NotesRepository", "Delete failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getNoteCount(userId: String, subjectName: String, mode: String): Int {
        return withTimeoutOrNull(5000) {
            try {
                val snapshot = firestore.collection("notes")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("subjectName", subjectName)
                    .whereEqualTo("mode", mode)
                    .get()
                    .await()
                snapshot.size()
            } catch (e: Exception) {
                0
            }
        } ?: 0
    }

    suspend fun getLastNoteDate(userId: String, subjectName: String, mode: String): Long? {
        return withTimeoutOrNull(5000) {
            try {
                val snapshot = firestore.collection("notes")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("subjectName", subjectName)
                    .whereEqualTo("mode", mode)
                    .get()
                    .await()

                snapshot.documents
                    .mapNotNull { it.getLong("updatedAt") }
                    .maxOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }
}
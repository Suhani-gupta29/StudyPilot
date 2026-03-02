package com.example.studypilot.ui.chat

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val roomsRef = db.collection("public_rooms")

    // ── Rooms ────────────────────────────────────────────────────────────────

    /**
     * Listen to ALL public rooms in real time, ordered by last activity.
     */
    fun listenToAllRooms(onUpdate: (List<Room>) -> Unit): ListenerRegistration {
        return roomsRef
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val rooms = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Room::class.java)?.copy(id = doc.id)
                }
                onUpdate(rooms)
            }
    }

    /**
     * Create a new public room. Returns the generated room ID.
     */
    fun createRoom(
        name: String,
        description: String,
        subject: String,
        createdByUid: String,
        createdByName: String,
        onSuccess: (roomId: String) -> Unit,
        onFailure: () -> Unit
    ) {
        val newRef = roomsRef.document()
        val room = hashMapOf(
            "id"            to newRef.id,
            "name"          to name,
            "description"   to description,
            "subject"       to subject,
            "memberIds" to listOf(createdByUid),
            "createdBy"     to createdByUid,
            "createdByName" to createdByName,
            "memberCount"   to 1,
            "createdAt"     to System.currentTimeMillis(),
            "lastMessage"   to "",
            "lastMessageAt" to System.currentTimeMillis()
        )
        newRef.set(room)
            .addOnSuccessListener { onSuccess(newRef.id) }
            .addOnFailureListener { onFailure() }
    }

    /**
     * Increment member count when a user joins a room.
     */
    fun joinRoom(roomId: String, userId: String) {
        val ref = roomsRef.document(roomId)
        db.runTransaction { transaction ->
            val snap = transaction.get(ref)
            val members = snap.get("memberIds") as? List<*> ?: emptyList<String>()
            if (!members.contains(userId)) {
                transaction.update(ref, "memberIds", FieldValue.arrayUnion(userId))
                transaction.update(ref, "memberCount", FieldValue.increment(1))
            }
        }
    }

    fun leaveRoom(roomId: String, userId: String) {
        val ref = roomsRef.document(roomId)
        db.runTransaction { transaction ->
            val snap = transaction.get(ref)
            val members = snap.get("memberIds") as? List<*> ?: emptyList<String>()
            if (members.contains(userId)) {
                transaction.update(ref, "memberIds", FieldValue.arrayRemove(userId))
                transaction.update(ref, "memberCount", FieldValue.increment(-1))
            }
        }
    }

    fun isUserMember(roomId: String, userId: String, onResult: (Boolean) -> Unit) {
        roomsRef.document(roomId).get().addOnSuccessListener { snap ->
            val members = snap.get("memberIds") as? List<*> ?: emptyList<String>()
            onResult(members.contains(userId))
        }
    }

    // ── Messages ─────────────────────────────────────────────────────────────

    /**
     * Listen to messages in a specific room in real time.
     */
    fun listenToMessages(roomId: String, onUpdate: (List<Message>) -> Unit): ListenerRegistration {
        return roomsRef.document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }
                onUpdate(messages)
            }
    }

    /**
     * Send a message to a room.
     */
    fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        text: String
    ) {
        val msgRef = roomsRef.document(roomId).collection("messages").document()
        val message = hashMapOf(
            "id"         to msgRef.id,
            "senderId"   to senderId,
            "senderName" to senderName,
            "text"       to text,
            "timestamp"  to System.currentTimeMillis()
        )
        msgRef.set(message)

        // Update room's last message preview
        roomsRef.document(roomId).update(
            mapOf(
                "lastMessage"   to text.take(60),
                "lastMessageAt" to System.currentTimeMillis()
            )
        )
    }
}
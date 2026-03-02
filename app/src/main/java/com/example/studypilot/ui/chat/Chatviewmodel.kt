package com.example.studypilot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// ── UI State ─────────────────────────────────────────────────────────────────

data class RoomDirectoryUiState(
    val rooms: List<Room> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedSubjectFilter: String = "All",
    val showCreateDialog: Boolean = false,
    val createName: String = "",
    val createDescription: String = "",
    val createSubject: String = "General",
    val isCreating: Boolean = false,
    val createError: String = ""
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val inputText: String = "",
    val room: Room = Room(),
    val isMember: Boolean = false
)

// ── ViewModel ────────────────────────────────────────────────────────────────

class ChatViewModel(
    private val repository: ChatRepository,
    private val userId: String,
    private val userName: String
) : ViewModel() {

    // Directory state
    private val _directoryState = MutableStateFlow(RoomDirectoryUiState())
    val directoryState: StateFlow<RoomDirectoryUiState> = _directoryState

    // Chat state
    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState

    private var roomsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    // ── Directory ────────────────────────────────────────────────────────────

    fun startListeningToRooms() {
        roomsListener?.remove()
        roomsListener = repository.listenToAllRooms { rooms ->
            _directoryState.update { it.copy(rooms = rooms, isLoading = false) }
        }
    }

    fun stopListeningToRooms() {
        roomsListener?.remove()
        roomsListener = null
    }

    fun onSearchQueryChange(query: String) {
        _directoryState.update { it.copy(searchQuery = query) }
    }

    fun onSubjectFilterChange(subject: String) {
        _directoryState.update { it.copy(selectedSubjectFilter = subject) }
    }

    fun filteredRooms(): List<Room> {
        val state = _directoryState.value
        return state.rooms.filter { room ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    room.name.contains(state.searchQuery, ignoreCase = true) ||
                    room.description.contains(state.searchQuery, ignoreCase = true)

            val matchesSubject = state.selectedSubjectFilter == "All" ||
                    room.subject == state.selectedSubjectFilter

            matchesSearch && matchesSubject
        }
    }

    // ── Create Room Dialog ───────────────────────────────────────────────────

    fun showCreateDialog() {
        _directoryState.update { it.copy(showCreateDialog = true, createError = "") }
    }

    fun hideCreateDialog() {
        _directoryState.update {
            it.copy(
                showCreateDialog = false,
                createName = "",
                createDescription = "",
                createSubject = "General",
                createError = ""
            )
        }
    }

    fun onCreateNameChange(name: String) {
        _directoryState.update { it.copy(createName = name, createError = "") }
    }

    fun onCreateDescriptionChange(desc: String) {
        _directoryState.update { it.copy(createDescription = desc) }
    }

    fun onCreateSubjectChange(subject: String) {
        _directoryState.update { it.copy(createSubject = subject) }
    }

    fun createRoom(onSuccess: (roomId: String) -> Unit) {
        val state = _directoryState.value
        if (state.createName.isBlank()) {
            _directoryState.update { it.copy(createError = "Room name cannot be empty") }
            return
        }

        _directoryState.update { it.copy(isCreating = true) }

        repository.createRoom(
            name          = state.createName.trim(),
            description   = state.createDescription.trim(),
            subject       = state.createSubject,
            createdByUid  = userId,
            createdByName = userName,
            onSuccess     = { roomId ->
                _directoryState.update { it.copy(isCreating = false) }
                hideCreateDialog()
                onSuccess(roomId)
            },
            onFailure = {
                _directoryState.update {
                    it.copy(isCreating = false, createError = "Failed to create room. Try again.")
                }
            }
        )
    }

    fun joinRoom(roomId: String) {
        repository.joinRoom(roomId, userId)
    }

    fun leaveRoom(roomId: String) {
        repository.leaveRoom(roomId, userId)
    }

    // ── Chat ─────────────────────────────────────────────────────────────────

    fun enterRoom(room: Room) {
        _chatState.update { it.copy(room = room, isLoading = true, messages = emptyList()) }
        messagesListener?.remove()
        messagesListener = repository.listenToMessages(room.id) { messages ->
            _chatState.update { it.copy(messages = messages, isLoading = false) }
        }
    }

    fun exitRoom() {
        messagesListener?.remove()
        messagesListener = null
        _chatState.update { ChatUiState() }
    }

    fun onInputChange(text: String) {
        _chatState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _chatState.value.inputText.trim()
        val roomId = _chatState.value.room.id
        if (text.isBlank() || roomId.isBlank()) return

        _chatState.update { it.copy(inputText = "") }
        repository.sendMessage(roomId, userId, userName, text)
    }

    fun checkAndJoin(roomId: String) {
        repository.isUserMember(roomId, userId) { isMember ->
            _chatState.update { it.copy(isMember = isMember) }
        }
    }

    fun joinCurrentRoom() {
        val roomId = _chatState.value.room.id
        repository.joinRoom(roomId, userId)
        _chatState.update { it.copy(isMember = true) }
    }

    override fun onCleared() {
        super.onCleared()
        roomsListener?.remove()
        messagesListener?.remove()
    }
}

// ── Factory ──────────────────────────────────────────────────────────────────

class ChatViewModelFactory(
    private val userId: String,
    private val userName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(ChatRepository(), userId, userName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
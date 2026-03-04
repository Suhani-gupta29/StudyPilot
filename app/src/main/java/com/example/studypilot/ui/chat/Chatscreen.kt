package com.example.studypilot.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studypilot.ui.theme.Roboto
import java.text.SimpleDateFormat
import java.util.*

// ── Colors — matches SubjectsScreen exactly ───────────────────────────────────
private val primaryBlue    = Color(0xFF1E88E5)
private val gradientTop    = Color(0xFFD6ECFB)
private val gradientBottom = Color(0xFFFFFFFF)
private val textPrimary    = Color(0xFF0A2540)
private val textSecondary  = Color(0xFF1E3A5F)
private val softDivider    = Color(0xFFE2EAF4)
private val cardWhite      = Color(0xFFFFFFFF)
private val cardBorder     = Color(0xFFCBD5E1)
private val myBubble       = Color(0xFF1E88E5)
private val inputFieldBg   = Color(0xFFF5FAFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    room: Room,
    currentUserId: String,
    onBack: () -> Unit
) {
    val state by viewModel.chatState.collectAsState()
    val listState = rememberLazyListState()
    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(room.id) {
        viewModel.enterRoom(room)
        viewModel.checkAndJoin(room.id)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.exitRoom() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(gradientTop, gradientBottom)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = textPrimary
                                )
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = room.name,
                                    fontFamily = Roboto,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = textPrimary,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = primaryBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${room.memberCount} members",
                                        fontFamily = Roboto,
                                        fontSize = 12.sp,
                                        color = primaryBlue,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        },
                        actions = {
                            // Subject pill
                            if (room.subject.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .border(1.dp, primaryBlue, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = room.subject.uppercase(),
                                        fontFamily = Roboto,
                                        color = primaryBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            IconButton(onClick = { showLeaveDialog = true }) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = "Leave Room",
                                    tint = Color(0xFFE53935)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                    Divider(color = softDivider, thickness = 1.dp)
                }
            },
            bottomBar = {
                if (state.isMember) {
                    MessageInputBar(
                        value = state.inputText,
                        onValueChange = viewModel::onInputChange,
                        onSend = viewModel::sendMessage
                    )
                } else {
                    JoinBar(onJoin = { viewModel.joinCurrentRoom() })
                }
            }
        ) { padding ->

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryBlue)
                    }
                }

                state.messages.isEmpty() -> {
                    EmptyChat(modifier = Modifier.fillMaxSize().padding(padding))
                }

                else -> {
                    val grouped = state.messages.groupBy { formatDate(it.timestamp) }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        grouped.forEach { (date, msgs) ->
                            item { DateDivider(date = date) }
                            items(msgs, key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    isMe = message.senderId == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Leave Dialog ──────────────────────────────────────────────────────────
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = {
                Text(
                    "Leave Room?",
                    fontFamily = Roboto,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Text(
                    "You can rejoin anytime from the room directory.",
                    fontFamily = Roboto,
                    color = textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveRoom(room.id)
                        showLeaveDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935))
                ) {
                    Text("Leave", fontFamily = Roboto, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel", fontFamily = Roboto, color = textSecondary)
                }
            },
            containerColor = cardWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyChat(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("💬", fontSize = 52.sp)
            Text(
                "No messages yet",
                fontFamily = Roboto,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textPrimary
            )
            Text(
                "Be the first to start the discussion!",
                fontFamily = Roboto,
                fontSize = 13.sp,
                color = textSecondary
            )
        }
    }
}

// ── Message Bubble ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: Message, isMe: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Avatar + name row for others
        if (!isMe) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(avatarColor(message.senderName)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.senderName.firstOrNull()?.uppercase() ?: "?",
                        fontFamily = Roboto,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message.senderName,
                    fontFamily = Roboto,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
            }
        }

        // Bubble
        Row(
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(
                        elevation = if (isMe) 4.dp else 2.dp,
                        shape = RoundedCornerShape(
                            topStart = if (isMe) 18.dp else 4.dp,
                            topEnd = if (isMe) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        ),
                        spotColor = if (isMe) primaryBlue.copy(alpha = 0.25f)
                        else Color.Black.copy(alpha = 0.06f)
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isMe) 18.dp else 4.dp,
                            topEnd = if (isMe) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .then(
                        if (!isMe) Modifier.border(1.dp, cardBorder,
                            RoundedCornerShape(
                                topStart = 4.dp, topEnd = 18.dp,
                                bottomStart = 18.dp, bottomEnd = 18.dp
                            )
                        ) else Modifier
                    )
                    .background(if (isMe) myBubble else cardWhite)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        fontFamily = Roboto,
                        color = if (isMe) Color.White else textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                    Text(
                        text = formatTime(message.timestamp),
                        fontFamily = Roboto,
                        color = if (isMe) Color.White.copy(alpha = 0.6f)
                        else textSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// ── Date Divider ──────────────────────────────────────────────────────────────

@Composable
private fun DateDivider(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(modifier = Modifier.weight(1f), color = softDivider)
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .background(Color(0xFFE8F3FC), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = date,
                fontFamily = Roboto,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = primaryBlue
            )
        }
        Divider(modifier = Modifier.weight(1f), color = softDivider)
    }
}

// ── Join Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun JoinBar(onJoin: () -> Unit) {
    Surface(
        shadowElevation = 8.dp,
        color = cardWhite,
        modifier = Modifier.border(
            width = 1.dp,
            color = softDivider,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "You're not a member yet",
                    fontFamily = Roboto,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textPrimary
                )
                Text(
                    "Join to send messages",
                    fontFamily = Roboto,
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }
            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.shadow(
                    4.dp,
                    RoundedCornerShape(12.dp),
                    spotColor = primaryBlue.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    "Join Room",
                    fontFamily = Roboto,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ── Message Input Bar ─────────────────────────────────────────────────────────

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = cardWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Type a message...",
                        fontFamily = Roboto,
                        color = textSecondary
                    )
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = cardBorder,
                    focusedContainerColor = inputFieldBg,
                    unfocusedContainerColor = inputFieldBg,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            val canSend = value.isNotBlank()
            FloatingActionButton(
                onClick = { if (canSend) onSend() },
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = if (canSend) 6.dp else 0.dp,
                        shape = CircleShape,
                        spotColor = primaryBlue.copy(alpha = 0.35f)
                    ),
                containerColor = if (canSend) primaryBlue else Color(0xFFE2EAF4),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

private fun formatDate(timestamp: Long): String {
    val cal = Calendar.getInstance().also { it.timeInMillis = timestamp }
    val today = Calendar.getInstance()
    return when {
        cal.get(Calendar.DATE) == today.get(Calendar.DATE) -> "Today"
        cal.get(Calendar.DATE) == today.get(Calendar.DATE) - 1 -> "Yesterday"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private val avatarColors = listOf(
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFF00897B),
    Color(0xFF43A047), Color(0xFFE53935), Color(0xFFFFB300),
    Color(0xFF3949AB), Color(0xFFF4511E)
)

private fun avatarColor(name: String): Color =
    avatarColors[name.hashCode().and(0x7fffffff) % avatarColors.size]
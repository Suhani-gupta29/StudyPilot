package com.example.studypilot.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studypilot.data.AiMessage
import com.example.studypilot.data.ImportedContent
import com.example.studypilot.ui.theme.Roboto
import kotlinx.coroutines.delay

private val primaryBlue = Color(0xFF1E88E5)
private val textPrimary = Color(0xFF102A43)
private val textSecondary = Color(0xFF627D98)
private val softDivider = Color(0xFFE6ECF5)
private val userBubble = Color(0xFF1E88E5)
private val aiBubble = Color(0xFFF1F5F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatPanel(
    subjectName: String,
    messages: List<AiMessage>,
    currentInput: String,
    isLoading: Boolean,
    errorMessage: String?,
    // ── new parameters (with defaults so existing call-sites still compile) ──
    chatAttachments: List<ImportedContent> = emptyList(),
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onClose: () -> Unit,
    onClearError: () -> Unit,
    onAttachFile: () -> Unit = {},
    onRemoveAttachment: (ImportedContent) -> Unit = {}
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.62f)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(primaryBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 16.sp)
                }
                Column {
                    Text(
                        text = "Study Assistant",
                        fontFamily = Roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary
                    )
                    Text(
                        text = subjectName,
                        fontFamily = Roboto,
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
            }
        }

        Divider(color = softDivider)

        // ── Error banner ──
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBEE))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    color = Color(0xFFD32F2F),
                    fontFamily = Roboto,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearError) {
                    Text("Dismiss", fontSize = 11.sp, color = Color(0xFFD32F2F))
                }
            }
        }

        // ── Messages list ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatMessageBubble(message = message)
            }
        }

        Divider(color = softDivider)

        // ── Chat attachment chips (queued files for next message) ──
        if (chatAttachments.isNotEmpty()) {
            ImportedContentChips(
                contents = chatAttachments,
                onRemove = onRemoveAttachment,
                modifier = Modifier.padding(top = 4.dp)
            )
            Divider(color = softDivider)
        }

        // ── Input row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 📎 Attach file button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE3F2FD), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onAttachFile) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attach PDF or image",
                        tint = primaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Text input
            TextField(
                value = currentInput,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (chatAttachments.isNotEmpty())
                            "Ask about the attached file…"
                        else
                            "Ask about $subjectName…",
                        fontFamily = Roboto,
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontFamily = Roboto
                )
            )

            // Send button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if ((currentInput.isNotBlank() || chatAttachments.isNotEmpty()) && !isLoading)
                            primaryBlue else Color(0xFFE0E7F1),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = primaryBlue
                    )
                } else {
                    IconButton(
                        onClick = onSendMessage,
                        enabled = currentInput.isNotBlank() || chatAttachments.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (currentInput.isNotBlank() || chatAttachments.isNotEmpty())
                                Color.White else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ChatMessageBubble(message: AiMessage) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    // Tracks whether the ✓ "copied" state is showing
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // 🤖 avatar (AI only)
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(primaryBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Bubble + copy button stacked in a Column (AI only)
        Column(horizontalAlignment = Alignment.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(if (isUser) userBubble else aiBubble)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (message.isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(textSecondary, CircleShape)
                            )
                        }
                    }
                } else {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = if (isUser) Color.White else textPrimary,
                        fontFamily = Roboto,
                        lineHeight = 20.sp
                    )
                }
            }

            // Copy button — shown only for finished AI messages
            if (!isUser && !message.isLoading) {
                Spacer(modifier = Modifier.height(2.dp))
                // Reset the "copied" tick after 2 seconds
                LaunchedEffect(copied) {
                    if (copied) {
                        delay(2000)
                        copied = false
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                ) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            copied = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = if (copied) "Copied" else "Copy response",
                            tint = if (copied) Color(0xFF4CAF50) else textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (copied) {
                        Text(
                            text = "Copied!",
                            fontSize = 10.sp,
                            color = Color(0xFF4CAF50),
                            fontFamily = Roboto
                        )
                    }
                }
            }
        }

        // 👤 avatar (user only)
        if (isUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(primaryBlue.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 14.sp)
            }
        }
    }
}
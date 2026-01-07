 package com.example.studypilot.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, isError: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, color = Color(0xFFE3F2FD).copy(alpha = 0.7f)) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
            disabledContainerColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedIndicatorColor = Color.White,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true,
        isError = isError
    )
}

@Composable
fun PasswordTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, isError: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, color = Color(0xFFE3F2FD).copy(alpha = 0.7f)) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
            disabledContainerColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedIndicatorColor = Color.White,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        isError = isError
    )
}

@Composable
fun AuthButton(text: String, onClick: () -> Unit, isLoading: Boolean) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E88E5)),
        enabled = !isLoading
    ) {
        if (isLoading) {
            // Show a loading indicator if needed
        } else {
            Text(text = text, fontSize = 16.sp)
        }
    }
}

package com.example.studypilot.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// Request data classes
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val temperature: Double = 0.7
)

data class GroqMessage(
    val role: String,
    val content: String
)

// Response data classes
data class GroqResponse(
    val choices: List<GroqChoice>?
)

data class GroqChoice(
    val message: GroqMessage?
)

class AiChatService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun sendMessage(
        apiKey: String,
        subjectName: String,
        mode: String,
        chatHistory: List<AiMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Build system prompt
            val systemPrompt = buildSystemPrompt(subjectName, mode)

            // Convert chat history to Groq format
            val messages = mutableListOf<GroqMessage>()
            messages.add(GroqMessage(role = "system", content = systemPrompt))

            // Add chat history (exclude loading messages)
            chatHistory
                .filter { !it.isLoading }
                .forEach { msg ->
                    messages.add(GroqMessage(role = msg.role, content = msg.content))
                }

            val requestBody = GroqRequest(
                model = "llama-3.1-8b-instant",
                messages = messages,
                maxTokens = 1024
            )

            val json = gson.toJson(requestBody)
            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                android.util.Log.e("AiChatService", "API Error: ${response.code} - $errorBody")
                return@withContext Result.failure(
                    Exception("API Error ${response.code}: Please try again")
                )
            }

            val responseBody = response.body?.string()
            if (responseBody == null) {
                return@withContext Result.failure(Exception("Empty response from AI"))
            }

            val groqResponse = gson.fromJson(responseBody, GroqResponse::class.java)
            val content = groqResponse.choices?.firstOrNull()?.message?.content

            if (content.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No response from AI"))
            }

            android.util.Log.d("AiChatService", "AI response received successfully")
            Result.success(content.trim())

        } catch (e: Exception) {
            android.util.Log.e("AiChatService", "Failed to send message: ${e.message}")
            Result.failure(Exception("Connection failed. Check internet and try again."))
        }
    }

    private fun buildSystemPrompt(subjectName: String, mode: String): String {
        val modeDescription = when (mode.uppercase()) {
            "EXAM" -> "preparing for an exam and needs precise, detailed explanations"
            "FOCUS" -> "in a focused deep study session and needs clear, structured answers"
            "CASUAL" -> "casually learning and prefers simple, easy to understand explanations"
            else -> "studying"
        }

        return """
            You are a helpful study assistant for a student studying $subjectName.
            The student is $modeDescription.
            
            Guidelines:
            - Keep responses concise and focused on $subjectName
            - Use simple language appropriate for studying
            - If asked something unrelated to studying, politely redirect
            - Use bullet points or numbered lists when explaining steps
            - Be encouraging and supportive
            - Maximum 3-4 paragraphs per response
        """.trimIndent()
    }
}
package com.prplegryn.verbo.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAiChatClient {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun chat(
        config: AiConfiguration,
        systemPrompt: String,
        userPrompt: String,
    ): String = withContext(Dispatchers.IO) {
        val body = ChatRequest(
            model = config.model.trim(),
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt),
            ),
            temperature = config.temperature.coerceIn(0.0, 2.0),
            maxTokens = config.maxOutputTokens.coerceIn(512, 32000),
        )
        val request = Request.Builder()
            .url(config.endpoint())
            .post(json.encodeToString(ChatRequest.serializer(), body).toRequestBody(JSON))
            .addHeader("Content-Type", "application/json")
            .apply {
                val key = config.apiKey.trim()
                if (key.isNotEmpty()) addHeader("Authorization", "Bearer $key")
            }
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body.string()
            if (!response.isSuccessful) {
                val clipped = responseText.take(700)
                error("AI API ${response.code}: $clipped")
            }
            val parsed = json.decodeFromString(ChatResponse.serializer(), responseText)
            val content = parsed.choices.firstOrNull()?.message?.content.orEmpty()
            ThinkTagCleaner.clean(content)
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @SerialName("max_tokens")
    val maxTokens: Int,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatResponse(
    val choices: List<Choice> = emptyList(),
)

@Serializable
private data class Choice(
    val message: ChatMessage = ChatMessage("", ""),
)

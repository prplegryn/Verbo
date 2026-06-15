package com.prplegryn.verbo.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiConfiguration(
    val baseUrl: String = "https://api.openai.com/v1",
    val endpointPath: String = "/chat/completions",
    val apiKey: String = "",
    val model: String = "gpt-4.1-mini",
    val temperature: Double = 0.2,
    val maxOutputTokens: Int = 4096,
) {
    fun endpoint(): String {
        val base = baseUrl.trim().trimEnd('/')
        val path = endpointPath.trim().ifBlank { "/chat/completions" }
        return base + if (path.startsWith("/")) path else "/$path"
    }

    fun isUsable(): Boolean =
        baseUrl.isNotBlank() && endpointPath.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()
}

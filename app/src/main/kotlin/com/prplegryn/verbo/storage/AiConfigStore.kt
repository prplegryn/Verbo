package com.prplegryn.verbo.storage

import android.content.Context
import com.prplegryn.verbo.ai.AiConfiguration
import kotlinx.serialization.json.Json

class AiConfigStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences("ai_config", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun load(): AiConfiguration {
        val raw = preferences.getString(KEY_CONFIG, null) ?: return AiConfiguration()
        return runCatching {
            json.decodeFromString(AiConfiguration.serializer(), raw)
        }.getOrDefault(AiConfiguration())
    }

    fun save(config: AiConfiguration) {
        preferences.edit()
            .putString(KEY_CONFIG, json.encodeToString(AiConfiguration.serializer(), config))
            .apply()
    }

    companion object {
        private const val KEY_CONFIG = "config"
    }
}

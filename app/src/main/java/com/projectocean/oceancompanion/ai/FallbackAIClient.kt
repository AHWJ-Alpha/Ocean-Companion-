package com.projectocean.oceancompanion.ai

import com.projectocean.oceancompanion.memory.PreferencesStore
import kotlinx.coroutines.flow.first

class FallbackAIClient(private val preferences: PreferencesStore) {
    suspend fun complete(request: AIRequest): AIResponse {
        val profiles = preferences.resolvedApiProfiles().filter { it.isUsable() }
        if (profiles.isEmpty()) return AIResponse("", "none")
        var lastError = ""
        profiles.forEach { profile ->
            val response = CustomAIClient(profile.baseUrl, profile.apiKey, profile.model).complete(request)
            if (!response.text.looksLikeFailure()) return response.copy(provider = profile.label.ifBlank { profile.provider })
            lastError = response.text
        }
        return AIResponse(lastError, "fallback")
    }

    suspend fun completeVision(prompt: String, imageBase64Png: String): AIResponse {
        val profiles = preferences.resolvedApiProfiles()
            .filter { it.isUsable() && it.supportsVision }
            .ifEmpty { preferences.resolvedApiProfiles().filter { it.isUsable() } }
        if (profiles.isEmpty()) return AIResponse("", "none")
        var lastError = ""
        profiles.forEach { profile ->
            val response = CustomAIClient(profile.baseUrl, profile.apiKey, profile.model).completeVision(prompt, imageBase64Png)
            if (!response.text.looksLikeFailure()) return response.copy(provider = profile.label.ifBlank { profile.provider })
            lastError = response.text
        }
        return AIResponse(lastError, "fallback")
    }

    private fun String.looksLikeFailure(): Boolean {
        val text = trim()
        if (text.isBlank()) return true
        return text.startsWith("AI API request failed:") ||
            text.startsWith("AI API connection failed:") ||
            text.startsWith("AI API did not return readable text")
    }
}

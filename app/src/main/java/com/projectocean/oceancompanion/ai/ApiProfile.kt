package com.projectocean.oceancompanion.ai

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ApiProfile(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "OpenAI",
    val provider: String = "openai",
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val enabled: Boolean = true,
    val supportsVision: Boolean = false
) {
    fun isUsable(): Boolean = enabled && baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("label", label)
        .put("provider", provider)
        .put("baseUrl", baseUrl)
        .put("apiKey", apiKey)
        .put("model", model)
        .put("enabled", enabled)
        .put("supportsVision", supportsVision)

    companion object {
        fun fromJson(json: JSONObject): ApiProfile = ApiProfile(
            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
            label = json.optString("label").ifBlank { json.optString("provider").ifBlank { "AI" } },
            provider = json.optString("provider").ifBlank { "custom" },
            baseUrl = json.optString("baseUrl"),
            apiKey = json.optString("apiKey"),
            model = json.optString("model"),
            enabled = json.optBoolean("enabled", true),
            supportsVision = json.optBoolean("supportsVision", false)
        )

        fun encode(profiles: List<ApiProfile>): String {
            val array = JSONArray()
            profiles.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun decode(raw: String): List<ApiProfile> = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(::fromJson)
            }
        }.getOrDefault(emptyList())
    }
}

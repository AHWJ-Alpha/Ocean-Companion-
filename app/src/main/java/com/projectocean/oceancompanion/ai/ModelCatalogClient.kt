package com.projectocean.oceancompanion.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ModelCatalogClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetchModels(profile: ApiProfile): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = profile.baseUrl.trim().trimEnd('/') + "/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${profile.apiKey.trim()}")
                .addHeader("Accept", "application/json")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("HTTP ${response.code}: ${raw.take(180)}")
                val root = JSONObject(raw)
                val data = root.optJSONArray("data") ?: return@use emptyList<String>()
                (0 until data.length()).mapNotNull { index ->
                    val item = data.optJSONObject(index)
                    item?.optString("id")?.trim()?.takeIf { it.isNotBlank() }
                }.distinct().sorted()
            }
        }
    }

    companion object {
        fun fallbackModels(provider: String): List<String> = when (provider.lowercase()) {
            "openai" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "o4-mini", "o3-mini")
            "deepseek" -> listOf("deepseek-chat", "deepseek-reasoner")
            "bailian" -> listOf("qwen-plus", "qwen-max", "qwen-vl-plus", "qwq-plus")
            "zhipu" -> listOf("glm-4-flash", "glm-4-plus", "glm-z1-flash")
            "moonshot" -> listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")
            "openrouter" -> listOf("openai/gpt-4o-mini", "deepseek/deepseek-chat", "deepseek/deepseek-r1", "google/gemini-2.0-flash-001")
            else -> listOf("gpt-4o-mini", "deepseek-chat", "qwen-plus")
        }

        fun looksLikeReasoningModel(model: String): Boolean {
            val normalized = model.lowercase()
            return listOf("reason", "thinking", "deepseek-r1", "deepseek-reasoner", "qwq", "o3", "o4", "glm-z1").any(normalized::contains)
        }
    }
}

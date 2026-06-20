package com.projectocean.oceancompanion.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CustomAIClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : AIClient {
    override suspend fun complete(request: AIRequest): AIResponse = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("model", model.trim())
            .put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", request.persona.systemPrompt))
                put(JSONObject().put("role", "user").put("content", request.prompt))
            })
            .put("temperature", 0.7)
            .put("stream", false)

        val httpRequest = Request.Builder()
            .url(baseUrl.trim().trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            httpClient.newCall(httpRequest).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AIResponse("AI API request failed: ${response.code} ${raw.take(500)}", "custom")
                }
                val content = JSONObject(raw)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                    .trim()
                AIResponse(content.ifBlank { "AI API returned an empty message." }, "custom")
            }
        }.getOrElse { error ->
            AIResponse("AI API connection failed: ${error.message ?: error::class.java.simpleName}", "custom")
        }
    }
}

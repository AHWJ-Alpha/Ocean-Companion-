package com.projectocean.oceancompanion.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
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

        executeChatRequest(body)
    }

    suspend fun completeVision(prompt: String, imageBase64Png: String): AIResponse = withContext(Dispatchers.IO) {
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/png;base64,$imageBase64Png")))
        val body = JSONObject()
            .put("model", model.trim())
            .put("messages", JSONArray().apply {
                put(JSONObject().put("role", "user").put("content", content))
            })
            .put("temperature", 0.45)
            .put("stream", false)

        executeChatRequest(body)
    }

    private fun executeChatRequest(body: JSONObject): AIResponse {
        val httpRequest = Request.Builder()
            .url(baseUrl.trim().trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return runCatching {
            httpClient.newCall(httpRequest).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use AIResponse("AI API request failed: ${response.code} ${raw.take(500)}", "custom")
                }
                val content = extractResponseText(raw)
                AIResponse(content.ifBlank { "AI API did not return readable text. Raw response: ${raw.take(500)}" }, "custom")
            }
        }.getOrElse { error ->
            AIResponse("AI API connection failed: ${error.message ?: error::class.java.simpleName}", "custom")
        }
    }

    private fun extractResponseText(raw: String): String {
        if (raw.isBlank()) return ""
        val root = JSONTokener(raw).nextValue() as? JSONObject ?: return raw.trim()
        val choices = root.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val first = choices.optJSONObject(0)
            val message = first?.optJSONObject("message")
            val messageText = listOfNotNull(
                message?.readFlexibleText("content"),
                message?.readFlexibleText("reasoning_content"),
                first?.optJSONObject("delta")?.readFlexibleText("content"),
                first?.readFlexibleText("text")
            ).firstOrNull { it.isNotBlank() }
            if (!messageText.isNullOrBlank()) return messageText.trim()
        }
        return listOf(
            root.readFlexibleText("output_text"),
            root.readFlexibleText("text"),
            root.optJSONObject("message")?.readFlexibleText("content").orEmpty()
        ).firstOrNull { it.isNotBlank() }.orEmpty().trim()
    }

    private fun JSONObject.readFlexibleText(key: String): String {
        if (!has(key) || isNull(key)) return ""
        return when (val value = opt(key)) {
            is String -> value
            is JSONArray -> (0 until value.length()).joinToString("") { index ->
                when (val item = value.opt(index)) {
                    is String -> item
                    is JSONObject -> item.optString("text").ifBlank { item.optString("content") }
                    else -> ""
                }
            }
            is JSONObject -> value.optString("text").ifBlank { value.optString("content") }
            else -> value?.toString().orEmpty()
        }
    }
}

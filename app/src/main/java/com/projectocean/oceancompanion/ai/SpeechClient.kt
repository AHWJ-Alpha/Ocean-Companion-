package com.projectocean.oceancompanion.ai

import com.projectocean.oceancompanion.memory.PreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.concurrent.TimeUnit

data class SpeechAudio(val bytes: ByteArray, val extension: String)

class SpeechClient(
    private val preferences: PreferencesStore,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()
) {
    suspend fun transcribe(wavFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val provider = preferences.sttProvider.first().normalizeSpeechProvider()
            val apiKey = preferences.sttApiKey.first().trim()
            if (provider == "system") error("系统语音识别由本地处理")
            if (apiKey.isBlank()) error("STT API Key 为空")
            val baseUrl = preferences.sttApiBaseUrl.first().ifBlank { defaultBaseUrl(provider) }
            val model = preferences.sttModel.first().ifBlank { defaultSttModel(provider) }
            val language = preferences.sttLanguage.first().ifBlank { "zh" }.replace("-CN", "")
            transcribeOpenAiCompatible(baseUrl, apiKey, model, language, wavFile)
        }
    }

    suspend fun synthesize(text: String): Result<SpeechAudio> = withContext(Dispatchers.IO) {
        runCatching {
            val provider = preferences.ttsProvider.first().normalizeSpeechProvider()
            val apiKey = preferences.ttsApiKey.first().trim()
            if (provider == "system") error("系统语音合成由本地处理")
            if (apiKey.isBlank()) error("TTS API Key 为空")
            val baseUrl = preferences.ttsApiBaseUrl.first().ifBlank { defaultBaseUrl(provider) }
            val model = preferences.ttsModel.first().ifBlank { defaultTtsModel(provider) }
            val voice = preferences.ttsVoice.first().ifBlank { defaultTtsVoice(provider) }
            synthesizeOpenAiCompatible(baseUrl, apiKey, model, voice, text)
        }
    }

    private fun transcribeOpenAiCompatible(baseUrl: String, apiKey: String, model: String, language: String, wavFile: File): String {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart("language", language)
            .addFormDataPart("file", wavFile.name, wavFile.asRequestBody("audio/wav".toMediaType()))
            .build()
        val request = Request.Builder()
            .url(baseUrl.trim().trimEnd('/') + "/audio/transcriptions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("STT 请求失败：${response.code} ${raw.take(240)}")
            return extractText(raw).ifBlank { error("STT 未返回可读文本") }
        }
    }

    private fun synthesizeOpenAiCompatible(baseUrl: String, apiKey: String, model: String, voice: String, text: String): SpeechAudio {
        val body = JSONObject()
            .put("model", model)
            .put("input", text.take(4000))
            .put("voice", voice)
            .put("format", "mp3")
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(baseUrl.trim().trimEnd('/') + "/audio/speech")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        httpClient.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes() ?: ByteArray(0)
            if (!response.isSuccessful) {
                val errorText = bytes.decodeToString().take(240)
                error("TTS 请求失败：${response.code} $errorText")
            }
            if (bytes.isEmpty()) error("TTS 未返回音频")
            return SpeechAudio(bytes, "mp3")
        }
    }

    private fun extractText(raw: String): String {
        if (raw.isBlank()) return ""
        val value = JSONTokener(raw).nextValue()
        return when (value) {
            is JSONObject -> value.optString("text")
                .ifBlank { value.optString("transcript") }
                .ifBlank { value.optString("output_text") }
            else -> raw.trim()
        }.trim()
    }

    companion object {
        fun defaultBaseUrl(provider: String): String = when (provider.normalizeSpeechProvider()) {
            "aliyun" -> "https://dashscope.aliyuncs.com/compatible-mode/v1"
            "openrouter" -> "https://openrouter.ai/api/v1"
            else -> "https://api.openai.com/v1"
        }

        fun defaultSttModel(provider: String): String = when (provider.normalizeSpeechProvider()) {
            "aliyun" -> "paraformer-realtime-v2"
            else -> "whisper-1"
        }

        fun defaultTtsModel(provider: String): String = when (provider.normalizeSpeechProvider()) {
            "aliyun" -> "cosyvoice-v1"
            else -> "tts-1"
        }

        fun defaultTtsVoice(provider: String): String = when (provider.normalizeSpeechProvider()) {
            "aliyun" -> "longxiaochun"
            else -> "alloy"
        }
    }
}

fun String.normalizeSpeechProvider(): String = trim().lowercase().let { value ->
    when (value) {
        "", "android", "native" -> "system"
        "custom", "openai-compatible", "openai_compatible" -> "openai"
        "bailian", "dashscope", "qwen" -> "aliyun"
        else -> value
    }
}

package com.projectocean.oceancompanion.ai

import com.projectocean.oceancompanion.memory.PreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
) {
    fun asPromptLine(index: Int): String = "$index. $title\n$url\n$snippet"
}

class SearchClient(
    private val preferences: PreferencesStore,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {
    suspend fun searchIfNeeded(query: String): List<SearchResult> {
        if (!looksLikeSearchQuery(query)) return emptyList()
        if (!preferences.searchEnabled.first()) return emptyList()
        val apiKey = preferences.searchApiKey.first().trim()
        if (apiKey.isBlank()) return emptyList()
        return search(query, apiKey)
    }

    private suspend fun search(query: String, apiKey: String): List<SearchResult> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = preferences.searchApiBaseUrl.first().trim()
            when (preferences.searchProvider.first().lowercase()) {
                "serpapi" -> searchSerpApi(query, apiKey, baseUrl)
                "custom" -> searchCustom(query, apiKey, baseUrl)
                else -> searchTavily(query, apiKey, baseUrl)
            }.take(5)
        }.getOrDefault(emptyList())
    }

    private fun searchTavily(query: String, apiKey: String, configuredBaseUrl: String): List<SearchResult> {
        val baseUrl = configuredBaseUrl.ifBlank { "https://api.tavily.com" }.trimEnd('/')
        val body = JSONObject()
            .put("api_key", apiKey)
            .put("query", query)
            .put("search_depth", "basic")
            .put("max_results", 5)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/search")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        return execute(request) { root -> parseArray(root.optJSONArray("results")) }
    }

    private fun searchSerpApi(query: String, apiKey: String, configuredBaseUrl: String): List<SearchResult> {
        val baseUrl = configuredBaseUrl.ifBlank { "https://serpapi.com/search.json" }
        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("engine", "google")
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("api_key", apiKey)
            ?.build()
            ?.toString()
            ?: "$baseUrl?q=${URLEncoder.encode(query, "UTF-8")}&api_key=${URLEncoder.encode(apiKey, "UTF-8")}"
        val request = Request.Builder().url(url).get().build()
        return execute(request) { root -> parseArray(root.optJSONArray("organic_results")) }
    }

    private fun searchCustom(query: String, apiKey: String, baseUrl: String): List<SearchResult> {
        if (baseUrl.isBlank()) return emptyList()
        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("query", query)
            ?.build()
            ?.toString()
            ?: baseUrl
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("X-API-Key", apiKey)
            .build()
        return execute(request) { root ->
            parseArray(root.optJSONArray("results") ?: root.optJSONArray("items") ?: root.optJSONArray("data"))
        }
    }

    private fun execute(request: Request, parser: (JSONObject) -> List<SearchResult>): List<SearchResult> {
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val raw = response.body?.string().orEmpty()
            val value = JSONTokener(raw).nextValue()
            return when (value) {
                is JSONObject -> parser(value)
                is JSONArray -> parseArray(value)
                else -> emptyList()
            }
        }
    }

    private fun parseArray(array: JSONArray?): List<SearchResult> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title").ifBlank { item.optString("name") }.take(120)
            val url = item.optString("url").ifBlank { item.optString("link") }
            val snippet = item.optString("snippet")
                .ifBlank { item.optString("content") }
                .ifBlank { item.optString("summary") }
                .take(500)
            if (title.isBlank() && snippet.isBlank()) null else SearchResult(title.ifBlank { url.ifBlank { "搜索结果" } }, url, snippet)
        }
    }

    companion object {
        fun looksLikeSearchQuery(text: String): Boolean {
            val lower = text.lowercase()
            return listOf("搜索", "查一下", "网上", "联网", "资料", "新闻", "最新", "来源", "引用", "论文", "价格", "版本", "search", "web", "latest")
                .any { lower.contains(it) }
        }
    }
}

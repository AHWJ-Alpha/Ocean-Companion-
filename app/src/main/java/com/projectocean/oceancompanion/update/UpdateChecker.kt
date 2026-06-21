package com.projectocean.oceancompanion.update

import com.projectocean.oceancompanion.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val body: String
)

class UpdateChecker(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    suspend fun checkLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "OceanCompanion/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val raw = response.body?.string().orEmpty()
                val root = JSONObject(raw)
                val tag = root.optString("tag_name").trim().removePrefix("v")
                val url = root.optString("html_url").trim()
                if (tag.isBlank() || url.isBlank()) return@withContext null
                if (!isNewer(tag, BuildConfig.VERSION_NAME)) return@withContext null
                UpdateInfo(
                    latestVersion = tag,
                    releaseUrl = url,
                    body = root.optString("body").trim()
                )
            }
        }.getOrNull()
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.versionParts()
        val localParts = local.versionParts()
        val max = maxOf(remoteParts.size, localParts.size)
        for (index in 0 until max) {
            val left = remoteParts.getOrElse(index) { 0 }
            val right = localParts.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun String.versionParts(): List<Int> = trim()
        .removePrefix("v")
        .split('.', '-', '_')
        .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }

    companion object {
        private const val LATEST_RELEASE_API = "https://api.github.com/repos/AHWJ-Alpha/Ocean-Companion-/releases/latest"
    }
}

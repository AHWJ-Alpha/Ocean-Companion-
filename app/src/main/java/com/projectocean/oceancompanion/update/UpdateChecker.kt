package com.projectocean.oceancompanion.update

import com.projectocean.oceancompanion.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
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
        fetchLatestRelease() ?: fetchNewestFromReleaseList()
    }

    private fun fetchLatestRelease(): UpdateInfo? = runCatching {
        val root = fetchJsonObject(LATEST_RELEASE_API) ?: return@runCatching null
        root.toUpdateInfoIfNewer()
    }.getOrNull()

    private fun fetchNewestFromReleaseList(): UpdateInfo? = runCatching {
        val array = fetchJsonArray(RELEASES_API) ?: return@runCatching null
        (0 until array.length())
            .mapNotNull { index -> array.optJSONObject(index)?.toUpdateInfoIfNewer() }
            .maxWithOrNull { left, right -> compareVersions(left.latestVersion, right.latestVersion) }
    }.getOrNull()

    private fun fetchJsonObject(url: String): JSONObject? {
        val raw = fetchRaw(url) ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    private fun fetchJsonArray(url: String): JSONArray? {
        val raw = fetchRaw(url) ?: return null
        return runCatching { JSONArray(raw) }.getOrNull()
    }

    private fun fetchRaw(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "OceanCompanion/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string().orEmpty()
        }
    }

    private fun JSONObject.toUpdateInfoIfNewer(): UpdateInfo? {
        val tag = optString("tag_name").trim().removePrefix("v")
        val url = optString("html_url").trim()
        if (tag.isBlank() || url.isBlank()) return null
        if (!isNewer(tag, BuildConfig.VERSION_NAME)) return null
        return UpdateInfo(
            latestVersion = tag,
            releaseUrl = url,
            body = optString("body").trim()
        )
    }

    private fun isNewer(remote: String, local: String): Boolean {
        return compareVersions(remote, local) > 0
    }

    private fun compareVersions(remote: String, local: String): Int {
        val remoteParts = remote.versionParts()
        val localParts = local.versionParts()
        val max = maxOf(remoteParts.size, localParts.size)
        for (index in 0 until max) {
            val left = remoteParts.getOrElse(index) { 0 }
            val right = localParts.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    private fun String.versionParts(): List<Int> = trim()
        .removePrefix("v")
        .split('.', '-', '_')
        .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }

    companion object {
        private const val LATEST_RELEASE_API = "https://api.github.com/repos/AHWJ-Alpha/Ocean-Companion-/releases/latest"
        private const val RELEASES_API = "https://api.github.com/repos/AHWJ-Alpha/Ocean-Companion-/releases?per_page=20"
    }
}

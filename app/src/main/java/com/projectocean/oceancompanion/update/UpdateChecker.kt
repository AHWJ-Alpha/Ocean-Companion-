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
        fetchLatestRelease() ?: fetchNewestFromReleaseList() ?: fetchNewestFromAtom() ?: fetchNewestFromHtml()
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

    private fun fetchNewestFromAtom(): UpdateInfo? = runCatching {
        val raw = fetchRaw(RELEASES_ATOM) ?: return@runCatching null
        findNewestTag(raw)?.toFallbackUpdateInfo()
    }.getOrNull()

    private fun fetchNewestFromHtml(): UpdateInfo? = runCatching {
        val raw = fetchRaw(RELEASES_PAGE) ?: return@runCatching null
        findNewestTag(raw)?.toFallbackUpdateInfo()
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
            .header("Accept", "application/vnd.github+json, application/atom+xml, text/html;q=0.9, */*;q=0.8")
            .header("Cache-Control", "no-cache")
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

    private fun findNewestTag(raw: String): String? {
        return TAG_REGEX.findAll(raw)
            .map { it.groupValues[1].removePrefix("v") }
            .filter { isNewer(it, BuildConfig.VERSION_NAME) }
            .maxWithOrNull { left, right -> compareVersions(left, right) }
    }

    private fun String.toFallbackUpdateInfo(): UpdateInfo = UpdateInfo(
        latestVersion = this,
        releaseUrl = "$RELEASE_TAG_PAGE_PREFIX$this",
        body = "检测到 Ocean Companion $this 可用。请打开 Release 页面下载新版 APK。"
    )

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
        private const val RELEASES_ATOM = "https://github.com/AHWJ-Alpha/Ocean-Companion-/releases.atom"
        private const val RELEASES_PAGE = "https://github.com/AHWJ-Alpha/Ocean-Companion-/releases"
        private const val RELEASE_TAG_PAGE_PREFIX = "https://github.com/AHWJ-Alpha/Ocean-Companion-/releases/tag/v"
        private val TAG_REGEX = Regex("/releases/tag/(v?[0-9]+(?:[._-][0-9]+)*)")
    }
}

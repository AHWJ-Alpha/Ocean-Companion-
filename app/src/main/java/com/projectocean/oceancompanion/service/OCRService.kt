package com.projectocean.oceancompanion.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.projectocean.oceancompanion.agent.SharedScreenContext
import java.io.BufferedReader
import java.io.InputStreamReader

class OCRService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FILE_SELECTED -> {
                val uri = intent.data
                val text = uri?.let(::readTextFromUri).orEmpty()
                SharedScreenContext.update(
                    "file",
                    if (text.isBlank()) {
                        "Selected file: ${uri?.toString().orEmpty()}\nFile text could not be extracted yet. Text, markdown, csv, json and xml files are supported in this build."
                    } else {
                        "Opened file: ${uri?.toString().orEmpty()}\n\nFile text:\n$text"
                    },
                    source = "file"
                )
            }
            ACTION_SCREEN_CAPTURE_READY -> {
                SharedScreenContext.update(
                    "screen_capture",
                    "Screen capture permission granted, but live OCR frame capture is not implemented in this build. Current screen understanding still depends on Accessibility text or selected files.",
                    source = "ocr_permission_only"
                )
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun readTextFromUri(uri: Uri): String = runCatching {
        val mime = contentResolver.getType(uri).orEmpty().lowercase()
        val name = uri.lastPathSegment.orEmpty().lowercase()
        val looksText = mime.startsWith("text/") || listOf(".txt", ".md", ".csv", ".json", ".xml", ".html", ".kt", ".java", ".py", ".js", ".ts").any { name.endsWith(it) }
        if (!looksText) return@runCatching ""
        contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                buildString {
                    var total = 0
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (total > MAX_FILE_TEXT_CHARS) break
                        appendLine(line)
                        total += line.length + 1
                    }
                }
            }
        }.orEmpty()
    }.getOrDefault("")

    companion object {
        const val ACTION_FILE_SELECTED = "com.projectocean.oceancompanion.action.FILE_SELECTED"
        const val ACTION_SCREEN_CAPTURE_READY = "com.projectocean.oceancompanion.action.SCREEN_CAPTURE_READY"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val MAX_FILE_TEXT_CHARS = 12000
    }
}

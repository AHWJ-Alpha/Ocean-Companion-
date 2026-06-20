package com.projectocean.oceancompanion.service

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Base64
import com.projectocean.oceancompanion.agent.SharedScreenContext
import com.projectocean.oceancompanion.ai.FallbackAIClient
import com.projectocean.oceancompanion.memory.PreferencesStore
import com.projectocean.oceancompanion.ocr.TextRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader

class OCRService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var preferences: PreferencesStore

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesStore(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FILE_SELECTED -> {
                val uri = intent.data
                scope.launch {
                    val result = uri?.let { readFileContext(it) } ?: FileContextResult(
                        title = "\u672a\u9009\u62e9\u6587\u4ef6",
                        text = "No file uri was provided.",
                        readable = false
                    )
                    SharedScreenContext.update("file", result.toContextText(uri), source = "file")
                    notifyFloatingService(result)
                    stopSelf(startId)
                }
                return START_NOT_STICKY
            }
            ACTION_SCREEN_CAPTURE_READY -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                scope.launch {
                    val message = analyzeCurrentScreenshot(resultCode, resultData)
                    SharedScreenContext.update("screen_capture", message, source = "vision_capture")
                    notifyScreenshotResult(message)
                    stopSelf(startId)
                }
                return START_NOT_STICKY
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun readFileContext(uri: Uri): FileContextResult = withContext(Dispatchers.IO) {
        val title = displayName(uri).ifBlank { uri.lastPathSegment.orEmpty().ifBlank { "\u5df2\u9009\u62e9\u6587\u4ef6" } }
        val mime = contentResolver.getType(uri).orEmpty().lowercase()
        val name = title.lowercase()
        val text = when {
            looksLikeText(mime, name) -> readTextFromUri(uri)
            mime.startsWith("image/") || imageExtensions.any { name.endsWith(it) } -> readImageText(uri)
            else -> ""
        }
        if (text.isBlank()) {
            FileContextResult(
                title = title,
                text = "\u5f53\u524d\u7248\u672c\u8fd8\u4e0d\u80fd\u76f4\u63a5\u63d0\u53d6\u8be5\u6587\u4ef6\u6b63\u6587\u3002\u5df2\u652f\u6301\u6587\u672c/Markdown/CSV/JSON/XML/\u4ee3\u7801\u6587\u4ef6\uff0c\u4ee5\u53ca\u56fe\u7247 OCR\u3002PDF\u3001PPT \u548c Word \u9700\u8981\u540e\u7eed\u63a5\u5165\u4e13\u95e8\u89e3\u6790\u5668\u3002",
                readable = false
            )
        } else {
            FileContextResult(title = title, text = text.take(MAX_FILE_TEXT_CHARS), readable = true)
        }
    }

    private fun readTextFromUri(uri: Uri): String = runCatching {
        val mime = contentResolver.getType(uri).orEmpty().lowercase()
        val name = uri.lastPathSegment.orEmpty().lowercase()
        if (!looksLikeText(mime, name)) return@runCatching ""
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

    private suspend fun readImageText(uri: Uri): String = runCatching {
        val bitmap = contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input) } ?: return@runCatching ""
        TextRecognizer().recognize(bitmap)
    }.getOrDefault("")

    private fun looksLikeText(mime: String, name: String): Boolean {
        return mime.startsWith("text/") || textExtensions.any { name.endsWith(it) }
    }

    private fun displayName(uri: Uri): String = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
        }.orEmpty()
    }.getOrDefault("")

    private fun notifyFloatingService(result: FileContextResult) {
        startService(Intent(this, FloatingService::class.java).apply {
            action = FloatingService.ACTION_FILE_CONTEXT_READY
            putExtra(FloatingService.EXTRA_FILE_TITLE, result.title)
            putExtra(FloatingService.EXTRA_FILE_READABLE, result.readable)
            putExtra(FloatingService.EXTRA_FILE_PREVIEW, result.text.take(900))
        })
    }

    private suspend fun analyzeCurrentScreenshot(resultCode: Int, resultData: Intent?): String {
        val bitmap = captureOneFrame(resultCode, resultData)
            ?: return "\u622a\u56fe\u5931\u8d25\uff1a\u672a\u80fd\u4ece MediaProjection \u83b7\u53d6\u5c4f\u5e55\u753b\u9762\u3002\u8bf7\u91cd\u65b0\u6388\u6743\u540e\u518d\u8bd5\u3002"
        val ocrText = runCatching { TextRecognizer().recognize(bitmap) }.getOrDefault("")
        val imageBase64 = bitmapToBase64(bitmap)
        val prompt = "\u8bf7\u5206\u6790\u8fd9\u5f20\u5f53\u524d\u624b\u673a\u5c4f\u5e55\u622a\u56fe\uff0c\u5148\u7b80\u8981\u63cf\u8ff0\u753b\u9762\uff0c\u518d\u63d0\u53d6\u5173\u952e\u4fe1\u606f\uff0c\u6700\u540e\u7ed9\u51fa\u5bf9\u7528\u6237\u6709\u5e2e\u52a9\u7684\u5efa\u8bae\u3002\n\nOCR \u6587\u672c\u53c2\u8003\uff1a\n${ocrText.ifBlank { "No OCR text." }}"
        val response = FallbackAIClient(preferences).completeVision(prompt, imageBase64)
        val result = response.text.ifBlank { ocrText.ifBlank { "\u5df2\u622a\u56fe\uff0c\u4f46\u6a21\u578b\u672a\u8fd4\u56de\u53ef\u8bfb\u5206\u6790\u3002" } }
        return "\u5f53\u524d\u622a\u56fe\u5206\u6790\uff1a\n$result"
    }

    private suspend fun captureOneFrame(resultCode: Int, resultData: Intent?): Bitmap? = withContext(Dispatchers.IO) {
        if (resultCode == 0 || resultData == null) return@withContext null
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = manager.getMediaProjection(resultCode, resultData) ?: return@withContext null
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val display = projection.createVirtualDisplay(
            "OceanCompanionCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null
        )
        try {
            delay(420)
            val image = reader.acquireLatestImage() ?: return@withContext null
            image.use {
                val plane = it.planes.firstOrNull() ?: return@withContext null
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val padded = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(buffer)
                Bitmap.createBitmap(padded, 0, 0, width, height).also { cropped ->
                    if (padded !== cropped) padded.recycle()
                }
            }
        } finally {
            display.release()
            reader.close()
            projection.stop()
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 92, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun notifyScreenshotResult(message: String) {
        startService(Intent(this, FloatingService::class.java).apply {
            action = FloatingService.ACTION_SCREEN_ANALYSIS_READY
            putExtra(FloatingService.EXTRA_SCREEN_ANALYSIS, message.take(1800))
        })
    }

    private data class FileContextResult(
        val title: String,
        val text: String,
        val readable: Boolean
    ) {
        fun toContextText(uri: Uri?): String {
            return if (readable) {
                "Opened file: $title\nUri: ${uri?.toString().orEmpty()}\n\nFile text:\n$text"
            } else {
                "Selected file: $title\nUri: ${uri?.toString().orEmpty()}\n\n$text"
            }
        }
    }

    companion object {
        const val ACTION_FILE_SELECTED = "com.projectocean.oceancompanion.action.FILE_SELECTED"
        const val ACTION_SCREEN_CAPTURE_READY = "com.projectocean.oceancompanion.action.SCREEN_CAPTURE_READY"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val MAX_FILE_TEXT_CHARS = 12000
        private val textExtensions = listOf(".txt", ".md", ".csv", ".json", ".xml", ".html", ".htm", ".kt", ".java", ".py", ".js", ".ts", ".css", ".log")
        private val imageExtensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".bmp")
    }
}

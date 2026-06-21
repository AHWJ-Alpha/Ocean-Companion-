package com.projectocean.oceancompanion

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import com.projectocean.oceancompanion.memory.PreferencesStore
import com.projectocean.oceancompanion.ocr.ScreenCapture
import com.projectocean.oceancompanion.service.FloatingService
import com.projectocean.oceancompanion.service.OCRService
import com.projectocean.oceancompanion.ui.OceanApp
import com.projectocean.oceancompanion.ui.theme.OceanTheme
import com.projectocean.oceancompanion.update.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private var pendingIconUri: Uri? = null

    private val overlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        startFloatingIfAllowed()
    }

    private val screenCapturePermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            startService(Intent(this, OCRService::class.java).apply {
                action = OCRService.ACTION_SCREEN_CAPTURE_READY
                putExtra(OCRService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(OCRService.EXTRA_RESULT_DATA, data)
            })
            Toast.makeText(this, "OCR 截屏授权已就绪", Toast.LENGTH_SHORT).show()
        }
    }

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startService(Intent(this, OCRService::class.java).apply {
                action = OCRService.ACTION_FILE_SELECTED
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
            Toast.makeText(this, "已选择文件，Ocean 将尝试读取上下文", Toast.LENGTH_SHORT).show()
        }
    }

    private val iconPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) startIconCrop(uri)
    }

    private val iconCropper = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val output = pendingIconUri
        if (result.resultCode == RESULT_OK && output != null) {
            CoroutineScope(Dispatchers.Main).launch {
                PreferencesStore(this@MainActivity).setIconImageUri(output.toString())
                Toast.makeText(this@MainActivity, "悬浮智能体图标已更新", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "未完成图标裁切", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = PreferencesStore(this)
            val themeMode by store.themeMode.collectAsState("system")
            val animePrimary by store.animePrimaryColor.collectAsState("#39C5BB")
            val animeSecondary by store.animeSecondaryColor.collectAsState("#00AEEF")
            OceanTheme(
                themeMode = themeMode,
                animePrimaryColor = animePrimary,
                animeSecondaryColor = animeSecondary
            ) {
                OceanApp(
                    onStartFloating = ::requestOverlayAndStart,
                    onOpenAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onRequestScreenCapture = { screenCapturePermission.launch(ScreenCapture(this).createCaptureIntent()) },
                    onPickFile = {
                        filePicker.launch(
                            arrayOf(
                                "text/*",
                                "image/*",
                                "application/json",
                                "application/xml",
                                "application/pdf",
                                "application/vnd.ms-powerpoint",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "*/*"
                            )
                        )
                    },
                    onPickIconImage = { iconPicker.launch(arrayOf("image/*")) }
                )
            }
        }
        showWhatsNewOnce()
        checkForUpdatesQuietly()
    }

    private fun showWhatsNewOnce() {
        CoroutineScope(Dispatchers.Main).launch {
            val store = PreferencesStore(this@MainActivity)
            val version = BuildConfig.VERSION_NAME
            if (store.lastWhatsNewVersion.first() == version) return@launch
            store.setLastWhatsNewVersion(version)
            if (!isFinishing && !isDestroyed) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("New! Ocean Companion $version")
                    .setMessage(
                        "本次更新重点：\n" +
                            "- 新增二次元主题模式，默认蓝青色调，并支持自定义两种主题色。\n" +
                            "- 主动弹幕和长对话窗口会跟随主题色变化。\n" +
                            "- 设置页保存后会保留当前配置，API Key 以加密样式显示。\n" +
                            "- 新增历史页，可按时间/主题查看、搜索和修改会话。\n" +
                            "- 统一页面圆角、间距和对齐风格。"
                    )
                    .setPositiveButton("知道了", null)
                    .show()
            }
        }
    }

    private fun checkForUpdatesQuietly() {
        CoroutineScope(Dispatchers.Main).launch {
            val store = PreferencesStore(this@MainActivity)
            val today = LocalDate.now().toString()
            if (store.lastUpdatePromptDay.first() == today) return@launch
            val update = UpdateChecker().checkLatest() ?: return@launch
            store.setLastUpdatePromptDay(today)
            if (!isFinishing && !isDestroyed) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("发现新版本 ${update.latestVersion}")
                    .setMessage(update.body.take(500).ifBlank { "检测到 Ocean Companion 有新版本可用。" })
                    .setPositiveButton("去下载") { _, _ -> openReleasePage(update.releaseUrl) }
                    .setNegativeButton("稍后", null)
                    .show()
            }
        }
    }

    private fun openReleasePage(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun startIconCrop(source: Uri) {
        val outputFile = File(cacheDir, "ocean_companion_icon.png")
        val outputUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", outputFile)
        pendingIconUri = outputUri
        val cropIntent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(source, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra("crop", "true")
            putExtra("aspectX", 1)
            putExtra("aspectY", 1)
            putExtra("outputX", 256)
            putExtra("outputY", 256)
            putExtra("scale", true)
            putExtra("return-data", false)
            putExtra("output", outputUri)
            putExtra("outputFormat", "PNG")
        }
        grantUriPermission(packageName, outputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        packageManager.queryIntentActivities(cropIntent, 0).forEach { resolveInfo ->
            grantUriPermission(resolveInfo.activityInfo.packageName, source, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            grantUriPermission(resolveInfo.activityInfo.packageName, outputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        runCatching { iconCropper.launch(cropIntent) }.onFailure {
            Toast.makeText(this, "当前系统没有可用的图片裁切器", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestOverlayAndStart() {
        if (Settings.canDrawOverlays(this)) {
            startFloatingIfAllowed()
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermission.launch(intent)
        }
    }

    private fun startFloatingIfAllowed() {
        if (Settings.canDrawOverlays(this)) {
            startForegroundService(Intent(this, FloatingService::class.java))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OceanPreview() {
    OceanTheme {
        OceanApp(
            onStartFloating = {},
            onOpenAccessibility = {},
            onRequestScreenCapture = {},
            onPickFile = {},
            onPickIconImage = {}
        )
    }
}

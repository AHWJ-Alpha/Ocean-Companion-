package com.projectocean.oceancompanion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import com.projectocean.oceancompanion.memory.PreferencesStore
import com.projectocean.oceancompanion.service.FloatingService
import com.projectocean.oceancompanion.service.OCRService
import com.projectocean.oceancompanion.ocr.ScreenCapture
import com.projectocean.oceancompanion.ui.OceanApp
import com.projectocean.oceancompanion.ui.theme.OceanTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

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
            Toast.makeText(this, "OCR \u622a\u5c4f\u6388\u6743\u5df2\u5c31\u7eea", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "\u5df2\u9009\u62e9\u6587\u4ef6\uff0cOcean \u5c06\u5c1d\u8bd5\u8bfb\u53d6\u4e0a\u4e0b\u6587", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@MainActivity, "\u60ac\u6d6e\u667a\u80fd\u4f53\u56fe\u6807\u5df2\u66f4\u65b0", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "\u672a\u5b8c\u6210\u56fe\u6807\u88c1\u5207", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OceanTheme {
                OceanApp(
                    onStartFloating = ::requestOverlayAndStart,
                    onOpenAccessibility = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onRequestScreenCapture = { screenCapturePermission.launch(ScreenCapture(this).createCaptureIntent()) },
                    onPickFile = { filePicker.launch(arrayOf("text/*", "image/*", "application/json", "application/xml", "application/pdf", "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "*/*")) },
                    onPickIconImage = { iconPicker.launch(arrayOf("image/*")) }
                )
            }
        }
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
            Toast.makeText(this, "\u5f53\u524d\u7cfb\u7edf\u6ca1\u6709\u53ef\u7528\u7684\u56fe\u7247\u88c1\u5207\u5668", Toast.LENGTH_LONG).show()
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
        OceanApp(onStartFloating = {}, onOpenAccessibility = {}, onRequestScreenCapture = {}, onPickFile = {}, onPickIconImage = {})
    }
}

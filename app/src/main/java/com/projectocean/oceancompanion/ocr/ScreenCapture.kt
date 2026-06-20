package com.projectocean.oceancompanion.ocr

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

class ScreenCapture(private val context: Context) {
    fun createCaptureIntent(): Intent {
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return manager.createScreenCaptureIntent()
    }
}

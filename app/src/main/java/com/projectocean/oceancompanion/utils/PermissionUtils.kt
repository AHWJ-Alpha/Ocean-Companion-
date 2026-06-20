package com.projectocean.oceancompanion.utils

import android.content.Context
import android.provider.Settings

object PermissionUtils {
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)
}

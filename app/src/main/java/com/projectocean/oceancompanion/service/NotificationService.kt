package com.projectocean.oceancompanion.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.projectocean.oceancompanion.MainActivity
import com.projectocean.oceancompanion.R

object NotificationService {
    const val CHANNEL_ID = "ocean_companion_status"
    const val FLOATING_NOTIFICATION_ID = 42

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Ocean Companion", NotificationManager.IMPORTANCE_LOW)
            channel.description = "\u4fdd\u6301 Ocean \u60ac\u6d6e\u4f19\u4f34\u6301\u7eed\u53ef\u7528\u3002"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun floatingNotification(context: Context): Notification {
        ensureChannel(context)
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ocean \u6b63\u5728\u684c\u9762\u966a\u4f34")
            .setContentText("\u70b9\u51fb\u60ac\u6d6e\u7403\u53ef\u5206\u6790\u3001\u804a\u5929\u6216\u603b\u7ed3\u5f53\u524d\u5185\u5bb9\u3002")
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }
}

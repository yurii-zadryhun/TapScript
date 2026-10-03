package dev.tapscript.platform.android.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class CaptureNotificationFactory(
    private val context: Context,
) {
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "TapScript screen capture",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    fun create(): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setContentTitle("TapScript is reading the screen")
        .setContentText("Screen capture is active")
        .setOngoing(true)
        .setSilent(true)
        .build()

    companion object {
        private const val CHANNEL_ID = "tapscript_capture"
    }
}

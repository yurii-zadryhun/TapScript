package dev.tapscript.platform.android.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.core.content.ContextCompat

class CaptureServiceController(
    private val context: Context,
) {
    fun createPermissionIntent(): Intent {
        val manager = context.getSystemService(MediaProjectionManager::class.java)
        return if (Build.VERSION.SDK_INT >= 34) {
            manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            manager.createScreenCaptureIntent()
        }
    }

    fun start(resultCode: Int, resultData: Intent) {
        val intent = Intent(context, ScreenCaptureService::class.java)
            .setAction(ScreenCaptureService.ACTION_START)
            .putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            .putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, resultData)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop() {
        context.stopService(Intent(context, ScreenCaptureService::class.java))
    }

    fun isPermissionResultValid(resultCode: Int, resultData: Intent?): Boolean =
        resultCode == Activity.RESULT_OK && resultData != null
}

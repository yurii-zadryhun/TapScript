package dev.tapscript.platform.android.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

data class LaunchableAppInfo(
    val label: String,
    val packageName: String,
    val icon: Bitmap? = null,
)

class AndroidInstalledAppProvider(context: Context) {
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager
    private val iconSizePx = (48 * applicationContext.resources.displayMetrics.density).toInt().coerceAtLeast(48)

    fun listLaunchableApps(): List<LaunchableAppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return queryLaunchableActivities(intent)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == applicationContext.packageName) return@mapNotNull null
                LaunchableAppInfo(
                    label = resolveInfo.loadLabel(packageManager).toString().ifBlank { packageName },
                    packageName = packageName,
                    icon = runCatching {
                        resolveInfo.loadIcon(packageManager).toBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
                    }.getOrNull(),
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    private fun queryLaunchableActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
}

package dev.tapscript.platform.android.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

data class LaunchableAppInfo(
    val label: String,
    val packageName: String,
)

class AndroidInstalledAppProvider(context: Context) {
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager

    fun listLaunchableApps(): List<LaunchableAppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return queryLaunchableActivities(intent)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == applicationContext.packageName) return@mapNotNull null
                LaunchableAppInfo(
                    label = resolveInfo.loadLabel(packageManager)?.toString()?.ifBlank { packageName } ?: packageName,
                    packageName = packageName,
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

package dev.tapscript.platform.android.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class AndroidPackageLauncher(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager

    fun launch(packageName: String): Result<Unit> = runCatching {
        require(packageName.isNotBlank()) { "Target package is empty" }
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: resolveLauncherIntent(packageName)
            ?: throw ActivityNotFoundException("No launchable activity found for $packageName")

        applicationContext.startActivity(
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
            ),
        )
    }

    private fun resolveLauncherIntent(packageName: String): Intent? {
        val query = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                query,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY)
        }.firstOrNull()?.activityInfo ?: return null

        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(resolved.packageName, resolved.name)
    }
}

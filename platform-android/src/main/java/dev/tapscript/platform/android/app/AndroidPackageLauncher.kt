package dev.tapscript.platform.android.app

import android.content.Context
import android.content.Intent

class AndroidPackageLauncher(
    private val context: Context,
) {
    fun launch(packageName: String): Result<Unit> = runCatching {
        require(packageName.isNotBlank()) { "Target package is empty" }
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

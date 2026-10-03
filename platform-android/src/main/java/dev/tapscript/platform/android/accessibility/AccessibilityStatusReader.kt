package dev.tapscript.platform.android.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager

class AccessibilityStatusReader(
    private val context: Context,
) {
    fun isEnabled(): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val expected = ComponentName(context, AutomationAccessibilityService::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val service = info.resolveInfo?.serviceInfo ?: return@any false
                ComponentName(service.packageName, service.name) == expected
            }
    }
}

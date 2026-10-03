package dev.tapscript.platform.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AutomationAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceRegistry.service = this
        refreshForegroundPackage(null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        refreshForegroundPackage(event)
    }

    fun currentForegroundPackage(): String? {
        val rootPackage = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (rootPackage != null) {
            AccessibilityServiceRegistry.activePackage = rootPackage
            return rootPackage
        }
        return AccessibilityServiceRegistry.activePackage
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (AccessibilityServiceRegistry.service === this) {
            AccessibilityServiceRegistry.service = null
            AccessibilityServiceRegistry.activePackage = null
        }
        super.onDestroy()
    }

    private fun refreshForegroundPackage(event: AccessibilityEvent?) {
        val rootPackage = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
        val eventPackage = event?.packageName
            ?.toString()
            ?.takeIf { it.isNotBlank() }
        val candidate = rootPackage ?: eventPackage ?: return
        AccessibilityServiceRegistry.activePackage = candidate
    }
}

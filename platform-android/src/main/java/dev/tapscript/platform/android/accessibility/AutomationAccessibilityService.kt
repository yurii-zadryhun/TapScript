package dev.tapscript.platform.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AutomationAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceRegistry.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.packageName
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let { AccessibilityServiceRegistry.activePackage = it }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (AccessibilityServiceRegistry.service === this) {
            AccessibilityServiceRegistry.service = null
            AccessibilityServiceRegistry.activePackage = null
        }
        super.onDestroy()
    }
}

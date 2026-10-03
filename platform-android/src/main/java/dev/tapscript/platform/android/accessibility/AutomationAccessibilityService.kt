package dev.tapscript.platform.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AutomationAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceRegistry.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (AccessibilityServiceRegistry.service === this) {
            AccessibilityServiceRegistry.service = null
        }
        super.onDestroy()
    }
}

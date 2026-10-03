package dev.tapscript.platform.android.accessibility

internal object AccessibilityServiceRegistry {
    @Volatile
    var service: AutomationAccessibilityService? = null
}

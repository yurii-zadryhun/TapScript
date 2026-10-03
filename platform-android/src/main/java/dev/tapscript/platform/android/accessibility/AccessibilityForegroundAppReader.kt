package dev.tapscript.platform.android.accessibility

import dev.tapscript.engine.api.ports.ForegroundAppReader

class AccessibilityForegroundAppReader : ForegroundAppReader {
    override fun currentPackage(): String? = AccessibilityServiceRegistry.activePackage
}

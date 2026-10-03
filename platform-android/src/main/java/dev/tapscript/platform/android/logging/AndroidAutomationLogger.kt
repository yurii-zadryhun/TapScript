package dev.tapscript.platform.android.logging

import android.util.Log
import dev.tapscript.engine.api.ports.AutomationLogger

class AndroidAutomationLogger(
    private val tag: String = "TapScript",
) : AutomationLogger {
    override fun debug(message: String) {
        Log.d(tag, message)
    }

    override fun info(message: String) {
        Log.i(tag, message)
    }

    override fun error(message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}

package dev.tapscript.app.logging

import dev.tapscript.engine.api.ports.AutomationLogger

class CompositeAutomationLogger(
    private vararg val delegates: AutomationLogger,
) : AutomationLogger {
    override fun debug(message: String) {
        delegates.forEach { it.debug(message) }
    }

    override fun info(message: String) {
        delegates.forEach { it.info(message) }
    }

    override fun error(message: String, throwable: Throwable?) {
        delegates.forEach { it.error(message, throwable) }
    }
}

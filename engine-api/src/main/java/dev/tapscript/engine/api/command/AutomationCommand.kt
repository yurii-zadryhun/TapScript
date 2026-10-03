package dev.tapscript.engine.api.command

sealed interface AutomationCommand {
    data class Tap(val targetId: String) : AutomationCommand
    data class Swipe(val targetId: String) : AutomationCommand
    data class Wait(val durationMs: Long) : AutomationCommand
    data class Log(val message: String) : AutomationCommand
}

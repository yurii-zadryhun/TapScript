package dev.tapscript.engine.api.command

sealed interface AutomationCommand {
    data class Tap(val targetId: String) : AutomationCommand
    data class RandomTap(val targetId: String, val radiusPx: Int) : AutomationCommand
    data class Swipe(val targetId: String) : AutomationCommand
    data class RandomSwipe(
        val targetId: String,
        val radiusPx: Int,
        val durationJitterMs: Long = 0,
    ) : AutomationCommand
    data class Wait(val durationMs: Long) : AutomationCommand
    data class RandomWait(val minDurationMs: Long, val maxDurationMs: Long) : AutomationCommand
    data class Log(val message: String) : AutomationCommand
}

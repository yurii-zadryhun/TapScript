package dev.tapscript.engine.api.command

import dev.tapscript.engine.api.model.VisualTone

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
    data class Highlight(
        val key: String,
        val targetId: String,
        val label: String = "",
        val tone: VisualTone = VisualTone.INFO,
    ) : AutomationCommand
    data class ShowInfo(
        val key: String,
        val title: String,
        val body: String = "",
        val tone: VisualTone = VisualTone.INFO,
    ) : AutomationCommand
    data class ClearVisual(val key: String) : AutomationCommand
    data object ClearVisuals : AutomationCommand
}

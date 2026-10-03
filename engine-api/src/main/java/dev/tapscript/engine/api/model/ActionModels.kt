package dev.tapscript.engine.api.model

data class ActionTarget(
    val id: String,
    val name: String,
    val kind: ActionKind,
    val start: NormalizedPoint,
    val end: NormalizedPoint? = null,
    val durationMs: Long = 250,
)

enum class ActionKind {
    TAP,
    SWIPE,
}

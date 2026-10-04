package dev.tapscript.engine.api.model

enum class VisualTone {
    NEUTRAL,
    INFO,
    SUCCESS,
    WARNING,
    DANGER,
}

sealed interface VisualTargetGeometry {
    data class Region(val bounds: NormalizedRect) : VisualTargetGeometry
    data class Tap(val point: NormalizedPoint) : VisualTargetGeometry
    data class Swipe(
        val start: NormalizedPoint,
        val end: NormalizedPoint,
    ) : VisualTargetGeometry
}

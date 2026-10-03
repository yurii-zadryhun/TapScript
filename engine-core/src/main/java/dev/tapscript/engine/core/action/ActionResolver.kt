package dev.tapscript.engine.core.action

import dev.tapscript.engine.api.model.ActionKind
import dev.tapscript.engine.api.model.ActionTarget
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.PixelPoint
import dev.tapscript.engine.api.model.PixelSize

class ActionResolver {
    fun resolveTap(profile: AutomationProfile, id: String, size: PixelSize): Result<PixelPoint> =
        find(profile, id, ActionKind.TAP).map { it.start.toPixel(size) }

    fun resolveSwipe(profile: AutomationProfile, id: String, size: PixelSize): Result<ResolvedSwipe> =
        find(profile, id, ActionKind.SWIPE).mapCatching { target ->
            val end = requireNotNull(target.end) { "Swipe '$id' has no end point" }
            ResolvedSwipe(
                start = target.start.toPixel(size),
                end = end.toPixel(size),
                durationMs = target.durationMs,
            )
        }

    private fun find(profile: AutomationProfile, id: String, kind: ActionKind): Result<ActionTarget> = runCatching {
        profile.actions.firstOrNull { it.id == id && it.kind == kind }
            ?: error("Action target '$id' with kind $kind was not found")
    }
}

data class ResolvedSwipe(
    val start: PixelPoint,
    val end: PixelPoint,
    val durationMs: Long,
)

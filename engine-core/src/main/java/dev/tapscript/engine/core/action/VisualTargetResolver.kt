package dev.tapscript.engine.core.action

import dev.tapscript.engine.api.model.ActionKind
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.VisualTargetGeometry

class VisualTargetResolver {
    fun resolve(profile: AutomationProfile, targetId: String): Result<VisualTargetGeometry> = runCatching {
        val regions = profile.regions.filter { it.id == targetId }
        val actions = profile.actions.filter { it.id == targetId }
        val matches = regions.size + actions.size

        require(matches == 1) {
            when {
                matches == 0 -> "Visual target '$targetId' was not found"
                else -> "Visual target '$targetId' is ambiguous across profile geometry"
            }
        }

        regions.singleOrNull()?.let { region ->
            return@runCatching VisualTargetGeometry.Region(region.bounds)
        }

        val action = actions.single()
        when (action.kind) {
            ActionKind.TAP -> VisualTargetGeometry.Tap(action.start)
            ActionKind.SWIPE -> VisualTargetGeometry.Swipe(
                start = action.start,
                end = requireNotNull(action.end) { "Swipe target '$targetId' has no end point" },
            )
        }
    }
}

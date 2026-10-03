package dev.tapscript.app.ui

import dev.tapscript.engine.api.model.ActionKind
import dev.tapscript.engine.api.model.ActionTarget
import dev.tapscript.engine.api.model.NormalizedPoint
import dev.tapscript.engine.api.model.NormalizedRect
import dev.tapscript.engine.api.model.RecognitionRegion
import java.util.UUID

internal object ProfileDraftFactory {
    fun textRegion(bounds: NormalizedRect): RecognitionRegion {
        val id = "region-${shortId()}"
        return RecognitionRegion(id = id, name = "Text region", bounds = bounds)
    }

    fun tapTarget(point: NormalizedPoint): ActionTarget {
        val id = "tap-${shortId()}"
        return ActionTarget(id = id, name = "Tap target", kind = ActionKind.TAP, start = point)
    }

    fun swipeTarget(start: NormalizedPoint, end: NormalizedPoint): ActionTarget {
        val id = "swipe-${shortId()}"
        return ActionTarget(
            id = id,
            name = "Swipe target",
            kind = ActionKind.SWIPE,
            start = start,
            end = end,
        )
    }

    private fun shortId(): String = UUID.randomUUID().toString().substring(0, 8)
}

package dev.tapscript.engine.core.action

import dev.tapscript.engine.api.model.ActionKind
import dev.tapscript.engine.api.model.ActionTarget
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.NormalizedPoint
import dev.tapscript.engine.api.model.NormalizedRect
import dev.tapscript.engine.api.model.RecognitionRegion
import dev.tapscript.engine.api.model.VisualTargetGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualTargetResolverTest {
    private val resolver = VisualTargetResolver()

    @Test
    fun resolvesRegionById() {
        val bounds = NormalizedRect(0.1f, 0.2f, 0.5f, 0.6f)
        val profile = AutomationProfile(
            name = "test",
            regions = listOf(RecognitionRegion(id = "candidate", name = "Candidate", bounds = bounds)),
        )

        assertEquals(
            VisualTargetGeometry.Region(bounds),
            resolver.resolve(profile, "candidate").getOrThrow(),
        )
    }

    @Test
    fun resolvesTapActionById() {
        val point = NormalizedPoint(0.7f, 0.8f)
        val profile = AutomationProfile(
            name = "test",
            actions = listOf(ActionTarget("equip", "Equip", ActionKind.TAP, point)),
        )

        assertEquals(
            VisualTargetGeometry.Tap(point),
            resolver.resolve(profile, "equip").getOrThrow(),
        )
    }

    @Test
    fun rejectsAmbiguousIdAcrossRegionAndAction() {
        val profile = AutomationProfile(
            name = "test",
            regions = listOf(
                RecognitionRegion(
                    id = "same",
                    name = "Same",
                    bounds = NormalizedRect(0.1f, 0.1f, 0.2f, 0.2f),
                ),
            ),
            actions = listOf(
                ActionTarget("same", "Same", ActionKind.TAP, NormalizedPoint(0.5f, 0.5f)),
            ),
        )

        assertTrue(resolver.resolve(profile, "same").isFailure)
    }
}

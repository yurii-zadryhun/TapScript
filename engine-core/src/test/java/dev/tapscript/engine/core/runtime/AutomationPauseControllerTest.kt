package dev.tapscript.engine.core.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationPauseControllerTest {
    @Test
    fun independentPauseTokensDoNotResumeEachOther() {
        val controller = AutomationPauseController()

        controller.pause(AutomationPauseController.MANUAL_TOKEN, "Paused by user")
        controller.pause(AutomationPauseController.OVERLAY_TOKEN, "Overlay open")

        assertTrue(controller.isPaused())
        assertTrue(controller.isPaused(AutomationPauseController.MANUAL_TOKEN))
        assertTrue(controller.isPaused(AutomationPauseController.OVERLAY_TOKEN))
        assertEquals("Paused by user", controller.currentReason())

        controller.resume(AutomationPauseController.OVERLAY_TOKEN)

        assertTrue(controller.isPaused())
        assertTrue(controller.isPaused(AutomationPauseController.MANUAL_TOKEN))
        assertFalse(controller.isPaused(AutomationPauseController.OVERLAY_TOKEN))
        assertEquals("Paused by user", controller.currentReason())
    }

    @Test
    fun clearRemovesEveryPauseOwner() {
        val controller = AutomationPauseController()
        controller.pause(AutomationPauseController.MANUAL_TOKEN, "Manual")
        controller.pause(AutomationPauseController.OVERLAY_TOKEN, "Overlay")

        controller.clear()

        assertFalse(controller.isPaused())
        assertEquals(null, controller.currentReason())
    }
}

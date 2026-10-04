package dev.tapscript.scripting.rhino

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.model.VisualTone
import dev.tapscript.engine.api.ports.ScriptRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RhinoScriptEngineTest {
    @Test
    fun readsNestedVariablesAndEmitsTap() {
        val result = RhinoScriptEngine().evaluate(
            ScriptRequest(
                source = "if (vars.candidate.score > vars.equipped.score) tap('equip');",
                variables = mapOf(
                    "candidate.score" to 120,
                    "equipped.score" to 100,
                ),
            ),
        )

        assertNull(result.error)
        assertEquals(listOf(AutomationCommand.Tap("equip")), result.commands)
    }

    @Test
    fun emitsExplicitRandomizedCommands() {
        val result = RhinoScriptEngine().evaluate(
            ScriptRequest(
                source = "tapRandom('equip', 12); swipeRandom('scroll', 8, 40); waitRandom(120, 180);",
                variables = emptyMap(),
            ),
        )

        assertNull(result.error)
        assertEquals(
            listOf(
                AutomationCommand.RandomTap("equip", 12),
                AutomationCommand.RandomSwipe("scroll", 8, 40),
                AutomationCommand.RandomWait(120, 180),
            ),
            result.commands,
        )
    }

    @Test
    fun emitsRuntimeVisualCommandsAndToneAliases() {
        val result = RhinoScriptEngine().evaluate(
            ScriptRequest(
                source = """
                    highlight('decision', 'equip', 'EQUIP +12.3', 'positive');
                    showInfo('details', 'Loot decision', 'Candidate wins', 'warn');
                    clearVisual('decision');
                    clearVisuals();
                """.trimIndent(),
                variables = emptyMap(),
            ),
        )

        assertNull(result.error)
        assertEquals(
            listOf(
                AutomationCommand.Highlight(
                    key = "decision",
                    targetId = "equip",
                    label = "EQUIP +12.3",
                    tone = VisualTone.SUCCESS,
                ),
                AutomationCommand.ShowInfo(
                    key = "details",
                    title = "Loot decision",
                    body = "Candidate wins",
                    tone = VisualTone.WARNING,
                ),
                AutomationCommand.ClearVisual("decision"),
                AutomationCommand.ClearVisuals,
            ),
            result.commands,
        )
    }

    @Test
    fun rejectsUnknownVisualToneWithoutEmittingPartialCommands() {
        val result = RhinoScriptEngine().evaluate(
            ScriptRequest(
                source = "highlight('decision', 'equip', 'test', 'ultraviolet');",
                variables = emptyMap(),
            ),
        )

        assertNotNull(result.error)
        assertEquals(emptyList<AutomationCommand>(), result.commands)
    }

    @Test
    fun rejectsInvalidRandomWaitRangeWithoutEmittingPartialCommands() {
        val result = RhinoScriptEngine().evaluate(
            ScriptRequest(
                source = "tap('equip'); waitRandom(300, 100);",
                variables = emptyMap(),
            ),
        )

        assertNotNull(result.error)
        assertEquals(emptyList<AutomationCommand>(), result.commands)
    }
}

package dev.tapscript.scripting.rhino

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.ports.ScriptRequest
import org.junit.Assert.assertEquals
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
}

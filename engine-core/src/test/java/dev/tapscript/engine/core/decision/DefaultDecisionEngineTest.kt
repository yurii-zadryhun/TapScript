package dev.tapscript.engine.core.decision

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.model.AutomationLogic
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationSnapshot
import dev.tapscript.engine.api.model.LogicMode
import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.ScriptEngine
import dev.tapscript.engine.api.ports.ScriptRequest
import dev.tapscript.engine.api.ports.ScriptResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDecisionEngineTest {
    @Test
    fun `script error is reported without emitting commands`() {
        val engine = decisionEngine(
            ScriptResult(commands = listOf(AutomationCommand.Tap("unsafe")), error = "bad script"),
        )

        val result = engine.decide(javascriptProfile(), emptySnapshot())

        assertEquals("bad script", result.error)
        assertTrue(result.commands.isEmpty())
    }

    @Test
    fun `successful script commands are preserved`() {
        val expected = listOf(AutomationCommand.Tap("equip"))
        val engine = decisionEngine(ScriptResult(commands = expected))

        val result = engine.decide(javascriptProfile(), emptySnapshot())

        assertEquals(null, result.error)
        assertEquals(expected, result.commands)
    }

    private fun decisionEngine(scriptResult: ScriptResult) = DefaultDecisionEngine(
        ruleEvaluator = RuleEvaluator(),
        scriptEngine = object : ScriptEngine {
            override fun evaluate(request: ScriptRequest): ScriptResult = scriptResult
        },
        logger = NoOpLogger,
    )

    private fun javascriptProfile() = AutomationProfile(
        name = "test",
        logic = AutomationLogic(mode = LogicMode.JAVASCRIPT, script = "ignored"),
    )

    private fun emptySnapshot() = AutomationSnapshot(
        values = emptyMap(),
        frameCapturedAtNanos = 0,
    )

    private data object NoOpLogger : AutomationLogger {
        override fun debug(message: String) = Unit
        override fun info(message: String) = Unit
        override fun error(message: String, throwable: Throwable?) = Unit
    }
}

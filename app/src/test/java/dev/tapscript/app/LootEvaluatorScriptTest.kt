package dev.tapscript.app

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.ports.ScriptRequest
import dev.tapscript.scripting.rhino.RhinoScriptEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LootEvaluatorScriptTest {
    private val engine = RhinoScriptEngine()

    @Test
    fun balancedGreatAndGoodRollBeatsGreatPlusBad() {
        val result = engine.evaluate(
            ScriptRequest(
                source = LootEvaluatorScript.source,
                variables = mapOf(
                    "candidate.stats" to listOf(
                        row("Attack Speed", 24.0),
                        row("Damage", 14.1),
                    ),
                    "equipped.stats" to listOf(
                        row("Attack Speed", 39.2),
                        row("Melee Damage", 50.0),
                    ),
                    "candidate.level" to 100,
                    "equipped.level" to 100,
                    "candidate.text" to "Ranged Weapon",
                    "equipped.text" to "Ranged Weapon",
                ),
            ),
        )

        assertNull(result.error)
        assertEquals("equip", result.randomTapTarget())
    }

    @Test
    fun meleeWeaponCandidateIsAlwaysRejected() {
        val result = engine.evaluate(
            ScriptRequest(
                source = LootEvaluatorScript.source,
                variables = mapOf(
                    "candidate.stats" to listOf(row("Mega Crit Chance", 10.0), row("Triple Hit Chance", 30.0)),
                    "equipped.stats" to listOf(row("Health Regen", 1.0)),
                    "candidate.level" to 130,
                    "equipped.level" to 1,
                    "candidate.text" to "Divine Melee Weapon",
                    "equipped.text" to "Ranged Weapon",
                ),
            ),
        )

        assertNull(result.error)
        assertEquals("sell", result.randomTapTarget())
    }

    @Test
    fun itemLevelBreaksOtherwiseEqualTie() {
        val sameStats = listOf(row("Attack Speed", 30.0), row("Damage", 10.0))
        val result = engine.evaluate(
            ScriptRequest(
                source = LootEvaluatorScript.source,
                variables = mapOf(
                    "candidate.stats" to sameStats,
                    "equipped.stats" to sameStats,
                    "candidate.level" to 120,
                    "equipped.level" to 100,
                    "candidate.text" to "Ranged Weapon",
                    "equipped.text" to "Ranged Weapon",
                ),
            ),
        )

        assertNull(result.error)
        assertEquals("equip", result.randomTapTarget())
    }

    private fun ScriptResultLike.randomTapTarget(): String? = error("unused")

    private fun dev.tapscript.engine.api.ports.ScriptResult.randomTapTarget(): String? =
        commands.filterIsInstance<AutomationCommand.RandomTap>().singleOrNull()?.targetId

    private fun row(name: String, value: Double): Map<String, Any> = mapOf(
        "name" to name,
        "value" to value,
    )

    private interface ScriptResultLike
}

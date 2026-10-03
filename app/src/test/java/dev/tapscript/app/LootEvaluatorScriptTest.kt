package dev.tapscript.app

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.ports.ScriptRequest
import dev.tapscript.engine.api.ports.ScriptResult
import dev.tapscript.scripting.rhino.RhinoScriptEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LootEvaluatorScriptTest {
    private val engine = RhinoScriptEngine()

    @Test
    fun balancedGreatAndGoodRollBeatsGreatPlusBad() {
        val result = evaluate(
            candidateStats = listOf(
                row("Attack Speed", 24.0),
                row("Damage", 14.1),
            ),
            equippedStats = listOf(
                row("Attack Speed", 39.2),
                row("Melee Damage", 50.0),
            ),
            candidateLevel = 100,
            equippedLevel = 100,
        )

        assertNull(result.error)
        assertEquals("equip", result.randomTapTarget())
    }

    @Test
    fun meleeWeaponCandidateIsAlwaysRejected() {
        val result = evaluate(
            candidateStats = listOf(row("Mega Crit Chance", 10.0), row("Triple Hit Chance", 30.0)),
            equippedStats = listOf(row("Health Regen", 1.0)),
            candidateLevel = 130,
            equippedLevel = 1,
            candidateText = "Divine Melee Weapon",
        )

        assertNull(result.error)
        assertEquals("sell", result.randomTapTarget())
    }

    @Test
    fun itemLevelBreaksOtherwiseEqualTie() {
        val sameStats = listOf(row("Attack Speed", 30.0), row("Damage", 10.0))
        val result = evaluate(
            candidateStats = sameStats,
            equippedStats = sameStats,
            candidateLevel = 120,
            equippedLevel = 100,
        )

        assertNull(result.error)
        assertEquals("equip", result.randomTapTarget())
    }

    private fun evaluate(
        candidateStats: List<Map<String, Any>>,
        equippedStats: List<Map<String, Any>>,
        candidateLevel: Int,
        equippedLevel: Int,
        candidateText: String = "Ranged Weapon",
    ): ScriptResult = engine.evaluate(
        ScriptRequest(
            source = LootEvaluatorScript.source,
            variables = mapOf(
                "candidate.stats" to candidateStats,
                "equipped.stats" to equippedStats,
                "candidate.level" to candidateLevel,
                "equipped.level" to equippedLevel,
                "candidate.text" to candidateText,
                "equipped.text" to "Ranged Weapon",
            ),
        ),
    )

    private fun ScriptResult.randomTapTarget(): String? =
        commands.filterIsInstance<AutomationCommand.RandomTap>().singleOrNull()?.targetId

    private fun row(name: String, value: Double): Map<String, Any> = mapOf(
        "name" to name,
        "value" to value,
    )
}

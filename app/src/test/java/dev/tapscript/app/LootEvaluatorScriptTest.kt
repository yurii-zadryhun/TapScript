package dev.tapscript.app

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.ports.ScriptRequest
import dev.tapscript.engine.api.ports.ScriptResult
import dev.tapscript.scripting.rhino.RhinoScriptEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LootEvaluatorScriptTest {
    private val engine = RhinoScriptEngine(timeoutMs = 1_000)

    @Test
    fun balancedGreatAndGoodRollBeatsGreatPlusBad() {
        val result = evaluate(
            candidateStats = complete(
                row("Attack Speed", 24.0),
                row("Damage", 14.1),
            ),
            equippedStats = complete(
                row("Attack Speed", 39.2),
                row("Melee Damage", 50.0),
            ),
            candidateLevel = 100,
            equippedLevel = 100,
        )

        assertNull(result.error)
        assertEquals("equip", result.highlightTarget("loot-action"))
        assertNoGestures(result)
    }

    @Test
    fun meleeTagFromGameTitleIsAlwaysRejected() {
        val result = evaluate(
            candidateStats = listOf(
                row("Critical Chance", 8.46),
                row("Double Hit Chance", 16.86),
                row("Companion Damage", 14.84),
            ),
            equippedStats = complete(row("Health Regen", 1.0), row("Damage", 1.0)),
            candidateLevel = 129,
            equippedLevel = 1,
            candidateText = "Divine Mace [Melee]",
        )

        assertNull(result.error)
        assertEquals("sell", result.highlightTarget("loot-action"))
        assertTrue(result.showInfo("loot-info")?.title?.contains("melee weapon", ignoreCase = true) == true)
        assertNoGestures(result)
    }

    @Test
    fun toleratesCommonMeeleTranspositionInBracketTag() {
        val result = evaluate(
            candidateStats = listOf(row("Mega Crit Chance", 10.0)),
            equippedStats = complete(row("Health Regen", 1.0), row("Damage", 1.0)),
            candidateLevel = 130,
            equippedLevel = 1,
            candidateText = "Divine Mace [Meele]",
        )

        assertNull(result.error)
        assertEquals("sell", result.highlightTarget("loot-action"))
        assertNoGestures(result)
    }

    @Test
    fun itemLevelBreaksOtherwiseEqualTie() {
        val sameStats = complete(
            row("Attack Speed", 30.0),
            row("Damage", 10.0),
        )
        val result = evaluate(
            candidateStats = sameStats,
            equippedStats = sameStats,
            candidateLevel = 120,
            equippedLevel = 100,
        )

        assertNull(result.error)
        assertEquals("equip", result.highlightTarget("loot-action"))
        assertNoGestures(result)
    }

    @Test
    fun incompleteOcrShowsCheckInsteadOfRecommendation() {
        val result = evaluate(
            candidateStats = listOf(row("Attack Speed", 30.0), row("Damage", 10.0)),
            equippedStats = complete(row("Attack Speed", 20.0), row("Damage", 8.0)),
            candidateLevel = 120,
            equippedLevel = 100,
        )

        assertNull(result.error)
        assertEquals("CHECK OCR", result.showInfo("loot-info")?.title)
        assertNull(result.highlightTarget("loot-action"))
        assertNoGestures(result)
    }

    @Test
    fun unknownStatFailsClosedInShadowMode() {
        val result = evaluate(
            candidateStats = listOf(
                row("Attack Speed", 30.0),
                row("Damage", 10.0),
                row("Mystery Power", 99.0),
                row("Triple Hit Chance", 20.0),
            ),
            equippedStats = complete(row("Attack Speed", 20.0), row("Damage", 8.0)),
            candidateLevel = 120,
            equippedLevel = 100,
        )

        assertNull(result.error)
        assertEquals("CHECK OCR", result.showInfo("loot-info")?.title)
        assertNull(result.highlightTarget("loot-action"))
        assertNoGestures(result)
    }

    private fun evaluate(
        candidateStats: List<Map<String, Any>>,
        equippedStats: List<Map<String, Any>>,
        candidateLevel: Int,
        equippedLevel: Int,
        candidateText: String = "Divine Crossbow [Range]",
    ): ScriptResult = engine.evaluate(
        ScriptRequest(
            source = LootEvaluatorScript.source,
            variables = mapOf(
                "candidate.stats" to candidateStats,
                "equipped.stats" to equippedStats,
                "candidate.level" to candidateLevel,
                "equipped.level" to equippedLevel,
                "candidate.text" to candidateText,
                "equipped.text" to "Divine Crossbow [Range]",
            ),
        ),
    )

    private fun complete(vararg primary: Map<String, Any>): List<Map<String, Any>> {
        val rows = primary.toMutableList()
        val fillers = listOf(
            row("Health Regen", 3.0),
            row("Companion Damage", 15.0),
            row("Movement Speed", 8.0),
            row("Knockback", 5.0),
        )
        for (filler in fillers) {
            if (rows.size >= 4) break
            rows += filler
        }
        return rows
    }

    private fun ScriptResult.highlightTarget(key: String): String? =
        commands.filterIsInstance<AutomationCommand.Highlight>()
            .singleOrNull { it.key == key }
            ?.targetId

    private fun ScriptResult.showInfo(key: String): AutomationCommand.ShowInfo? =
        commands.filterIsInstance<AutomationCommand.ShowInfo>()
            .singleOrNull { it.key == key }

    private fun assertNoGestures(result: ScriptResult) {
        assertFalse(result.commands.any { command ->
            command is AutomationCommand.Tap ||
                command is AutomationCommand.RandomTap ||
                command is AutomationCommand.Swipe ||
                command is AutomationCommand.RandomSwipe
        })
        assertNotNull(result.showInfo("loot-info"))
    }

    private fun row(name: String, value: Double): Map<String, Any> = mapOf(
        "name" to name,
        "value" to value,
    )
}

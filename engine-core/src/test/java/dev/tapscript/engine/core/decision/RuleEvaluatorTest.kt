package dev.tapscript.engine.core.decision

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleEvaluatorTest {
    @Test
    fun emitsActionWhenVariableIsGreater() {
        val rule = DecisionRule(
            name = "Equip better item",
            left = Operand(OperandKind.VARIABLE, "candidate.score"),
            operator = ComparisonOperator.GREATER_THAN,
            right = Operand(OperandKind.VARIABLE, "equipped.score"),
            actions = listOf(RuleAction(RuleActionKind.TAP, targetId = "equip")),
        )
        val snapshot = AutomationSnapshot(
            values = mapOf("candidate.score" to 120.0, "equipped.score" to 100.0),
            frameCapturedAtNanos = 1L,
        )

        assertEquals(listOf(AutomationCommand.Tap("equip")), RuleEvaluator().evaluate(listOf(rule), snapshot))
    }
}

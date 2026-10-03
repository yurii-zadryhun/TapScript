package dev.tapscript.engine.core.decision

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.model.AutomationSnapshot
import dev.tapscript.engine.api.model.ComparisonOperator
import dev.tapscript.engine.api.model.DecisionRule
import dev.tapscript.engine.api.model.Operand
import dev.tapscript.engine.api.model.OperandKind
import dev.tapscript.engine.api.model.RuleActionKind

class RuleEvaluator {
    fun evaluate(rules: List<DecisionRule>, snapshot: AutomationSnapshot): List<AutomationCommand> =
        rules.asSequence()
            .filter { it.enabled }
            .filter { matches(it, snapshot.values) }
            .flatMap { rule -> rule.actions.asSequence().map(::toCommand) }
            .toList()

    private fun matches(rule: DecisionRule, values: Map<String, Any?>): Boolean {
        val left = resolve(rule.left, values)
        if (rule.operator == ComparisonOperator.EXISTS) return left != null
        val right = resolve(rule.right, values)

        return when (rule.operator) {
            ComparisonOperator.EQUALS -> normalized(left) == normalized(right)
            ComparisonOperator.NOT_EQUALS -> normalized(left) != normalized(right)
            ComparisonOperator.GREATER_THAN -> compareNumbers(left, right) { a, b -> a > b }
            ComparisonOperator.GREATER_OR_EQUAL -> compareNumbers(left, right) { a, b -> a >= b }
            ComparisonOperator.LESS_THAN -> compareNumbers(left, right) { a, b -> a < b }
            ComparisonOperator.LESS_OR_EQUAL -> compareNumbers(left, right) { a, b -> a <= b }
            ComparisonOperator.CONTAINS -> left?.toString()?.contains(right?.toString().orEmpty(), ignoreCase = true) == true
            ComparisonOperator.EXISTS -> left != null
        }
    }

    private fun resolve(operand: Operand, values: Map<String, Any?>): Any? = when (operand.kind) {
        OperandKind.VARIABLE -> values[operand.value]
        OperandKind.NUMBER -> operand.value.toDoubleOrNull()
        OperandKind.TEXT -> operand.value
        OperandKind.BOOLEAN -> operand.value.toBooleanStrictOrNull()
    }

    private fun normalized(value: Any?): Any? = when (value) {
        is Number -> value.toDouble()
        is String -> value.trim()
        else -> value
    }

    private fun compareNumbers(left: Any?, right: Any?, block: (Double, Double) -> Boolean): Boolean {
        val leftNumber = (left as? Number)?.toDouble() ?: left?.toString()?.toDoubleOrNull() ?: return false
        val rightNumber = (right as? Number)?.toDouble() ?: right?.toString()?.toDoubleOrNull() ?: return false
        return block(leftNumber, rightNumber)
    }

    private fun toCommand(action: dev.tapscript.engine.api.model.RuleAction): AutomationCommand = when (action.kind) {
        RuleActionKind.TAP -> AutomationCommand.Tap(action.targetId)
        RuleActionKind.SWIPE -> AutomationCommand.Swipe(action.targetId)
        RuleActionKind.WAIT -> AutomationCommand.Wait(action.durationMs)
        RuleActionKind.LOG -> AutomationCommand.Log(action.message)
    }
}

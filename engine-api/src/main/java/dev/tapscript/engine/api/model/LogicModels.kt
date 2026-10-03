package dev.tapscript.engine.api.model

import java.util.UUID

data class AutomationLogic(
    val mode: LogicMode = LogicMode.RULES,
    val rules: List<DecisionRule> = emptyList(),
    val script: String = "",
)

enum class LogicMode {
    RULES,
    JAVASCRIPT,
}

data class DecisionRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val left: Operand,
    val operator: ComparisonOperator,
    val right: Operand,
    val actions: List<RuleAction>,
)

data class Operand(
    val kind: OperandKind,
    val value: String,
)

enum class OperandKind {
    VARIABLE,
    NUMBER,
    TEXT,
    BOOLEAN,
}

enum class ComparisonOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_OR_EQUAL,
    LESS_THAN,
    LESS_OR_EQUAL,
    CONTAINS,
    EXISTS,
}

data class RuleAction(
    val kind: RuleActionKind,
    val targetId: String = "",
    val durationMs: Long = 0,
    val message: String = "",
)

enum class RuleActionKind {
    TAP,
    SWIPE,
    WAIT,
    LOG,
}

package dev.tapscript.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.*

@Composable
fun DecisionRuleDialog(
    existing: DecisionRule?,
    actionTargets: List<ActionTarget>,
    onDismiss: () -> Unit,
    onSave: (DecisionRule) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "Decision rule") }
    var leftVariable by remember { mutableStateOf(existing?.left?.value.orEmpty()) }
    var operator by remember { mutableStateOf(existing?.operator ?: ComparisonOperator.GREATER_THAN) }
    var rightKind by remember { mutableStateOf(existing?.right?.kind ?: OperandKind.NUMBER) }
    var rightValue by remember { mutableStateOf(existing?.right?.value ?: "0") }
    var actionKind by remember { mutableStateOf(existing?.actions?.firstOrNull()?.kind ?: RuleActionKind.TAP) }
    var targetId by remember { mutableStateOf(existing?.actions?.firstOrNull()?.targetId.orEmpty()) }
    var duration by remember { mutableStateOf(existing?.actions?.firstOrNull()?.durationMs?.toString() ?: "120") }
    var message by remember { mutableStateOf(existing?.actions?.firstOrNull()?.message.orEmpty()) }

    val rule = buildRuleOrNull(
        existing, name, leftVariable, operator, rightKind, rightValue,
        actionKind, targetId, duration, message,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add rule" else "Edit rule") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Rule name") }, singleLine = true)
                OutlinedTextField(
                    leftVariable,
                    { leftVariable = it.trim() },
                    Modifier.fillMaxWidth(),
                    label = { Text("Left variable") },
                    placeholder = { Text("candidate.score") },
                    singleLine = true,
                )
                EnumPicker("Operator", operator, ComparisonOperator.entries, { it.name.lowercase().replace('_', ' ') }) { operator = it }
                if (operator != ComparisonOperator.EXISTS) {
                    EnumPicker("Right side type", rightKind, OperandKind.entries, { it.name.lowercase() }) { rightKind = it }
                    OutlinedTextField(rightValue, { rightValue = it }, Modifier.fillMaxWidth(), label = { Text("Right value") }, singleLine = true)
                }
                EnumPicker("Then", actionKind, RuleActionKind.entries, { it.name.lowercase() }) { actionKind = it }
                ActionRuleFields(actionKind, actionTargets, targetId, { targetId = it }, duration, { duration = it }, message, { message = it })
            }
        },
        confirmButton = { Button(onClick = { rule?.let(onSave) }, enabled = rule != null) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ActionRuleFields(
    kind: RuleActionKind,
    targets: List<ActionTarget>,
    targetId: String,
    onTargetId: (String) -> Unit,
    duration: String,
    onDuration: (String) -> Unit,
    message: String,
    onMessage: (String) -> Unit,
) {
    when (kind) {
        RuleActionKind.TAP, RuleActionKind.SWIPE -> EnumPicker(
            label = "Action target",
            selected = targets.firstOrNull { it.id == targetId },
            values = targets,
            text = { "${it.name} (${it.id})" },
            onSelected = { onTargetId(it.id) },
        )
        RuleActionKind.WAIT -> OutlinedTextField(duration, onDuration, label = { Text("Wait ms") }, singleLine = true)
        RuleActionKind.LOG -> OutlinedTextField(message, onMessage, Modifier.fillMaxWidth(), label = { Text("Log message") })
    }
}

private fun buildRuleOrNull(
    existing: DecisionRule?,
    name: String,
    leftVariable: String,
    operator: ComparisonOperator,
    rightKind: OperandKind,
    rightValue: String,
    actionKind: RuleActionKind,
    targetId: String,
    duration: String,
    message: String,
): DecisionRule? = runCatching {
    require(leftVariable.isNotBlank())
    if (operator != ComparisonOperator.EXISTS) {
        validateOperand(rightKind, rightValue)
    }
    val action = when (actionKind) {
        RuleActionKind.TAP, RuleActionKind.SWIPE -> {
            require(targetId.isNotBlank())
            RuleAction(kind = actionKind, targetId = targetId)
        }
        RuleActionKind.WAIT -> RuleAction(kind = actionKind, durationMs = duration.toLong().coerceIn(0, 60_000))
        RuleActionKind.LOG -> RuleAction(kind = actionKind, message = message)
    }
    DecisionRule(
        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
        name = name.ifBlank { "Decision rule" },
        enabled = existing?.enabled ?: true,
        left = Operand(OperandKind.VARIABLE, leftVariable.trim()),
        operator = operator,
        right = Operand(rightKind, rightValue),
        actions = listOf(action),
    )
}.getOrNull()


private fun validateOperand(kind: OperandKind, value: String) {
    when (kind) {
        OperandKind.VARIABLE, OperandKind.TEXT -> require(value.isNotBlank()) { "Right value is required" }
        OperandKind.NUMBER -> require(value.toDoubleOrNull() != null) { "Right value must be a number" }
        OperandKind.BOOLEAN -> require(value.toBooleanStrictOrNull() != null) { "Right value must be true or false" }
    }
}

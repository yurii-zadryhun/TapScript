package dev.tapscript.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.AutomationLogic
import dev.tapscript.engine.api.model.ComparisonOperator
import dev.tapscript.engine.api.model.DecisionRule
import dev.tapscript.engine.api.model.LogicMode

@Composable
internal fun ProfileLogicSection(
    logic: AutomationLogic,
    onLogicChange: (AutomationLogic) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (DecisionRule) -> Unit,
    onDeleteRule: (DecisionRule) -> Unit,
) {
    EditorSection("Decision logic") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LogicMode.entries.forEach { mode ->
                if (logic.mode == mode) {
                    Button(onClick = {}) { Text(mode.displayName()) }
                } else {
                    OutlinedButton(onClick = { onLogicChange(logic.copy(mode = mode)) }) { Text(mode.displayName()) }
                }
            }
        }
        when (logic.mode) {
            LogicMode.RULES -> RuleList(logic.rules, onAddRule, onEditRule, onDeleteRule)
            LogicMode.JAVASCRIPT -> ScriptEditor(logic.script) { onLogicChange(logic.copy(script = it)) }
        }
    }
}

@Composable
private fun RuleList(
    rules: List<DecisionRule>,
    onAdd: () -> Unit,
    onEdit: (DecisionRule) -> Unit,
    onDelete: (DecisionRule) -> Unit,
) {
    if (rules.isEmpty()) Hint("Rules are ideal for simple compare → tap workflows.")
    rules.forEach { rule ->
        ConfigItem(
            title = rule.name,
            subtitle = "${rule.left.value} ${rule.operator.symbol()} ${rule.right.value}",
            onEdit = { onEdit(rule) },
            onDelete = { onDelete(rule) },
        )
    }
    OutlinedButton(onClick = onAdd) { Text("Add rule") }
}

@Composable
private fun ScriptEditor(script: String, onChange: (String) -> Unit) {
    Text(
        "API: vars, tap/tapRandom, swipe/swipeRandom, waitMs/waitRandom, log",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary,
    )
    OutlinedTextField(
        value = script,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("JavaScript") },
        minLines = 14,
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    )
}

private fun LogicMode.displayName(): String = if (this == LogicMode.RULES) "Rules" else "JavaScript"

private fun ComparisonOperator.symbol(): String = when (this) {
    ComparisonOperator.EQUALS -> "=="
    ComparisonOperator.NOT_EQUALS -> "!="
    ComparisonOperator.GREATER_THAN -> ">"
    ComparisonOperator.GREATER_OR_EQUAL -> ">="
    ComparisonOperator.LESS_THAN -> "<"
    ComparisonOperator.LESS_OR_EQUAL -> "<="
    ComparisonOperator.CONTAINS -> "contains"
    ComparisonOperator.EXISTS -> "exists"
}

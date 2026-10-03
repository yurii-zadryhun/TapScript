package dev.tapscript.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.ActionKind
import dev.tapscript.engine.api.model.ActionTarget
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.ComparisonOperator
import dev.tapscript.engine.api.model.DecisionRule
import dev.tapscript.engine.api.model.MatchMode
import dev.tapscript.engine.api.model.Operand
import dev.tapscript.engine.api.model.OperandKind
import dev.tapscript.engine.api.model.RecognitionRegion
import dev.tapscript.engine.api.model.RegexExtractorSpec
import dev.tapscript.engine.api.model.RegexFieldSpec
import dev.tapscript.engine.api.model.RuleAction
import dev.tapscript.engine.api.model.RuleActionKind
import dev.tapscript.engine.api.model.TextRecognitionConfig
import dev.tapscript.engine.api.model.ValueType
import dev.tapscript.platform.android.app.LaunchableAppInfo
import java.util.UUID
import java.util.regex.Pattern

@Composable
internal fun OverlayProfileIdentitySection(
    profile: AutomationProfile,
    installedApps: List<LaunchableAppInfo>,
    onChange: (AutomationProfile) -> Unit,
) {
    var showApps by remember(profile.id) { mutableStateOf(false) }
    var query by remember(profile.id) { mutableStateOf("") }
    val selectedApp = installedApps.firstOrNull { it.packageName == profile.targetPackage }
    val filteredApps = remember(installedApps, query) {
        val needle = query.trim()
        installedApps
            .asSequence()
            .filter {
                needle.isBlank() ||
                    it.label.contains(needle, ignoreCase = true) ||
                    it.packageName.contains(needle, ignoreCase = true)
            }
            .take(12)
            .toList()
    }

    EditorSection("Identity") {
        OutlinedTextField(
            value = profile.name,
            onValueChange = { onChange(profile.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Profile name") },
            singleLine = true,
        )
        Text("Automation scope", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = profile.targetPackage.isBlank(),
                onClick = {
                    showApps = false
                    onChange(profile.copy(targetPackage = ""))
                },
                label = { Text("Whole screen") },
            )
            FilterChip(
                selected = profile.targetPackage.isNotBlank(),
                onClick = { showApps = true },
                label = { Text("Specific app") },
            )
        }

        if (profile.targetPackage.isNotBlank()) {
            Text(
                selectedApp?.label ?: profile.targetPackage,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                profile.targetPackage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { showApps = !showApps }) {
                Text(if (showApps) "Hide app list" else "Change app")
            }
        }

        if (showApps) {
            HorizontalDivider()
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search installed apps") },
                singleLine = true,
            )
            if (filteredApps.isEmpty()) {
                Hint("No matching launchable apps.")
            } else {
                filteredApps.forEach { app ->
                    OutlinedButton(
                        onClick = {
                            onChange(profile.copy(targetPackage = app.packageName))
                            showApps = false
                            query = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(app.label)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OverlayRegionEditor(
    region: RecognitionRegion,
    onSave: (RecognitionRegion) -> Unit,
    onCancel: () -> Unit,
) {
    val extractor = region.textConfig.extractors.firstOrNull()
    var id by remember(region.id) { mutableStateOf(region.id) }
    var name by remember(region.id) { mutableStateOf(region.name) }
    var variable by remember(region.id) { mutableStateOf(extractor?.variable.orEmpty()) }
    var pattern by remember(region.id) {
        mutableStateOf(extractor?.pattern ?: "\\+(?<value>\\d+(?:[.,]\\d+)?)%")
    }
    var matchMode by remember(region.id) { mutableStateOf(extractor?.matchMode ?: MatchMode.FIRST) }
    var valueGroup by remember(region.id) {
        mutableStateOf(extractor?.fields?.firstOrNull { it.name == "value" }?.group ?: "value")
    }
    var valueType by remember(region.id) {
        mutableStateOf(extractor?.fields?.firstOrNull { it.name == "value" }?.type ?: ValueType.NUMBER)
    }
    var nameGroup by remember(region.id) {
        mutableStateOf(extractor?.fields?.firstOrNull { it.name == "name" }?.group.orEmpty())
    }
    var ignoreCase by remember(region.id) { mutableStateOf(extractor?.ignoreCase ?: false) }

    val regexValid = runCatching { Pattern.compile(pattern) }.isSuccess
    val saveValue = buildOverlayRegion(
        original = region,
        id = id,
        name = name,
        variable = variable,
        pattern = pattern,
        matchMode = matchMode,
        valueGroup = valueGroup,
        valueType = valueType,
        nameGroup = nameGroup,
        ignoreCase = ignoreCase,
    )

    InlineEditorSection(title = "OCR region", onCancel = onCancel) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = id,
            onValueChange = { id = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Region id") },
            singleLine = true,
        )
        Text(
            "Area: ${region.bounds.asPercentText()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = variable,
            onValueChange = { variable = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Output variable") },
            placeholder = { Text("candidate.attackSpeed") },
            supportingText = { Text("Leave empty to keep only raw OCR text.") },
            singleLine = true,
        )
        OutlinedTextField(
            value = pattern,
            onValueChange = { pattern = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Regex") },
            isError = !regexValid,
            supportingText = { Text(if (regexValid) "Named groups are supported." else "Invalid regular expression") },
            minLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = matchMode == MatchMode.FIRST,
                onClick = { matchMode = MatchMode.FIRST },
                label = { Text("First") },
            )
            FilterChip(
                selected = matchMode == MatchMode.ALL,
                onClick = { matchMode = MatchMode.ALL },
                label = { Text("All matches") },
            )
        }
        OutlinedTextField(
            value = valueGroup,
            onValueChange = { valueGroup = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Value group") },
            singleLine = true,
        )
        EnumChips(
            values = ValueType.entries,
            selected = valueType,
            label = { it.name.lowercase() },
            onSelected = { valueType = it },
        )
        OutlinedTextField(
            value = nameGroup,
            onValueChange = { nameGroup = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Optional name group") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = ignoreCase, onCheckedChange = { ignoreCase = it })
            Text("Ignore case")
        }
        Button(
            onClick = { saveValue?.let(onSave) },
            enabled = saveValue != null && regexValid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apply region")
        }
    }
}

@Composable
internal fun OverlayActionEditor(
    action: ActionTarget,
    onSave: (ActionTarget) -> Unit,
    onCancel: () -> Unit,
) {
    var id by remember(action.id) { mutableStateOf(action.id) }
    var name by remember(action.id) { mutableStateOf(action.name) }
    var durationText by remember(action.id) { mutableStateOf(action.durationMs.toString()) }
    val saveValue = runCatching {
        action.copy(
            id = id.requireId(),
            name = name.ifBlank { id },
            durationMs = if (action.kind == ActionKind.SWIPE) {
                (durationText.toLongOrNull() ?: action.durationMs).coerceIn(50, 10_000)
            } else {
                action.durationMs
            },
        )
    }.getOrNull()

    InlineEditorSection(
        title = if (action.kind == ActionKind.TAP) "Tap target" else "Swipe target",
        onCancel = onCancel,
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = id,
            onValueChange = { id = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Action id") },
            singleLine = true,
        )
        Text(
            if (action.kind == ActionKind.TAP) {
                "Tap: ${action.start.asPercentText()}"
            } else {
                "Swipe: ${action.start.asPercentText()} → ${action.end?.asPercentText().orEmpty()}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action.kind == ActionKind.SWIPE) {
            OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Duration ms") },
                singleLine = true,
            )
        }
        Button(
            onClick = { saveValue?.let(onSave) },
            enabled = saveValue != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apply action")
        }
    }
}

@Composable
internal fun OverlayRuleEditor(
    existing: DecisionRule?,
    actionTargets: List<ActionTarget>,
    onSave: (DecisionRule) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "Decision rule") }
    var leftVariable by remember(existing?.id) { mutableStateOf(existing?.left?.value.orEmpty()) }
    var operator by remember(existing?.id) { mutableStateOf(existing?.operator ?: ComparisonOperator.GREATER_THAN) }
    var rightKind by remember(existing?.id) { mutableStateOf(existing?.right?.kind ?: OperandKind.NUMBER) }
    var rightValue by remember(existing?.id) { mutableStateOf(existing?.right?.value ?: "0") }
    var actionKind by remember(existing?.id) {
        mutableStateOf(existing?.actions?.firstOrNull()?.kind ?: RuleActionKind.TAP)
    }
    var targetId by remember(existing?.id) { mutableStateOf(existing?.actions?.firstOrNull()?.targetId.orEmpty()) }
    var durationText by remember(existing?.id) {
        mutableStateOf((existing?.actions?.firstOrNull()?.durationMs ?: 120L).toString())
    }
    var message by remember(existing?.id) { mutableStateOf(existing?.actions?.firstOrNull()?.message.orEmpty()) }

    val saveValue = buildOverlayRule(
        existing = existing,
        name = name,
        leftVariable = leftVariable,
        operator = operator,
        rightKind = rightKind,
        rightValue = rightValue,
        actionKind = actionKind,
        targetId = targetId,
        durationText = durationText,
        message = message,
    )

    InlineEditorSection(title = if (existing == null) "New rule" else "Edit rule", onCancel = onCancel) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Rule name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = leftVariable,
            onValueChange = { leftVariable = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Left variable") },
            placeholder = { Text("candidate.score") },
            singleLine = true,
        )
        Text("Operator", style = MaterialTheme.typography.labelMedium)
        EnumChips(
            values = ComparisonOperator.entries,
            selected = operator,
            label = { it.shortLabel() },
            onSelected = { operator = it },
        )
        if (operator != ComparisonOperator.EXISTS) {
            Text("Right side", style = MaterialTheme.typography.labelMedium)
            EnumChips(
                values = OperandKind.entries,
                selected = rightKind,
                label = { it.name.lowercase() },
                onSelected = { rightKind = it },
            )
            OutlinedTextField(
                value = rightValue,
                onValueChange = { rightValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Right value") },
                singleLine = true,
            )
        }
        Text("Then", style = MaterialTheme.typography.labelMedium)
        EnumChips(
            values = RuleActionKind.entries,
            selected = actionKind,
            label = { it.name.lowercase() },
            onSelected = { actionKind = it },
        )
        when (actionKind) {
            RuleActionKind.TAP, RuleActionKind.SWIPE -> {
                val compatible = actionTargets.filter {
                    (actionKind == RuleActionKind.TAP && it.kind == ActionKind.TAP) ||
                        (actionKind == RuleActionKind.SWIPE && it.kind == ActionKind.SWIPE)
                }
                if (compatible.isEmpty()) {
                    Hint("Create a compatible action target first.")
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        compatible.forEach { target ->
                            FilterChip(
                                selected = target.id == targetId,
                                onClick = { targetId = target.id },
                                label = { Text(target.name) },
                            )
                        }
                    }
                }
            }
            RuleActionKind.WAIT -> OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Wait ms") },
                singleLine = true,
            )
            RuleActionKind.LOG -> OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Log message") },
            )
        }
        Button(
            onClick = { saveValue?.let(onSave) },
            enabled = saveValue != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apply rule")
        }
    }
}

@Composable
private fun InlineEditorSection(
    title: String,
    onCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    EditorSection(title) {
        content()
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun <T> EnumChips(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(label(value)) },
            )
        }
    }
}

private fun buildOverlayRegion(
    original: RecognitionRegion,
    id: String,
    name: String,
    variable: String,
    pattern: String,
    matchMode: MatchMode,
    valueGroup: String,
    valueType: ValueType,
    nameGroup: String,
    ignoreCase: Boolean,
): RecognitionRegion? = runCatching {
    val cleanId = id.requireId()
    Pattern.compile(pattern)
    val fields = buildList {
        add(RegexFieldSpec(name = "value", group = valueGroup.requireId(), type = valueType))
        if (nameGroup.isNotBlank()) {
            add(RegexFieldSpec(name = "name", group = nameGroup.requireId(), type = ValueType.TEXT))
        }
    }
    original.copy(
        id = cleanId,
        name = name.ifBlank { cleanId },
        textConfig = TextRecognitionConfig(
            extractors = if (variable.isBlank()) emptyList() else listOf(
                RegexExtractorSpec(
                    variable = variable.trim(),
                    pattern = pattern,
                    matchMode = matchMode,
                    fields = fields,
                    ignoreCase = ignoreCase,
                ),
            ),
        ),
    )
}.getOrNull()

private fun buildOverlayRule(
    existing: DecisionRule?,
    name: String,
    leftVariable: String,
    operator: ComparisonOperator,
    rightKind: OperandKind,
    rightValue: String,
    actionKind: RuleActionKind,
    targetId: String,
    durationText: String,
    message: String,
): DecisionRule? = runCatching {
    require(leftVariable.isNotBlank())
    if (operator != ComparisonOperator.EXISTS) {
        when (rightKind) {
            OperandKind.NUMBER -> require(rightValue.toDoubleOrNull() != null)
            OperandKind.BOOLEAN -> require(rightValue.toBooleanStrictOrNull() != null)
            OperandKind.VARIABLE, OperandKind.TEXT -> require(rightValue.isNotBlank())
        }
    }
    val action = when (actionKind) {
        RuleActionKind.TAP, RuleActionKind.SWIPE -> {
            require(targetId.isNotBlank())
            RuleAction(kind = actionKind, targetId = targetId)
        }
        RuleActionKind.WAIT -> RuleAction(
            kind = actionKind,
            durationMs = (durationText.toLongOrNull() ?: 0L).coerceIn(0, 60_000),
        )
        RuleActionKind.LOG -> RuleAction(kind = actionKind, message = message)
    }
    DecisionRule(
        id = existing?.id ?: UUID.randomUUID().toString(),
        name = name.ifBlank { "Decision rule" },
        enabled = existing?.enabled ?: true,
        left = Operand(OperandKind.VARIABLE, leftVariable.trim()),
        operator = operator,
        right = Operand(rightKind, rightValue),
        actions = listOf(action),
    )
}.getOrNull()

private fun dev.tapscript.engine.api.model.NormalizedRect.asPercentText(): String =
    "L %.1f%% · T %.1f%% · R %.1f%% · B %.1f%%".format(left * 100f, top * 100f, right * 100f, bottom * 100f)

private fun dev.tapscript.engine.api.model.NormalizedPoint.asPercentText(): String =
    "%.1f%%, %.1f%%".format(x * 100f, y * 100f)

private fun ComparisonOperator.shortLabel(): String = when (this) {
    ComparisonOperator.EQUALS -> "=="
    ComparisonOperator.NOT_EQUALS -> "!="
    ComparisonOperator.GREATER_THAN -> ">"
    ComparisonOperator.GREATER_OR_EQUAL -> ">="
    ComparisonOperator.LESS_THAN -> "<"
    ComparisonOperator.LESS_OR_EQUAL -> "<="
    ComparisonOperator.CONTAINS -> "contains"
    ComparisonOperator.EXISTS -> "exists"
}

package dev.tapscript.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.*
import java.util.regex.Pattern

@Composable
fun TextRegionDialog(
    existing: RecognitionRegion?,
    onDismiss: () -> Unit,
    onSave: (RecognitionRegion) -> Unit,
) {
    val extractor = existing?.textConfig?.extractors?.firstOrNull()
    val generatedId = remember(existing?.id) { existing?.id ?: "region-${java.util.UUID.randomUUID().toString().take(8)}" }
    var id by remember(existing?.id) { mutableStateOf(generatedId) }
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "Text region") }
    var left by remember(existing?.id) { mutableStateOf(existing?.bounds?.left?.toString() ?: "0.1") }
    var top by remember(existing?.id) { mutableStateOf(existing?.bounds?.top?.toString() ?: "0.1") }
    var right by remember(existing?.id) { mutableStateOf(existing?.bounds?.right?.toString() ?: "0.9") }
    var bottom by remember(existing?.id) { mutableStateOf(existing?.bounds?.bottom?.toString() ?: "0.2") }
    var variable by remember(existing?.id) { mutableStateOf(extractor?.variable.orEmpty()) }
    var pattern by remember(existing?.id) { mutableStateOf(extractor?.pattern ?: "\\+(?<value>\\d+(?:[.,]\\d+)?)%") }
    var matchMode by remember(existing?.id) { mutableStateOf(extractor?.matchMode ?: MatchMode.FIRST) }
    var valueGroup by remember(existing?.id) {
        mutableStateOf(extractor?.fields?.firstOrNull { it.name == "value" }?.group ?: "value")
    }
    var valueType by remember(existing?.id) {
        mutableStateOf(extractor?.fields?.firstOrNull { it.name == "value" }?.type ?: ValueType.NUMBER)
    }
    var nameGroup by remember(existing?.id) {
        mutableStateOf(extractor?.fields?.firstOrNull { it.name == "name" }?.group.orEmpty())
    }
    var ignoreCase by remember(existing?.id) { mutableStateOf(extractor?.ignoreCase ?: false) }
    var showAdvancedCoordinates by remember(existing?.id) { mutableStateOf(existing == null) }

    val region = buildRegionOrNull(
        existing, id, name, left, top, right, bottom,
        variable, pattern, matchMode, valueGroup, valueType, nameGroup, ignoreCase,
    )
    val regexValid = runCatching { Pattern.compile(pattern) }.isSuccess

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = editorDialogProperties(),
        title = { Text(if (existing == null) "Add text region" else "Edit text region") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IdentityFields(id, { id = it.trim() }, name, { name = it })

                region?.let {
                    Text(
                        "Area: ${it.bounds.asPercentText()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showAdvancedCoordinates = !showAdvancedCoordinates }) {
                    Text(if (showAdvancedCoordinates) "Hide advanced coordinates" else "Advanced coordinates")
                }
                if (showAdvancedCoordinates) {
                    RectangleFields(left, { left = it }, top, { top = it }, right, { right = it }, bottom, { bottom = it })
                }

                HorizontalDivider()
                Text("Extractor", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = variable,
                    onValueChange = { variable = it.trim() },
                    label = { Text("Output variable") },
                    placeholder = { Text("candidate.attackSpeed") },
                    supportingText = { Text("Leave empty if you only need raw OCR text for inspection.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Regex") },
                    supportingText = {
                        Text(if (regexValid) "Use named groups, e.g. (?<value>...)" else "Invalid regular expression")
                    },
                    isError = !regexValid,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceButton("First", matchMode == MatchMode.FIRST) { matchMode = MatchMode.FIRST }
                    ChoiceButton("All matches", matchMode == MatchMode.ALL) { matchMode = MatchMode.ALL }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = valueGroup,
                        onValueChange = { valueGroup = it.trim() },
                        label = { Text("Value group") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    ValueTypePicker(valueType = valueType, onChange = { valueType = it })
                }
                OutlinedTextField(
                    value = nameGroup,
                    onValueChange = { nameGroup = it.trim() },
                    label = { Text("Optional name group") },
                    placeholder = { Text("name") },
                    supportingText = { Text("With All matches, this creates [{value, name}, …]") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = ignoreCase, onCheckedChange = { ignoreCase = it })
                    Text("Ignore case")
                }
            }
        },
        confirmButton = {
            Button(onClick = { region?.let(onSave) }, enabled = region != null && regexValid) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun IdentityFields(
    id: String,
    onId: (String) -> Unit,
    name: String,
    onName: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(id, onId, Modifier.weight(1f), label = { Text("Region id") }, singleLine = true)
        OutlinedTextField(name, onName, Modifier.weight(1f), label = { Text("Name") }, singleLine = true)
    }
}

@Composable
private fun RectangleFields(
    left: String, onLeft: (String) -> Unit,
    top: String, onTop: (String) -> Unit,
    right: String, onRight: (String) -> Unit,
    bottom: String, onBottom: (String) -> Unit,
) {
    Text("Normalized rectangle (0..1)", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CoordinateField("Left", left, onLeft, Modifier.weight(1f))
        CoordinateField("Top", top, onTop, Modifier.weight(1f))
        CoordinateField("Right", right, onRight, Modifier.weight(1f))
        CoordinateField("Bottom", bottom, onBottom, Modifier.weight(1f))
    }
}

private fun NormalizedRect.asPercentText(): String =
    "L %.1f%% · T %.1f%% · R %.1f%% · B %.1f%%".format(
        left * 100f,
        top * 100f,
        right * 100f,
        bottom * 100f,
    )

private fun buildRegionOrNull(
    existing: RecognitionRegion?,
    id: String,
    name: String,
    left: String,
    top: String,
    right: String,
    bottom: String,
    variable: String,
    pattern: String,
    matchMode: MatchMode,
    valueGroup: String,
    valueType: ValueType,
    nameGroup: String,
    ignoreCase: Boolean,
): RecognitionRegion? = runCatching {
    val fields = buildList {
        add(RegexFieldSpec("value", valueGroup.requireId(), valueType))
        if (nameGroup.isNotBlank()) add(RegexFieldSpec("name", nameGroup.trim(), ValueType.TEXT))
    }
    RecognitionRegion(
        id = id.requireId(),
        name = name.ifBlank { id },
        bounds = NormalizedRect(
            left.requireUnitFloat(),
            top.requireUnitFloat(),
            right.requireUnitFloat(),
            bottom.requireUnitFloat(),
        ),
        enabled = existing?.enabled ?: true,
        recognizer = RecognizerKind.TEXT,
        textConfig = TextRecognitionConfig(
            extractors = if (variable.isBlank()) emptyList() else listOf(
                RegexExtractorSpec(variable.trim(), pattern, matchMode, fields, ignoreCase),
            ),
        ),
    )
}.getOrNull()

package dev.tapscript.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.tapscript.engine.api.model.ValueType

@Composable
internal fun CoordinateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val valid = value.toFloatOrNull()?.let { it in 0f..1f } == true
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        isError = !valid,
        singleLine = true,
    )
}

@Composable
internal fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) }
    else OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
internal fun ValueTypePicker(valueType: ValueType, onChange: (ValueType) -> Unit) {
    EnumPicker(
        label = "Value type",
        selected = valueType,
        values = listOf(ValueType.NUMBER, ValueType.TEXT, ValueType.BOOLEAN),
        text = { it.name.lowercase() },
        onSelected = onChange,
    )
}

@Composable
internal fun <T> EnumPicker(
    label: String,
    selected: T?,
    values: List<T>,
    text: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected?.let(text) ?: "Select…")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(text(value)) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

internal fun String.requireUnitFloat(): Float =
    requireNotNull(toFloatOrNull()) { "Not a number" }.also { require(it in 0f..1f) }

internal fun String.requireId(): String = trim().also {
    require(it.isNotBlank()) { "Id is required" }
    require(it.all { char -> char.isLetterOrDigit() || char == '_' || char == '-' || char == '.' }) {
        "Id contains unsupported characters"
    }
}

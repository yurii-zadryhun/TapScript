package dev.tapscript.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
internal fun ConfigItem(
    title: String,
    subtitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRedraw: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onRedraw != null) {
                TextButton(onClick = onRedraw) { Text("Draw") }
            }
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onDelete) { Text("Remove") }
        }
        HorizontalDivider()
    }
}

@Composable
internal fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun NumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValid: (Double) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.let(onValid)
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
    )
}

internal fun Float.shortCoordinate(): String = "%.3f".format(this)

internal fun <T> List<T>.upsertBy(item: T, id: (T) -> String): List<T> {
    val targetId = id(item)
    return if (any { id(it) == targetId }) map { if (id(it) == targetId) item else it } else this + item
}

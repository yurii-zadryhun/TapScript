package dev.tapscript.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.*

@Composable
fun ActionTargetDialog(
    existing: ActionTarget?,
    onDismiss: () -> Unit,
    onSave: (ActionTarget) -> Unit,
) {
    val generatedId = remember(existing?.id) { existing?.id ?: "action-${java.util.UUID.randomUUID().toString().take(8)}" }
    var id by remember(existing?.id) { mutableStateOf(generatedId) }
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "Action target") }
    var kind by remember { mutableStateOf(existing?.kind ?: ActionKind.TAP) }
    var startX by remember { mutableStateOf(existing?.start?.x?.toString() ?: "0.5") }
    var startY by remember { mutableStateOf(existing?.start?.y?.toString() ?: "0.5") }
    var endX by remember { mutableStateOf(existing?.end?.x?.toString() ?: "0.7") }
    var endY by remember { mutableStateOf(existing?.end?.y?.toString() ?: "0.5") }
    var duration by remember { mutableStateOf(existing?.durationMs?.toString() ?: "250") }

    val action = buildActionOrNull(id, name, kind, startX, startY, endX, endY, duration)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = editorDialogProperties(),
        title = { Text(if (existing == null) "Add action target" else "Edit action target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(id, { id = it.trim() }, Modifier.weight(1f), label = { Text("Action id") }, singleLine = true)
                    OutlinedTextField(name, { name = it }, Modifier.weight(1f), label = { Text("Name") }, singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceButton("Tap", kind == ActionKind.TAP) { kind = ActionKind.TAP }
                    ChoiceButton("Swipe", kind == ActionKind.SWIPE) { kind = ActionKind.SWIPE }
                }
                Text("Start point", style = MaterialTheme.typography.labelMedium)
                PointFields(startX, { startX = it }, startY, { startY = it })
                if (kind == ActionKind.SWIPE) {
                    Text("End point", style = MaterialTheme.typography.labelMedium)
                    PointFields(endX, { endX = it }, endY, { endY = it })
                    OutlinedTextField(duration, { duration = it }, label = { Text("Duration ms") }, singleLine = true)
                }
            }
        },
        confirmButton = { Button(onClick = { action?.let(onSave) }, enabled = action != null) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PointFields(x: String, onX: (String) -> Unit, y: String, onY: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CoordinateField("X", x, onX, Modifier.weight(1f))
        CoordinateField("Y", y, onY, Modifier.weight(1f))
    }
}

private fun buildActionOrNull(
    id: String,
    name: String,
    kind: ActionKind,
    startX: String,
    startY: String,
    endX: String,
    endY: String,
    duration: String,
): ActionTarget? = runCatching {
    ActionTarget(
        id = id.requireId(),
        name = name.ifBlank { id },
        kind = kind,
        start = NormalizedPoint(startX.requireUnitFloat(), startY.requireUnitFloat()),
        end = if (kind == ActionKind.SWIPE) NormalizedPoint(endX.requireUnitFloat(), endY.requireUnitFloat()) else null,
        durationMs = duration.toLongOrNull()?.coerceIn(50, 10_000) ?: 250,
    )
}.getOrNull()

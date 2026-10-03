package dev.tapscript.app.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.tapscript.engine.api.model.ActionTarget

@Composable
internal fun ProfileActionsSection(
    actions: List<ActionTarget>,
    onAdd: () -> Unit,
    onEdit: (ActionTarget) -> Unit,
    onDelete: (ActionTarget) -> Unit,
) {
    EditorSection("Action targets") {
        if (actions.isEmpty()) Hint("Name tap/swipe targets once; scripts and rules reference names, not pixels.")
        actions.forEach { action ->
            ConfigItem(
                title = action.name,
                subtitle = "${action.id} • ${action.kind.name.lowercase()} • ${action.start.x.shortCoordinate()}, ${action.start.y.shortCoordinate()}",
                onEdit = { onEdit(action) },
                onDelete = { onDelete(action) },
            )
        }
        OutlinedButton(onClick = onAdd) { Text("Add action target") }
    }
}

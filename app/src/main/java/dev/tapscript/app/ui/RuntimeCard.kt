package dev.tapscript.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun RuntimeCard(state: DashboardUiState, onStopSession: () -> Unit) {
    val session = state.sessionStatus
    var logsExpanded by remember(session.profileId) { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Runtime", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(session.profileName, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = onStopSession) { Text("Stop") }
            }
            Text(
                text = "${session.phase}: ${session.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Frame age", "${session.metrics.frameAgeMs} ms", Modifier.weight(1f))
                Metric("OCR", "${session.metrics.recognitionMs} ms", Modifier.weight(1f))
                Metric("Decision", "${session.metrics.decisionMs} ms", Modifier.weight(1f))
            }
            Text(
                text = "Last command: ${session.metrics.lastCommand.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodySmall,
            )
            session.snapshot?.values?.entries?.take(8)?.let { values ->
                if (values.isNotEmpty()) {
                    HorizontalDivider()
                    values.forEach { (key, value) ->
                        Text("$key = ${value.toCompactText()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Live logs · ${state.liveLogs.size}",
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(onClick = { logsExpanded = !logsExpanded }) {
                    Text(if (logsExpanded) "Collapse" else "Open")
                }
            }
            if (logsExpanded) {
                LiveLogViewer(logs = state.liveLogs, modifier = Modifier.fillMaxWidth())
            } else if (state.liveLogs.isNotEmpty()) {
                state.liveLogs.takeLast(4).forEach { LogEntryRow(it) }
            } else {
                Text(
                    "Execution and JavaScript log() output will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

private fun Any?.toCompactText(): String {
    val raw = when (this) {
        null -> "null"
        is List<*> -> joinToString(prefix = "[", postfix = "]", limit = 3)
        is Map<*, *> -> entries.joinToString(prefix = "{", postfix = "}", limit = 3)
        else -> toString()
    }
    return raw.take(120)
}

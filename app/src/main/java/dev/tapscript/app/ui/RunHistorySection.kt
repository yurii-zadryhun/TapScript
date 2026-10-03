package dev.tapscript.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.AutomationLogEntry
import dev.tapscript.engine.api.model.AutomationRunRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun RunHistorySection(
    records: List<AutomationRunRecord>,
    onDelete: (AutomationRunRecord) -> Unit,
    onClear: () -> Unit,
) {
    var selected by remember { mutableStateOf<AutomationRunRecord?>(null) }
    var showAll by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Run history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Persistent logs, actions, timing, and outcomes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (records.isNotEmpty()) {
                TextButton(onClick = { showAll = true }) { Text("View all") }
            }
        }

        if (records.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Completed profile runs will appear here and stay available after the session stops.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            records.take(3).forEach { record ->
                RunHistoryCard(record = record, onClick = { selected = record })
            }
        }
    }

    if (showAll) {
        AlertDialog(
            onDismissRequest = { showAll = false },
            confirmButton = { TextButton(onClick = { showAll = false }) { Text("Close") } },
            dismissButton = {
                if (records.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onClear()
                            showAll = false
                        },
                    ) { Text("Clear history") }
                }
            },
            title = { Text("Run history") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(records, key = { it.id }) { record ->
                        RunHistoryCard(
                            record = record,
                            onClick = {
                                showAll = false
                                selected = record
                            },
                        )
                    }
                }
            },
        )
    }

    selected?.let { record ->
        RunDetailDialog(
            record = record,
            onDismiss = { selected = null },
            onDelete = {
                onDelete(record)
                selected = null
            },
        )
    }
}

@Composable
private fun RunHistoryCard(record: AutomationRunRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.profileName, fontWeight = FontWeight.SemiBold)
                Text(record.outcome.name.lowercase().replaceFirstChar { it.titlecase() })
            }
            Text(
                "${formatTimestamp(record.startedAtEpochMs)} · ${formatDuration(record.durationMs)} · ${record.logs.size} log entries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RunDetailDialog(
    record: AutomationRunRecord,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { OutlinedButton(onClick = onDelete) { Text("Delete") } },
        title = { Text(record.profileName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Started ${formatTimestamp(record.startedAtEpochMs)}\nDuration ${formatDuration(record.durationMs)} · ${record.outcome.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                if (record.logs.isEmpty()) {
                    Text("No log entries")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(record.logs) { entry ->
                            LogEntryRow(entry)
                        }
                    }
                }
            }
        },
    )
}

@Composable
internal fun LogEntryRow(entry: AutomationLogEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "${formatLogTime(entry.timestampEpochMs)}  ${entry.level.name}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            entry.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epochMs))

private fun formatLogTime(epochMs: Long): String =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(epochMs))

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = durationMs % 1_000
    return if (minutes > 0) "%dm %02ds".format(minutes, seconds) else "%d.%03ds".format(seconds, millis)
}

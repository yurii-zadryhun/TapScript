package dev.tapscript.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.AutomationLogEntry
import dev.tapscript.engine.api.model.AutomationLogLevel

@Composable
internal fun LiveLogViewer(
    logs: List<AutomationLogEntry>,
    modifier: Modifier = Modifier,
    maxVisibleEntries: Int = 2_000,
) {
    var query by remember { mutableStateOf("") }
    var selectedLevels by remember {
        mutableStateOf(AutomationLogLevel.entries.toSet())
    }
    var followTail by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val visible = remember(logs, query, selectedLevels, maxVisibleEntries) {
        val needle = query.trim()
        logs.asSequence()
            .filter { it.level in selectedLevels }
            .filter { needle.isBlank() || it.message.contains(needle, ignoreCase = true) }
            .takeLast(maxVisibleEntries)
            .toList()
    }

    LaunchedEffect(visible.size, followTail) {
        if (followTail && visible.isNotEmpty()) {
            listState.scrollToItem(visible.lastIndex)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${logs.size} entries",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Follow", style = MaterialTheme.typography.labelSmall)
                Switch(checked = followTail, onCheckedChange = { followTail = it })
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search logs") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AutomationLogLevel.entries.forEach { level ->
                FilterChip(
                    selected = level in selectedLevels,
                    onClick = {
                        selectedLevels = if (level in selectedLevels) {
                            selectedLevels - level
                        } else {
                            selectedLevels + level
                        }
                    },
                    label = { Text(level.name.lowercase()) },
                )
            }
        }
        if (visible.isEmpty()) {
            Text(
                if (logs.isEmpty()) "No live log entries yet." else "No log entries match the current filters.",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(
                    count = visible.size,
                    key = { index -> "${visible[index].timestampEpochMs}-$index" },
                ) { index ->
                    LogEntryRow(visible[index])
                }
            }
        }
    }
}

private fun <T> Sequence<T>.takeLast(limit: Int): Sequence<T> {
    if (limit <= 0) return emptySequence()
    val buffer = ArrayDeque<T>(limit)
    for (item in this) {
        if (buffer.size == limit) buffer.removeFirst()
        buffer.addLast(item)
    }
    return buffer.asSequence()
}

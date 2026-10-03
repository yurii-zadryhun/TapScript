package dev.tapscript.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.AutomationProfile

@Composable
internal fun ProfileCard(
    profile: AutomationProfile,
    canRun: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
    onLaunch: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${profile.regions.size} region(s) • ${profile.actions.size} action(s) • ${profile.logic.mode.name.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (profile.targetPackage.isNotBlank()) {
                Text(profile.targetPackage, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onStart, enabled = canRun) { Text("Start") }
                if (profile.targetPackage.isNotBlank()) {
                    OutlinedButton(onClick = onLaunch) { Text("Launch app") }
                }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            if (!canRun) {
                Text(
                    "Enable accessibility and capture before starting.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

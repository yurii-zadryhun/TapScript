package dev.tapscript.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tapscript.app.overlay.FloatingOverlayState

@Composable
internal fun FloatingControlsCard(
    state: FloatingOverlayState,
    onSetEnabled: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Floating controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (state.visible) "Bubble is visible over other apps" else "Runtime status and quick controls over the target app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.enabled && state.permissionGranted,
                    onCheckedChange = { enabled ->
                        if (enabled && !state.permissionGranted) onRequestPermission() else onSetEnabled(enabled)
                    },
                    enabled = state.permissionGranted,
                )
            }

            if (!state.permissionGranted) {
                Text(
                    "Android needs ‘display over other apps’ permission for the draggable bubble and overlay UI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow floating controls")
                }
            } else {
                Text(
                    "Drag the bubble anywhere. Tap it for status, Open TapScript, Stop profile, or Hide bubble.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

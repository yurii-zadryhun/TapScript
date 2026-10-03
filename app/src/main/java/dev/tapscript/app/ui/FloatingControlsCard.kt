package dev.tapscript.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tapscript.app.overlay.FloatingOverlayState

@Composable
internal fun FloatingControlsCard(
    state: FloatingOverlayState,
    onRequestPermission: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Floating controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    !state.permissionGranted -> "Overlay permission is required."
                    state.visible -> "Compact TapScript control rail is active."
                    else -> "Starting floating controls…"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!state.permissionGranted) {
                Text(
                    "Android needs ‘display over other apps’ permission for the control rail, visual picker, and workspace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow floating controls")
                }
            } else {
                Text(
                    "The rail stays available while TapScript is open or minimized. Drag it anywhere; tap to expand controls. Use × to stop TapScript and close its runtime surfaces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

package dev.tapscript.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tapscript.platform.android.capture.CaptureState

@Composable
internal fun ReadinessCard(
    accessibilityEnabled: Boolean,
    captureState: CaptureState,
    onOpenAccessibilitySettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            StatusLine(
                title = "Accessibility gestures",
                ready = accessibilityEnabled,
                detail = if (accessibilityEnabled) "Ready" else "Required for taps and swipes",
            )
            StatusLine(
                title = "Screen capture",
                ready = captureState is CaptureState.Active,
                detail = captureLabel(captureState),
            )
            if (!accessibilityEnabled) {
                OutlinedButton(onClick = onOpenAccessibilitySettings) { Text("Accessibility settings") }
            }
            Text(
                "Screen capture starts when you press Start and stops with the profile. Android asks for capture consent for every new session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusLine(title: String, ready: Boolean, detail: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = if (ready) "● Active" else "○ Idle",
            color = if (ready) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun captureLabel(state: CaptureState): String = when (state) {
    CaptureState.Idle -> "Starts automatically with a profile"
    CaptureState.Starting -> "Starting…"
    is CaptureState.Active -> "${state.width}×${state.height} active"
    is CaptureState.Error -> "Error: ${state.message}"
}

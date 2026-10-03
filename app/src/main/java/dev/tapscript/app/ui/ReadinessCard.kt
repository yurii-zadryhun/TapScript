package dev.tapscript.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tapscript.platform.android.capture.CaptureState

@Composable
internal fun ReadinessCard(
    accessibilityEnabled: Boolean,
    captureState: CaptureState,
    onRequestCapture: () -> Unit,
    onStopCapture: () -> Unit,
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
            Text("Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!accessibilityEnabled) {
                    OutlinedButton(onClick = onOpenAccessibilitySettings) { Text("Accessibility settings") }
                }
                if (captureState is CaptureState.Active) {
                    OutlinedButton(onClick = onStopCapture) { Text("Stop capture") }
                } else {
                    Button(onClick = onRequestCapture) { Text("Allow screen capture") }
                }
            }
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
            text = if (ready) "● Ready" else "○ Setup",
            color = if (ready) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun captureLabel(state: CaptureState): String = when (state) {
    CaptureState.Idle -> "Not active"
    CaptureState.Starting -> "Starting…"
    is CaptureState.Active -> "${state.width}×${state.height} active"
    is CaptureState.Error -> "Error: ${state.message}"
}

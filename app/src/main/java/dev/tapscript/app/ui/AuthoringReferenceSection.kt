package dev.tapscript.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AuthoringReferenceSection(
    bitmap: Bitmap?,
    livePickerAvailable: Boolean,
    onPreviewLive: () -> Unit,
    onPickLiveRegion: () -> Unit,
    onPickLiveTap: () -> Unit,
    onPickLiveSwipe: () -> Unit,
    onImportScreenshot: () -> Unit,
    onDrawRegion: () -> Unit,
    onPlaceTap: () -> Unit,
    onDrawSwipe: () -> Unit,
) {
    EditorSection("Visual setup") {
        Hint(
            if (livePickerAvailable) {
                "Preview all configured geometry on the dimmed target app, or draw new coordinates directly on it."
            } else {
                "Select a target app above to pick directly on it, or use a screenshot reference for whole-screen profiles."
            },
        )
        OutlinedButton(
            onClick = onPreviewLive,
            modifier = Modifier.fillMaxWidth(),
            enabled = livePickerAvailable,
        ) { Text("Preview profile layout on target app") }
        Button(
            onClick = onPickLiveRegion,
            modifier = Modifier.fillMaxWidth(),
            enabled = livePickerAvailable,
        ) { Text("Pick OCR region on target app") }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPickLiveTap,
                modifier = Modifier.weight(1f),
                enabled = livePickerAvailable,
            ) { Text("Pick tap") }
            Button(
                onClick = onPickLiveSwipe,
                modifier = Modifier.weight(1f),
                enabled = livePickerAvailable,
            ) { Text("Pick swipe") }
        }

        HorizontalDivider()
        Hint("Screenshot fallback: useful for whole-screen profiles or authoring without launching the target app.")
        OutlinedButton(onClick = onImportScreenshot, modifier = Modifier.fillMaxWidth()) {
            Text(if (bitmap == null) "Import screenshot" else "Replace screenshot")
        }
        if (bitmap != null) {
            Hint("Reference loaded: ${bitmap.width}×${bitmap.height}. The image is not stored in the profile.")
            OutlinedButton(onClick = onDrawRegion, modifier = Modifier.fillMaxWidth()) { Text("Draw region on screenshot") }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPlaceTap, modifier = Modifier.weight(1f)) { Text("Place tap") }
                OutlinedButton(onClick = onDrawSwipe, modifier = Modifier.weight(1f)) { Text("Draw swipe") }
            }
        }
    }
}

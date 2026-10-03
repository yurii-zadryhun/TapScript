package dev.tapscript.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AuthoringReferenceSection(
    bitmap: Bitmap?,
    onImportScreenshot: () -> Unit,
    onDrawRegion: () -> Unit,
    onPlaceTap: () -> Unit,
    onDrawSwipe: () -> Unit,
) {
    EditorSection("Visual setup") {
        Hint(
            if (bitmap == null) {
                "Import a game screenshot, then draw OCR regions and gesture targets instead of typing coordinates. The image is only an editor reference and is not saved in the profile."
            } else {
                "Reference loaded: ${bitmap.width}×${bitmap.height}. Coordinates are stored normalized, so the profile is resolution-independent."
            },
        )
        OutlinedButton(onClick = onImportScreenshot, modifier = Modifier.fillMaxWidth()) {
            Text(if (bitmap == null) "Import screenshot" else "Replace screenshot")
        }
        if (bitmap != null) {
            Button(onClick = onDrawRegion, modifier = Modifier.fillMaxWidth()) { Text("Draw text region") }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlaceTap, modifier = Modifier.weight(1f)) { Text("Place tap") }
                Button(onClick = onDrawSwipe, modifier = Modifier.weight(1f)) { Text("Draw swipe") }
            }
        }
    }
}

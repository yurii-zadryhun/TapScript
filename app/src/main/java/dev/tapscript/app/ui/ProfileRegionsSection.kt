package dev.tapscript.app.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.tapscript.engine.api.model.NormalizedRect
import dev.tapscript.engine.api.model.RecognitionRegion

@Composable
internal fun ProfileRegionsSection(
    regions: List<RecognitionRegion>,
    onAdd: () -> Unit,
    onEdit: (RecognitionRegion) -> Unit,
    onDelete: (RecognitionRegion) -> Unit,
    onRedraw: ((RecognitionRegion) -> Unit)? = null,
) {
    EditorSection("Recognition regions") {
        if (regions.isEmpty()) Hint("No regions. Add a small rectangle around text you want to read.")
        regions.forEach { region ->
            ConfigItem(
                title = region.name,
                subtitle = "${region.id} • ${region.bounds.compact()} • ${region.textConfig.extractors.size} extractor(s)",
                onEdit = { onEdit(region) },
                onDelete = { onDelete(region) },
                onRedraw = onRedraw?.let { redraw -> { redraw(region) } },
            )
        }
        OutlinedButton(onClick = onAdd) { Text("Add text region") }
    }
}

private fun NormalizedRect.compact(): String =
    "${left.shortCoordinate()},${top.shortCoordinate()} → ${right.shortCoordinate()},${bottom.shortCoordinate()}"

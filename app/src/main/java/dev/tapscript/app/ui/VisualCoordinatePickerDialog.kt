package dev.tapscript.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.tapscript.engine.api.model.NormalizedPoint
import dev.tapscript.engine.api.model.NormalizedRect
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal enum class VisualPickMode {
    REGION,
    POINT,
    SWIPE,
}

@Composable
internal fun VisualCoordinatePickerDialog(
    bitmap: Bitmap,
    mode: VisualPickMode,
    onDismiss: () -> Unit,
    onRegionPicked: (NormalizedRect) -> Unit = {},
    onPointPicked: (NormalizedPoint) -> Unit = {},
    onSwipePicked: (NormalizedPoint, NormalizedPoint) -> Unit = { _, _ -> },
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var dragStart by remember { mutableStateOf<NormalizedPoint?>(null) }
    var dragCurrent by remember { mutableStateOf<NormalizedPoint?>(null) }
    var point by remember { mutableStateOf<NormalizedPoint?>(null) }
    val highlight = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                PickerHeader(mode = mode, onDismiss = onDismiss)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                        .onSizeChanged { canvasSize = it },
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Authoring screenshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    SelectionOverlay(
                        bitmap = bitmap,
                        canvasSize = canvasSize,
                        mode = mode,
                        dragStart = dragStart,
                        dragCurrent = dragCurrent,
                        point = point,
                        highlight = highlight,
                        onDragStart = {
                            dragStart = it
                            dragCurrent = it
                        },
                        onDrag = { dragCurrent = it },
                        onTap = { point = it },
                    )
                }
                PickerFooter(
                    mode = mode,
                    canConfirm = canConfirm(mode, dragStart, dragCurrent, point),
                    onReset = {
                        dragStart = null
                        dragCurrent = null
                        point = null
                    },
                    onConfirm = {
                        when (mode) {
                            VisualPickMode.REGION -> validRect(dragStart, dragCurrent)?.let(onRegionPicked)
                            VisualPickMode.POINT -> point?.let(onPointPicked)
                            VisualPickMode.SWIPE -> if (validSwipe(dragStart, dragCurrent)) {
                                onSwipePicked(requireNotNull(dragStart), requireNotNull(dragCurrent))
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PickerHeader(mode: VisualPickMode, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(mode.title(), style = MaterialTheme.typography.titleLarge)
            Text(
                mode.instructions(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onDismiss) { Text("Close") }
    }
}

@Composable
private fun BoxScope.SelectionOverlay(
    bitmap: Bitmap,
    canvasSize: IntSize,
    mode: VisualPickMode,
    dragStart: NormalizedPoint?,
    dragCurrent: NormalizedPoint?,
    point: NormalizedPoint?,
    highlight: Color,
    onDragStart: (NormalizedPoint) -> Unit,
    onDrag: (NormalizedPoint) -> Unit,
    onTap: (NormalizedPoint) -> Unit,
) {
    val transform = remember(bitmap.width, bitmap.height, canvasSize) {
        ImageFitTransform.create(bitmap.width, bitmap.height, canvasSize)
    }
    val gestureModifier = when (mode) {
        VisualPickMode.REGION, VisualPickMode.SWIPE -> Modifier.pointerInput(transform, mode) {
            detectDragGestures(
                onDragStart = { offset -> transform.toNormalized(offset)?.let(onDragStart) },
                onDrag = { change, _ ->
                    transform.toNormalized(change.position)?.let(onDrag)
                    change.consume()
                },
            )
        }
        VisualPickMode.POINT -> Modifier.pointerInput(transform) {
            detectTapGestures { offset -> transform.toNormalized(offset)?.let(onTap) }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().then(gestureModifier)) {
        if (mode == VisualPickMode.REGION) {
            validRect(dragStart, dragCurrent)?.let { rect ->
                val topLeft = transform.toCanvas(NormalizedPoint(rect.left, rect.top))
                val bottomRight = transform.toCanvas(NormalizedPoint(rect.right, rect.bottom))
                drawRect(
                    color = highlight,
                    topLeft = topLeft,
                    size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y),
                    style = Stroke(width = 4f),
                )
            }
        }
        if (mode == VisualPickMode.SWIPE && dragStart != null && dragCurrent != null) {
            val start = transform.toCanvas(dragStart)
            val end = transform.toCanvas(dragCurrent)
            drawLine(highlight, start, end, strokeWidth = 6f)
            drawCircle(highlight, radius = 10f, center = start, style = Stroke(width = 4f))
            drawCircle(highlight, radius = 10f, center = end)
        }
        if (mode == VisualPickMode.POINT) {
            point?.let { selected ->
                val center = transform.toCanvas(selected)
                drawCircle(color = highlight, radius = 18f, center = center, style = Stroke(width = 5f))
                drawCircle(color = highlight, radius = 4f, center = center)
            }
        }
    }
}

@Composable
private fun PickerFooter(
    mode: VisualPickMode,
    canConfirm: Boolean,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Reset") }
        Button(onClick = onConfirm, enabled = canConfirm, modifier = Modifier.weight(1f)) {
            Text(mode.confirmLabel())
        }
    }
}

private fun canConfirm(
    mode: VisualPickMode,
    dragStart: NormalizedPoint?,
    dragCurrent: NormalizedPoint?,
    point: NormalizedPoint?,
): Boolean = when (mode) {
    VisualPickMode.REGION -> validRect(dragStart, dragCurrent) != null
    VisualPickMode.POINT -> point != null
    VisualPickMode.SWIPE -> validSwipe(dragStart, dragCurrent)
}

private fun validRect(start: NormalizedPoint?, end: NormalizedPoint?): NormalizedRect? {
    if (start == null || end == null) return null
    val left = min(start.x, end.x)
    val top = min(start.y, end.y)
    val right = max(start.x, end.x)
    val bottom = max(start.y, end.y)
    if (right - left < MIN_REGION_SIZE || bottom - top < MIN_REGION_SIZE) return null
    return NormalizedRect(left, top, right, bottom)
}

private fun validSwipe(start: NormalizedPoint?, end: NormalizedPoint?): Boolean {
    if (start == null || end == null) return false
    return hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble()) >= MIN_SWIPE_DISTANCE
}

private fun VisualPickMode.title(): String = when (this) {
    VisualPickMode.REGION -> "Draw recognition region"
    VisualPickMode.POINT -> "Place tap target"
    VisualPickMode.SWIPE -> "Draw swipe target"
}

private fun VisualPickMode.instructions(): String = when (this) {
    VisualPickMode.REGION -> "Drag tightly around the text you want OCR to read."
    VisualPickMode.POINT -> "Tap the center of the control TapScript should press."
    VisualPickMode.SWIPE -> "Drag from the swipe start point to the end point."
}

private fun VisualPickMode.confirmLabel(): String = when (this) {
    VisualPickMode.REGION -> "Use region"
    VisualPickMode.POINT -> "Use point"
    VisualPickMode.SWIPE -> "Use swipe"
}

private const val MIN_REGION_SIZE = 0.005f
private const val MIN_SWIPE_DISTANCE = 0.02

package dev.tapscript.app.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import dev.tapscript.engine.api.model.NormalizedPoint
import dev.tapscript.engine.api.model.NormalizedRect
import dev.tapscript.engine.api.model.VisualTargetGeometry
import dev.tapscript.engine.api.model.VisualTone
import dev.tapscript.engine.api.ports.RuntimeVisualPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class RuntimeVisualOverlayController(context: Context) : RuntimeVisualPresenter {
    private val applicationContext = context.applicationContext
    private val windowManager = applicationContext.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val elements = linkedMapOf<String, RuntimeVisualElement>()
    private var overlayView: RuntimeVisualView? = null

    override suspend fun showHighlight(
        key: String,
        target: VisualTargetGeometry,
        label: String,
        tone: VisualTone,
    ): Result<Unit> = mutate {
        requireOverlayPermission()
        elements[key] = RuntimeVisualElement.Highlight(target, label, tone)
        render()
    }

    override suspend fun showInfo(
        key: String,
        title: String,
        body: String,
        tone: VisualTone,
    ): Result<Unit> = mutate {
        requireOverlayPermission()
        elements[key] = RuntimeVisualElement.Info(title, body, tone)
        render()
    }

    override suspend fun clear(key: String): Result<Unit> = mutate {
        elements.remove(key)
        render()
    }

    override suspend fun clearAll(): Result<Unit> = mutate {
        removeOverlay()
    }

    /** Immediate best-effort cleanup for app shutdown paths that are not suspendable. */
    fun dismiss() {
        mainHandler.post { removeOverlay() }
    }

    private suspend fun mutate(block: () -> Unit): Result<Unit> = try {
        withContext(Dispatchers.Main.immediate) { block() }
        Result.success(Unit)
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }

    private fun requireOverlayPermission() {
        check(Settings.canDrawOverlays(applicationContext)) {
            "Display-over-other-apps permission is required for runtime visuals"
        }
    }

    private fun render() {
        if (elements.isEmpty()) {
            removeOverlay()
            return
        }

        val view = overlayView ?: RuntimeVisualView(applicationContext).also { created ->
            val layout = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            windowManager.addView(created, layout)
            overlayView = created
        }

        view.setElements(elements.values.toList())
    }

    private fun removeOverlay() {
        elements.clear()
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
    }
}

private sealed interface RuntimeVisualElement {
    data class Highlight(
        val target: VisualTargetGeometry,
        val label: String,
        val tone: VisualTone,
    ) : RuntimeVisualElement

    data class Info(
        val title: String,
        val body: String,
        val tone: VisualTone,
    ) : RuntimeVisualElement
}

private class RuntimeVisualView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }
    private val softFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(238, 27, 30, 39)
        style = Paint.Style.FILL
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(15f)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(220, 224, 235)
        textSize = sp(12f)
    }
    private val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(11f)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val chipBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 25, 28, 36)
        style = Paint.Style.FILL
    }
    private var elements: List<RuntimeVisualElement> = emptyList()

    fun setElements(elements: List<RuntimeVisualElement>) {
        this.elements = elements
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        elements.filterIsInstance<RuntimeVisualElement.Highlight>()
            .forEach { drawHighlight(canvas, it) }
        drawInfoCards(canvas, elements.filterIsInstance<RuntimeVisualElement.Info>())
    }

    private fun drawHighlight(canvas: Canvas, highlight: RuntimeVisualElement.Highlight) {
        val accent = accent(highlight.tone)
        strokePaint.color = accent
        softFillPaint.color = Color.argb(28, Color.red(accent), Color.green(accent), Color.blue(accent))

        when (val target = highlight.target) {
            is VisualTargetGeometry.Region -> drawRegion(canvas, target.bounds, highlight.label, accent)
            is VisualTargetGeometry.Tap -> drawTap(canvas, target.point, highlight.label, accent)
            is VisualTargetGeometry.Swipe -> drawSwipe(canvas, target.start, target.end, highlight.label, accent)
        }
    }

    /**
     * Region strokes are deliberately drawn outside the configured ROI. MediaProjection may capture
     * application overlays; keeping the stroke outside avoids contaminating the next OCR pass and
     * avoids a visual-overlay -> change-detector feedback loop inside the watched region.
     */
    private fun drawRegion(canvas: Canvas, bounds: NormalizedRect, label: String, accent: Int) {
        val original = bounds.toRect(width, height)
        val outset = strokePaint.strokeWidth / 2f + dp(5f)
        val outlined = RectF(
            (original.left - outset).coerceAtLeast(1f),
            (original.top - outset).coerceAtLeast(1f),
            (original.right + outset).coerceAtMost(width - 1f),
            (original.bottom + outset).coerceAtMost(height - 1f),
        )
        canvas.drawRoundRect(outlined, dp(8f), dp(8f), strokePaint)
        drawChip(canvas, label, outlined.left, outlined.top - dp(7f), accent)
    }

    private fun drawTap(canvas: Canvas, point: NormalizedPoint, label: String, accent: Int) {
        val (x, y) = point.toScreen(width, height)
        val radius = dp(24f)
        canvas.drawCircle(x, y, radius, softFillPaint)
        canvas.drawCircle(x, y, radius, strokePaint)
        canvas.drawLine(x - radius * 1.25f, y, x + radius * 1.25f, y, strokePaint)
        canvas.drawLine(x, y - radius * 1.25f, x, y + radius * 1.25f, strokePaint)
        drawChip(canvas, label, x + radius + dp(6f), y - dp(8f), accent)
    }

    private fun drawSwipe(
        canvas: Canvas,
        start: NormalizedPoint,
        end: NormalizedPoint,
        label: String,
        accent: Int,
    ) {
        val (sx, sy) = start.toScreen(width, height)
        val (ex, ey) = end.toScreen(width, height)
        canvas.drawLine(sx, sy, ex, ey, strokePaint)
        canvas.drawCircle(sx, sy, dp(7f), strokePaint)
        canvas.drawCircle(ex, ey, dp(11f), strokePaint)
        drawChip(canvas, label, ex + dp(12f), ey, accent)
    }

    private fun drawChip(canvas: Canvas, rawLabel: String, anchorX: Float, anchorY: Float, accent: Int) {
        if (rawLabel.isBlank()) return
        val maxTextWidth = (width - dp(32f)).coerceAtLeast(dp(80f))
        val label = ellipsize(rawLabel.trim(), chipTextPaint, maxTextWidth)
        val horizontal = dp(8f)
        val vertical = dp(5f)
        val textWidth = chipTextPaint.measureText(label)
        val metrics = chipTextPaint.fontMetrics
        val chipHeight = metrics.descent - metrics.ascent + vertical * 2f
        val desiredLeft = anchorX
        val left = desiredLeft.coerceIn(dp(8f), max(dp(8f), width - textWidth - horizontal * 2f - dp(8f)))
        val bottom = anchorY.coerceIn(chipHeight + dp(8f), height - dp(8f))
        val rect = RectF(left, bottom - chipHeight, left + textWidth + horizontal * 2f, bottom)
        canvas.drawRoundRect(rect, dp(8f), dp(8f), chipBackgroundPaint)
        softFillPaint.color = accent
        canvas.drawRoundRect(
            RectF(rect.left, rect.top, rect.left + dp(4f), rect.bottom),
            dp(4f),
            dp(4f),
            softFillPaint,
        )
        canvas.drawText(label, rect.left + horizontal, rect.bottom - vertical - metrics.descent, chipTextPaint)
    }

    private fun drawInfoCards(canvas: Canvas, infos: List<RuntimeVisualElement.Info>) {
        if (infos.isEmpty()) return

        val visible = infos.takeLast(MAX_INFO_CARDS)
        val sideMargin = dp(16f)
        val maxCardWidth = min(width - sideMargin * 2f, dp(520f))
        var top = dp(44f)

        visible.forEach { info ->
            val accent = accent(info.tone)
            val innerWidth = maxCardWidth - dp(28f)
            val bodyLines = wrapText(info.body, bodyPaint, innerWidth, MAX_BODY_LINES)
            val titleHeight = titlePaint.fontSpacing
            val bodyHeight = if (bodyLines.isEmpty()) 0f else bodyLines.size * bodyPaint.fontSpacing + dp(5f)
            val cardHeight = dp(22f) + titleHeight + bodyHeight + dp(12f)
            val left = (width - maxCardWidth) / 2f
            val rect = RectF(left, top, left + maxCardWidth, top + cardHeight)

            canvas.drawRoundRect(rect, dp(16f), dp(16f), cardPaint)
            softFillPaint.color = accent
            canvas.drawRoundRect(
                RectF(rect.left, rect.top, rect.left + dp(5f), rect.bottom),
                dp(5f),
                dp(5f),
                softFillPaint,
            )

            var baseline = rect.top + dp(15f) - titlePaint.fontMetrics.ascent
            canvas.drawText(
                ellipsize(info.title, titlePaint, innerWidth),
                rect.left + dp(16f),
                baseline,
                titlePaint,
            )
            baseline += titlePaint.fontSpacing + dp(5f)
            bodyLines.forEach { line ->
                canvas.drawText(line, rect.left + dp(16f), baseline, bodyPaint)
                baseline += bodyPaint.fontSpacing
            }

            top = rect.bottom + dp(8f)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val lines = mutableListOf<String>()

        text.lines().forEach { paragraph ->
            var remaining = paragraph.trim()
            if (remaining.isEmpty()) {
                if (lines.size < maxLines) lines += ""
                return@forEach
            }

            while (remaining.isNotEmpty() && lines.size < maxLines) {
                var count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
                if (count < remaining.length) {
                    val whitespace = remaining.lastIndexOf(' ', startIndex = count - 1)
                    if (whitespace > 0) count = whitespace
                }
                lines += remaining.take(count).trim()
                remaining = remaining.drop(count).trimStart()
            }
        }

        if (lines.size == maxLines && text.length > lines.sumOf { it.length }) {
            val last = lines.lastOrNull().orEmpty()
            lines[lines.lastIndex] = ellipsize("$last …", paint, maxWidth)
        }
        return lines
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val suffix = "…"
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + suffix) > maxWidth) end--
        return text.substring(0, end).trimEnd() + suffix
    }

    private fun accent(tone: VisualTone): Int = when (tone) {
        VisualTone.NEUTRAL -> Color.rgb(170, 176, 194)
        VisualTone.INFO -> Color.rgb(115, 160, 255)
        VisualTone.SUCCESS -> Color.rgb(83, 218, 139)
        VisualTone.WARNING -> Color.rgb(255, 190, 90)
        VisualTone.DANGER -> Color.rgb(255, 103, 103)
    }

    private fun NormalizedRect.toRect(width: Int, height: Int): RectF = RectF(
        left * width,
        top * height,
        right * width,
        bottom * height,
    )

    private fun NormalizedPoint.toScreen(width: Int, height: Int): Pair<Float, Float> =
        x * width to y * height

    private fun dp(value: Float): Float = value * density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity

    private companion object {
        const val MAX_INFO_CARDS = 3
        const val MAX_BODY_LINES = 8
    }
}

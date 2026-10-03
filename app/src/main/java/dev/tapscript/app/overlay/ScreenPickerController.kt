package dev.tapscript.app.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import dev.tapscript.app.MainActivity
import dev.tapscript.engine.api.model.NormalizedPoint
import dev.tapscript.engine.api.model.NormalizedRect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ScreenPickerController(context: Context) {
    private val applicationContext = context.applicationContext
    private val windowManager = applicationContext.getSystemService(WindowManager::class.java)
    private val mutableState = MutableStateFlow(ScreenPickerState())
    private var pickerView: PickerView? = null
    private var resultSequence = 0L
    private var returnToTapScriptAfterPick = true

    val state: StateFlow<ScreenPickerState> = mutableState

    fun start(mode: ScreenPickMode, returnToTapScript: Boolean = true): Result<Unit> = runCatching {
        check(Settings.canDrawOverlays(applicationContext)) {
            "Display-over-other-apps permission is required for live picking"
        }
        cancel(returnToTapScript = false)
        returnToTapScriptAfterPick = returnToTapScript

        val view = PickerView(
            context = applicationContext,
            mode = mode,
            onComplete = ::complete,
            onCancel = { cancel(returnToTapScriptAfterPick) },
        )
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        pickerView = view
        windowManager.addView(view, layout)
        mutableState.value = mutableState.value.copy(activeMode = mode)
    }

    fun consumeResult(sequence: Long) {
        if (mutableState.value.resultSequence == sequence) {
            mutableState.value = mutableState.value.copy(result = null)
        }
    }

    fun cancel(returnToTapScript: Boolean = true) {
        pickerView?.let { view -> runCatching { windowManager.removeView(view) } }
        pickerView = null
        mutableState.value = mutableState.value.copy(activeMode = null)
        if (returnToTapScript) openTapScript()
    }

    private fun complete(result: ScreenPickResult) {
        pickerView?.let { view -> runCatching { windowManager.removeView(view) } }
        pickerView = null
        resultSequence += 1
        mutableState.value = ScreenPickerState(
            activeMode = null,
            result = result,
            resultSequence = resultSequence,
        )
        if (returnToTapScriptAfterPick) openTapScript()
    }

    private fun openTapScript() {
        applicationContext.startActivity(
            Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }

    private class PickerView(
        context: Context,
        private val mode: ScreenPickMode,
        private val onComplete: (ScreenPickResult) -> Unit,
        private val onCancel: () -> Unit,
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(45, 0, 0, 0) }
        private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(115, 160, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 115, 160, 255)
            style = Paint.Style.FILL
        }
        private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(225, 30, 33, 43) }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f * density
        }
        private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(205, 210, 225)
            textSize = 12f * density
        }
        private val cancelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 130, 130)
            textSize = 13f * density
        }
        private val cancelRect = RectF()
        private var startX: Float? = null
        private var startY: Float? = null
        private var currentX: Float? = null
        private var currentY: Float? = null

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
            drawInstruction(canvas)
            drawSelection(canvas)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_UP && cancelRect.contains(event.x, event.y)) {
                onCancel()
                return true
            }

            when (mode) {
                ScreenPickMode.POINT -> handlePoint(event)
                ScreenPickMode.SWIPE,
                ScreenPickMode.REGION -> handleDrag(event)
            }
            return true
        }

        private fun handlePoint(event: MotionEvent) {
            currentX = event.x
            currentY = event.y
            invalidate()
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                onComplete(ScreenPickResult.Point(normalize(event.x, event.y)))
            }
        }

        private fun handleDrag(event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    currentX = event.x
                    currentY = event.y
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    currentX = event.x
                    currentY = event.y
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    val sx = startX ?: return
                    val sy = startY ?: return
                    val ex = event.x
                    val ey = event.y
                    if (abs(ex - sx) < 6f * density && abs(ey - sy) < 6f * density) {
                        currentX = null
                        currentY = null
                        startX = null
                        startY = null
                        invalidate()
                        return
                    }
                    when (mode) {
                        ScreenPickMode.SWIPE -> onComplete(
                            ScreenPickResult.Swipe(
                                start = normalize(sx, sy),
                                end = normalize(ex, ey),
                            ),
                        )
                        ScreenPickMode.REGION -> {
                            val left = min(sx, ex).coerceIn(0f, width.toFloat()) / width.coerceAtLeast(1)
                            val top = min(sy, ey).coerceIn(0f, height.toFloat()) / height.coerceAtLeast(1)
                            val right = max(sx, ex).coerceIn(0f, width.toFloat()) / width.coerceAtLeast(1)
                            val bottom = max(sy, ey).coerceIn(0f, height.toFloat()) / height.coerceAtLeast(1)
                            onComplete(
                                ScreenPickResult.Region(
                                    NormalizedRect(
                                        left = left.coerceIn(0f, 0.9999f),
                                        top = top.coerceIn(0f, 0.9999f),
                                        right = right.coerceIn(0.0001f, 1f),
                                        bottom = bottom.coerceIn(0.0001f, 1f),
                                    ),
                                ),
                            )
                        }
                        ScreenPickMode.POINT -> Unit
                    }
                }
            }
        }

        private fun drawInstruction(canvas: Canvas) {
            val margin = 14f * density
            val height = 72f * density
            val panel = RectF(margin, margin, width - margin, margin + height)
            canvas.drawRoundRect(panel, 18f * density, 18f * density, panelPaint)
            canvas.drawText(mode.title, panel.left + 16f * density, panel.top + 27f * density, textPaint)
            canvas.drawText(mode.hint, panel.left + 16f * density, panel.top + 51f * density, secondaryTextPaint)

            val cancelWidth = 62f * density
            cancelRect.set(panel.right - cancelWidth - 10f * density, panel.top, panel.right, panel.bottom)
            canvas.drawText("CANCEL", cancelRect.left, panel.top + 27f * density, cancelPaint)
        }

        private fun drawSelection(canvas: Canvas) {
            val x = currentX ?: return
            val y = currentY ?: return
            when (mode) {
                ScreenPickMode.POINT -> {
                    val radius = 18f * density
                    canvas.drawCircle(x, y, radius, fillPaint)
                    canvas.drawCircle(x, y, radius, accentPaint)
                    canvas.drawLine(x - radius * 1.4f, y, x + radius * 1.4f, y, accentPaint)
                    canvas.drawLine(x, y - radius * 1.4f, x, y + radius * 1.4f, accentPaint)
                }
                ScreenPickMode.SWIPE -> {
                    val sx = startX ?: return
                    val sy = startY ?: return
                    canvas.drawLine(sx, sy, x, y, accentPaint)
                    canvas.drawCircle(sx, sy, 8f * density, accentPaint)
                    canvas.drawCircle(x, y, 8f * density, accentPaint)
                }
                ScreenPickMode.REGION -> {
                    val sx = startX ?: return
                    val sy = startY ?: return
                    val rect = RectF(min(sx, x), min(sy, y), max(sx, x), max(sy, y))
                    canvas.drawRect(rect, fillPaint)
                    canvas.drawRect(rect, accentPaint)
                }
            }
        }

        private fun normalize(x: Float, y: Float): NormalizedPoint = NormalizedPoint(
            x = (x / width.coerceAtLeast(1)).coerceIn(0f, 1f),
            y = (y / height.coerceAtLeast(1)).coerceIn(0f, 1f),
        )
    }
}

enum class ScreenPickMode(val title: String, val hint: String) {
    POINT("Pick tap target", "Tap the exact place TapScript should press"),
    SWIPE("Pick swipe", "Drag from the swipe start to its end"),
    REGION("Pick OCR region", "Drag a rectangle around the text to recognize"),
}

sealed interface ScreenPickResult {
    data class Point(val point: NormalizedPoint) : ScreenPickResult
    data class Swipe(val start: NormalizedPoint, val end: NormalizedPoint) : ScreenPickResult
    data class Region(val bounds: NormalizedRect) : ScreenPickResult
}

data class ScreenPickerState(
    val activeMode: ScreenPickMode? = null,
    val result: ScreenPickResult? = null,
    val resultSequence: Long = 0,
)

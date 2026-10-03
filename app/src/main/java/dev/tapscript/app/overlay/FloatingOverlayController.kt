package dev.tapscript.app.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import dev.tapscript.app.MainActivity
import dev.tapscript.engine.api.model.AutomationSessionStatus
import dev.tapscript.engine.api.model.SessionPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingOverlayController(
    context: Context,
    sessionStatus: StateFlow<AutomationSessionStatus>,
    private val onStopSession: () -> Unit,
) {
    private val applicationContext = context.applicationContext
    private val windowManager = applicationContext.getSystemService(WindowManager::class.java)
    private val preferences = applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(snapshotState())

    private var bubbleView: View? = null
    private var bubbleLayout: WindowManager.LayoutParams? = null
    private var menuView: View? = null
    private var menuLayout: WindowManager.LayoutParams? = null
    private var statusDot: View? = null
    private var statusText: TextView? = null
    private var profileText: TextView? = null
    private var stopButton: TextView? = null
    private var latestStatus = AutomationSessionStatus()

    val state: StateFlow<FloatingOverlayState> = mutableState

    init {
        mainScope.launch {
            sessionStatus.collect { status ->
                latestStatus = status
                updateStatusViews()
            }
        }
        refresh()
    }

    fun refresh() {
        mainScope.launch {
            val permissionGranted = Settings.canDrawOverlays(applicationContext)
            if (!permissionGranted) {
                removeOverlay()
            } else if (preferences.getBoolean(KEY_ENABLED, false)) {
                showOverlay()
            }
            mutableState.value = snapshotState()
        }
    }

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
        mainScope.launch {
            if (enabled && Settings.canDrawOverlays(applicationContext)) {
                showOverlay()
            } else {
                removeOverlay()
            }
            mutableState.value = snapshotState()
        }
    }

    private fun showOverlay() {
        if (bubbleView != null) return
        if (!Settings.canDrawOverlays(applicationContext)) return

        val bubble = createBubbleView()
        val layout = WindowManager.LayoutParams(
            dp(BUBBLE_SIZE_DP),
            dp(BUBBLE_SIZE_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = preferences.getInt(KEY_X, dp(DEFAULT_X_DP))
            y = preferences.getInt(KEY_Y, dp(DEFAULT_Y_DP))
        }

        bubbleView = bubble
        bubbleLayout = layout
        windowManager.addView(bubble, layout)
        clampBubbleToScreen()
        updateStatusViews()
        mutableState.value = snapshotState()
    }

    private fun removeOverlay() {
        hideMenu()
        bubbleView?.let { view -> runCatching { windowManager.removeView(view) } }
        bubbleView = null
        bubbleLayout = null
        statusDot = null
        mutableState.value = snapshotState()
    }

    private fun createBubbleView(): View {
        val root = FrameLayout(applicationContext).apply {
            background = roundedBackground(BUBBLE_BACKGROUND, dp(BUBBLE_SIZE_DP / 2))
            elevation = dp(8).toFloat()
            contentDescription = "TapScript floating controls"
        }

        val label = TextView(applicationContext).apply {
            text = "TS"
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(
            label,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        val dot = View(applicationContext).apply {
            background = statusDotBackground(IDLE_COLOR)
        }
        val dotSize = dp(11)
        root.addView(
            dot,
            FrameLayout.LayoutParams(dotSize, dotSize, Gravity.END or Gravity.BOTTOM).apply {
                marginEnd = dp(4)
                bottomMargin = dp(4)
            },
        )
        statusDot = dot
        installDragAndClick(root)
        return root
    }

    private fun installDragAndClick(view: View) {
        val touchSlop = ViewConfiguration.get(applicationContext).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragged = false

        view.setOnTouchListener { _, event ->
            val layout = bubbleLayout ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = layout.x
                    startY = layout.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) dragged = true
                    if (dragged) {
                        layout.x = startX + dx.toInt()
                        layout.y = startY + dy.toInt()
                        clampBubbleToScreen()
                        bubbleView?.let { windowManager.updateViewLayout(it, layout) }
                        updateMenuPosition()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragged) {
                        preferences.edit()
                            .putInt(KEY_X, layout.x)
                            .putInt(KEY_Y, layout.y)
                            .apply()
                    } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleMenu() {
        if (menuView == null) showMenu() else hideMenu()
    }

    private fun showMenu() {
        if (menuView != null) return
        val menu = LinearLayout(applicationContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground(PANEL_BACKGROUND, dp(18))
            elevation = dp(12).toFloat()
        }

        menu.addView(textView("TapScript", 17f, Color.WHITE, bold = true))
        profileText = textView("", 13f, SECONDARY_TEXT).also(menu::addView)
        statusText = textView("", 13f, SECONDARY_TEXT).also(menu::addView)
        menu.addView(spacer(dp(10)))

        menu.addView(actionButton("Open TapScript") { openMainActivity() })
        stopButton = actionButton("Stop profile") {
            onStopSession()
            hideMenu()
        }.also(menu::addView)
        menu.addView(actionButton("Hide bubble") {
            preferences.edit().putBoolean(KEY_ENABLED, false).apply()
            removeOverlay()
        })

        val layout = WindowManager.LayoutParams(
            dp(PANEL_WIDTH_DP),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        menuView = menu
        menuLayout = layout
        updateMenuPosition()
        windowManager.addView(menu, layout)
        updateStatusViews()
    }

    private fun hideMenu() {
        menuView?.let { view -> runCatching { windowManager.removeView(view) } }
        menuView = null
        menuLayout = null
        statusText = null
        profileText = null
        stopButton = null
    }

    private fun updateMenuPosition() {
        val bubble = bubbleLayout ?: return
        val panel = menuLayout ?: return
        val screenWidth = applicationContext.resources.displayMetrics.widthPixels
        val panelWidth = dp(PANEL_WIDTH_DP)
        val bubbleSize = dp(BUBBLE_SIZE_DP)
        val gap = dp(8)

        panel.x = if (bubble.x + bubbleSize + gap + panelWidth <= screenWidth) {
            bubble.x + bubbleSize + gap
        } else {
            (bubble.x - panelWidth - gap).coerceAtLeast(0)
        }
        panel.y = bubble.y.coerceAtLeast(0)
        menuView?.let { view -> runCatching { windowManager.updateViewLayout(view, panel) } }
    }

    private fun clampBubbleToScreen() {
        val layout = bubbleLayout ?: return
        val metrics = applicationContext.resources.displayMetrics
        layout.x = layout.x.coerceIn(0, (metrics.widthPixels - dp(BUBBLE_SIZE_DP)).coerceAtLeast(0))
        layout.y = layout.y.coerceIn(0, (metrics.heightPixels - dp(BUBBLE_SIZE_DP)).coerceAtLeast(0))
    }

    private fun updateStatusViews() {
        val status = latestStatus
        statusDot?.background = statusDotBackground(statusColor(status.phase))
        profileText?.text = status.profileName.ifBlank { "No active profile" }
        statusText?.text = "${status.phase.displayName()}: ${status.message.ifBlank { "Ready" }}"
        val active = status.phase !in setOf(SessionPhase.IDLE, SessionPhase.STOPPED, SessionPhase.ERROR)
        stopButton?.apply {
            isEnabled = active
            alpha = if (active) 1f else 0.45f
        }
    }

    private fun openMainActivity() {
        applicationContext.startActivity(
            Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
        hideMenu()
    }

    private fun snapshotState(): FloatingOverlayState = FloatingOverlayState(
        permissionGranted = Settings.canDrawOverlays(applicationContext),
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        visible = bubbleView != null,
    )

    private fun actionButton(label: String, onClick: () -> Unit): TextView =
        textView(label, 14f, Color.WHITE, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedBackground(BUTTON_BACKGROUND, dp(12))
            setOnClickListener { onClick() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            params.topMargin = dp(6)
            layoutParams = params
        }

    private fun textView(text: String, sizeSp: Float, color: Int, bold: Boolean = false): TextView =
        TextView(applicationContext).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun spacer(height: Int): View = View(applicationContext).apply {
        layoutParams = LinearLayout.LayoutParams(1, height)
    }

    private fun roundedBackground(color: Int, radiusPx: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = radiusPx.toFloat()
    }

    private fun statusDotBackground(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(dp(2), Color.WHITE)
    }

    private fun statusColor(phase: SessionPhase): Int = when (phase) {
        SessionPhase.RUNNING -> RUNNING_COLOR
        SessionPhase.WAITING_FOR_FRAME -> WAITING_COLOR
        SessionPhase.PAUSED -> PAUSED_COLOR
        SessionPhase.ERROR -> ERROR_COLOR
        SessionPhase.IDLE, SessionPhase.STOPPED -> IDLE_COLOR
    }

    private fun SessionPhase.displayName(): String = name.lowercase().replaceFirstChar { it.titlecase() }

    private fun dp(value: Int): Int = (value * applicationContext.resources.displayMetrics.density).toInt()

    companion object {
        private const val PREFERENCES = "floating_overlay"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val BUBBLE_SIZE_DP = 54
        private const val PANEL_WIDTH_DP = 232
        private const val DEFAULT_X_DP = 12
        private const val DEFAULT_Y_DP = 180

        private val BUBBLE_BACKGROUND = Color.rgb(38, 42, 54)
        private val PANEL_BACKGROUND = Color.rgb(30, 33, 43)
        private val BUTTON_BACKGROUND = Color.rgb(61, 67, 86)
        private val SECONDARY_TEXT = Color.rgb(194, 199, 216)
        private val IDLE_COLOR = Color.rgb(130, 137, 158)
        private val WAITING_COLOR = Color.rgb(92, 160, 255)
        private val RUNNING_COLOR = Color.rgb(73, 201, 126)
        private val PAUSED_COLOR = Color.rgb(255, 190, 80)
        private val ERROR_COLOR = Color.rgb(255, 95, 95)
    }
}

data class FloatingOverlayState(
    val permissionGranted: Boolean,
    val enabled: Boolean,
    val visible: Boolean,
)

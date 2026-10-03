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
    private val onOpenWorkspace: () -> Unit,
    private val onTogglePause: () -> Unit,
    private val onStopSession: () -> Unit,
    private val onCloseTapScript: () -> Unit,
) {
    private val applicationContext = context.applicationContext
    private val windowManager = applicationContext.getSystemService(WindowManager::class.java)
    private val preferences = applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(snapshotState())

    private var railView: View? = null
    private var railLayout: WindowManager.LayoutParams? = null
    private var menuView: View? = null
    private var menuLayout: WindowManager.LayoutParams? = null
    private var statusDot: View? = null
    private var statusText: TextView? = null
    private var profileText: TextView? = null
    private var pauseButton: TextView? = null
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

    /** Floating controls are part of TapScript itself, not an independently hideable feature. */
    fun refresh() {
        mainScope.launch {
            if (Settings.canDrawOverlays(applicationContext)) {
                showOverlay()
            } else {
                removeOverlay()
            }
            mutableState.value = snapshotState()
        }
    }

    /** Kept for older UI callers while they migrate to the always-on model. */
    fun setEnabled(enabled: Boolean) {
        if (enabled) refresh()
    }

    fun closeOverlay() {
        mainScope.launch { removeOverlay() }
    }

    private fun showOverlay() {
        if (railView != null || !Settings.canDrawOverlays(applicationContext)) return

        val rail = createRailView()
        val layout = WindowManager.LayoutParams(
            dp(RAIL_WIDTH_DP),
            dp(RAIL_HEIGHT_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = preferences.getInt(KEY_X, dp(DEFAULT_X_DP))
            y = preferences.getInt(KEY_Y, dp(DEFAULT_Y_DP))
        }

        railView = rail
        railLayout = layout
        windowManager.addView(rail, layout)
        clampRailToScreen()
        updateStatusViews()
        mutableState.value = snapshotState()
    }

    private fun removeOverlay() {
        hideMenu()
        railView?.let { view -> runCatching { windowManager.removeView(view) } }
        railView = null
        railLayout = null
        statusDot = null
        mutableState.value = snapshotState()
    }

    private fun createRailView(): View {
        val root = FrameLayout(applicationContext).apply {
            background = roundedBackground(RAIL_BACKGROUND, dp(18))
            elevation = dp(8).toFloat()
            contentDescription = "TapScript controls"
        }

        val label = TextView(applicationContext).apply {
            text = "TS\n⌄"
            gravity = Gravity.CENTER
            textSize = 12f
            setLineSpacing(0f, 0.92f)
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
        val dotSize = dp(10)
        root.addView(
            dot,
            FrameLayout.LayoutParams(dotSize, dotSize, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dp(6)
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
            val layout = railLayout ?: return@setOnTouchListener false
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
                        clampRailToScreen()
                        railView?.let { windowManager.updateViewLayout(it, layout) }
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
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground(PANEL_BACKGROUND, dp(18))
            elevation = dp(12).toFloat()
        }

        menu.addView(textView("TapScript", 16f, Color.WHITE, bold = true))
        profileText = textView("", 12f, SECONDARY_TEXT).also(menu::addView)
        statusText = textView("", 12f, SECONDARY_TEXT).also(menu::addView)
        menu.addView(spacer(dp(8)))

        menu.addView(actionButton("◫", "Workspace") {
            hideMenu()
            onOpenWorkspace()
        })
        pauseButton = actionButton("Ⅱ", "Pause / resume") {
            onTogglePause()
            updateStatusViews()
        }.also(menu::addView)
        stopButton = actionButton("■", "Stop profile") {
            onStopSession()
        }.also(menu::addView)
        menu.addView(actionButton("×", "Close TapScript") {
            removeOverlay()
            onCloseTapScript()
            closeMainActivityTask()
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
        pauseButton = null
        stopButton = null
    }

    private fun updateMenuPosition() {
        val rail = railLayout ?: return
        val panel = menuLayout ?: return
        val screenWidth = applicationContext.resources.displayMetrics.widthPixels
        val panelWidth = dp(PANEL_WIDTH_DP)
        val railWidth = dp(RAIL_WIDTH_DP)
        val gap = dp(8)

        panel.x = if (rail.x + railWidth + gap + panelWidth <= screenWidth) {
            rail.x + railWidth + gap
        } else {
            (rail.x - panelWidth - gap).coerceAtLeast(0)
        }
        panel.y = rail.y.coerceAtLeast(0)
        menuView?.let { view -> runCatching { windowManager.updateViewLayout(view, panel) } }
    }

    private fun clampRailToScreen() {
        val layout = railLayout ?: return
        val metrics = applicationContext.resources.displayMetrics
        layout.x = layout.x.coerceIn(0, (metrics.widthPixels - dp(RAIL_WIDTH_DP)).coerceAtLeast(0))
        layout.y = layout.y.coerceIn(0, (metrics.heightPixels - dp(RAIL_HEIGHT_DP)).coerceAtLeast(0))
    }

    private fun updateStatusViews() {
        val status = latestStatus
        statusDot?.background = statusDotBackground(statusColor(status.phase))
        profileText?.text = status.profileName.ifBlank { "No active profile" }
        statusText?.text = "${status.phase.displayName()}: ${status.message.ifBlank { "Ready" }}"
        val active = status.phase !in setOf(SessionPhase.IDLE, SessionPhase.STOPPED, SessionPhase.ERROR)
        listOf(pauseButton, stopButton).forEach { button ->
            button?.isEnabled = active
            button?.alpha = if (active) 1f else 0.45f
        }
        pauseButton?.text = if (status.phase == SessionPhase.PAUSED) "▶   Resume" else "Ⅱ   Pause"
    }

    private fun closeMainActivityTask() {
        applicationContext.startActivity(
            Intent(applicationContext, MainActivity::class.java).apply {
                action = MainActivity.ACTION_CLOSE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }

    private fun snapshotState(): FloatingOverlayState {
        val permission = Settings.canDrawOverlays(applicationContext)
        return FloatingOverlayState(
            permissionGranted = permission,
            enabled = permission,
            visible = railView != null,
        )
    }

    private fun actionButton(symbol: String, label: String, onClick: () -> Unit): TextView =
        textView("$symbol   $label", 14f, Color.WHITE, bold = true).apply {
            gravity = Gravity.CENTER_VERTICAL
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
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val RAIL_WIDTH_DP = 42
        private const val RAIL_HEIGHT_DP = 72
        private const val PANEL_WIDTH_DP = 224
        private const val DEFAULT_X_DP = 8
        private const val DEFAULT_Y_DP = 180

        private val RAIL_BACKGROUND = Color.rgb(38, 42, 54)
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

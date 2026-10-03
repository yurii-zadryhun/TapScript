package dev.tapscript.app.ui

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties

/**
 * Overlay workspace content is hosted directly by WindowManager rather than an Activity.
 * Compose dialogs/popups therefore need to use the same application-overlay window type.
 * Normal activity UI keeps the platform defaults.
 */
internal val LocalOverlayWindow = staticCompositionLocalOf { false }

@Composable
internal fun editorDialogProperties(): DialogProperties = DialogProperties(
    windowType = windowType(),
)

@Composable
internal fun editorPopupProperties(): PopupProperties = PopupProperties(
    windowType = windowType(),
)

@Composable
private fun windowType(): Int = if (LocalOverlayWindow.current) {
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
} else {
    WindowManager.LayoutParams.TYPE_APPLICATION
}

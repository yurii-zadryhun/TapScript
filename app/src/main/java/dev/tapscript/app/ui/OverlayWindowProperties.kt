package dev.tapscript.app.ui

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties

/**
 * The same editor composables are reused in the Activity and in the WindowManager overlay.
 * Dialogs/popups inspect their Compose host so callers do not need an overlay-specific API.
 */
@Composable
internal fun editorDialogProperties(): DialogProperties = DialogProperties(
    windowType = hostWindowType(),
)

@Composable
internal fun editorPopupProperties(): PopupProperties = PopupProperties(
    windowType = hostWindowType(),
)

@Composable
private fun hostWindowType(): Int {
    val hostType = (LocalView.current.rootView.layoutParams as? WindowManager.LayoutParams)?.type
    return if (hostType == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_APPLICATION
    }
}

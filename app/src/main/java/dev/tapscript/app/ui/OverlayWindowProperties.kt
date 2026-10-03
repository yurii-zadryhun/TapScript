package dev.tapscript.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties

/**
 * Centralized editor window properties.
 *
 * The current Compose artifacts don't expose a configurable WindowManager type
 * on DialogProperties/PopupProperties. Overlay authoring therefore keeps
 * platform dialogs out of its primary flow and uses inline editor surfaces.
 */
@Composable
internal fun editorDialogProperties(): DialogProperties = DialogProperties()

@Composable
internal fun editorPopupProperties(): PopupProperties = PopupProperties()

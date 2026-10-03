package dev.tapscript.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tapscript.engine.api.model.AutomationProfile

@Composable
fun TapScriptApp(
    viewModel: DashboardViewModel,
    onRequestCapture: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editorProfile by remember { mutableStateOf<AutomationProfile?>(null) }
    var creatingProfile by remember { mutableStateOf(false) }

    if (editorProfile != null || creatingProfile) {
        ProfileEditorScreen(
            initialProfile = editorProfile ?: AutomationProfile(name = "New profile"),
            installedApps = state.installedApps,
            onCancel = {
                editorProfile = null
                creatingProfile = false
            },
            onSave = { profile ->
                viewModel.saveProfile(profile)
                editorProfile = null
                creatingProfile = false
            },
        )
    } else {
        DashboardScreen(
            state = state,
            onRequestCapture = onRequestCapture,
            onStopCapture = viewModel::stopCapture,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onSetOverlayEnabled = viewModel::setOverlayEnabled,
            onRequestOverlayPermission = onRequestOverlayPermission,
            onCreateProfile = { creatingProfile = true },
            onEditProfile = { editorProfile = it },
            onDeleteProfile = viewModel::deleteProfile,
            onStartProfile = viewModel::start,
            onStopSession = viewModel::stop,
            onLaunchTarget = viewModel::launchTarget,
            onDeleteRun = viewModel::deleteRun,
            onClearHistory = viewModel::clearHistory,
            onDismissError = viewModel::clearError,
        )
    }
}

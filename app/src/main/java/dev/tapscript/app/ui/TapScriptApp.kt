package dev.tapscript.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onImportProfiles: () -> Unit,
    onExportProfiles: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val screenPickerState by viewModel.screenPickerState.collectAsStateWithLifecycle()
    var editorProfile by remember { mutableStateOf<AutomationProfile?>(null) }
    var creatingProfile by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DashboardEffect.RequestCapturePermission -> onRequestCapture()
            }
        }
    }

    if (editorProfile != null || creatingProfile) {
        ProfileEditorScreen(
            initialProfile = editorProfile ?: AutomationProfile(name = "New profile"),
            installedApps = state.installedApps,
            overlayPermissionGranted = state.overlayState.permissionGranted,
            screenPickerState = screenPickerState,
            onBeginLivePick = viewModel::beginScreenPick,
            onConsumeLivePick = viewModel::consumeScreenPick,
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
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onRequestOverlayPermission = onRequestOverlayPermission,
            onCreateProfile = { creatingProfile = true },
            onImportProfiles = onImportProfiles,
            onExportProfiles = onExportProfiles,
            onEditProfile = { editorProfile = it },
            onDeleteProfile = viewModel::deleteProfile,
            onStartProfile = viewModel::start,
            onStopSession = viewModel::stop,
            onLaunchTarget = viewModel::launchTarget,
            onDeleteRun = viewModel::deleteRun,
            onClearHistory = viewModel::clearHistory,
            onDismissError = viewModel::clearError,
            onDismissInfo = viewModel::clearInfo,
        )
    }
}

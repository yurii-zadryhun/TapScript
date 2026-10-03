package dev.tapscript.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.tapscript.app.AppGraph
import dev.tapscript.app.overlay.FloatingOverlayState
import dev.tapscript.app.overlay.ScreenPickMode
import dev.tapscript.app.overlay.ScreenPickerState
import dev.tapscript.engine.api.model.AutomationLogEntry
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationRunRecord
import dev.tapscript.engine.api.model.AutomationSessionStatus
import dev.tapscript.engine.api.model.SessionPhase
import dev.tapscript.platform.android.app.LaunchableAppInfo
import dev.tapscript.platform.android.capture.CaptureState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class DashboardViewModel(
    private val graph: AppGraph,
) : ViewModel() {
    private val profiles = MutableStateFlow<List<AutomationProfile>>(emptyList())
    private val runHistory = MutableStateFlow<List<AutomationRunRecord>>(emptyList())
    private val installedApps = MutableStateFlow<List<LaunchableAppInfo>>(emptyList())
    private val accessibilityEnabled = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val infoMessage = MutableStateFlow<String?>(null)
    private val mutableEffects = MutableSharedFlow<DashboardEffect>(extraBufferCapacity = 1)
    private var pendingStartProfileId: String? = null

    val effects: SharedFlow<DashboardEffect> = mutableEffects.asSharedFlow()
    val screenPickerState: StateFlow<ScreenPickerState> = graph.screenPickerController.state

    private val contentState = combine(profiles, runHistory, installedApps) { profiles, history, apps ->
        DashboardContentState(profiles, history, apps)
    }

    private val runtimeState = combine(
        accessibilityEnabled,
        graph.captureStatusStore.state,
        graph.sessionManager.status,
        graph.historyRecorder.liveLogs,
        graph.overlayController.state,
    ) { accessibility, capture, session, logs, overlay ->
        DashboardRuntimeState(accessibility, capture, session, logs, overlay)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        contentState,
        runtimeState,
        errorMessage,
        infoMessage,
    ) { content, runtime, error, info ->
        DashboardUiState(
            profiles = content.profiles,
            runHistory = content.runHistory,
            installedApps = content.installedApps,
            accessibilityEnabled = runtime.accessibilityEnabled,
            captureState = runtime.captureState,
            sessionStatus = runtime.sessionStatus,
            liveLogs = runtime.liveLogs,
            overlayState = runtime.overlayState,
            errorMessage = error,
            infoMessage = info,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        accessibilityEnabled.value = graph.accessibilityStatusReader.isEnabled()
        graph.overlayController.refresh()
        viewModelScope.launch {
            profiles.value = graph.profileRepository.list()
            runHistory.value = graph.sessionHistoryRepository.list()
            installedApps.value = withContext(Dispatchers.Default) {
                graph.installedAppProvider.listLaunchableApps()
            }
        }
    }

    fun saveProfile(profile: AutomationProfile) {
        viewModelScope.launch {
            runCatching { graph.profileRepository.save(profile) }
                .onSuccess { refresh() }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun deleteProfile(profile: AutomationProfile) {
        viewModelScope.launch {
            runCatching { graph.profileRepository.delete(profile.id) }
                .onSuccess { refresh() }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun importProfiles(uri: Uri) {
        viewModelScope.launch {
            runCatching { graph.profileTransferService.importFrom(uri) }
                .onSuccess { imported ->
                    profiles.value = graph.profileRepository.list()
                    infoMessage.value = "Imported $imported profile${if (imported == 1) "" else "s"}."
                }
                .onFailure { errorMessage.value = it.message ?: "Could not import profiles" }
        }
    }

    fun exportProfiles(uri: Uri) {
        viewModelScope.launch {
            runCatching { graph.profileTransferService.exportTo(uri) }
                .onSuccess { exported ->
                    infoMessage.value = "Exported $exported profile${if (exported == 1) "" else "s"}."
                }
                .onFailure { errorMessage.value = it.message ?: "Could not export profiles" }
        }
    }

    fun deleteRun(record: AutomationRunRecord) {
        viewModelScope.launch {
            runCatching { graph.sessionHistoryRepository.delete(record.id) }
                .onSuccess { runHistory.value = graph.sessionHistoryRepository.list() }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            runCatching { graph.sessionHistoryRepository.clear() }
                .onSuccess { runHistory.value = emptyList() }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun beginScreenPick(profile: AutomationProfile, mode: ScreenPickMode) {
        errorMessage.value = null
        if (!graph.overlayController.state.value.permissionGranted) {
            errorMessage.value = "Allow display over other apps before using the live picker."
            return
        }
        if (profile.targetPackage.isBlank()) {
            errorMessage.value = "Choose a target app to use live picking. Whole-screen profiles can still use the screenshot picker."
            return
        }
        graph.packageLauncher.launch(profile.targetPackage)
            .onFailure {
                errorMessage.value = it.message
                return
            }
        viewModelScope.launch {
            delay(450)
            graph.screenPickerController.start(mode = mode, profile = profile)
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun consumeScreenPick(sequence: Long) {
        graph.screenPickerController.consumeResult(sequence)
    }

    fun start(profile: AutomationProfile) {
        errorMessage.value = null
        if (!accessibilityEnabled.value) {
            errorMessage.value = "Enable TapScript Accessibility before starting a profile."
            return
        }
        if (graph.sessionManager.status.value.phase !in setOf(
                SessionPhase.IDLE,
                SessionPhase.STOPPED,
                SessionPhase.ERROR,
            )
        ) {
            errorMessage.value = "Another profile is already running."
            return
        }

        pendingStartProfileId = profile.id
        if (graph.captureStatusStore.state.value is CaptureState.Active) {
            startPendingProfile()
        } else {
            mutableEffects.tryEmit(DashboardEffect.RequestCapturePermission)
        }
    }

    fun onCapturePermissionResult(granted: Boolean) {
        if (!granted) {
            pendingStartProfileId = null
            errorMessage.value = "Screen capture permission is required to run the profile."
            return
        }

        viewModelScope.launch {
            val captureState = withTimeoutOrNull(CAPTURE_START_TIMEOUT_MS) {
                graph.captureStatusStore.state.first {
                    it is CaptureState.Active || it is CaptureState.Error
                }
            }
            when (captureState) {
                is CaptureState.Active -> startPendingProfile()
                is CaptureState.Error -> {
                    pendingStartProfileId = null
                    errorMessage.value = "Screen capture failed: ${captureState.message}"
                }
                else -> {
                    pendingStartProfileId = null
                    errorMessage.value = "Screen capture did not start in time."
                }
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            graph.sessionManager.stopAndJoin()
            runHistory.value = graph.sessionHistoryRepository.list()
        }
    }

    fun launchTarget(profile: AutomationProfile) {
        if (profile.targetPackage.isBlank()) return
        graph.packageLauncher.launch(profile.targetPackage)
            .onFailure { errorMessage.value = it.message }
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun clearInfo() {
        infoMessage.value = null
    }

    private fun startPendingProfile() {
        val profileId = pendingStartProfileId ?: return
        pendingStartProfileId = null
        viewModelScope.launch {
            val profile = graph.profileRepository.get(profileId)
            if (profile == null) {
                errorMessage.value = "Profile was deleted before it could start."
                graph.captureController.stop()
                return@launch
            }
            if (profile.targetPackage.isNotBlank()) {
                graph.packageLauncher.launch(profile.targetPackage)
                    .onFailure { throwable ->
                        errorMessage.value = throwable.message ?: "Could not launch target app"
                        graph.captureController.stop()
                        return@launch
                    }
                delay(TARGET_LAUNCH_SETTLE_MS)
            }
            graph.sessionManager.start(profile.id)
        }
    }

    class Factory(
        private val graph: AppGraph,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(graph) as T
    }

    private companion object {
        const val CAPTURE_START_TIMEOUT_MS = 5_000L
        const val TARGET_LAUNCH_SETTLE_MS = 350L
    }
}

sealed interface DashboardEffect {
    data object RequestCapturePermission : DashboardEffect
}

private data class DashboardContentState(
    val profiles: List<AutomationProfile>,
    val runHistory: List<AutomationRunRecord>,
    val installedApps: List<LaunchableAppInfo>,
)

private data class DashboardRuntimeState(
    val accessibilityEnabled: Boolean,
    val captureState: CaptureState,
    val sessionStatus: AutomationSessionStatus,
    val liveLogs: List<AutomationLogEntry>,
    val overlayState: FloatingOverlayState,
)

data class DashboardUiState(
    val profiles: List<AutomationProfile> = emptyList(),
    val runHistory: List<AutomationRunRecord> = emptyList(),
    val installedApps: List<LaunchableAppInfo> = emptyList(),
    val accessibilityEnabled: Boolean = false,
    val captureState: CaptureState = CaptureState.Idle,
    val sessionStatus: AutomationSessionStatus = AutomationSessionStatus(),
    val liveLogs: List<AutomationLogEntry> = emptyList(),
    val overlayState: FloatingOverlayState = FloatingOverlayState(
        permissionGranted = false,
        enabled = false,
        visible = false,
    ),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

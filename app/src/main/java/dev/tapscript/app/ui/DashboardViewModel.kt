package dev.tapscript.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.tapscript.app.AppGraph
import dev.tapscript.engine.api.model.AutomationLogEntry
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationRunRecord
import dev.tapscript.engine.api.model.AutomationSessionStatus
import dev.tapscript.platform.android.app.LaunchableAppInfo
import dev.tapscript.platform.android.capture.CaptureState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(
    private val graph: AppGraph,
) : ViewModel() {
    private val profiles = MutableStateFlow<List<AutomationProfile>>(emptyList())
    private val runHistory = MutableStateFlow<List<AutomationRunRecord>>(emptyList())
    private val installedApps = MutableStateFlow<List<LaunchableAppInfo>>(emptyList())
    private val accessibilityEnabled = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val contentState = combine(profiles, runHistory, installedApps) { profiles, history, apps ->
        DashboardContentState(profiles, history, apps)
    }

    private val runtimeState = combine(
        accessibilityEnabled,
        graph.captureStatusStore.state,
        graph.sessionManager.status,
        graph.historyRecorder.liveLogs,
    ) { accessibility, capture, session, logs ->
        DashboardRuntimeState(accessibility, capture, session, logs)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        contentState,
        runtimeState,
        errorMessage,
    ) { content, runtime, error ->
        DashboardUiState(
            profiles = content.profiles,
            runHistory = content.runHistory,
            installedApps = content.installedApps,
            accessibilityEnabled = runtime.accessibilityEnabled,
            captureState = runtime.captureState,
            sessionStatus = runtime.sessionStatus,
            liveLogs = runtime.liveLogs,
            errorMessage = error,
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

    fun start(profile: AutomationProfile) {
        errorMessage.value = null
        graph.sessionManager.start(profile.id)
    }

    fun stop() {
        graph.sessionManager.stop()
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            runHistory.value = graph.sessionHistoryRepository.list()
        }
    }

    fun stopCapture() {
        graph.captureController.stop()
    }

    fun launchTarget(profile: AutomationProfile) {
        if (profile.targetPackage.isBlank()) return
        graph.packageLauncher.launch(profile.targetPackage)
            .onFailure { errorMessage.value = it.message }
    }

    fun clearError() {
        errorMessage.value = null
    }

    class Factory(
        private val graph: AppGraph,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(graph) as T
    }
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
)

data class DashboardUiState(
    val profiles: List<AutomationProfile> = emptyList(),
    val runHistory: List<AutomationRunRecord> = emptyList(),
    val installedApps: List<LaunchableAppInfo> = emptyList(),
    val accessibilityEnabled: Boolean = false,
    val captureState: CaptureState = CaptureState.Idle,
    val sessionStatus: AutomationSessionStatus = AutomationSessionStatus(),
    val liveLogs: List<AutomationLogEntry> = emptyList(),
    val errorMessage: String? = null,
)

package dev.tapscript.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.tapscript.app.AppGraph
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationSessionStatus
import dev.tapscript.platform.android.capture.CaptureState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val graph: AppGraph,
) : ViewModel() {
    private val profiles = MutableStateFlow<List<AutomationProfile>>(emptyList())
    private val accessibilityEnabled = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        profiles,
        accessibilityEnabled,
        graph.captureStatusStore.state,
        graph.sessionManager.status,
        errorMessage,
    ) { profiles, accessibility, capture, session, error ->
        DashboardUiState(
            profiles = profiles,
            accessibilityEnabled = accessibility,
            captureState = capture,
            sessionStatus = session,
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

    fun start(profile: AutomationProfile) {
        errorMessage.value = null
        graph.sessionManager.start(profile.id)
    }

    fun stop() {
        graph.sessionManager.stop()
    }

    fun stopCapture() {
        graph.captureController.stop()
    }

    fun launchTarget(profile: AutomationProfile) {
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

data class DashboardUiState(
    val profiles: List<AutomationProfile> = emptyList(),
    val accessibilityEnabled: Boolean = false,
    val captureState: CaptureState = CaptureState.Idle,
    val sessionStatus: AutomationSessionStatus = AutomationSessionStatus(),
    val errorMessage: String? = null,
)

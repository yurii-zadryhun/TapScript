package dev.tapscript.app.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import dev.tapscript.app.ui.OverlayWorkspaceScreen
import dev.tapscript.app.ui.theme.TapScriptTheme
import dev.tapscript.engine.api.model.AutomationLogEntry
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationSessionStatus
import dev.tapscript.engine.api.ports.ProfileRepository
import dev.tapscript.engine.core.frame.ConflatedFrameBus
import dev.tapscript.engine.core.runtime.AutomationPauseController
import dev.tapscript.platform.android.app.AndroidInstalledAppProvider
import dev.tapscript.platform.android.app.LaunchableAppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayWorkspaceController(
    context: Context,
    private val profileRepository: ProfileRepository,
    private val installedAppProvider: AndroidInstalledAppProvider,
    private val frameBus: ConflatedFrameBus,
    private val screenPickerController: ScreenPickerController,
    private val pauseController: AutomationPauseController,
    private val sessionStatus: StateFlow<AutomationSessionStatus>,
    private val liveLogs: StateFlow<List<AutomationLogEntry>>,
    private val onStopSession: () -> Unit,
) {
    private val applicationContext = context.applicationContext
    private val windowManager = applicationContext.getSystemService(WindowManager::class.java)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(OverlayWorkspaceState())

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var pickerWasActive = false

    val state: StateFlow<OverlayWorkspaceState> = mutableState

    init {
        mainScope.launch {
            screenPickerController.state.collect { pickerState ->
                if (pickerState.activeMode != null) {
                    pickerWasActive = true
                } else if (pickerWasActive) {
                    pickerWasActive = false
                    composeView?.visibility = View.VISIBLE
                    mutableState.value = mutableState.value.copy(visible = composeView != null)
                }
            }
        }
    }

    fun open(preferredProfileId: String? = sessionStatus.value.profileId.takeIf { it.isNotBlank() }) {
        if (!Settings.canDrawOverlays(applicationContext)) {
            mutableState.value = mutableState.value.copy(
                errorMessage = "Allow display over other apps before opening the workspace.",
            )
            return
        }

        pauseController.pause(
            AutomationPauseController.OVERLAY_TOKEN,
            "Paused while TapScript workspace is open",
        )
        ensureView()
        mainScope.launch { loadData(preferredProfileId) }
    }

    fun close() {
        screenPickerController.cancel(returnToTapScript = false)
        val view = composeView
        composeView = null
        view?.let { runCatching { windowManager.removeView(it) } }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        pickerWasActive = false
        recycleFrozenFrame()
        mutableState.value = OverlayWorkspaceState()
        pauseController.resume(AutomationPauseController.OVERLAY_TOKEN)
    }

    fun selectProfile(profileId: String) {
        val profile = mutableState.value.profiles.firstOrNull { it.id == profileId } ?: return
        mutableState.value = mutableState.value.copy(selectedProfile = profile, errorMessage = null)
    }

    fun saveProfile(profile: AutomationProfile) {
        mainScope.launch {
            runCatching { profileRepository.save(profile) }
                .onSuccess {
                    val profiles = profileRepository.list()
                    mutableState.value = mutableState.value.copy(
                        profiles = profiles,
                        selectedProfile = profiles.firstOrNull { it.id == profile.id } ?: profile,
                        errorMessage = null,
                    )
                }
                .onFailure { throwable ->
                    mutableState.value = mutableState.value.copy(
                        errorMessage = throwable.message ?: "Could not save profile",
                    )
                }
        }
    }

    fun beginLivePick(mode: ScreenPickMode) {
        val view = composeView ?: return
        mutableState.value = mutableState.value.copy(errorMessage = null)
        view.visibility = View.INVISIBLE
        mutableState.value = mutableState.value.copy(visible = false)
        screenPickerController.start(mode, returnToTapScript = false)
            .onFailure { throwable ->
                view.visibility = View.VISIBLE
                mutableState.value = mutableState.value.copy(
                    visible = true,
                    errorMessage = throwable.message ?: "Could not start visual picker",
                )
            }
    }

    fun consumeScreenPick(sequence: Long) {
        screenPickerController.consumeResult(sequence)
    }

    fun freezeCurrentFrame() {
        val view = composeView ?: return
        mainScope.launch {
            mutableState.value = mutableState.value.copy(isFreezingFrame = true, errorMessage = null)
            view.visibility = View.INVISIBLE
            delay(FRAME_HIDE_SETTLE_MS)
            runCatching { frameBus.snapshotNextFrame(FRAME_SNAPSHOT_TIMEOUT_MS) }
                .onSuccess { frame ->
                    val old = mutableState.value.frozenFrame
                    mutableState.value = mutableState.value.copy(
                        frozenFrame = frame.bitmap,
                        isFreezingFrame = false,
                    )
                    old?.takeUnless { it === frame.bitmap || it.isRecycled }?.recycle()
                }
                .onFailure { throwable ->
                    mutableState.value = mutableState.value.copy(
                        isFreezingFrame = false,
                        errorMessage = throwable.message ?: "No captured frame arrived",
                    )
                }
            view.visibility = View.VISIBLE
            mutableState.value = mutableState.value.copy(visible = true)
        }
    }

    fun clearFrozenFrame() {
        recycleFrozenFrame()
        mutableState.value = mutableState.value.copy(frozenFrame = null)
    }

    fun clearError() {
        mutableState.value = mutableState.value.copy(errorMessage = null)
    }

    private fun ensureView() {
        composeView?.let {
            it.visibility = View.VISIBLE
            mutableState.value = mutableState.value.copy(visible = true)
            return
        }

        val owner = OverlayLifecycleOwner().also { it.start() }
        val view = ComposeView(applicationContext)
        ViewTreeLifecycleOwner.set(view, owner)
        ViewTreeSavedStateRegistryOwner.set(view, owner)
        view.setContent {
            TapScriptTheme {
                val workspaceState by state.collectAsState()
                val runtimeStatus by sessionStatus.collectAsState()
                val logs by liveLogs.collectAsState()
                val pickerState by screenPickerController.state.collectAsState()
                OverlayWorkspaceScreen(
                    state = workspaceState,
                    runtimeStatus = runtimeStatus,
                    liveLogs = logs,
                    screenPickerState = pickerState,
                    onClose = ::close,
                    onSelectProfile = ::selectProfile,
                    onSaveProfile = ::saveProfile,
                    onBeginLivePick = ::beginLivePick,
                    onConsumeLivePick = ::consumeScreenPick,
                    onFreezeFrame = ::freezeCurrentFrame,
                    onClearFrozenFrame = ::clearFrozenFrame,
                    onTogglePause = {},
                    onStopSession = onStopSession,
                    onDismissError = ::clearError,
                )
            }
        }

        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        lifecycleOwner = owner
        composeView = view
        runCatching { windowManager.addView(view, layout) }
            .onSuccess {
                mutableState.value = mutableState.value.copy(visible = true, errorMessage = null)
            }
            .onFailure { throwable ->
                composeView = null
                lifecycleOwner = null
                owner.destroy()
                pauseController.resume(AutomationPauseController.OVERLAY_TOKEN)
                mutableState.value = mutableState.value.copy(
                    visible = false,
                    errorMessage = throwable.message ?: "Could not open overlay workspace",
                )
            }
    }

    private suspend fun loadData(preferredProfileId: String?) {
        runCatching {
            val profiles = profileRepository.list()
            val apps = withContext(Dispatchers.Default) { installedAppProvider.listLaunchableApps() }
            val selected = profiles.firstOrNull { it.id == preferredProfileId }
                ?: mutableState.value.selectedProfile?.let { current -> profiles.firstOrNull { it.id == current.id } }
                ?: profiles.firstOrNull()
            Triple(profiles, apps, selected)
        }.onSuccess { (profiles, apps, selected) ->
            mutableState.value = mutableState.value.copy(
                profiles = profiles,
                installedApps = apps,
                selectedProfile = selected,
                isLoading = false,
                errorMessage = null,
            )
        }.onFailure { throwable ->
            mutableState.value = mutableState.value.copy(
                isLoading = false,
                errorMessage = throwable.message ?: "Could not load workspace data",
            )
        }
    }

    private fun recycleFrozenFrame() {
        mutableState.value.frozenFrame?.takeUnless { it.isRecycled }?.recycle()
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        init {
            savedStateController.performAttach()
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        fun start() {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }

    private companion object {
        const val FRAME_HIDE_SETTLE_MS = 80L
        const val FRAME_SNAPSHOT_TIMEOUT_MS = 1_500L
    }
}

data class OverlayWorkspaceState(
    val visible: Boolean = false,
    val isLoading: Boolean = true,
    val profiles: List<AutomationProfile> = emptyList(),
    val installedApps: List<LaunchableAppInfo> = emptyList(),
    val selectedProfile: AutomationProfile? = null,
    val frozenFrame: Bitmap? = null,
    val isFreezingFrame: Boolean = false,
    val errorMessage: String? = null,
)

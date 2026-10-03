package dev.tapscript.platform.android.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CaptureStatusStore {
    private val mutableState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val state: StateFlow<CaptureState> = mutableState

    fun update(state: CaptureState) {
        mutableState.value = state
    }
}

sealed interface CaptureState {
    data object Idle : CaptureState
    data object Starting : CaptureState
    data class Active(val width: Int, val height: Int) : CaptureState
    data class Error(val message: String) : CaptureState
}

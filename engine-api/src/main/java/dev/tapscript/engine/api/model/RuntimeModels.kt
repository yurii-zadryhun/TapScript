package dev.tapscript.engine.api.model

import android.graphics.Bitmap

data class ScreenFrame(
    val bitmap: Bitmap,
    val capturedAtNanos: Long,
    val interactionSize: PixelSize = PixelSize(bitmap.width, bitmap.height),
) {
    val captureSize: PixelSize
        get() = PixelSize(bitmap.width, bitmap.height)
}

data class RecognizedText(
    val text: String,
    val lines: List<RecognizedLine> = emptyList(),
)

data class RecognizedLine(
    val text: String,
    val confidence: Float? = null,
)

data class RegionObservation(
    val regionId: String,
    val rawText: String,
    val variables: Map<String, Any?>,
    val recognitionMs: Long,
    val errorMessage: String? = null,
)

data class AutomationSnapshot(
    val values: Map<String, Any?>,
    val frameCapturedAtNanos: Long,
    val observations: List<RegionObservation> = emptyList(),
)

data class AutomationMetrics(
    val frameAgeMs: Long = 0,
    val recognitionMs: Long = 0,
    val decisionMs: Long = 0,
    val lastCommand: String = "",
)

data class AutomationSessionStatus(
    val phase: SessionPhase = SessionPhase.IDLE,
    val profileId: String = "",
    val profileName: String = "",
    val message: String = "",
    val snapshot: AutomationSnapshot? = null,
    val metrics: AutomationMetrics = AutomationMetrics(),
)

enum class SessionPhase {
    IDLE,
    WAITING_FOR_FRAME,
    PAUSED,
    RUNNING,
    ERROR,
    STOPPED,
}

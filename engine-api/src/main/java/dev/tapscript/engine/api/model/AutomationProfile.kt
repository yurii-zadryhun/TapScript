package dev.tapscript.engine.api.model

import java.util.UUID

data class AutomationProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetPackage: String = "",
    val regions: List<RecognitionRegion> = emptyList(),
    val actions: List<ActionTarget> = emptyList(),
    val logic: AutomationLogic = AutomationLogic(),
    val settings: RuntimeSettings = RuntimeSettings(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class RuntimeSettings(
    val minFrameIntervalMs: Long = 120,
    val changeThreshold: Double = 0.02,
    val postActionCooldownMs: Long = 0,
)

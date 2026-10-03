package dev.tapscript.engine.api.model

import java.util.UUID

data class AutomationRunRecord(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val profileName: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val outcome: RunOutcome = RunOutcome.RUNNING,
    val logs: List<AutomationLogEntry> = emptyList(),
) {
    val durationMs: Long
        get() = ((endedAtEpochMs ?: System.currentTimeMillis()) - startedAtEpochMs).coerceAtLeast(0)
}

data class AutomationLogEntry(
    val timestampEpochMs: Long,
    val level: AutomationLogLevel,
    val message: String,
)

enum class AutomationLogLevel {
    DEBUG,
    INFO,
    ERROR,
}

enum class RunOutcome {
    RUNNING,
    STOPPED,
    ERROR,
}

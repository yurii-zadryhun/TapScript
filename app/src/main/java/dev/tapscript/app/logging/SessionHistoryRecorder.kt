package dev.tapscript.app.logging

import dev.tapscript.engine.api.model.AutomationLogEntry
import dev.tapscript.engine.api.model.AutomationLogLevel
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationRunRecord
import dev.tapscript.engine.api.model.RunOutcome
import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.SessionHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionHistoryRecorder(
    private val repository: SessionHistoryRepository,
    private val maxEntriesPerRun: Int = 5_000,
) : AutomationLogger {
    private val lock = Any()
    private var activeRun: MutableRun? = null
    private val mutableLiveLogs = MutableStateFlow<List<AutomationLogEntry>>(emptyList())

    val liveLogs: StateFlow<List<AutomationLogEntry>> = mutableLiveLogs

    fun begin(profile: AutomationProfile) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            activeRun = MutableRun(
                record = AutomationRunRecord(
                    profileId = profile.id,
                    profileName = profile.name,
                    startedAtEpochMs = now,
                ),
                logs = mutableListOf(),
            )
            appendLocked(
                AutomationLogEntry(
                    timestampEpochMs = now,
                    level = AutomationLogLevel.INFO,
                    message = "Session started${profile.targetPackage.takeIf { it.isNotBlank() }?.let { " for $it" } ?: " for whole screen"}",
                ),
            )
        }
    }

    suspend fun finish(outcome: RunOutcome) {
        val completed = synchronized(lock) {
            val active = activeRun ?: return
            val now = System.currentTimeMillis()
            appendLocked(
                AutomationLogEntry(
                    timestampEpochMs = now,
                    level = AutomationLogLevel.INFO,
                    message = "Session finished: ${outcome.name.lowercase()}",
                ),
            )
            activeRun = null
            mutableLiveLogs.value = emptyList()
            active.record.copy(
                endedAtEpochMs = now,
                outcome = outcome,
                logs = active.logs.toList(),
            )
        }
        repository.save(completed)
    }

    override fun debug(message: String) = append(AutomationLogLevel.DEBUG, message)

    override fun info(message: String) = append(AutomationLogLevel.INFO, message)

    override fun error(message: String, throwable: Throwable?) {
        val details = throwable?.stackTraceToString()?.take(MAX_THROWABLE_CHARS)
        append(
            AutomationLogLevel.ERROR,
            if (details.isNullOrBlank()) message else "$message\n$details",
        )
    }

    private fun append(level: AutomationLogLevel, message: String) {
        synchronized(lock) {
            appendLocked(
                AutomationLogEntry(
                    timestampEpochMs = System.currentTimeMillis(),
                    level = level,
                    message = message,
                ),
            )
        }
    }

    private fun appendLocked(entry: AutomationLogEntry) {
        val active = activeRun ?: return
        if (active.logs.size >= maxEntriesPerRun) {
            active.logs.removeAt(0)
        }
        active.logs += entry
        mutableLiveLogs.value = active.logs.toList()
    }

    private data class MutableRun(
        val record: AutomationRunRecord,
        val logs: MutableList<AutomationLogEntry>,
    )

    private companion object {
        const val MAX_THROWABLE_CHARS = 20_000
    }
}

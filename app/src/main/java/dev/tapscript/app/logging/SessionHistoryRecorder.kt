package dev.tapscript.app.logging

import dev.tapscript.engine.api.model.AutomationLogEntry
import dev.tapscript.engine.api.model.AutomationLogLevel
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationRunRecord
import dev.tapscript.engine.api.model.RunOutcome
import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.SessionHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionHistoryRecorder(
    private val repository: SessionHistoryRepository,
    private val maxEntriesPerRun: Int = 5_000,
) : AutomationLogger {
    private val lock = Any()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val persistenceMutex = Mutex()
    private var activeRun: MutableRun? = null
    private var checkpointJob: Job? = null
    private val mutableLiveLogs = MutableStateFlow<List<AutomationLogEntry>>(emptyList())

    val liveLogs: StateFlow<List<AutomationLogEntry>> = mutableLiveLogs

    fun begin(profile: AutomationProfile) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            checkpointJob?.cancel()
            checkpointJob = null
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
        val pendingCheckpoint: Job?
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
            pendingCheckpoint = checkpointJob
            checkpointJob = null
            activeRun = null
            active.record.copy(
                endedAtEpochMs = now,
                outcome = outcome,
                logs = active.logs.toList(),
            )
        }

        pendingCheckpoint?.cancelAndJoin()
        persistenceMutex.withLock { repository.save(completed) }
        mutableLiveLogs.value = emptyList()
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
        scheduleCheckpointLocked()
    }

    private fun scheduleCheckpointLocked() {
        if (checkpointJob?.isActive == true) return
        checkpointJob = ioScope.launch {
            delay(CHECKPOINT_DELAY_MS)
            val snapshot = synchronized(lock) {
                val active = activeRun ?: return@launch
                active.record.copy(
                    outcome = RunOutcome.RUNNING,
                    logs = active.logs.toList(),
                )
            }
            persistenceMutex.withLock { repository.save(snapshot) }
            synchronized(lock) {
                if (checkpointJob === coroutineContext[Job]) checkpointJob = null
            }
        }
    }

    private data class MutableRun(
        val record: AutomationRunRecord,
        val logs: MutableList<AutomationLogEntry>,
    )

    private companion object {
        const val MAX_THROWABLE_CHARS = 20_000
        const val CHECKPOINT_DELAY_MS = 1_000L
    }
}

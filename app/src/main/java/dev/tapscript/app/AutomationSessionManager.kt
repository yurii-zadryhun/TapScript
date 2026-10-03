package dev.tapscript.app

import dev.tapscript.app.logging.SessionHistoryRecorder
import dev.tapscript.engine.api.model.RunOutcome
import dev.tapscript.engine.api.model.SessionPhase
import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.ProfileRepository
import dev.tapscript.engine.core.runtime.AutomationRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutomationSessionManager(
    private val scope: CoroutineScope,
    private val profileRepository: ProfileRepository,
    private val runner: AutomationRunner,
    private val logger: AutomationLogger,
    private val historyRecorder: SessionHistoryRecorder,
) {
    private var sessionJob: Job? = null

    val status = runner.status

    fun start(profileId: String) {
        val previous = sessionJob
        sessionJob = scope.launch {
            previous?.cancelAndJoin()
            val profile = profileRepository.get(profileId)
            if (profile == null) {
                logger.error("Profile '$profileId' was not found")
                return@launch
            }

            historyRecorder.begin(profile)
            logger.info("Starting profile '${profile.name}'")
            try {
                runner.run(profile)
            } finally {
                val outcome = if (runner.status.value.phase == SessionPhase.ERROR) {
                    RunOutcome.ERROR
                } else {
                    RunOutcome.STOPPED
                }
                withContext(NonCancellable) {
                    runCatching { historyRecorder.finish(outcome) }
                        .onFailure { logger.error("Could not persist session history", it) }
                }
            }
        }
    }

    fun stop() {
        if (sessionJob?.isActive == true) logger.info("Stop requested")
        sessionJob?.cancel()
        sessionJob = null
    }
}

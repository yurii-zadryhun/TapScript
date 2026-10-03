package dev.tapscript.app

import dev.tapscript.app.logging.SessionHistoryRecorder
import dev.tapscript.engine.api.model.RunOutcome
import dev.tapscript.engine.api.model.SessionPhase
import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.ProfileRepository
import dev.tapscript.engine.core.runtime.AutomationPauseController
import dev.tapscript.engine.core.runtime.AutomationRunner
import dev.tapscript.platform.android.capture.CaptureServiceController
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
    private val pauseController: AutomationPauseController,
    private val logger: AutomationLogger,
    private val historyRecorder: SessionHistoryRecorder,
    private val captureController: CaptureServiceController,
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
                captureController.stop()
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
                    captureController.stop()
                }
            }
        }
    }

    fun pause() {
        if (sessionJob?.isActive != true) return
        pauseController.pause(AutomationPauseController.MANUAL_TOKEN, "Paused by user")
        runner.reflectPause("Paused by user")
    }

    fun resume() {
        pauseController.resume(AutomationPauseController.MANUAL_TOKEN)
        runner.reflectResumeRequest()
    }

    fun isManuallyPaused(): Boolean =
        pauseController.isPaused(AutomationPauseController.MANUAL_TOKEN)

    fun togglePause() {
        if (isManuallyPaused()) resume() else pause()
    }

    fun stop() {
        scope.launch { stopAndJoin() }
    }

    suspend fun stopAndJoin() {
        val job = sessionJob
        if (job?.isActive == true) logger.info("Stop requested")
        pauseController.clear()
        sessionJob = null
        job?.cancelAndJoin()
        captureController.stop()
    }
}

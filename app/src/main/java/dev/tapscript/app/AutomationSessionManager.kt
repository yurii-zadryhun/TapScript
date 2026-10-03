package dev.tapscript.app

import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.ProfileRepository
import dev.tapscript.engine.core.runtime.AutomationRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

class AutomationSessionManager(
    private val scope: CoroutineScope,
    private val profileRepository: ProfileRepository,
    private val runner: AutomationRunner,
    private val logger: AutomationLogger,
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
            runner.run(profile)
        }
    }

    fun stop() {
        sessionJob?.cancel()
        sessionJob = null
    }
}

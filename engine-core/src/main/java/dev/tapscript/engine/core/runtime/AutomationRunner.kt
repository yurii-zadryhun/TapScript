package dev.tapscript.engine.core.runtime

import dev.tapscript.engine.api.model.AutomationMetrics
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationSessionStatus
import dev.tapscript.engine.api.model.AutomationSnapshot
import dev.tapscript.engine.api.model.ScreenFrame
import dev.tapscript.engine.api.model.SessionPhase
import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.DecisionEngine
import dev.tapscript.engine.api.ports.ForegroundAppReader
import dev.tapscript.engine.api.ports.ScreenFrameSource
import dev.tapscript.engine.core.action.CommandExecutor
import dev.tapscript.engine.core.frame.SampledFrameChangeDetector
import dev.tapscript.engine.core.recognition.RecognitionPipeline
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive

class AutomationRunner(
    private val frameSource: ScreenFrameSource,
    private val changeDetector: SampledFrameChangeDetector,
    private val recognitionPipeline: RecognitionPipeline,
    private val decisionEngine: DecisionEngine,
    private val commandExecutor: CommandExecutor,
    private val foregroundAppReader: ForegroundAppReader,
    private val pauseController: AutomationPauseController,
    private val logger: AutomationLogger,
) {
    private val mutableStatus = MutableStateFlow(AutomationSessionStatus())
    val status: StateFlow<AutomationSessionStatus> = mutableStatus

    suspend fun run(profile: AutomationProfile) {
        pauseController.clear()
        changeDetector.reset()
        var afterTimestamp = 0L
        var lastProcessedAtMs = 0L
        var lastMetrics = AutomationMetrics()
        var pausedForTarget = false
        var runtimePauseReason: String? = null

        mutableStatus.value = AutomationSessionStatus(
            phase = SessionPhase.WAITING_FOR_FRAME,
            profileId = profile.id,
            profileName = profile.name,
            message = "Waiting for screen capture",
        )

        try {
            while (currentCoroutineContext().isActive) {
                val frame = frameSource.awaitFrame(afterTimestamp)
                afterTimestamp = frame.capturedAtNanos
                try {
                    val requestedPause = pauseController.currentReason()
                    if (requestedPause != null) {
                        if (runtimePauseReason != requestedPause) {
                            logger.info("Paused '${profile.name}': $requestedPause")
                            runtimePauseReason = requestedPause
                        }
                        mutableStatus.value = mutableStatus.value.copy(
                            phase = SessionPhase.PAUSED,
                            profileId = profile.id,
                            profileName = profile.name,
                            message = requestedPause,
                        )
                        continue
                    }
                    if (runtimePauseReason != null) {
                        logger.info("Resumed '${profile.name}'")
                        runtimePauseReason = null
                        changeDetector.reset()
                    }

                    val targetPackage = profile.targetPackage.trim()
                    if (targetPackage.isNotEmpty()) {
                        val activePackage = foregroundAppReader.currentPackage()
                        if (activePackage != targetPackage) {
                            if (!pausedForTarget) {
                                logger.info(
                                    "Paused '${profile.name}': target app '$targetPackage' is not active" +
                                        (activePackage?.let { " (active: '$it')" } ?: ""),
                                )
                            }
                            pausedForTarget = true
                            mutableStatus.value = mutableStatus.value.copy(
                                phase = SessionPhase.PAUSED,
                                profileId = profile.id,
                                profileName = profile.name,
                                message = "Paused until target app is active",
                            )
                            continue
                        }

                        if (pausedForTarget) {
                            logger.info("Resumed '${profile.name}': target app '$targetPackage' is active")
                            pausedForTarget = false
                            changeDetector.reset()
                        }
                    }

                    val nowMs = System.currentTimeMillis()
                    val elapsed = nowMs - lastProcessedAtMs
                    val requiredDelay = profile.settings.minFrameIntervalMs - elapsed
                    if (requiredDelay > 0) delay(requiredDelay)
                    lastProcessedAtMs = System.currentTimeMillis()

                    val watchedRegions = profile.regions
                        .asSequence()
                        .filter { it.enabled }
                        .map { it.bounds }
                        .toList()
                    if (!changeDetector.hasMeaningfulChange(frame, watchedRegions, profile.settings.changeThreshold)) {
                        continue
                    }

                    val recognitionStarted = System.nanoTime()
                    val observations = recognitionPipeline.observe(profile, frame)
                    val recognitionMs = (System.nanoTime() - recognitionStarted) / 1_000_000
                    observations
                        .filter { it.errorMessage != null }
                        .forEach { observation ->
                            logger.error(
                                "Recognition failed in region '${observation.regionId}': ${observation.errorMessage}",
                            )
                        }

                    val values = observations
                        .flatMap { it.variables.entries }
                        .associate { it.toPair() }
                    val snapshot = AutomationSnapshot(
                        values = values,
                        frameCapturedAtNanos = frame.capturedAtNanos,
                        observations = observations,
                    )

                    val decisionStarted = System.nanoTime()
                    val decision = decisionEngine.decide(profile, snapshot)
                    val decisionMs = (System.nanoTime() - decisionStarted) / 1_000_000
                    val commands = decision.commands

                    var lastCommand = lastMetrics.lastCommand
                    commandExecutor.execute(
                        commands = commands,
                        profile = profile,
                        screenSize = frame.interactionSize,
                        onCommand = { lastCommand = it },
                    )

                    lastMetrics = AutomationMetrics(
                        frameAgeMs = ((System.nanoTime() - frame.capturedAtNanos) / 1_000_000).coerceAtLeast(0),
                        recognitionMs = recognitionMs,
                        decisionMs = decisionMs,
                        lastCommand = lastCommand,
                    )

                    val failedRegions = observations.count { it.errorMessage != null }
                    mutableStatus.value = AutomationSessionStatus(
                        phase = SessionPhase.RUNNING,
                        profileId = profile.id,
                        profileName = profile.name,
                        message = when {
                            decision.error != null -> "Script error: ${decision.error}"
                            failedRegions > 0 -> "Observed with $failedRegions recognition error(s); no unsafe fallback"
                            commands.isEmpty() -> "Observed; no action"
                            else -> "Executed ${commands.size} command(s)"
                        },
                        snapshot = snapshot,
                        metrics = lastMetrics,
                    )

                    if (commands.isNotEmpty() && profile.settings.postActionCooldownMs > 0) {
                        delay(profile.settings.postActionCooldownMs)
                    }
                } finally {
                    recycle(frame)
                }
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            mutableStatus.value = mutableStatus.value.copy(
                phase = SessionPhase.STOPPED,
                profileId = profile.id,
                profileName = profile.name,
                message = "Stopped",
            )
            throw cancellation
        } catch (throwable: Throwable) {
            logger.error("Automation session failed", throwable)
            mutableStatus.value = mutableStatus.value.copy(
                phase = SessionPhase.ERROR,
                profileId = profile.id,
                profileName = profile.name,
                message = throwable.message ?: throwable::class.java.simpleName,
            )
        } finally {
            pauseController.clear()
        }
    }

    private fun recycle(frame: ScreenFrame) {
        if (!frame.bitmap.isRecycled) frame.bitmap.recycle()
    }
}

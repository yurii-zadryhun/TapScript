package dev.tapscript.engine.core.action

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.PixelSize
import dev.tapscript.engine.api.model.VisualTargetGeometry
import dev.tapscript.engine.api.model.VisualTone
import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.GestureDispatcher
import dev.tapscript.engine.api.ports.RuntimeVisualPresenter
import kotlinx.coroutines.delay

class CommandExecutor(
    private val gestureDispatcher: GestureDispatcher,
    private val actionResolver: ActionResolver,
    private val logger: AutomationLogger,
    private val randomizer: CommandRandomizer = CommandRandomizer(),
    private val visualPresenter: RuntimeVisualPresenter = NoOpRuntimeVisualPresenter,
    private val visualTargetResolver: VisualTargetResolver = VisualTargetResolver(),
) {
    suspend fun execute(
        commands: List<AutomationCommand>,
        profile: AutomationProfile,
        screenSize: PixelSize,
        onCommand: (String) -> Unit = {},
    ) {
        for (command in commands) {
            when (command) {
                is AutomationCommand.Tap -> executeTap(command.targetId, profile, screenSize, onCommand)
                is AutomationCommand.RandomTap -> executeRandomTap(command, profile, screenSize, onCommand)
                is AutomationCommand.Swipe -> executeSwipe(command.targetId, profile, screenSize, onCommand)
                is AutomationCommand.RandomSwipe -> executeRandomSwipe(command, profile, screenSize, onCommand)
                is AutomationCommand.Wait -> executeWait(command.durationMs, onCommand)
                is AutomationCommand.RandomWait -> executeRandomWait(command, onCommand)
                is AutomationCommand.Log -> {
                    onCommand("log")
                    logger.info("[script] ${command.message}")
                }
                is AutomationCommand.Highlight -> executeHighlight(command, profile, onCommand)
                is AutomationCommand.ShowInfo -> executeShowInfo(command, onCommand)
                is AutomationCommand.ClearVisual -> executeClearVisual(command.key, onCommand)
                AutomationCommand.ClearVisuals -> executeClearVisuals(onCommand)
            }
        }
    }

    suspend fun clearVisuals() {
        visualPresenter.clearAll()
            .onFailure { logger.error("Could not clear runtime visuals", it) }
    }

    private suspend fun executeTap(
        targetId: String,
        profile: AutomationProfile,
        screenSize: PixelSize,
        onCommand: (String) -> Unit,
    ) {
        val description = "tap:$targetId"
        onCommand(description)
        logger.info("Executing $description")
        val point = actionResolver.resolveTap(profile, targetId, screenSize).getOrThrow()
        gestureDispatcher.tap(point).getOrThrow()
    }

    private suspend fun executeRandomTap(
        command: AutomationCommand.RandomTap,
        profile: AutomationProfile,
        screenSize: PixelSize,
        onCommand: (String) -> Unit,
    ) {
        val radius = command.radiusPx.coerceIn(0, CommandRandomizer.MAX_POSITION_JITTER_PX)
        val point = actionResolver.resolveTap(profile, command.targetId, screenSize).getOrThrow()
        val randomized = randomizer.jitterPoint(point, radius, screenSize)
        val description = "tapRandom:${command.targetId}:±${radius}px"
        onCommand(description)
        logger.info("Executing $description at ${randomized.x},${randomized.y}")
        gestureDispatcher.tap(randomized).getOrThrow()
    }

    private suspend fun executeSwipe(
        targetId: String,
        profile: AutomationProfile,
        screenSize: PixelSize,
        onCommand: (String) -> Unit,
    ) {
        val description = "swipe:$targetId"
        onCommand(description)
        logger.info("Executing $description")
        val swipe = actionResolver.resolveSwipe(profile, targetId, screenSize).getOrThrow()
        gestureDispatcher.swipe(swipe.start, swipe.end, swipe.durationMs).getOrThrow()
    }

    private suspend fun executeRandomSwipe(
        command: AutomationCommand.RandomSwipe,
        profile: AutomationProfile,
        screenSize: PixelSize,
        onCommand: (String) -> Unit,
    ) {
        val radius = command.radiusPx.coerceIn(0, CommandRandomizer.MAX_POSITION_JITTER_PX)
        val durationJitter = command.durationJitterMs.coerceIn(0, CommandRandomizer.MAX_WAIT_MS)
        val swipe = actionResolver.resolveSwipe(profile, command.targetId, screenSize).getOrThrow()
        val start = randomizer.jitterPoint(swipe.start, radius, screenSize)
        val end = randomizer.jitterPoint(swipe.end, radius, screenSize)
        val duration = randomizer.durationAround(swipe.durationMs, durationJitter)
        val description = "swipeRandom:${command.targetId}:±${radius}px:±${durationJitter}ms"
        onCommand(description)
        logger.info("Executing $description for ${duration}ms")
        gestureDispatcher.swipe(start, end, duration).getOrThrow()
    }

    private suspend fun executeWait(durationMs: Long, onCommand: (String) -> Unit) {
        val safeDuration = durationMs.coerceIn(0, CommandRandomizer.MAX_WAIT_MS)
        val description = "wait:${safeDuration}ms"
        onCommand(description)
        logger.debug("Executing $description")
        delay(safeDuration)
    }

    private suspend fun executeRandomWait(
        command: AutomationCommand.RandomWait,
        onCommand: (String) -> Unit,
    ) {
        val duration = randomizer.durationBetween(command.minDurationMs, command.maxDurationMs)
        val description = "waitRandom:${command.minDurationMs}-${command.maxDurationMs}ms -> ${duration}ms"
        onCommand(description)
        logger.debug("Executing $description")
        delay(duration)
    }

    private suspend fun executeHighlight(
        command: AutomationCommand.Highlight,
        profile: AutomationProfile,
        onCommand: (String) -> Unit,
    ) {
        val description = "highlight:${command.key}:${command.targetId}"
        onCommand(description)

        val target = visualTargetResolver.resolve(profile, command.targetId)
            .getOrElse { throwable ->
                logger.error("Could not resolve $description", throwable)
                return
            }

        visualPresenter.showHighlight(
            key = command.key,
            target = target,
            label = command.label,
            tone = command.tone,
        ).onFailure { logger.error("Could not show $description", it) }
    }

    private suspend fun executeShowInfo(
        command: AutomationCommand.ShowInfo,
        onCommand: (String) -> Unit,
    ) {
        val description = "showInfo:${command.key}"
        onCommand(description)
        visualPresenter.showInfo(
            key = command.key,
            title = command.title,
            body = command.body,
            tone = command.tone,
        ).onFailure { logger.error("Could not show $description", it) }
    }

    private suspend fun executeClearVisual(key: String, onCommand: (String) -> Unit) {
        val description = "clearVisual:$key"
        onCommand(description)
        visualPresenter.clear(key)
            .onFailure { logger.error("Could not execute $description", it) }
    }

    private suspend fun executeClearVisuals(onCommand: (String) -> Unit) {
        val description = "clearVisuals"
        onCommand(description)
        clearVisuals()
    }

    private object NoOpRuntimeVisualPresenter : RuntimeVisualPresenter {
        override suspend fun showHighlight(
            key: String,
            target: VisualTargetGeometry,
            label: String,
            tone: VisualTone,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun showInfo(
            key: String,
            title: String,
            body: String,
            tone: VisualTone,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun clear(key: String): Result<Unit> = Result.success(Unit)

        override suspend fun clearAll(): Result<Unit> = Result.success(Unit)
    }
}
